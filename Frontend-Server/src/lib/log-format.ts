/**
 * Log lines in the layout of the repository (Tomcat / Log4j), so both land in the log
 * aggregation in one shape and a request id query finds both. Mirrors
 * `appender.console.layout.pattern` in Backend/alfresco/module/src/main/amp/config/log4j2.properties:
 *
 *     %d{ISO8601} {RequestId=… SpanId=… TraceId=…} %-5p [%c{3}] [%t] %m
 *
 * e.g. `2026-09-29T10:15:30,123 {RequestId=3f2b8c1e-… TraceId=80f1…} WARN  [config-cache] [main] backend unreachable source=poll`
 *
 * - the id block only lists the ids present, and is left out without any
 * - `%c` is the `name` binding of a child logger, `frontend-server` otherwise
 * - `%t` is always `main` - node runs the handlers on one thread
 * - newlines are stripped from the message, as `%replace{%m}{[\r\n]+}{}` does; the fields
 *   pino logs besides the message follow it as ` key=value`
 * - an `err` is written as its stack on the lines below, as Log4j writes a Throwable
 *
 * Pino hands every record to {@link LogStream.write} as one JSON line; this formats it.
 */

const LEVELS: Readonly<Record<number, string>> = { 10: 'TRACE', 20: 'DEBUG', 30: 'INFO', 40: 'WARN', 50: 'ERROR', 60: 'FATAL' };

/** Log4j MDC keys of the repository, in its order, and the pino binding each comes from. */
const IDS = [
    ['RequestId', 'requestId'],
    ['SpanId', 'spanId'],
    ['TraceId', 'traceId'],
] as const;

export const DEFAULT_LOGGER_NAME = 'frontend-server';
const THREAD = 'main';

/** Written by pino itself or rendered elsewhere in the line - never repeated as key=value. */
const RESERVED = new Set(['level', 'time', 'pid', 'hostname', 'msg', 'name', 'err', 'reqId', ...IDS.map(([, field]) => field)]);

const NEWLINES = /[\r\n]+/g;

export interface LogStream {
    write(line: string): void;
}

/** A pino destination writing the repository layout to `out` (stdout by default). */
export function createLogStream(out: { write(chunk: string): unknown } = process.stdout): LogStream {
    return {
        write(line: string): void {
            out.write(formatLogLine(line));
        },
    };
}

/** One pino JSON record -> one line in the repository layout (plus the stack of an `err`), newline terminated. */
export function formatLogLine(json: string): string {
    let record: Record<string, unknown>;
    try {
        record = JSON.parse(json) as Record<string, unknown>;
    } catch {
        // not pino's - pass it on rather than lose it
        return json.endsWith('\n') ? json : `${json}\n`;
    }
    const level = LEVELS[record.level as number] ?? String(record.level ?? '');
    const name = typeof record.name === 'string' && record.name !== '' ? record.name : DEFAULT_LOGGER_NAME;
    const ids = IDS.filter(([, field]) => present(record[field])).map(([key, field]) => `${key}=${String(record[field])}`);
    const fields = Object.entries(record)
        .filter(([key, value]) => !RESERVED.has(key) && value !== undefined)
        .map(([key, value]) => ` ${key}=${renderValue(value)}`)
        .join('');
    const parts = [
        formatTimestamp(typeof record.time === 'number' ? record.time : Date.now()),
        ...(ids.length > 0 ? [`{${ids.join(' ')}}`] : []),
        level.padEnd(5),
        `[${name}]`,
        `[${THREAD}]`,
        `${String(record.msg ?? '')}${fields}`.replace(NEWLINES, ''),
    ];
    return `${parts.join(' ')}\n${renderError(record.err)}`;
}

/** Log4j's `%d{ISO8601}`: local time, `yyyy-MM-dd'T'HH:mm:ss,SSS`. */
export function formatTimestamp(epochMs: number): string {
    const d = new Date(epochMs);
    const pad = (value: number, length = 2) => String(value).padStart(length, '0');
    return (
        `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
        `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())},${pad(d.getMilliseconds(), 3)}`
    );
}

function present(value: unknown): boolean {
    return value !== undefined && value !== null && value !== '';
}

function renderValue(value: unknown): string {
    return typeof value === 'string' ? value : JSON.stringify(value);
}

/** pino's err serializer yields `{ type, message, stack }` - the stack already starts with type and message. */
function renderError(err: unknown): string {
    if (typeof err !== 'object' || err === null) {
        return '';
    }
    const { stack, type, message } = err as { stack?: unknown; type?: unknown; message?: unknown };
    const text = typeof stack === 'string' && stack !== '' ? stack : `${String(type ?? 'Error')}: ${String(message ?? '')}`;
    return `${text}\n`;
}
