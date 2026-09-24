import { STATUS_CODES } from 'node:http';
import type { FastifyReply, FastifyRequest } from 'fastify';

/**
 * Mirrors `ErrorFilter.handleError` (reached via `NgErrorServlet` / `error.html`),
 * so errors this server answers itself look exactly like the ones Tomcat produces:
 *
 * * `text/html`        -> status kept, browser sent to the angular error page
 *                         `components/error/<status>` (`URLTool.getNgErrorUrl`)
 * * `application/json` -> `ErrorResponse` shape, `{"error":"<status>","message":...}`
 * * anything else      -> plain text
 */
export function acceptsHtml(request: FastifyRequest): boolean {
    return (request.headers.accept ?? '').toLowerCase().includes('text/html');
}

export function sendError(request: FastifyRequest, reply: FastifyReply, contextPath: string, statusCode: number): FastifyReply {
    const accept = (request.headers.accept ?? '').toLowerCase();
    const message = STATUS_CODES[statusCode] ?? String(statusCode);
    reply.code(statusCode);
    if (acceptsHtml(request)) {
        // the backend uses an inline script with the CSP nonce - a meta refresh needs none
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
