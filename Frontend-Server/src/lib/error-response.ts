import { STATUS_CODES } from 'node:http';
import type { FastifyReply, FastifyRequest } from 'fastify';
import type { CustomErrorPage } from './custom-error-page.ts';

/**
 * Mirrors `ErrorFilter.handleError` (also behind `ErrorPageServlet`, tomcat's error page),
 * so errors this server answers itself look exactly like the ones Tomcat produces:
 *
 * * `text/html`        -> status kept, browser sent to the angular error page
 *                         `components/error/<status>` by a relative meta refresh
 *                         (`URLTool.getErrorPageUrl`)
 * * `application/json` -> `ErrorResponse` shape, `{"error":"<status>","message":...}`
 * * anything else      -> plain text
 *
 * With a CustomErrorPage configured for the status, browsers get that instead.
 */
export function acceptsHtml(request: FastifyRequest): boolean {
    return (request.headers.accept ?? '').toLowerCase().includes('text/html');
}

export function sendError(
    request: FastifyRequest,
    reply: FastifyReply,
    contextPath: string,
    statusCode: number,
    custom?: CustomErrorPage,
): FastifyReply {
    if (custom?.handles(request, statusCode)) {
        // back to the url that failed - for a proxy error a retry may well work
        return custom.send(request, reply, statusCode, request.url, contextPath);
    }
    const accept = (request.headers.accept ?? '').toLowerCase();
    const message = STATUS_CODES[statusCode] ?? String(statusCode);
    reply.code(statusCode);
    if (acceptsHtml(request)) {
        // a meta refresh needs no CSP nonce - the same markup as ErrorFilter.handleError
        const target = `${contextPath}/components/error/${statusCode}`;
        return reply
            .type('text/html; charset=utf-8')
            .send(`<head><meta http-equiv="refresh" content="0;url=${target}"></head>`);
    }
    if (accept.includes('application/json')) {
        return reply.type('application/json').send({ error: String(statusCode), message });
    }
    return reply.type('text/plain; charset=utf-8').send(message);
}

/**
 * `NgServlet.getErrorCode`: `components/error/<code>` is answered with `<code>`,
 * anything unparsable with 500. Undefined for every other url.
 */
export function shellErrorStatus(url: string): number | undefined {
    const segments = new URL(url, 'http://localhost').pathname.split('/').filter((segment) => segment !== '');
    if (segments[0] !== 'components' || segments[1] !== 'error') {
        return undefined;
    }
    const code = Number(segments.at(-1));
    return Number.isInteger(code) && code >= 400 && code <= 599 ? code : 500;
}

/** the angular page shown while the backend is unreachable, relative to CONTEXT_PATH - it needs no backend itself */
export const SERVICE_UNAVAILABLE_PATH = '/components/error/503';

/** the url to return to once the backend is back, read by the angular page */
export const SERVICE_UNAVAILABLE_NEXT_PARAM = 'next';

/** how often clients may retry, the `Retry-After` of every "unavailable" answer */
export const RETRY_AFTER_SECONDS = 10;

export function isServiceUnavailablePage(url: string, contextPath: string): boolean {
    return new URL(url, 'http://localhost').pathname.replace(/\/+$/, '') === `${contextPath}${SERVICE_UNAVAILABLE_PATH}`;
}

/**
 * The backend is unreachable: 503 with `Retry-After`. Browsers are sent to the angular
 * "service unavailable" page, which returns to `request.url` by itself once the backend
 * answers again. Everything else gets the `ErrorResponse` shape / plain text, like `sendError`.
 */
export function sendUnavailable(request: FastifyRequest, reply: FastifyReply, contextPath: string, custom?: CustomErrorPage): FastifyReply {
    reply.code(503).header('Retry-After', String(RETRY_AFTER_SECONDS));
    if (custom?.handles(request, 503)) {
        // the custom page has to bring the browser back itself, e.g. by a meta refresh to {{next}}
        return custom.send(request, reply, 503, request.url, contextPath);
    }
    if (acceptsHtml(request)) {
        const target = `${contextPath}${SERVICE_UNAVAILABLE_PATH}?${SERVICE_UNAVAILABLE_NEXT_PARAM}=${encodeURIComponent(request.url)}`;
        return reply
            .type('text/html; charset=utf-8')
            .header('Cache-Control', 'no-store')
            .send(`<head><meta http-equiv="refresh" content="0;url=${escapeAttribute(target)}"></head>`);
    }
    return sendError(request, reply, contextPath, 503, custom);
}

function escapeAttribute(value: string): string {
    return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
}
