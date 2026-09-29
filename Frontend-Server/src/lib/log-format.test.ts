import assert from 'node:assert/strict';
import { test } from 'node:test';
import Fastify from 'fastify';
import { createLogStream, formatLogLine, formatTimestamp } from './log-format.ts';

const TIME = new Date(2026, 8, 29, 10, 5, 3, 7).getTime();
const TS = '2026-09-29T10:05:03,007';
const REQUEST_ID = '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b';

const line = (record: Record<string, unknown>) => formatLogLine(JSON.stringify({ time: TIME, pid: 1, hostname: 'h', ...record }));

test('timestamps like log4j %d{ISO8601}: local time, comma before the millis', () => {
    assert.equal(formatTimestamp(TIME), TS);
});

test('the repository layout: ids, padded level, logger, thread, message', () => {
    assert.equal(
        line({ level: 40, requestId: REQUEST_ID, traceId: 'trace-1', spanId: 'span-1', reqId: REQUEST_ID, msg: 'hello' }),
        `${TS} {RequestId=${REQUEST_ID} SpanId=span-1 TraceId=trace-1} WARN  [frontend-server] [main] hello\n`,
    );
});

test('only the ids present, and no id block without any', () => {
    assert.equal(line({ level: 30, requestId: REQUEST_ID, msg: 'x' }), `${TS} {RequestId=${REQUEST_ID}} INFO  [frontend-server] [main] x\n`);
    assert.equal(line({ level: 50, msg: 'x' }), `${TS} ERROR [frontend-server] [main] x\n`);
});

test('the name binding is the logger, further fields follow the message as key=value', () => {
    assert.equal(
        line({ level: 20, name: 'config-cache', msg: 'backend unreachable', source: 'poll', attempt: 3, domains: ['a', 'b'] }),
        `${TS} DEBUG [config-cache] [main] backend unreachable source=poll attempt=3 domains=["a","b"]\n`,
    );
});

test('newlines are stripped from the line, an error follows as its stack', () => {
    const err = { type: 'Error', message: 'boom', stack: 'Error: boom\n    at x (file.ts:1:1)' };
    assert.equal(
        line({ level: 50, msg: 'two\nlines', err }),
        `${TS} ERROR [frontend-server] [main] twolines\nError: boom\n    at x (file.ts:1:1)\n`,
    );
});

test('a line that is not pino json is passed on unchanged', () => {
    assert.equal(formatLogLine('plain text'), 'plain text\n');
});

test('wired into the fastify logger: child bindings and serialized errors end up in the layout', async () => {
    const written: string[] = [];
    const app = Fastify({ logger: { level: 'info', stream: createLogStream({ write: (chunk: string) => written.push(chunk) }) } });
    await app.ready();
    written.length = 0;
    app.log.child({ name: 'config-cache', requestId: REQUEST_ID }).warn({ err: new Error('boom') }, 'loading failed');
    assert.equal(written.length, 1);
    const [first, second] = written[0]!.split('\n');
    assert.match(first!, new RegExp(`^\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d,\\d{3} \\{RequestId=${REQUEST_ID}\\} WARN  \\[config-cache\\] \\[main\\] loading failed$`));
    assert.equal(second, 'Error: boom');
    await app.close();
});
