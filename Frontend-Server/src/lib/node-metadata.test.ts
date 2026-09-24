import assert from 'node:assert/strict';
import type { OutgoingHttpHeaders } from 'node:http';
import { test } from 'node:test';
import type { BackendResponse, JsonGetter } from './backend-client.ts';
import { loadNode, parseShellTarget } from './node-metadata.ts';

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

/** Answers from a path -> response table and records the calls. */
function fakeBackend(routes: Record<string, BackendResponse>) {
    const calls: { path: string; headers: OutgoingHttpHeaders }[] = [];
    const client: JsonGetter = {
        getJson: async (path: string, headers: OutgoingHttpHeaders = {}) => {
            calls.push({ path, headers });
            return routes[path] ?? { status: 404, headers: {}, body: undefined };
        },
    };
    return { client, calls };
}

const NODE_PATH = '/edu-sharing/rest/node/v1/nodes/-home-/n1/metadata?propertyFilter=-all-';
const NODE = { ref: { id: 'n1' }, properties: { 'cclom:title': ['current'] }, preview: { url: 'p' } };

test('loads the node with the given identity and hands on its Set-Cookie', async () => {
    const { client, calls } = fakeBackend({
        [NODE_PATH]: { status: 200, headers: { 'set-cookie': ['JSESSIONID=1'] }, body: { node: NODE } },
    });
    const loaded = await loadNode(client, '/edu-sharing', { kind: 'render', nodeId: 'n1', version: undefined }, { cookie: 'c' });
    assert.equal(loaded?.properties['cclom:title']?.[0], 'current');
    assert.deepEqual(loaded?.setCookie, ['JSESSIONID=1']);
    assert.equal(calls[0]?.headers['cookie'], 'c');
});

test('versioned render url: properties from the version, the rest from the node', async () => {
    const { client, calls } = fakeBackend({
        [NODE_PATH]: { status: 200, headers: {}, body: { node: NODE } },
        '/edu-sharing/rest/node/v1/nodes/-home-/n1/versions/1/0/metadata?propertyFilter=-all-': {
            status: 200,
            headers: {},
            body: { version: { properties: { 'cclom:title': ['old'] } } },
        },
    });
    const loaded = await loadNode(client, '/edu-sharing', { kind: 'render', nodeId: 'n1', version: '1.0' }, {});
    assert.equal(loaded?.properties['cclom:title']?.[0], 'old');
    assert.equal(loaded?.node.preview?.url, 'p');
    assert.equal(calls.length, 2);
});

test('a node the caller may not read yields undefined', async () => {
    const { client } = fakeBackend({ [NODE_PATH]: { status: 403, headers: {}, body: undefined } });
    assert.equal(await loadNode(client, '/edu-sharing', { kind: 'collection', nodeId: 'n1' }, {}), undefined);
});

test('an unparsable version skips the backend entirely', async () => {
    const { client, calls } = fakeBackend({});
    assert.equal(await loadNode(client, '/edu-sharing', { kind: 'render', nodeId: 'n1', version: 'latest' }, {}), undefined);
    assert.equal(calls.length, 0);
});
