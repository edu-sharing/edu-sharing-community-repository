import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

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

function bool(name: string, fallback: boolean): boolean {
    const value = process.env[name];
    if (value === undefined || value === '') {
        return fallback;
    }
    return ['1', 'true', 'yes', 'on'].includes(value.toLowerCase());
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
    /** port of the management server (`/metrics`, `/health`); 0 disables it */
    readonly managementPort: number;
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
        distDir: path.resolve(packageRoot, str('DIST_DIR', path.join(packageRoot, 'dist'))),
        // transitional: see docs/02-routing.md - the target state is an exhaustive
        // BACKEND_PATHS list and a 404 for anything else
        proxyUnmatched: bool('PROXY_UNMATCHED', true),
        // matches nginx.ingress.kubernetes.io/proxy-read-timeout: 180
        proxyTimeoutMs: int('PROXY_TIMEOUT_MS', 180_000),
        // matches nginx.ingress.kubernetes.io/proxy-body-size: 5g
        bodyLimitBytes: int('BODY_LIMIT_BYTES', 5 * 1024 * 1024 * 1024),
        logLevel: str('LOG_LEVEL', 'info'),
        dev: bool('DEV', false),
        trustProxy: bool('TRUST_PROXY', true),
        enrichTimeoutMs: int('ENRICH_TIMEOUT_MS', 2_000),
        configTimeoutMs: int('CONFIG_TIMEOUT_MS', 10_000),
        configPollIntervalMs: int('CONFIG_POLL_INTERVAL_MS', 60_000),
        backendProbeTimeoutMs: int('BACKEND_PROBE_TIMEOUT_MS', 5_000),
        configMaxAgeMs: int('CONFIG_MAX_AGE_MS', 600_000),
        // same port as the repository's jmx exporter - never the public app port
        managementPort: int('MANAGEMENT_PORT', 9090),
    };
}
