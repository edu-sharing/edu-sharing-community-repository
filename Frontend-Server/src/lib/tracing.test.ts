import assert from 'node:assert/strict';
import { test } from 'node:test';
import { collectTraceHeaders, ensureRequestId, isTraceHeader, isValidRequestId } from './tracing.ts';

test('recognises every header shape the backend looks for', () => {
    for (const name of [
        'X-B3-TraceId',
        'X-B3-SpanId',
        'X-B3-ParentSpanId',
        'X-B3-Sampled',
        'X-B3-Flags',
        'X-Ot-Span-Context',
        'X-Request-Id',
    ]) {
        assert.equal(isTraceHeader(name), true, name);
    }
    assert.equal(isTraceHeader('Authorization'), false);
    assert.equal(isTraceHeader('X-Forwarded-For'), false);
    // the old client generated id - the request id replaced it
    assert.equal(isTraceHeader('X-Client-Trace-Id'), false);
});

test('collects trace headers lower-cased and leaves others behind', () => {
    const collected = collectTraceHeaders({
        'x-b3-traceid': 'abc',
        'X-Request-Id': 'request',
        authorization: 'Basic secret',
    } as never);
    assert.deepEqual(collected, { 'x-b3-traceid': 'abc', 'x-request-id': 'request' });
});

test('keeps an incoming request id and generates one otherwise', () => {
    const given = '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b';
    assert.equal(ensureRequestId({ 'x-request-id': given }), given);
    assert.equal(ensureRequestId({ 'x-request-id': given.toUpperCase() }), given.toUpperCase());
    assert.notEqual(ensureRequestId({}), ensureRequestId({}));
});

test('replaces an incoming request id that is not a uuid', () => {
    for (const invalid of ['', 'req-1', `3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b${'x'.repeat(1000)}`, '3f2b8c1e5d4a4e6f9a7b1c2d3e4f5a6b']) {
        const id = ensureRequestId({ 'x-request-id': invalid });
        assert.notEqual(id, invalid);
        assert.equal(isValidRequestId(id), true);
    }
});
