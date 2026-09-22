import assert from 'node:assert/strict';
import { test } from 'node:test';
import { collectTraceHeaders, ensureRequestId, isTraceHeader } from './tracing.ts';

test('recognises every header shape the backend looks for', () => {
    for (const name of [
        'X-B3-TraceId',
        'X-B3-SpanId',
        'X-B3-ParentSpanId',
        'X-B3-Sampled',
        'X-B3-Flags',
        'X-Ot-Span-Context',
        'X-Request-Id',
        'X-Client-Trace-Id',
    ]) {
        assert.equal(isTraceHeader(name), true, name);
    }
    assert.equal(isTraceHeader('Authorization'), false);
    assert.equal(isTraceHeader('X-Forwarded-For'), false);
});

test('collects trace headers lower-cased and leaves others behind', () => {
    const collected = collectTraceHeaders({
        'x-b3-traceid': 'abc',
        'X-Client-Trace-Id': 'client',
        authorization: 'Basic secret',
    } as never);
    assert.deepEqual(collected, { 'x-b3-traceid': 'abc', 'x-client-trace-id': 'client' });
});

test('keeps an incoming request id and generates one otherwise', () => {
    assert.equal(ensureRequestId({ 'x-request-id': 'given' }), 'given');
    assert.notEqual(ensureRequestId({}), ensureRequestId({}));
});
