import { fileURLToPath } from 'node:url';
import path from 'node:path';

const packageRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

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
    readonly backendUrl: string;
    readonly distDir: string;
    readonly proxyUnmatched: boolean;
    readonly proxyTimeoutMs: number;
    readonly bodyLimitBytes: number;
    readonly logLevel: string;
    readonly dev: boolean;
    readonly trustProxy: boolean;
}

export function loadConfig(): Config {
    return {
        host: str('HOST', '0.0.0.0'),
        port: int('PORT', 8080),
        contextPath: normalizeContextPath(str('CONTEXT_PATH', '/edu-sharing')),
        // without context path - the incoming request already carries it
        backendUrl: str('BACKEND_URL', 'http://repository-service:8080').replace(/\/+$/, ''),
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
    };
}
