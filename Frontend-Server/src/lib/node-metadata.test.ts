import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseShellTarget } from './node-metadata.ts';

test('parses render urls with and without version', () => {
    assert.deepEqual(parseShellTarget('/components/render/abc-123'), { kind: 'render', nodeId: 'abc-123', version: undefined });
    assert.deepEqual(parseShellTarget('/components/render/abc-123/1.2?closeOnBack=true'), {
        kind: 'render',
        nodeId: 'abc-123',
        version: '1.2',
    });
    // URLHelper.getNgRenderNodeUrl: -1 means latest
    assert.deepEqual(parseShellTarget('/components/render/abc/-1'), { kind: 'render', nodeId: 'abc', version: undefined });
});

test('parses collection urls from the id query parameter', () => {
    assert.deepEqual(parseShellTarget('/components/collections?scope=MY&id=c-1'), { kind: 'collection', nodeId: 'c-1' });
    assert.equal(parseShellTarget('/components/collections'), undefined);
    // NgServlet matched "id=" as a substring - "nodeid=" must not count
    assert.equal(parseShellTarget('/components/collections?nodeid=c-1'), undefined);
});

test('ignores every other shell url and anything that is no node id', () => {
    for (const url of ['/', '/components/workspace', '/components/render', '/components/render/..%2Fadmin', '/v/abc']) {
        assert.equal(parseShellTarget(url), undefined, url);
    }
});
