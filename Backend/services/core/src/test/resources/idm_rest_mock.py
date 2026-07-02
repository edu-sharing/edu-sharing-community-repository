#!/usr/bin/env python3
"""
Mock-Implementierung des logineo-idm-rest-Service (siehe openapi.yaml).

Implementiert einzig den Endpunkt:

    POST /api/v1/authenticate
        - Basic Auth des technischen Benutzers (sonst 401)
        - JSON-Body { "principalName": "...", "password": "..." }
        - 204  -> Zugangsdaten gueltig
        - 422  -> Principal-Authentifizierung fehlgeschlagen
                  (falsches Passwort / gesperrt / unbekannt - bewusst nicht unterscheidbar)
        - 400  -> fehlerhafter Request
        - 401  -> Basic Auth des technischen Benutzers fehlgeschlagen

Keine externen Abhaengigkeiten - nur Python-3-Standardbibliothek.

Konfiguration ueber Environment-Variablen:
    IDM_MOCK_PORT     Port (Default 8099)
    IDM_TECH_USER     technischer Benutzer fuer Basic Auth (Default "edu-sharing")
    IDM_TECH_PASSWORD Passwort des technischen Benutzers   (Default "secret")
    IDM_USERS         gueltige End-Principals als
                      "principal1:pw1,principal2:pw2"
                      (Default "muster.m@demo.logineo.de:test123")

Start:
    python3 idm_rest_mock.py

Beispiele:
    # erfolgreiche Authentifizierung -> 204
    curl -i -u edu-sharing:secret -H 'Content-Type: application/json' \
         -d '{"principalName":"muster.m@demo.logineo.de","password":"test123"}' \
         http://localhost:8099/api/v1/authenticate

    # falsches Passwort -> 422
    curl -i -u edu-sharing:secret -H 'Content-Type: application/json' \
         -d '{"principalName":"muster.m@demo.logineo.de","password":"wrong"}' \
         http://localhost:8099/api/v1/authenticate

    # falscher technischer Benutzer -> 401
    curl -i -u edu-sharing:falsch -H 'Content-Type: application/json' \
         -d '{"principalName":"muster.m@demo.logineo.de","password":"test123"}' \
         http://localhost:8099/api/v1/authenticate
"""
import base64
import json
import logging
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("IDM_MOCK_PORT", "8099"))
TECH_USER = os.environ.get("IDM_TECH_USER", "edu-sharing")
TECH_PASSWORD = os.environ.get("IDM_TECH_PASSWORD", "secret")


def _parse_users(raw):
    users = {}
    for entry in raw.split(","):
        entry = entry.strip()
        if not entry:
            continue
        # nur am ersten ':' splitten, Passwoerter duerfen ':' enthalten
        principal, _, password = entry.partition(":")
        users[principal.strip()] = password
    return users


USERS = _parse_users(os.environ.get("IDM_USERS", "zach@oncase.com.br:test123"))

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("idm-rest-mock")


class Handler(BaseHTTPRequestHandler):
    server_version = "logineo-idm-rest-mock/1.0"

    # ---- Helpers -------------------------------------------------------

    def _send_error_body(self, status, message):
        body = json.dumps({"message": message}).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_no_content(self):
        self.send_response(204)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def _check_basic_auth(self):
        """Return True if the technical user's basic auth is valid."""
        header = self.headers.get("Authorization", "")
        if not header.lower().startswith("basic "):
            return False
        try:
            decoded = base64.b64decode(header[6:].strip()).decode("utf-8")
        except Exception:
            return False
        user, _, password = decoded.partition(":")
        return user == TECH_USER and password == TECH_PASSWORD

    def _read_body(self):
        """Read the request body, supporting both Content-Length and chunked transfer-encoding."""
        te = (self.headers.get("Transfer-Encoding", "") or "").lower()
        if "chunked" in te:
            chunks = []
            while True:
                size_line = self.rfile.readline().strip()
                # chunk size may carry extensions after ';'
                size = int(size_line.split(b";")[0] or b"0", 16)
                if size == 0:
                    self.rfile.readline()  # trailing CRLF after the last chunk
                    break
                chunks.append(self.rfile.read(size))
                self.rfile.readline()  # CRLF after each chunk
            return b"".join(chunks)
        length = int(self.headers.get("Content-Length", "0") or "0")
        return self.rfile.read(length) if length else b""

    def _log_request(self, raw):
        log.info("--> %s %s", self.command, self.path)
        for key, value in self.headers.items():
            log.info("    %s: %s", key, value)
        log.info("    raw body (%d bytes): %r", len(raw), raw)

    # ---- Routing -------------------------------------------------------

    def do_POST(self):
        # read the body first so we can always log exactly what arrived
        raw = self._read_body()
        self._log_request(raw)

        if self.path.rstrip("/") != "/api/v1/authenticate":
            self._send_error_body(404, "Not found.")
            return

        # 1) Basic Auth des technischen Benutzers
        if not self._check_basic_auth():
            log.info("401 - basic auth of technical user failed")
            self.send_response(401)
            self.send_header("WWW-Authenticate", 'Basic realm="logineo-idm-rest"')
            self._finish_error_after_headers("Basic Auth fehlgeschlagen.")
            return

        # 2) Request-Body validieren
        try:
            payload = json.loads(raw.decode("utf-8")) if raw else {}
        except (ValueError, UnicodeDecodeError):
            log.info("400 - malformed json body")
            self._send_error_body(400, "Ungueltiger Request (kein gueltiges JSON).")
            return

        principal = payload.get("principalName")
        password = payload.get("password")
        if not isinstance(principal, str) or not isinstance(password, str) or not principal:
            log.info("400 - missing principalName/password")
            self._send_error_body(400, "Ungueltiger Request (fehlende oder fehlerhafte Felder).")
            return

        # 3) Principal-Authentifizierung
        if USERS.get(principal) == password:
            log.info("204 - authenticated principal=%s", principal)
            self._send_no_content()
        else:
            # einheitlich 422 fuer falsches Passwort / gesperrt / unbekannt
            log.info("422 - authentication failed principal=%s", principal)
            self._send_error_body(422, "Benutzername oder Passwort falsch.")

    def _finish_error_after_headers(self, message):
        body = json.dumps({"message": message}).encode("utf-8")
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    # quieter default logging (we log ourselves)
    def log_message(self, fmt, *args):
        return


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    log.info("logineo-idm-rest mock listening on http://0.0.0.0:%d", PORT)
    log.info("technical user: %s / %s", TECH_USER, TECH_PASSWORD)
    log.info("valid principals: %s", ", ".join(sorted(USERS)) or "(none)")
    log.info("POST /api/v1/authenticate")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log.info("shutting down")
        server.shutdown()


if __name__ == "__main__":
    main()
