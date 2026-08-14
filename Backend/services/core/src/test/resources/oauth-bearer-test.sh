#!/usr/bin/env bash
#
# Testet den Weg "client credentials bei Keycloak -> bearer token gegen edu-sharing".
#
#   1. holt ein access token per client_credentials beim idp
#   2. dekodiert es und zeigt die claims, auf die das repository prueft
#   3. ruft damit /rest/iam/v1/people/-home-/-me- auf und zeigt, als welcher
#      edu-sharing nutzer der aufruf ankommt
#
# Schritt 2 lohnt sich einzeln: was tatsaechlich in aud und azp steht, haengt an
# realm-einstellungen und zugewiesenen scopes - dort scheitern solche
# integrationen erfahrungsgemaess zuerst.
#
# Aufruf:
#   ISSUER=https://keycloak.example.org/realms/edu \
#   CLIENT_ID=provisioning-app CLIENT_SECRET=... \
#   REPO_BASE=http://localhost:8080/edu-sharing \
#   ./oauth-bearer-test.sh
#
set -uo pipefail

ISSUER="${ISSUER:-https://keycloak.example.org/realms/edu}"
CLIENT_ID="${CLIENT_ID:-provisioning-app}"
CLIENT_SECRET="${CLIENT_SECRET:-}"
# optional, nur noetig wenn der audience mapper in einem optionalen client scope liegt
SCOPE="${SCOPE:-}"
REPO_BASE="${REPO_BASE:-http://localhost:8080/edu-sharing}"
# was in der trustedIssuers-config des repositories steht - leer lassen zum ueberspringen
EXPECTED_AUDIENCE="${EXPECTED_AUDIENCE:-}"
EXPECTED_AZP="${EXPECTED_AZP:-$CLIENT_ID}"

TOKEN_URL="${ISSUER%/}/protocol/openid-connect/token"
API_URL="${REPO_BASE%/}/rest/iam/v1/people/-home-/-me-"

for tool in curl python3; do
    command -v "$tool" >/dev/null || { echo "fehlt: $tool"; exit 1; }
done
[ -n "$CLIENT_SECRET" ] || { echo "CLIENT_SECRET ist nicht gesetzt"; exit 1; }

hr() { printf '%s\n' "------------------------------------------------------------"; }

# --- 1. token holen --------------------------------------------------------
hr
echo "1) token holen: $TOKEN_URL"
hr

form=(--data-urlencode "grant_type=client_credentials"
      --data-urlencode "client_id=$CLIENT_ID"
      --data-urlencode "client_secret=$CLIENT_SECRET")
[ -n "$SCOPE" ] && form+=(--data-urlencode "scope=$SCOPE")

token_response="$(curl -sS -X POST "$TOKEN_URL" "${form[@]}")" || exit 1

access_token="$(printf '%s' "$token_response" | python3 -c '
import json,sys
try:
    print(json.load(sys.stdin).get("access_token",""))
except Exception:
    print("")
')"

if [ -z "$access_token" ]; then
    echo "kein access_token erhalten. antwort des idp:"
    printf '%s\n' "$token_response" | python3 -m json.tool 2>/dev/null || printf '%s\n' "$token_response"
    exit 1
fi
echo "ok, token laenge: ${#access_token}"

# --- 2. token anschauen ----------------------------------------------------
echo
hr
echo "2) claims des tokens"
hr

printf '%s' "$access_token" | python3 -c '
import base64, json, sys, datetime

def seg(part):
    part += "=" * (-len(part) % 4)
    return json.loads(base64.urlsafe_b64decode(part))

token = sys.stdin.read().strip()
parts = token.split(".")
if len(parts) != 3:
    print("das ist kein jwt (%d teile)" % len(parts))
    print("hinweis: keycloak liefert opake tokens, wenn im client")
    print("advanced settings die access token signatur deaktiviert ist")
    sys.exit(1)

header, payload = seg(parts[0]), seg(parts[1])
print("header: alg=%s kid=%s" % (header.get("alg"), header.get("kid")))
print()

def show(name, value, note=""):
    print("  %-18s %s%s" % (name + ":", value, note))

exp = payload.get("exp")
show("iss", payload.get("iss"), "   <- trustedIssuers[].issuerUri")
show("aud", payload.get("aud"), "   <- trustedIssuers[].audience")
show("azp", payload.get("azp"), "   <- trustedIssuers[].authorizedParty")
show("sub", payload.get("sub"), "   <- usernameClaim (default)")
show("preferred_username", payload.get("preferred_username"))
if exp:
    left = exp - int(datetime.datetime.now().timestamp())
    show("exp", "%s (noch %ds)" % (datetime.datetime.fromtimestamp(exp).isoformat(), left))
print()
print("vollstaendige payload:")
print(json.dumps(payload, indent=2, sort_keys=True))
'

# --- 3. gegen die erwartung pruefen ---------------------------------------
echo
hr
echo "3) abgleich mit der repository-config"
hr

printf '%s' "$access_token" | EXPECTED_AUDIENCE="$EXPECTED_AUDIENCE" EXPECTED_AZP="$EXPECTED_AZP" python3 -c '
import base64, json, os, sys

part = sys.stdin.read().strip().split(".")[1]
part += "=" * (-len(part) % 4)
payload = json.loads(base64.urlsafe_b64decode(part))

aud = payload.get("aud")
aud = aud if isinstance(aud, list) else ([aud] if aud else [])
azp = payload.get("azp")

problems = 0
expected_aud = os.environ.get("EXPECTED_AUDIENCE", "")
expected_azp = os.environ.get("EXPECTED_AZP", "")

if expected_aud:
    if expected_aud in aud:
        print("  ok      aud enthaelt %r" % expected_aud)
    else:
        problems += 1
        print("  FEHLER  aud ist %r, erwartet war %r" % (aud, expected_aud))
        print("          -> audience mapper im client scope fehlt oder greift nicht")
if expected_azp:
    if azp == expected_azp:
        print("  ok      azp ist %r" % azp)
    else:
        problems += 1
        print("  FEHLER  azp ist %r, erwartet war %r" % (azp, expected_azp))
if not expected_aud and not expected_azp:
    print("  (uebersprungen, weder EXPECTED_AUDIENCE noch EXPECTED_AZP gesetzt)")

sys.exit(1 if problems else 0)
'
expectation_ok=$?

# --- 4. gegen edu-sharing rufen -------------------------------------------
echo
hr
echo "4) api-aufruf: $API_URL"
hr

response="$(curl -sS -o /tmp/oauth-bearer-test-body.$$ -w '%{http_code}' \
    -H "Authorization: Bearer $access_token" \
    -H "Accept: application/json" \
    "$API_URL")"
body="$(cat "/tmp/oauth-bearer-test-body.$$")"
rm -f "/tmp/oauth-bearer-test-body.$$"

echo "http status: $response"
echo
if [ "$response" = "200" ]; then
    printf '%s' "$body" | python3 -c '
import json,sys
d = json.load(sys.stdin)
p = d.get("person", d)
print("angekommen als edu-sharing nutzer:")
print("  authorityName: %s" % p.get("authorityName"))
print("  userName:      %s" % p.get("userName"))
print("  status:        %s" % (p.get("status") or {}).get("status"))
' 2>/dev/null || printf '%s\n' "$body"
else
    printf '%s\n' "$body" | python3 -m json.tool 2>/dev/null || printf '%s\n' "$body"
    echo
    echo "bei 401: im repository-log nachsehen, die meldung unterscheidet"
    echo "  'rejected bearer token of issuer ...'   -> claim abgelehnt (aud/azp/exp/iss)"
    echo "  'could not verify bearer token ...'     -> signatur oder jwks nicht erreichbar"
    echo "  'neither is that issuer trusted ...'    -> iss steht nicht in trustedIssuers"
    echo "  eine exception aus authenticateUser     -> token ok, nutzer fehlt im repository"
fi

exit $((response == 200 ? expectation_ok : 1))
