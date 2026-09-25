import type { FastifyInstance } from 'fastify';
import fastifyMetricsModule from 'fastify-metrics';
import type { ConfigCache, UnreachableSource } from './config-cache.ts';

/**
 * Prometheus metrics.
 *
 * Measured on the app server by fastify-metrics (node default metrics plus a
 * duration histogram per route pattern, i.e. `/edu-sharing/rest/*`, never the
 * concrete url), plus the counters below for what this server itself does.
 *
 * Exposed by the management server (management-server.ts), never on the public
 * app port.
 */

const PREFIX = 'frontend_server_';

// commonjs package: from esm the default import is module.exports, the plugin is its `default`
const fastifyMetrics = fastifyMetricsModule.default;

export type ProxyReason = 'backend-path' | 'unmatched';

export interface AppMetrics {
    /** one proxied request, observed when its response is sent */
    proxied(reason: ProxyReason, statusCode: number, seconds: number): void;
    /** the node metadata fetch of a render / collection view */
    enrichment(result: 'ok' | 'missing' | 'failed', seconds: number): void;
    /** a shell page sent without CSP - no config loadable, or one without angular.headers */
    shellWithoutCsp(cause: 'no-config' | 'no-csp'): void;
    /** a request answered with 503 because the backend is unreachable - an app page or a proxied request */
    unavailable(kind: 'page' | 'proxy'): void;
}

/** Registers the measuring side on the app server; `/metrics` itself is served by the management server. */
export async function registerMetrics(app: FastifyInstance, configCache: ConfigCache): Promise<AppMetrics> {
    await app.register(fastifyMetrics, {
        // no route on the app server, see management-server.ts
        endpoint: null,
        // one process builds one app; tests build a fresh one per file
        clearRegisterOnInit: true,
        defaultMetrics: { enabled: true },
        routeMetrics: { enabled: true },
    });
    const client = app.metrics.client;

    const proxyDuration = new client.Histogram({
        name: `${PREFIX}proxy_duration_seconds`,
        help: 'Requests proxied to tomcat, by why they were proxied and the status answered',
        labelNames: ['reason', 'status_code'] as const,
        buckets: [0.01, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10, 30],
    });
    const enrichmentDuration = new client.Histogram({
        name: `${PREFIX}shell_enrichment_duration_seconds`,
        help: 'Node metadata fetches enriching render and collection views, by result',
        labelNames: ['result'] as const,
        buckets: [0.01, 0.05, 0.1, 0.25, 0.5, 1, 2],
    });
    const withoutCsp = new client.Counter({
        name: `${PREFIX}shell_without_csp_total`,
        help: 'Shell pages sent without Content-Security-Policy, by cause',
        labelNames: ['cause'] as const,
    });
    new client.Gauge({
        name: `${PREFIX}config_age_seconds`,
        help: 'Age of the cached backend config per domain - no series means no config loaded yet',
        labelNames: ['domain'] as const,
        collect() {
            this.reset();
            const now = Date.now();
            for (const { domain, fetchedAt } of configCache.entries()) {
                this.set({ domain }, (now - fetchedAt) / 1000);
            }
        },
    });

    new client.Gauge({
        name: `${PREFIX}backend_reachable`,
        help: '1 while the backend answers, 0 while it is unreachable (browsers get the "service unavailable" page)',
        collect() {
            this.set(configCache.isBackendReachable() ? 1 : 0);
        },
    });
    // the gauge only shows the state at scrape time - these make short outages visible too
    const unreachable = new client.Counter({
        name: `${PREFIX}backend_unreachable_total`,
        help: 'Times the backend turned unreachable, by what found out (poll, config, proxy)',
        labelNames: ['source'] as const,
    });
    configCache.onUnreachable((source: UnreachableSource) => unreachable.inc({ source }));
    const unavailableResponses = new client.Counter({
        name: `${PREFIX}unavailable_responses_total`,
        help: 'Requests answered with 503 because the backend is unreachable, by kind (page, proxy)',
        labelNames: ['kind'] as const,
    });

    return {
        proxied: (reason, statusCode, seconds) => proxyDuration.observe({ reason, status_code: String(statusCode) }, seconds),
        enrichment: (result, seconds) => enrichmentDuration.observe({ result }, seconds),
        shellWithoutCsp: (cause) => withoutCsp.inc({ cause }),
        unavailable: (kind) => unavailableResponses.inc({ kind }),
    };
}
