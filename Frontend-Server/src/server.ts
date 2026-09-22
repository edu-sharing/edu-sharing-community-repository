import path from 'node:path';
import Fastify, { type FastifyInstance, type FastifyReply, type FastifyRequest } from 'fastify';
import fastifyStatic from '@fastify/static';
import replyFrom from '@fastify/reply-from';
import type { Config } from './config.ts';
import { IndexHtml } from './lib/index-html.ts';
import { ensureRequestId, REQUEST_ID_HEADER, traceLogBindings } from './lib/tracing.ts';
import { ANGULAR_ROUTE_PREFIXES, BACKEND_PATHS } from './routes/backend-paths.ts';

/** Angular emits `main-A1B2C3D4.js`; those are content hashed and immutable. */
const HASHED_ASSET = /-[A-Z0-9]{8,}\.(js|css)$/;

export async function buildServer(config: Config): Promise<FastifyInstance> {
    const app = Fastify({
        logger: {
            level: config.logLevel,
            ...(config.dev ? { transport: { target: 'pino-pretty' } } : {}),
        },
        trustProxy: config.trustProxy,
        bodyLimit: config.bodyLimitBytes,
        disableRequestLogging: !config.dev,
        genReqId: (request) => ensureRequestId(request.headers),
        // binds traceId/spanId/requestId to every log line, so Node and Tomcat
        // logs can be queried with the same ids
        childLoggerFactory: (logger, bindings, options, rawRequest) =>
            logger.child({ ...bindings, ...traceLogBindings(rawRequest.headers, ensureRequestId(rawRequest.headers)) }, options),
    });

    // Never parse a body - everything with one is proxied, so it is streamed through.
    app.addContentTypeParser('*', (_request, payload, done) => done(null, payload));

    // echo the request id back so responses this server answers itself stay correlatable
    app.addHook('onRequest', async (request, reply) => {
        reply.header(REQUEST_ID_HEADER, request.id);
    });

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

    /**
     * All trace headers pass through untouched - @fastify/reply-from forwards the
     * request headers verbatim, we only append the X-Forwarded-* set so Tomcat
     * (URLTool) keeps building correct absolute URLs.
     */
    function toBackend(request: FastifyRequest, reply: FastifyReply): FastifyReply {
        return reply.from(request.url, {
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
                };
            },
        });
    }

    function sendIndex(_request: FastifyRequest, reply: FastifyReply): void {
        reply
            .type('text/html; charset=utf-8')
            // mirrors DisableCacheFilter, mapped to /index.html, /components/* and /v/*
            .header('Cache-Control', 'no-store, no-cache, must-revalidate')
            .header('Pragma', 'no-cache')
            .send(index.render());
    }

    // --- health -----------------------------------------------------------------
    // registered first and never proxied, so a cold backend still reports ready
    app.get(`${ctx}/_health`, async () => ({ status: 'ok', backend: config.backendUrl }));

    // --- backend ----------------------------------------------------------------
    for (const backendPath of BACKEND_PATHS) {
        app.all(`${ctx}${backendPath}`, toBackend);
        app.all(`${ctx}${backendPath}/*`, toBackend);
    }

    // --- angular shell ----------------------------------------------------------
    app.get(ctx === '' ? '/' : ctx, (_request, reply) => reply.redirect(`${ctx}/`, 302));
    app.get(`${ctx}/`, sendIndex);
    app.get(`${ctx}/index.html`, sendIndex);
    for (const prefix of ANGULAR_ROUTE_PREFIXES) {
        app.get(`${ctx}${prefix}`, sendIndex);
        app.get(`${ctx}${prefix}/*`, sendIndex);
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
        },
    });

    // --- fallback ---------------------------------------------------------------
    app.setNotFoundHandler((request, reply) => {
        if (!request.url.startsWith(`${ctx}/`)) {
            return reply.code(404).send({ statusCode: 404, error: 'Not Found', message: 'Not Found' });
        }
        if (!config.proxyUnmatched) {
            return reply.code(404).send({ statusCode: 404, error: 'Not Found', message: 'Not Found' });
        }
        // TRANSITIONAL. The target state is an exhaustive BACKEND_PATHS and a plain
        // 404 here - this branch only exists so the migration cannot break a path
        // nobody anticipated, and so these warnings tell us which paths to add.
        // Removal plan: docs/06-migration-phases.md, phase 2.
        request.log.warn({ path: request.url, method: request.method }, 'unmatched path, proxying to backend');
        return toBackend(request, reply);
    });

    return app;
}
