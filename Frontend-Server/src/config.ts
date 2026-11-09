import fs from 'node:fs';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import type { ErrorPageConfig } from './lib/custom-error-page.ts';

const packageRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

/**
 * Loads `<package root>/.env` for local development - independent of the working
 * directory, so a plain `node src/index.ts` from the IDE picks it up as well as
 * `npm run dev`. Variables already set in the environment win. The image ships no
 * `.env`, there this is a no-op.
 */
export function loadDotEnv(): void {
    const file = path.join(packageRoot, '.env');
    if (fs.existsSync(file)) {
        process.loadEnvFile(file);
    }
}

function str(name: string, fallback: string): string {
    const value = process.env[name];
    return value === undefined || value === '' ? fallback : value;
}

/** undefined when the variable is unset or empty */
function optionalStr(name: string): string | undefined {
    const value = process.env[name];
    return value === undefined || value === '' ? undefined : value;
}

function int(name: string, fallback: number): number {
    const value = process.env[name];
    if (value === undefined || value === '') {
        return fallback;
    }
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) {
        throw new Error(`Environment variable ${name} must be an integer, got "${value}"`);
    }
    return parsed;
}

const MB = 1024 * 1024;

/**
 * Default budget of the static file cache: 20 % of the memory the process may use - the
 * cgroup limit in a container (`process.constrainedMemory()`), else the host memory -
 * within 16 MB and 256 MB. The cached buffers live outside the V8 heap (external memory),
 * so they count against the container limit, not against --max-old-space-size. Together
 * with the heap (40 %, image default) this leaves ~40 % to code, sockets and streams.
 *
 * @param constrained `process.constrainedMemory()` - 0, or a huge value, without a limit
 */
export function autoStaticCacheBytes(constrained: number, total: number): number {
    const available = constrained > 0 && constrained < total ? constrained : total;
    return Math.min(256 * MB, Math.max(16 * MB, Math.floor(available * 0.2)));
}

/** `auto` (default) or a size in MB, see autoStaticCacheBytes */
function staticCacheBytes(name: string): number {
    const value = process.env[name];
    if (value === undefined || value === '' || value === 'auto') {
        return autoStaticCacheBytes(process.constrainedMemory(), os.totalmem());
    }
    return int(name, 0) * MB;
}

function bool(name: string, fallback: boolean): boolean {
    const value = process.env[name];
    if (value === undefined || value === '') {
        return fallback;
    }
    return ['1', 'true', 'yes', 'on'].includes(value.toLowerCase());
}

/**
 * `ERROR_PAGE_URL` / `ERROR_PAGE_FILE` (+ `ERROR_PAGE_STATUSES`, default `502,503,504`),
 * see lib/custom-error-page.ts. Undefined when neither is set. The file is read here,
 * once - the page has to be there while the backend is not, and a missing file fails
 * the startup instead of the first outage.
 */
function errorPage(): ErrorPageConfig | undefined {
    const url = str('ERROR_PAGE_URL', '');
    const file = str('ERROR_PAGE_FILE', '');
    if (url === '' && file === '') {
        return undefined;
    }
    let html: string | undefined;
    if (file !== '') {
        try {
            html = fs.readFileSync(file, 'utf8');
        } catch (error) {
            throw new Error(`ERROR_PAGE_FILE "${file}" is not readable: ${(error as Error).message}`);
        }
    }
    return {
        statuses: str('ERROR_PAGE_STATUSES', '502,503,504'),
        ...(url === '' ? {} : { url }),
        ...(html === undefined ? {} : { html }),
    };
}

/**
 * Strips a trailing slash so that `${contextPath}/foo` never yields a double slash.
 * `/edu-sharing/` and `/edu-sharing` both become `/edu-sharing`.
 */
function normalizeContextPath(raw: string): string {
    const trimmed = raw.replace(/\/+$/, '');
    if (trimmed !== '' && !trimmed.startsWith('/')) {
        throw new Error(`CONTEXT_PATH must start with "/", got "${raw}"`);
    }
    return trimmed;
}

export interface Config {
    readonly host: string;
    readonly port: number;
    readonly contextPath: string;
    /** external tomcat connector - every proxied and on-behalf-of-user request, 2FA applies */
    readonly backendUrl: string;
    /** internal tomcat connector - config fetches only, see ValuesBackendInternal */
    readonly backendInternalUrl: string;
    /** the search cluster, same one the backend reaches through `elasticsearch.servers` */
    readonly elasticUrl: string;
    /**
     * Override of the workspace index. Normally unset: the index is
     * `SearchServiceElastic.WORKSPACE_INDEX`, which carries the release in its name, and the
     * backend hands it out with its config (`elasticWorkspaceIndex`) - so an upgrade needs no
     * configuration here. A missing index is logged as an error, see server.ts.
     */
    readonly elasticIndex: string | undefined;
    readonly distDir: string;
    readonly proxyUnmatched: boolean;
    readonly proxyTimeoutMs: number;
    readonly bodyLimitBytes: number;
    readonly logLevel: string;
    readonly dev: boolean;
    readonly trustProxy: boolean;
    /**
     * timeout of the node metadata fetch of render / collection views (LRMI, oEmbed) -
     * the page waits for it, on expiry the plain shell is sent
     */
    readonly enrichTimeoutMs: number;
    /**
     * timeout of the config fetch - rare (cached per domain, refreshed in the background),
     * only a domain's first page waits for it
     */
    readonly configTimeoutMs: number;
    /** how often `_about.lastCacheUpdate` is polled to detect a backend config refresh; 0 disables */
    readonly configPollIntervalMs: number;
    /**
     * timeout of that `_about` poll - also the reachability probe, so longer than the
     * enrichment budget: a busy tomcat must not count as gone
     */
    readonly backendProbeTimeoutMs: number;
    /** upper bound for a cached config entry, in case a refresh is missed */
    readonly configMaxAgeMs: number;
    /**
     * how long a shutdown (SIGTERM / SIGINT) waits for running requests before the process
     * exits anyway - keep it below terminationGracePeriodSeconds of kubernetes
     */
    readonly shutdownTimeoutMs: number;
    /** port of the management server (`/metrics`, `/health`); 0 disables it */
    readonly managementPort: number;
    /** memory budget of the static file cache (lib/static-cache.ts), in bytes; 0 disables it - `auto` by default, see autoStaticCacheBytes */
    readonly staticCacheMaxBytes: number;
    /** larger files bypass the static file cache and are streamed from disk, in bytes */
    readonly staticCacheMaxFileBytes: number;
    /** the operator's error page for browsers instead of the angular one; undefined keeps the angular page */
    readonly errorPage?: ErrorPageConfig | undefined;
}

export function loadConfig(): Config {
    return {
        host: str('HOST', '0.0.0.0'),
        port: int('PORT', 8080),
        contextPath: normalizeContextPath(str('CONTEXT_PATH', '/edu-sharing')),
        // without context path - the incoming request already carries it.
        // 8081 is the external connector: tomcat treats 8080 (the home repository port) as
        // internal and skips 2FA there (ApplicationInfoList.isInternalPortRequest)
        backendUrl: str('BACKEND_URL', 'http://repository-service:8081').replace(/\/+$/, ''),
        backendInternalUrl: str('BACKEND_INTERNAL_URL', 'http://repository-service:8080').replace(/\/+$/, ''),
        elasticUrl: str('ELASTIC_URL', 'http://repository-search-elastic-index:9200').replace(/\/+$/, ''),
        elasticIndex: optionalStr('ELASTIC_INDEX'),
        distDir: path.resolve(packageRoot, str('DIST_DIR', path.join(packageRoot, 'dist'))),
        // transitional: the target state is an exhaustive
        // BACKEND_PATHS list and a 404 for anything else
        proxyUnmatched: bool('PROXY_UNMATCHED', true),
        // matches nginx.ingress.kubernetes.io/proxy-read-timeout: 180
        proxyTimeoutMs: int('PROXY_TIMEOUT_MS', 180_000),
        // matches nginx.ingress.kubernetes.io/proxy-body-size: 5g
        bodyLimitBytes: int('BODY_LIMIT_BYTES', 5 * 1024 * 1024 * 1024),
        logLevel: str('LOG_LEVEL', 'info'),
        dev: bool('DEV', false),
        trustProxy: bool('TRUST_PROXY', true),
        enrichTimeoutMs: int('ENRICH_TIMEOUT_MS', 500),
        configTimeoutMs: int('CONFIG_TIMEOUT_MS', 10_000),
        configPollIntervalMs: int('CONFIG_POLL_INTERVAL_MS', 60_000),
        backendProbeTimeoutMs: int('BACKEND_PROBE_TIMEOUT_MS', 5_000),
        configMaxAgeMs: int('CONFIG_MAX_AGE_MS', 600_000),
        shutdownTimeoutMs: int('SHUTDOWN_TIMEOUT_MS', 5_000),
        // same port as the repository's jmx exporter - never the public app port
        managementPort: int('MANAGEMENT_PORT', 9090),
        // the first page's files take ~10 MB with all variants; 51 MB in a 256Mi pod
        staticCacheMaxBytes: staticCacheBytes('STATIC_CACHE_MAX_MB'),
        // 8 MB: the largest bundles (monaco ts worker, pdf vendor) are 6-7 MB
        staticCacheMaxFileBytes: int('STATIC_CACHE_MAX_FILE_MB', 8) * MB,
        errorPage: errorPage(),
    };
}
