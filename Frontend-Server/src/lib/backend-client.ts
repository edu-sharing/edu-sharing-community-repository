import http, { type IncomingHttpHeaders, type OutgoingHttpHeaders } from 'node:http';
import https from 'node:https';
import type { FastifyRequest } from 'fastify';
import { collectTraceHeaders, REQUEST_ID_HEADER } from './tracing.ts';

export interface BackendResponse {
    readonly status: number;
    readonly headers: IncomingHttpHeaders;
    /** parsed JSON for a 2xx answer, otherwise undefined */
    readonly body: unknown;
}

/**
 * JSON GETs against Tomcat for data this server needs itself (config, node metadata).
 *
 * node:http instead of fetch: fetch forbids setting `Host`, and the backend resolves
 * the config context from it (`ConfigServiceFactory.getCurrentDomain`).
 */
export class BackendClient {
    readonly #base: URL;
    readonly #timeoutMs: number;

    constructor(baseUrl: string, timeoutMs: number) {
        this.#base = new URL(baseUrl);
        this.#timeoutMs = timeoutMs;
    }

    getJson(pathAndQuery: string, headers: OutgoingHttpHeaders = {}): Promise<BackendResponse> {
        const url = new URL(pathAndQuery, this.#base);
        const transport = url.protocol === 'https:' ? https : http;
        return new Promise((resolve, reject) => {
            const request = transport.request(
                url,
                { method: 'GET', headers: { accept: 'application/json', ...headers }, timeout: this.#timeoutMs },
                (response) => {
                    const chunks: Buffer[] = [];
                    response.on('data', (chunk: Buffer) => chunks.push(chunk));
                    response.on('error', reject);
                    response.on('end', () => {
                        const status = response.statusCode ?? 0;
                        let body: unknown;
                        if (status >= 200 && status < 300) {
                            try {
                                body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
                            } catch (cause) {
                                reject(new Error(`backend answered ${url.pathname} with invalid JSON`, { cause }));
                                return;
                            }
                        }
                        resolve({ status, headers: response.headers, body });
                    });
                },
            );
            request.on('timeout', () => request.destroy(new Error(`backend call ${url.pathname} timed out`)));
            request.on('error', reject);
            request.end();
        });
    }
}

/** What callers need - lets tests hand in a fake. */
export type JsonGetter = Pick<BackendClient, 'getJson'>;

/*
 * There is deliberately no `onBehalfOf` counterpart any more: this server never
 * forwards the browser's identity (session cookie, Authorization) to its own
 * backend calls. This does NOT concern the proxy in server.ts - that one forwards
 * everything, and has to. Without an ingress in front there is no session affinity, so such
 * a call can land on a pod that does not know the session - the result would be
 * unreliable rather than authenticated. Every call goes out anonymously.
 *
 * Temporary. What it costs and what has to be in place to bring it back:
 * docs/02-routing.md, "No identity is forwarded".
 */

/** Domain and trace of a browser request, but no identity - for public data like the config. */
/**
 * The config cache key of a host: lower case (host names are case insensitive, browsers
 * always send them so), without a trailing dot.
 */
export function normalizeDomain(host: string): string {
    return host.trim().toLowerCase().replace(/\.$/, '');
}

export function anonymousFor(request: FastifyRequest): OutgoingHttpHeaders {
    return {
        ...collectTraceHeaders(request.headers),
        [REQUEST_ID_HEADER]: request.id,
        // same set toBackend sends for proxied requests - but in the case a browser sends:
        // the backend matches context domains case sensitively, the cache key is lower case
        ...(request.headers.host === undefined ? {} : { host: request.headers.host.toLowerCase() }),
        'x-forwarded-host': normalizeDomain(request.hostname),
        'x-forwarded-proto': request.protocol,
    };
}

/** codes of a connection that never reached tomcat - as opposed to a slow or failing request */
const UNREACHABLE_CODES = new Set(['ECONNREFUSED', 'ENOTFOUND', 'EAI_AGAIN', 'EHOSTUNREACH', 'ENETUNREACH', 'UND_ERR_CONNECT_TIMEOUT']);

/**
 * The backend could not be connected to at all. Takes the errors of this client as well
 * as those @fastify/reply-from hands to `onError` (the original error is their `cause`).
 */
export function isConnectionError(error: unknown): boolean {
    const codes = [error, (error as { cause?: unknown } | undefined)?.cause].map(
        (candidate) => (candidate as { code?: unknown } | undefined)?.code,
    );
    return codes.some((code) => typeof code === 'string' && UNREACHABLE_CODES.has(code));
}
