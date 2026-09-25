import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import Fastify, { LogController, type FastifyInstance, type FastifyReply, type FastifyRequest } from 'fastify';
import fastifyStatic from '@fastify/static';
import replyFrom from '@fastify/reply-from';
import type { Config } from './config.ts';
import { anonymousFor, BackendClient, onBehalfOf } from './lib/backend-client.ts';
import { ConfigCache } from './lib/config-cache.ts';
import { acceptsHtml, sendError, shellErrorStatus } from './lib/error-response.ts';
import { IndexHtml } from './lib/index-html.ts';
import { registerMetrics, type ProxyReason } from './lib/metrics.ts';
import { loadNode, parseShellTarget } from './lib/node-metadata.ts';
import { securityHeaders } from './lib/security-headers.ts';
import { enrichShell } from './lib/shell-head.ts';
import { ensureRequestId, REQUEST_ID_HEADER, traceLogBindings } from './lib/tracing.ts';
import { ANGULAR_ROUTE_PREFIXES, BACKEND_PATHS, WEBDAV_METHODS } from './routes/backend-paths.ts';

/** Angular emits `main-A1B2C3D4.js`; those are content hashed and immutable. */
const HASHED_ASSET = /-[A-Z0-9]{8,}\.(js|css)$/;

/** package.json sits one level above both src/ and build/. */
function serverVersion(): string {
    const pkg = JSON.parse(fs.readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as { version?: string };
    return `edu-sharing-frontend-server/${pkg.version ?? 'unknown'} node/${process.versions.node}`;
}

/** pino-pretty is a devDependency - the image runs `npm ci --omit=dev`, DEV=true must not crash there. */
function hasPinoPretty(): boolean {
    try {
        createRequire(import.meta.url).resolve('pino-pretty');
        return true;
    } catch {
        return false;
    }
}

interface ProxyInfo {
    reason: ProxyReason;
    upstream: string;
    startedAt: number;
}

declare module 'fastify' {
    interface FastifyInstance {
        /** readiness for the management server, see ConfigCache.isReady */
        backendReady(): boolean;
    }
}

export async function buildServer(config: Config): Promise<FastifyInstance> {
    const app = Fastify({
        logger: {
            level: config.logLevel,
            ...(config.dev && hasPinoPretty() ? { transport: { target: 'pino-pretty' } } : {}),
        },
        trustProxy: config.trustProxy,
        bodyLimit: config.bodyLimitBytes,
        // per-request log lines (incoming / completed) only in dev
        logController: new LogController({ disableRequestLogging: !config.dev }),
        genReqId: (request) => ensureRequestId(request.headers),
        // binds traceId/spanId/requestId to every log line, so Node and Tomcat
        // logs can be queried with the same ids
        childLoggerFactory: (logger, bindings, options, rawRequest) =>
            logger.child({ ...bindings, ...traceLogBindings(rawRequest.headers, ensureRequestId(rawRequest.headers)) }, options),
    });

    // Never parse a body - everything with one is proxied, so it is streamed through.
    app.addContentTypeParser('*', (_request, payload, done) => done(null, payload));

    // the WebDAV verbs of /webdav (Tomcat's WebDAV servlet) - fastify only routes the
    // methods it knows, `all` would skip these and they would end up in the not-found handler
    for (const method of WEBDAV_METHODS) {
        app.addHttpMethod(method, { hasBody: true });
    }

    // echo the request id back so responses this server answers itself stay correlatable
    app.addHook('onRequest', async (request, reply) => {
        reply.header(REQUEST_ID_HEADER, request.id);
    });

    // filled by toBackend, read by the dev headers below
    const proxied = new WeakMap<FastifyRequest, ProxyInfo>();

    if (config.dev) {
        // dev only - production stays silent about versions and topology, like Fastify's
        // default. onSend runs after reply-from copied the backend headers, so these win
        // over anything the backend sends
        const server = serverVersion();
        app.addHook('onSend', async (request, reply, payload) => {
            reply.header('Server', server);
            const info = proxied.get(request);
            if (info) {
                reply.header('X-Proxy-Reason', info.reason);
                reply.header('X-Proxy-Upstream', info.upstream);
                reply.header('X-Proxy-Time', `${Date.now() - info.startedAt}ms`);
            }
            return payload;
        });
    }

    await app.register(replyFrom, {
        base: config.backendUrl,
        undici: {
            bodyTimeout: config.proxyTimeoutMs,
            headersTimeout: config.proxyTimeoutMs,
            connections: 128,
            pipelining: 1,
        },
    });

    const ctx = config.contextPath;
    const index = IndexHtml.load(config.distDir, { watchForChanges: config.dev });
    index.verifyBaseHref(ctx, app.log);

    // browser traffic goes to the external connector only: tomcat skips 2FA for
    // requests on its internal port (ApplicationInfoList.isInternalPortRequest)
    if (new URL(config.backendUrl).host === new URL(config.backendInternalUrl).host) {
        app.log.error(
            { backendUrl: config.backendUrl, backendInternalUrl: config.backendInternalUrl },
            'BACKEND_URL and BACKEND_INTERNAL_URL point to the same host and port - BACKEND_URL must be the external connector (else 2FA is bypassed for every proxied request), BACKEND_INTERNAL_URL the internal one',
        );
    }
    const backend = new BackendClient(config.backendUrl, config.enrichTimeoutMs);
    // config only - the internal port hands out ValuesBackendInternal
    const internalBackend = new BackendClient(config.backendInternalUrl, config.enrichTimeoutMs);
    const configCache = new ConfigCache(internalBackend, {
        contextPath: ctx,
        pollIntervalMs: config.configPollIntervalMs,
        maxAgeMs: config.configMaxAgeMs,
        log: app.log,
    });
    configCache.start();
    app.addHook('onClose', async () => configCache.stop());
    app.decorate('backendReady', () => configCache.isReady());

    // before any route, so fastify-metrics sees every route pattern
    const metrics = await registerMetrics(app, configCache);
    app.addHook('onResponse', async (request, reply) => {
        const info = proxied.get(request);
        if (info) {
            metrics.proxied(info.reason, reply.statusCode, (Date.now() - info.startedAt) / 1000);
        }
    });

    /**
     * All trace headers pass through untouched - @fastify/reply-from forwards the
     * request headers verbatim, we only append the X-Forwarded-* set so Tomcat
     * (URLTool) keeps building correct absolute URLs.
     */
    function toBackend(request: FastifyRequest, reply: FastifyReply, reason: ProxyReason = 'backend-path'): FastifyReply {
        proxied.set(request, { reason, upstream: `${config.backendUrl}${request.url}`, startedAt: Date.now() });
        return reply.from(request.url, {
            // TRANSITIONAL, with the unmatched fallback: a path tomcat does not know either
            // is answered with this server's own error page - tomcat's (ErrorFilter) points
            // at the configured repository domain instead of the requested one
            ...(reason === 'unmatched'
                ? {
                      // the reply handed in is this very reply, only typed more generally
                      onResponse: (_request, _reply, response) => {
                          if (response.statusCode === 404 && acceptsHtml(request)) {
                              response.stream.resume();
                              for (const header of ['content-length', 'content-type', 'content-encoding']) {
                                  reply.removeHeader(header);
                              }
                              return sendError(request, reply, ctx, 404);
                          }
                          return reply.send(response.stream);
                      },
                  }
                : {}),
            rewriteRequestHeaders: (originalRequest, headers) => {
                const forwardedFor = headers['x-forwarded-for'];
                const clientIp = originalRequest.socket.remoteAddress ?? '';
                return {
                    ...headers,
                    [REQUEST_ID_HEADER]: request.id,
                    'x-forwarded-for':
                        typeof forwardedFor === 'string' && forwardedFor !== ''
                            ? `${forwardedFor}, ${clientIp}`
                            : clientIp,
                    'x-forwarded-proto': request.protocol,
                    'x-forwarded-host': request.hostname,
                    // reply-from sets Host to the upstream - the backend resolves the
                    // config context from the domain (ConfigServiceFactory.getCurrentDomain)
                    // and must see the browser's, exactly as behind nginx
                    ...(originalRequest.headers.host === undefined ? {} : { host: originalRequest.headers.host }),
                };
            },
        });
    }

    /**
     * `NgServlet.doGet` plus `SecurityHeadersFilter`: the shell with custom html headers
     * and security headers sharing one nonce, the render and collection views enriched
     * for crawlers and oEmbed consumers, and `components/error/<code>` answered with
     * that status.
     */
    async function sendIndex(request: FastifyRequest, reply: FastifyReply): Promise<FastifyReply> {
        const url = request.url.slice(ctx.length);
        const errorStatus = shellErrorStatus(url);
        if (errorStatus !== undefined) {
            reply.code(errorStatus);
        }
        const target = parseShellTarget(url);
        // the config is needed on every shell page (headers, custom html) and is cached;
        // node metadata only for the render and collection views
        const enrichmentStart = Date.now();
        const [configResult, nodeResult] = await Promise.allSettled([
            configCache.get(request.hostname, anonymousFor(request)),
            target === undefined ? Promise.resolve(undefined) : loadNode(backend, ctx, target, onBehalfOf(request)),
        ]);
        if (target !== undefined) {
            const result = nodeResult.status === 'rejected' ? 'failed' : nodeResult.value === undefined ? 'missing' : 'ok';
            metrics.enrichment(result, (Date.now() - enrichmentStart) / 1000);
        }
        if (nodeResult.status === 'rejected') {
            request.log.warn({ err: nodeResult.reason, nodeId: target?.nodeId }, 'loading node metadata for the shell failed');
        }
        const repositoryConfig = configResult.status === 'fulfilled' ? configResult.value : null;
        const node = nodeResult.status === 'fulfilled' ? nodeResult.value : undefined;

        const nonce = IndexHtml.newNonce();
        let html = index.render(nonce, repositoryConfig?.customHtmlHeaders ?? '');
        if (target !== undefined) {
            const origin = `${request.protocol}://${request.host}`;
            html = enrichShell(html, {
                target,
                loaded: node,
                config: repositoryConfig,
                pageUrl: `${origin}${request.url}`,
                appUrl: `${origin}${ctx}/`,
            });
        }
        // the metadata call may have opened the (guest) session - NgServlet did the same
        // for the shell request, so the browser keeps it instead of Tomcat collecting
        // one orphaned session per page view
        if (node?.setCookie !== undefined) {
            reply.header('set-cookie', node.setCookie);
        }
        if (repositoryConfig === null) {
            metrics.shellWithoutCsp('no-config');
            request.log.error(
                { backendInternalUrl: config.backendInternalUrl },
                'backend config not available - shell sent without security headers and custom html headers',
            );
        } else {
            const headers = securityHeaders(repositoryConfig.angularHeaders, nonce);
            if (headers['Content-Security-Policy'] === undefined) {
                metrics.shellWithoutCsp('no-csp');
                request.log.warn('backend config has no angular.headers Content-Security-Policy - shell sent without CSP');
            }
            reply.headers(headers);
        }
        return reply
            .type('text/html; charset=utf-8')
            // mirrors DisableCacheFilter, mapped to /index.html, /components/* and /v/*
            .header('Cache-Control', 'no-store, no-cache, must-revalidate')
            .header('Pragma', 'no-cache')
            .send(html);
    }

    // health: on the management server, see lib/management-server.ts

    // --- backend ----------------------------------------------------------------
    for (const backendPath of BACKEND_PATHS) {
        app.all(`${ctx}${backendPath}`, (request, reply) => toBackend(request, reply));
        app.all(`${ctx}${backendPath}/*`, (request, reply) => toBackend(request, reply));
    }

    // --- angular shell ----------------------------------------------------------
    app.get(ctx === '' ? '/' : ctx, (_request, reply) => reply.redirect(`${ctx}/`, 302));
    if (ctx !== '') {
        // the server owns the whole host, so the bare root lands on the app
        app.get('/', (_request, reply) => reply.redirect(`${ctx}/`, 302));
    }
    app.get(`${ctx}/`, sendIndex);
    app.get(`${ctx}/index.html`, sendIndex);
    for (const prefix of ANGULAR_ROUTE_PREFIXES) {
        app.get(`${ctx}${prefix}`, sendIndex);
        app.get(`${ctx}${prefix}/*`, sendIndex);
    }

    const webComponentsDir = path.join(path.resolve(config.distDir), 'web-components') + path.sep;
    function isWebComponentFile(filePath: string): boolean {
        return path.resolve(filePath).startsWith(webComponentsDir);
    }

    // --- static -----------------------------------------------------------------
    await app.register(fastifyStatic, {
        root: config.distDir,
        prefix: `${ctx}/`,
        index: false,
        redirect: false,
        // fully managed below - @fastify/static's own value would win otherwise
        cacheControl: false,
        setHeaders: (reply, filePath) => {
            reply.header(
                'Cache-Control',
                HASHED_ASSET.test(path.basename(filePath))
                    ? 'public, max-age=31536000, immutable'
                    : 'public, max-age=300',
            );
            // WebComponentsFilter: the web components are embedded on foreign origins
            if (isWebComponentFile(filePath)) {
                reply.header('Access-Control-Allow-Origin', '*');
            }
        },
    });

    // --- fallback ---------------------------------------------------------------
    app.setNotFoundHandler((request, reply) => {
        if (!request.url.startsWith(`${ctx}/`) || !config.proxyUnmatched) {
            return sendError(request, reply, ctx, 404);
        }
        // TRANSITIONAL. The target state is an exhaustive BACKEND_PATHS and a plain
        // 404 here - this branch only exists so the migration cannot break a path
        // nobody anticipated, and so these warnings tell us which paths to add.
        // Removal plan: docs/06-migration-phases.md, phase 2.
        request.log.warn({ path: request.url, method: request.method }, 'unmatched path, proxying to backend');
        return toBackend(request, reply, 'unmatched');
    });

    return app;
}
