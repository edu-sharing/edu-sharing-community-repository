#!/usr/bin/env bash
#
# webdav-macos-mimetype-repro.sh
#
# Faithfully replays the macOS Finder WebDAV upload sequence captured in
# 260623.pcapng (a .jar rejected by the mimetype verification policy), using curl.
# This reproduces the "empty orphan node" bug WITHOUT needing a real Mac.
#
# The real macOS sequence for the main file (from the trace) is:
#   PROPFIND name            -> 404   (probe)
#   PUT name  Content-Length:0 -> 201  (creates an EMPTY node, committed in its own txn)
#   LOCK name                -> 200   (lock token)
#   UNLOCK name              -> 204
#   PROPFIND name            -> 207   (exists, len 0)
#   LOCK name                -> 200   (same token)
#   GET  name                -> 200   (len 0)
#   PUT name  X-Expected-Entity-Length:<n>  If:(<token>)  -> 500  (mimetype REJECTS)
#   UNLOCK name              -> 204
# Result on stock code: the empty node from the 0-byte PUT survives -> orphan.
# With the Edu_SharingPutMethod fix: the node is removed after the failed PUT.
#
# Usage:
#   ./webdav-macos-mimetype-repro.sh [-u user:pass] [-b appbase] [-d folder] [-n name] [-F file] [--appledouble]
# Defaults match the captured session:
#   -b http://repository.127.0.0.1.nip.io:8100/edu-sharing   -u admin:admin   -d drtest
#
set -u

AUTH="admin:admin"
APP_BASE="http://repository.127.0.0.1.nip.io:8100/edu-sharing"
FOLDER="drtest"
NAME="mimetype-repro.jar"
FILE=""               # if empty, a small blocked .jar is generated
DO_APPLEDOUBLE=0
UA="WebDAVFS/3.0.0 (03008000) Darwin/21.6.0 (x86_64)"

while [ $# -gt 0 ]; do
  case "$1" in
    -u) AUTH="$2"; shift 2;;
    -b) APP_BASE="$2"; shift 2;;
    -d) FOLDER="$2"; shift 2;;
    -n) NAME="$2"; shift 2;;
    -F) FILE="$2"; shift 2;;
    --appledouble) DO_APPLEDOUBLE=1; shift;;
    -h|--help) sed -n '2,33p' "$0"; exit 0;;
    *) echo "unknown arg: $1" >&2; exit 2;;
  esac
done

APP_BASE="${APP_BASE%/}"
WEBDAV="$APP_BASE/webdav"
FOLDER="${FOLDER#/}"; FOLDER="${FOLDER%/}"
CURL=(curl -sS -k -u "$AUTH" -A "$UA")

# ---- payload: a real ZIP/.jar so the mimetype filter blocks it --------------
# The generated payload is kept on disk (not auto-deleted) so you can inspect/reuse it.
if [ -z "$FILE" ]; then
  FILE="$(mktemp /tmp/mimetype-repro.XXXXXX.jar)"
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$FILE" <<'PY'
import zipfile, sys
with zipfile.ZipFile(sys.argv[1], "w") as z:
    z.writestr("README.txt", "this .jar should be rejected by the mimetype verification")
PY
  else
    # minimal: PK zip magic + filler so it is detected as a zip/jar
    { printf 'PK\003\004'; head -c 512 /dev/urandom; } > "$FILE"
  fi
fi
if [ ! -s "$FILE" ]; then
  echo "ERROR: payload file is empty or missing: $FILE" >&2
  exit 1
fi
SIZE="$(wc -c < "$FILE" | tr -d ' ')"

echo "=================================================================="
echo " macOS WebDAV mimetype-rejection repro (from 260623.pcapng)"
echo " payload: $FILE  ($SIZE bytes)   [kept on disk for inspection]"
command -v file >/dev/null 2>&1 && echo "          $(file -b "$FILE")"
echo " target : $WEBDAV/$FOLDER/$NAME"
echo "=================================================================="

# helper: PROPFIND probe -> prints existence + getcontentlength
probe() {
  local url="$1" body code len
  body="$("${CURL[@]}" -X PROPFIND -H "Depth: 0" -w $'\n__HTTP__%{http_code}' "$url" 2>/dev/null)"
  code="${body##*__HTTP__}"; body="${body%__HTTP__*}"
  case "$code" in
    207|200)
      len="$(printf '%s' "$body" | grep -oiE 'getcontentlength[^>]*>[0-9]+' | grep -oE '[0-9]+$' | head -n1)"
      echo "    STATE: EXISTS (PROPFIND $code, getcontentlength=${len:-?})";;
    404) echo "    STATE: ABSENT (404)";;
    *)   echo "    STATE: PROPFIND $code";;
  esac
}

# replays the macOS sequence for one resource; $1 = full URL
replay() {
  local URL="$1" code token

  echo; echo "##### resource: $URL"

  echo "-- PROPFIND (probe)";                 "${CURL[@]}" -X PROPFIND -H "Depth: 0" -o /dev/null -w '   PROPFIND -> %{http_code}\n' "$URL"

  echo "-- PUT 0-byte placeholder";
  code=$("${CURL[@]}" -X PUT -H "Content-Length: 0" -o /dev/null -w '%{http_code}' "$URL")
  echo "   PUT(0) -> $code"; probe "$URL"      # expect 201 then EXISTS len 0

  echo "-- LOCK / UNLOCK";
  local LOCKBODY='<?xml version="1.0" encoding="utf-8"?><D:lockinfo xmlns:D="DAV:"><D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype><D:owner><D:href>macos-repro</D:href></D:owner></D:lockinfo>'
  token=$("${CURL[@]}" -X LOCK -H 'Depth: 0' -H 'Timeout: Second-600' -H 'Content-Type: text/xml; charset="utf-8"' --data "$LOCKBODY" "$URL" \
            | grep -oiE 'opaquelocktoken:[A-Za-z0-9:_.-]+' | head -n1)
  echo "   LOCK -> token ${token:-<none>}"
  "${CURL[@]}" -X UNLOCK -H "Lock-Token: <$token>" -o /dev/null -w '   UNLOCK -> %{http_code}\n' "$URL"

  echo "-- LOCK again + GET (macOS re-locks before content)";
  token=$("${CURL[@]}" -X LOCK -H 'Depth: 0' -H 'Timeout: Second-600' -H 'Content-Type: text/xml; charset="utf-8"' --data "$LOCKBODY" "$URL" \
            | grep -oiE 'opaquelocktoken:[A-Za-z0-9:_.-]+' | head -n1)
  "${CURL[@]}" -X GET -o /dev/null -w '   GET -> %{http_code}\n' "$URL"

  echo "-- PUT real content ($SIZE bytes via -T, X-Expected-Entity-Length + If:lock)  [EXPECT 500]";
  local resp body
  resp=$("${CURL[@]}" -T "$FILE" -H "X-Expected-Entity-Length: $SIZE" -H "If: (<$token>)" \
           -w $'\n__HTTP__%{http_code}' "$URL")
  code="${resp##*__HTTP__}"; body="${resp%__HTTP__*}"
  echo "   PUT(content) -> $code   (4xx/5xx = mimetype rejected = good)"
  probe "$URL"

  echo "-- UNLOCK";
  "${CURL[@]}" -X UNLOCK -H "Lock-Token: <$token>" -o /dev/null -w '   UNLOCK -> %{http_code}\n' "$URL"
}

# main file
replay "$WEBDAV/$FOLDER/$NAME"

# AppleDouble companion (macOS also creates ._<name>; its content PUTs are rejected too)
if [ "$DO_APPLEDOUBLE" = 1 ]; then
  replay "$WEBDAV/$FOLDER/._$NAME"
fi

# ---- verdict ----------------------------------------------------------------
echo; echo "================================ VERDICT ================================="
verdict_for() {
  local url="$1" label="$2" body code len
  body="$("${CURL[@]}" -X PROPFIND -H "Depth: 0" -w $'\n__HTTP__%{http_code}' "$url" 2>/dev/null)"
  code="${body##*__HTTP__}"; body="${body%__HTTP__*}"
  len="$(printf '%s' "$body" | grep -oiE 'getcontentlength[^>]*>[0-9]+' | grep -oE '[0-9]+$' | head -n1)"
  case "$code" in
    404) echo "  ✅ $label: ABSENT (404) - cleaned up (fix active)";;
    207|200)
      if [ "${len:-0}" = "0" ] || [ -z "$len" ]; then
        echo "  ❌ $label: EMPTY orphan remains (len=${len:-?}) - the bug"
      else
        echo "  ⚠  $label: remains WITH content (len=$len) - not rejected?"
      fi;;
    *) echo "  ⚠  $label: PROPFIND $code";;
  esac
}
verdict_for "$WEBDAV/$FOLDER/$NAME" "$NAME"
[ "$DO_APPLEDOUBLE" = 1 ] && verdict_for "$WEBDAV/$FOLDER/._$NAME" "._$NAME"
echo "=========================================================================="
echo "tip: list the whole folder with"
echo "  curl -s -u $AUTH -X PROPFIND -H 'Depth: 1' $WEBDAV/$FOLDER/ | grep -oE '<D:href>[^<]+'"
