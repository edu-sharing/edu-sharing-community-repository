# edu-sharing Frontend-Server

Node.js / Fastify server that delivers the edu-sharing Angular application and
proxies everything backend-related to the Tomcat repository service. It is the
step-by-step replacement for
`Backend/services/core/src/main/java/org/edu_sharing/repository/server/NgServlet.java`.

**No Angular SSR** — the browser build is served as static files, the SPA shell
is enriched at the string level exactly like `NgServlet` does it today.

## Requirements

Node 24 (`.nvmrc`). The repository's Angular build still pins Node 22 via the
root pom's `node.version`; these are deliberately separate toolchains.

## Local development

```bash
nvm use                 # node 24
npm ci
cp .env.example .env    # BACKEND_URL / BACKEND_INTERNAL_URL of a running edu-sharing
npm run build:frontend  # builds ../Frontend into ../Frontend/dist (only once, or after frontend changes)

npm run dev             # http://localhost:4200/edu-sharing/
```

`.env` is read from the package root by the server itself, so starting
`src/index.ts` directly - e.g. a Node.js run/debug configuration in IDEA - works
the same as `npm run dev`. Node 24 runs the TypeScript sources natively (type
stripping, no type check - that is `npm run check:types`).

`DEV=true` enables request logging and re-reads `index.html` whenever the Angular
build writes a new one, so a `npm run build` in `Frontend/` is picked up without
a restart.

## Commands

| Command | Purpose |
|---------|---------|
| `npm run dev` | run from TypeScript sources with `--watch` |
| `npm run build` | compile to `build/` |
| `npm run build:frontend` | production build of the Angular app into `../Frontend/dist` |
| `npm start` | run the compiled server |
| `npm test` | unit + routing tests (fake backend, no network) |
| `npm run check:types` | type check only |

## Configuration

| Variable | Default | |
|----------|---------|---|
| `PORT` | `8080` | |
| `CONTEXT_PATH` | `/edu-sharing` | must match the Angular `baseHref` |
| `BACKEND_URL` | `http://repository-service:8081` | Tomcat external connector, **without** context path |
| `BACKEND_INTERNAL_URL` | `http://repository-service:8080` | Tomcat internal connector, config fetches only |
| `DIST_DIR` | `./dist` | Angular build output |
| `ERROR_PAGE_URL` | – | absolute url browsers are sent to on an error instead of the Angular page, `{status}` / `{next}` replaced |
| `ERROR_PAGE_FILE` | – | html file served instead (read once at startup), `{{status}}` `{{message}}` `{{requestId}}` `{{next}}` `{{nonce}}` replaced |
| `ERROR_PAGE_STATUSES` | `502,503,504` | statuses the custom page is used for: codes, classes (`5xx`) or `all` |

Custom error pages: `src/lib/custom-error-page.ts`; Helm / Compose setup in the chart README
(`deploy/docker/helm/frontend-server/src/main/chart/README.md`).

## How routing works

1. Backend prefix (`/rest`, `/eduservlet`, …) → proxied to Tomcat
2. `/`, `/index.html`, `/components/*`, `/v/*` → `index.html` with a fresh CSP nonce
3. Existing file in `dist/` → static
4. Anything else → proxied to Tomcat with a `warn` log *(transitional)*

The authoritative list lives in
[`src/routes/backend-paths.ts`](src/routes/backend-paths.ts) and is derived from
`Backend/services/webapp/src/main/webapp/WEB-INF/web.xml`.

## Logging

Log lines use the repository's Log4j layout (`src/lib/log-format.ts`), so Node and
Tomcat logs have one shape in the log aggregation:

```
2026-09-29T10:15:30,123 {RequestId=3f2b8c1e-… TraceId=80f1…} WARN  [config-cache] [main] backend unreachable source=poll
```

## Tracing

`X-B3-*` and `X-OT-*` are forwarded unchanged and asserted by a test; `X-Request-Id` is kept if it is a UUID and generated otherwise, as in the
backend — the backend reads them into its Log4j
`ThreadContext` and would lose the trace at this hop otherwise.