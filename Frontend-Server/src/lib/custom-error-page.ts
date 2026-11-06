import { randomBytes } from 'node:crypto';
import { STATUS_CODES } from 'node:http';
import type { FastifyReply, FastifyRequest } from 'fastify';

/**
 * Custom error pages for browsers (`Accept: text/html`), instead of the angular
 * `components/error/<status>` page. Configured by the operator, see Config.errorPage:
 *
 * * `url`  - browsers are sent there by a meta refresh; `{status}` and `{next}` (the
 *            absolute url to return to, url-encoded) are replaced. The status code stays
 *            on the response - a 302 would hide the error from monitoring and crawlers.
 * * `html` - served as is, read once at startup, so it works without the backend.
 *            `{{status}}`, `{{message}}`, `{{requestId}}`, `{{next}}` (relative path to
 *            return to) and `{{nonce}}` (for an inline `<script nonce="{{nonce}}">`)
 *            are replaced, html-escaped.
 *
 * `url` wins over `html`. Without either, or for a status not listed, nothing changes.
 * JSON / plain text answers for API clients are never affected.
 */
export interface ErrorPageConfig {
    /** comma separated statuses (`503`) or classes (`5xx`), or `all` */
    readonly statuses: string;
    readonly url?: string;
    readonly html?: string;
}

export type ErrorPageKind = 'html' | 'url';

/**
 * The page has neither the repository's CSP (the config may be what is missing) nor
 * a reason to load anything from this host: inline styles, images and fonts from
 * anywhere secure, scripts only inline with the nonce.
 */
function contentSecurityPolicy(nonce: string): string {
    return [
        "default-src 'none'",
        `script-src 'nonce-${nonce}'`,
        "style-src 'unsafe-inline' https:",
        'img-src data: https:',
        'font-src data: https:',
        "base-uri 'none'",
        "form-action 'none'",
        "frame-ancestors 'self'",
    ].join('; ');
}

type StatusMatcher = (status: number) => boolean;

export function parseStatuses(spec: string): StatusMatcher {
    const entries = spec
        .split(',')
        .map((entry) => entry.trim().toLowerCase())
        .filter((entry) => entry !== '');
    if (entries.includes('all')) {
        return () => true;
    }
    const exact = new Set<number>();
    const classes = new Set<number>();
    for (const entry of entries) {
        if (/^[45]\d\d$/.test(entry)) {
            exact.add(Number(entry));
        } else if (/^[45]xx$/.test(entry)) {
            classes.add(Number(entry[0]));
        } else {
            throw new Error(`ERROR_PAGE_STATUSES: "${entry}" is neither a 4xx/5xx status, a class like "5xx" nor "all"`);
        }
    }
    return (status) => exact.has(status) || classes.has(Math.floor(status / 100));
}

/** a path on this host - an incoming `next` must never send the browser elsewhere */
export function isLocalPath(value: string): boolean {
    return value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/\\');
}

function escapeHtml(value: string): string {
    return value
        .replaceAll('&', '&amp;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}

export class CustomErrorPage {
    private readonly applies: StatusMatcher;
    private readonly url: string | undefined;
    private readonly html: string | undefined;
    private readonly onSend: (kind: ErrorPageKind, status: number) => void;

    /** throws on an invalid configuration - called at startup, so a typo fails the deployment */
    constructor(config: ErrorPageConfig, onSend: (kind: ErrorPageKind, status: number) => void = () => {}) {
        this.onSend = onSend;
        this.applies = parseStatuses(config.statuses);
        if (config.url !== undefined) {
            let parsed: URL;
            try {
                parsed = new URL(config.url);
            } catch {
                throw new Error(`ERROR_PAGE_URL must be an absolute url, got "${config.url}"`);
            }
            if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
                throw new Error(`ERROR_PAGE_URL must be an http(s) url, got "${config.url}"`);
            }
        }
        this.url = config.url;
        this.html = config.url === undefined ? config.html : undefined;
    }

    get kind(): ErrorPageKind | undefined {
        return this.url !== undefined ? 'url' : this.html !== undefined ? 'html' : undefined;
    }

    /** whether `send` answers this request - a browser and a configured status */
    handles(request: FastifyRequest, status: number): boolean {
        return this.kind !== undefined && this.applies(status) && (request.headers.accept ?? '').toLowerCase().includes('text/html');
    }

    /**
     * Answers with the custom page, keeping `status`. Headers set before (`Retry-After`)
     * are kept. Only call after `handles` said yes.
     *
     * @param next where the browser may return to - a path on this host, else the app root
     */
    send(request: FastifyRequest, reply: FastifyReply, status: number, next: string, contextPath: string): FastifyReply {
        const back = isLocalPath(next) ? next : `${contextPath}/`;
        reply.code(status).header('Cache-Control', 'no-store').header('X-Content-Type-Options', 'nosniff');
        if (this.url !== undefined) {
            this.onSend('url', status);
            const target = this.url
                .replaceAll('{status}', String(status))
                .replaceAll('{next}', encodeURIComponent(`${request.protocol}://${request.host}${back}`));
            return reply
                .type('text/html; charset=utf-8')
                .send(`<head><meta http-equiv="refresh" content="0;url=${escapeHtml(target)}"></head>`);
        }
        this.onSend('html', status);
        const nonce = randomBytes(16).toString('base64');
        const values: Record<string, string> = {
            status: String(status),
            message: STATUS_CODES[status] ?? String(status),
            requestId: request.id,
            next: back,
            nonce,
        };
        const body = this.html!.replace(/\{\{\s*(status|message|requestId|next|nonce)\s*\}\}/g, (_match, key: string) =>
            escapeHtml(values[key]!),
        );
        return reply
            .type('text/html; charset=utf-8')
            .header('Content-Security-Policy', contentSecurityPolicy(nonce))
            .send(body);
    }
}
