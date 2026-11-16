import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseShellPage, parseShellTarget } from './node-metadata.ts';

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

test('pages: exactly components/<name>, whatever the query', () => {
    assert.equal(parseShellPage('/components/search'), 'search');
    assert.equal(parseShellPage('/components/search?q=physics'), 'search');
    assert.equal(parseShellPage('/components/start'), 'start');
    assert.equal(parseShellPage('/components/my-page/'), 'my-page');
    // the collections overview is a page, a collection is a node view (parseShellTarget)
    assert.equal(parseShellPage('/components/collections'), 'collections');
});

test('pages: no node view, no error page, nothing nested, nothing odd', () => {
    for (const url of [
        '/',
        '/components',
        '/components/render',
        '/components/render/abc',
        '/components/error',
        '/components/error/404',
        '/components/search/results',
        '/components/Search',
        '/components/1abc',
        '/components/..%2Fadmin',
        '/components/a_b',
        '/v/abc',
        '/assets/x',
    ]) {
        assert.equal(parseShellPage(url), undefined, url);
    }
});
