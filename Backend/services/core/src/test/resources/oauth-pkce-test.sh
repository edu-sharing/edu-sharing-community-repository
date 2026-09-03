#!/usr/bin/env bash
#
# Faehrt den kompletten Authorization Code Flow mit PKCE als *public client* gegen
# den edu-sharing oauth2server - also genau das, was ein Browser-Plugin tut, nur
# ohne Browser-Plugin.
#
#   1. erzeugt code_verifier und code_challenge (S256)
#   2. baut die Authorize-URL, die du im Browser oeffnest und wieder zurueckgibst
#   3. tauscht den Code gegen Tokens - ohne client_secret, nur client_id + code_verifier
#   4. zeigt die Claims des Access Tokens
#   5. loest, falls vorhanden, das Refresh Token ein und prueft die Rotation
#   6. ruft die edu-sharing API und zeigt, als wer der Aufruf ankommt
#
# Die Default-Redirect-URI ist bewusst eine chromiumapp.org-Adresse: der Browser
# kann sie nicht laden, in der Adresszeile steht danach aber der Code, und die
# Anfrage laeuft dabei ueber das redirectUriPatterns-Matching - also ueber
# denselben Pfad wie das echte Plugin.
#
# Passende Client-Konfiguration in edu-sharing.conf:
#
#   security.authentication.oauth2 {
#       enabled = true
#       clients = [
#           {
#               clientId: "browser-plugin"
#               clientAuthenticationMethod: "none"
#               authorizationGrantTypes: ["authorization_code", "refresh_token"]
#               redirectUriPatterns: ["https://*.chromiumapp.org/"]
#               scopes: ["profile"]
#               requireConsent: true
#               forceRefreshToken: true
#           }
#       ]
#   }
#
# Aufruf:
#   REPO_BASE=http://repository.127.0.0.1.nip.io:8100/edu-sharing ./oauth-pkce-test.sh
#
set -uo pipefail

REPO_BASE="${REPO_BASE:-http://localhost:8080/edu-sharing}"
CLIENT_ID="${CLIENT_ID:-browser-plugin}"
REDIRECT_URI="${REDIRECT_URI:-https://abcdefghijklmnopabcdefghijklmnop.chromiumapp.org/}"
SCOPE="${SCOPE:-profile}"

REPO_BASE="${REPO_BASE%/}"
AUTHORIZE_URL="$REPO_BASE/oauth2server/authorize"
TOKEN_URL="$REPO_BASE/oauth2server/token"
API_URL="$REPO_BASE/rest/iam/v1/people/-home-/-me-"

command -v curl >/dev/null || { echo "fehlt: curl"; exit 1; }
command -v python3 >/dev/null || { echo "fehlt: python3"; exit 1; }

hr() { printf '%s\n' "------------------------------------------------------------"; }

urlencode() { python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"; }

json_get() { python3 -c '
import json,sys
try:
    print(json.load(sys.stdin).get(sys.argv[1], "") or "")
except Exception:
    print("")
' "$1"; }

show_json() { python3 -m json.tool 2>/dev/null || cat; }

show_claims() { python3 -c '
import base64, json, sys, datetime
token = sys.stdin.read().strip()
parts = token.split(".")
if len(parts) != 3:
    print("  (kein jwt)"); sys.exit(0)
part = parts[1] + "=" * (-len(parts[1]) % 4)
payload = json.loads(base64.urlsafe_b64decode(part))
for name in ("iss", "sub", "aud", "scope", "client_id"):
    if name in payload:
        print("  %-10s %s" % (name + ":", payload[name]))
exp = payload.get("exp")
if exp:
    left = exp - int(datetime.datetime.now().timestamp())
    print("  %-10s %s (noch %ds)" % ("exp:", datetime.datetime.fromtimestamp(exp).isoformat(), left))
'; }

# --- 1. pkce ---------------------------------------------------------------
verifier="$(python3 -c 'import base64,os; print(base64.urlsafe_b64encode(os.urandom(32)).decode().rstrip("="))')"
challenge="$(python3 -c '
import base64, hashlib, sys
print(base64.urlsafe_b64encode(hashlib.sha256(sys.argv[1].encode()).digest()).decode().rstrip("="))
' "$verifier")"
state="$(python3 -c 'import base64,os; print(base64.urlsafe_b64encode(os.urandom(12)).decode().rstrip("="))')"

hr
echo "1) pkce"
hr
echo "  code_verifier:  $verifier"
echo "  code_challenge: $challenge (S256)"

# --- 2. authorize ----------------------------------------------------------
authorize="$AUTHORIZE_URL?response_type=code"
authorize+="&client_id=$(urlencode "$CLIENT_ID")"
authorize+="&redirect_uri=$(urlencode "$REDIRECT_URI")"
authorize+="&scope=$(urlencode "$SCOPE")"
authorize+="&state=$state"
authorize+="&code_challenge=$challenge&code_challenge_method=S256"

echo
hr
echo "2) im browser oeffnen, anmelden, ggf. zustimmen"
hr
echo
echo "$authorize"
echo
echo "Die Seite danach kann nicht laden - das ist so gewollt. Kopiere die URL"
echo "aus der Adresszeile hierher (die mit ?code=...):"
printf '> '
read -r redirected

code="$(python3 -c '
import sys, urllib.parse
q = urllib.parse.parse_qs(urllib.parse.urlparse(sys.argv[1].strip()).query)
print((q.get("code") or [""])[0])
' "$redirected")"
returned_state="$(python3 -c '
import sys, urllib.parse
q = urllib.parse.parse_qs(urllib.parse.urlparse(sys.argv[1].strip()).query)
print((q.get("state") or [""])[0])
' "$redirected")"

if [ -z "$code" ]; then
    echo
    echo "kein code in der url gefunden. steht dort stattdessen error=...?"
    echo "  invalid_request + redirect_uri  -> redirect_uri passt zu keinem"
    echo "                                     redirectUris/redirectUriPatterns eintrag"
    echo "  invalid_scope                   -> scope nicht registriert, oder openid"
    echo "                                     angefragt obwohl oidc aus ist"
    echo "  invalid_request + code_challenge -> requireProofKey an, aber kein challenge"
    exit 1
fi
[ "$returned_state" = "$state" ] || { echo "state stimmt nicht ueberein - abbruch"; exit 1; }
echo "  code erhalten, state ok"

# --- 3. token --------------------------------------------------------------
echo
hr
echo "3) code einloesen (ohne secret, nur client_id + code_verifier)"
hr

response="$(curl -sS -X POST "$TOKEN_URL" \
    --data-urlencode "grant_type=authorization_code" \
    --data-urlencode "client_id=$CLIENT_ID" \
    --data-urlencode "code=$code" \
    --data-urlencode "redirect_uri=$REDIRECT_URI" \
    --data-urlencode "code_verifier=$verifier")" || exit 1

access_token="$(printf '%s' "$response" | json_get access_token)"
refresh_token="$(printf '%s' "$response" | json_get refresh_token)"

if [ -z "$access_token" ]; then
    echo "kein access_token. antwort:"
    printf '%s\n' "$response" | show_json
    echo
    echo "kommt hier html statt json, greift der login-entry-point - dann ist die"
    echo "client-authentifizierung fehlgeschlagen, nicht der code."
    exit 1
fi
echo "  access_token  laenge ${#access_token}"
if [ -n "$refresh_token" ]; then
    echo "  refresh_token laenge ${#refresh_token}"
else
    echo "  refresh_token FEHLT - forceRefreshToken steht nicht auf true, oder"
    echo "                refresh_token fehlt in authorizationGrantTypes"
fi

echo
echo "  claims:"
printf '%s' "$access_token" | show_claims

# --- 4. refresh ------------------------------------------------------------
if [ -n "$refresh_token" ]; then
    echo
    hr
    echo "4) refresh (ohne secret, nur client_id + refresh_token)"
    hr

    refresh_response="$(curl -sS -X POST "$TOKEN_URL" \
        --data-urlencode "grant_type=refresh_token" \
        --data-urlencode "client_id=$CLIENT_ID" \
        --data-urlencode "refresh_token=$refresh_token")" || exit 1

    new_access="$(printf '%s' "$refresh_response" | json_get access_token)"
    new_refresh="$(printf '%s' "$refresh_response" | json_get refresh_token)"

    if [ -z "$new_access" ]; then
        echo "  fehlgeschlagen. antwort:"
        printf '%s\n' "$refresh_response" | show_json
        echo
        echo "  html statt json heisst: der public client wurde am token endpoint"
        echo "  nicht authentifiziert."
    else
        echo "  neues access_token  laenge ${#new_access}"
        if [ -z "$new_refresh" ]; then
            echo "  kein neues refresh_token - rotation ist aus, das alte gilt weiter"
        elif [ "$new_refresh" = "$refresh_token" ]; then
            echo "  refresh_token unveraendert - rotation ist aus"
        else
            echo "  refresh_token wurde rotiert - das alte ist ab jetzt ungueltig,"
            echo "  ein client muss das neue speichern"
        fi
        access_token="$new_access"
    fi
fi

# --- 5. api ----------------------------------------------------------------
echo
hr
echo "5) api-aufruf: $API_URL"
hr

body_file="$(mktemp)"
status="$(curl -sS -o "$body_file" -w '%{http_code}' \
    -H "Authorization: Bearer $access_token" -H "Accept: application/json" "$API_URL")"
body="$(cat "$body_file")"; rm -f "$body_file"

echo "  http status: $status"
if [ "$status" = "200" ]; then
    printf '%s' "$body" | python3 -c '
import json,sys
p = json.load(sys.stdin).get("person", {})
print("  angekommen als: %s" % p.get("authorityName"))
' 2>/dev/null || printf '%s\n' "$body"
else
    printf '%s\n' "$body" | show_json
fi

[ "$status" = "200" ]
