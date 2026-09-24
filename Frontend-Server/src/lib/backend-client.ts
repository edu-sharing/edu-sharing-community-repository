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

/**
 * Headers for a backend call made on behalf of a browser request: the same
 * identity (session cookie / authorization), the same domain and the same trace.
 * Permission checks therefore behave exactly as for the browser itself.
 */
export function onBehalfOf(request: FastifyRequest): OutgoingHttpHeaders {
    return {
        ...anonymousFor(request),
        ...(request.headers.cookie === undefined ? {} : { cookie: request.headers.cookie }),
        ...(request.headers.authorization === undefined ? {} : { authorization: request.headers.authorization }),
        ...(request.headers['accept-language'] === undefined ? {} : { 'accept-language': request.headers['accept-language'] }),
    };
}

/** Domain and trace of a browser request, but no identity - for public data like the config. */
export function anonymousFor(request: FastifyRequest): OutgoingHttpHeaders {
    return {
        ...collectTraceHeaders(request.headers),
        [REQUEST_ID_HEADER]: request.id,
        // same set toBackend sends for proxied requests
        ...(request.headers.host === undefined ? {} : { host: request.headers.host }),
        'x-forwarded-host': request.hostname,
        'x-forwarded-proto': request.protocol,
    };
}
