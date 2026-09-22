import { randomUUID } from 'node:crypto';
import type { IncomingHttpHeaders } from 'node:http';

/**
 * Trace headers the backend consumes. Mirrors `Context.B3.isX3Header` in
 * Backend/alfresco/module/.../authentication/Context.java - keep both in sync.
 *
 * The backend reads X-B3-TraceId / X-B3-SpanId / X-B3-Sampled / X-Client-Trace-Id
 * into the Log4j ThreadContext and re-attaches the full set to its own outgoing
 * calls. If this proxy drops them, traces break at the Node hop.
 */
export const TRACE_HEADER_PREFIXES = ['x-b3-', 'x-ot-'] as const;
export const TRACE_HEADER_NAMES = ['x-request-id', 'x-client-trace-id'] as const;

export const REQUEST_ID_HEADER = 'x-request-id';
export const TRACE_ID_HEADER = 'x-b3-traceid';
export const SPAN_ID_HEADER = 'x-b3-spanid';

export function isTraceHeader(name: string): boolean {
    const lower = name.toLowerCase();
    return (
        TRACE_HEADER_PREFIXES.some((prefix) => lower.startsWith(prefix)) ||
        (TRACE_HEADER_NAMES as readonly string[]).includes(lower)
    );
}

/** Every trace header present on the request, lower-cased, single valued. */
export function collectTraceHeaders(headers: IncomingHttpHeaders): Record<string, string> {
    const collected: Record<string, string> = {};
    for (const [name, value] of Object.entries(headers)) {
        if (value === undefined || !isTraceHeader(name)) {
            continue;
        }
        collected[name.toLowerCase()] = Array.isArray(value) ? value.join(',') : value;
    }
    return collected;
}

/**
 * Never overwrite an incoming id - this server joins a trace, it does not start
 * one. Only X-Request-Id is generated when absent, so that responses this server
 * produces itself stay correlatable.
 */
export function ensureRequestId(headers: IncomingHttpHeaders): string {
    const existing = headers[REQUEST_ID_HEADER];
    if (typeof existing === 'string' && existing !== '') {
        return existing;
    }
    if (Array.isArray(existing) && existing[0] !== undefined && existing[0] !== '') {
        return existing[0];
    }
    return randomUUID();
}

function single(value: string | string[] | undefined): string | undefined {
    if (value === undefined) {
        return undefined;
    }
    return Array.isArray(value) ? value[0] : value;
}

/** Fields attached to every log line so Node and Tomcat logs join on the same ids. */
export function traceLogBindings(headers: IncomingHttpHeaders, requestId: string) {
    return {
        requestId,
        traceId: single(headers[TRACE_ID_HEADER]),
        spanId: single(headers[SPAN_ID_HEADER]),
    };
}
