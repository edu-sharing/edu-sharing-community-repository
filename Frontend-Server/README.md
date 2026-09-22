# edu-sharing Frontend-Server

Node.js / Fastify server that delivers the edu-sharing Angular application and
proxies everything backend-related to the Tomcat repository service. It is the
step-by-step replacement for
`Backend/services/core/src/main/java/org/edu_sharing/repository/server/NgServlet.java`.

**No Angular SSR** — the browser build is served as static files, the SPA shell
is enriched at the string level exactly like `NgServlet` does it today.

📖 **Planning documents: [`docs/`](docs/)** — start with
[`docs/00-overview.md`](docs/00-overview.md).

## Requirements

Node 24 (`.nvmrc`). The repository's Angular build still pins Node 22 via the
root pom's `node.version`; these are deliberately separate toolchains.

## Local development

```bash
nvm use                 # node 24
npm ci
cp .env.example .env    # point BACKEND_URL at a running edu-sharing
(cd ../Frontend && npm run build)   # produces ../Frontend/dist

npm run dev             # http://localhost:4200/edu-sharing/
```

`DEV=true` enables pretty logs and re-reads `index.html` whenever the Angular
build writes a new one, so a `npm run build` in `Frontend/` is picked up without
a restart.

## Commands

| Command | Purpose |
|---------|---------|
| `npm run dev` | run from TypeScript sources with `--watch` |
| `npm run build` | compile to `build/` |
| `npm start` | run the compiled server |
| `npm test` | unit + routing tests (fake backend, no network) |
| `npm run check:types` | type check only |

## Configuration

Environment only — see the table in
[`docs/01-architecture.md`](docs/01-architecture.md#configuration). The important
ones:

| Variable | Default | |
|----------|---------|---|
| `PORT` | `8080` | |
| `CONTEXT_PATH` | `/edu-sharing` | must match the Angular `baseHref` |
| `BACKEND_URL` | `http://repository-service:8080` | Tomcat, **without** context path |
| `DIST_DIR` | `./dist` | Angular build output |

## How routing works

1. Backend prefix (`/rest`, `/eduservlet`, …) → proxied to Tomcat
2. `/`, `/index.html`, `/components/*`, `/v/*` → `index.html` with a fresh CSP nonce
3. Existing file in `dist/` → static
4. Anything else → proxied to Tomcat with a `warn` log *(transitional)*

The authoritative list lives in
[`src/routes/backend-paths.ts`](src/routes/backend-paths.ts) and is derived from
`Backend/services/webapp/src/main/webapp/WEB-INF/web.xml`. Details and the known
behavioural deltas: [`docs/02-routing.md`](docs/02-routing.md).

## Tracing

`X-B3-*`, `X-OT-*`, `X-Request-Id` and `X-Client-Trace-Id` are forwarded
unchanged and asserted by a test — the backend reads them into its Log4j
`ThreadContext` and would lose the trace at this hop otherwise. See
[`docs/05-tracing.md`](docs/05-tracing.md).
