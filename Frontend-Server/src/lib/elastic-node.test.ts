import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { ElasticSearcher, SearchRequest } from './elastic-client.ts';
import { loadNode, normalizeProperties, previewUrl, primaryLanguage, publicNodeQuery } from './elastic-node.ts';

const CONTEXT = { index: 'workspace_11.0', appBaseUrl: 'https://repo.example.org/edu-sharing', language: 'de' };

/** Hands out one `_source` and records the requests, so the query can be asserted. */
function fakeElastic(source: Record<string, unknown> | undefined) {
    const requests: SearchRequest[] = [];
    const client: ElasticSearcher = {
        firstSource: async (request) => {
            requests.push(request);
            return source;
        },
    };
    return { client, requests };
}

test('the query filters on the node id, public read and the workspace store', () => {
    const query = publicNodeQuery('workspace_11.0', 'n1');
    assert.equal(query.index, 'workspace_11.0');
    assert.deepEqual(query._source, ['properties']);
    assert.deepEqual(query.query?.bool?.filter, [
        { ids: { values: ['n1'] } },
        { term: { 'permissions.read': 'GROUP_EVERYONE' } },
        { term: { 'nodeRef.storeRef.protocol': 'workspace' } },
    ]);
});

test('a node that is not public or not in the workspace store never reaches us', async () => {
    // both cases look the same from here: the filters are part of the query, so
    // elastic answers with no hit at all - there is nothing to forget to check
    const { client } = fakeElastic(undefined);
    assert.equal(await loadNode(client, { kind: 'render', nodeId: 'private', version: undefined }, CONTEXT), undefined);
    assert.equal(await loadNode(client, { kind: 'collection', nodeId: 'archived' }, CONTEXT), undefined);
});

test('a versioned render url is not enriched and asks elastic nothing', async () => {
    const { client, requests } = fakeElastic({ properties: { 'cclom:title': 'current' } });
    assert.equal(await loadNode(client, { kind: 'render', nodeId: 'n1', version: '1.0' }, CONTEXT), undefined);
    assert.equal(requests.length, 0);
});

test('derives mimetype, preview and license from the raw properties', async () => {
    const { client } = fakeElastic({
        properties: {
            'cclom:title': 'Photosynthesis',
            'cclom:format': 'application/pdf',
            'ccm:commonlicense_key': ['CC_BY'],
            'ccm:commonlicense_cc_version': '4.0',
        },
    });
    const loaded = await loadNode(client, { kind: 'render', nodeId: 'n1', version: undefined }, CONTEXT);
    assert.equal(loaded?.mimetype, 'application/pdf');
    assert.equal(
        loaded?.previewUrl,
        'https://repo.example.org/edu-sharing/preview?nodeId=n1&storeProtocol=workspace&storeId=SpacesStore',
    );
    // 4.0 carries no country segment, the language comes from the request
    assert.equal(loaded?.licenseUrl, 'https://creativecommons.org/licenses/by/4.0/deed.de');
});

test('a license key without a url yields none, rather than a broken link', async () => {
    const { client } = fakeElastic({ properties: { 'ccm:commonlicense_key': ['CUSTOM'] } });
    const loaded = await loadNode(client, { kind: 'render', nodeId: 'n1', version: undefined }, CONTEXT);
    assert.equal(loaded?.licenseUrl, undefined);
});

test('scalar, array and numeric property values all become arrays of strings', () => {
    // the index stores what was indexed; `properties[key]?.[0]` on a bare string would
    // silently yield its first character
    const properties = normalizeProperties({
        'cclom:title': 'Photosynthesis',
        'ccm:educontextname': ['default', 'school'],
        'cm:modified': 1790085148522,
        'ccm:empty': [],
        'ccm:nothing': null,
    });
    assert.deepEqual(properties['cclom:title'], ['Photosynthesis']);
    assert.deepEqual(properties['ccm:educontextname'], ['default', 'school']);
    assert.deepEqual(properties['cm:modified'], ['1790085148522']);
    assert.equal(properties['ccm:empty'], undefined);
    assert.equal(properties['ccm:nothing'], undefined);
});

test('_LONG is aliased onto the plain property, which is what the index holds', () => {
    // the REST api exposed the epoch millis as cm:created_LONG; the lrmi date fields
    // and custom exporter.lrmi mappings still ask for that name
    const properties = normalizeProperties({ 'cm:created': 1763387774186 });
    assert.deepEqual(properties['cm:created_LONG'], ['1763387774186']);
    assert.deepEqual(properties['cm:created'], ['1763387774186']);
});

test('an existing _LONG value is not overwritten by the alias', () => {
    const properties = normalizeProperties({ 'cm:created': 'formatted', 'cm:created_LONG': 1763387774186 });
    assert.deepEqual(properties['cm:created_LONG'], ['1763387774186']);
});

test('normalizing anything that is not an object yields no properties', () => {
    for (const value of [undefined, null, 'x', 42]) {
        assert.deepEqual(normalizeProperties(value), {});
    }
});

test('the preview url escapes the node id', () => {
    assert.match(previewUrl('https://x/edu-sharing', 'a b&c'), /nodeId=a%20b%26c&/);
});

test('the license language is the primary tag of Accept-Language', () => {
    assert.equal(primaryLanguage('de-DE,de;q=0.9,en;q=0.8'), 'de');
    assert.equal(primaryLanguage('EN'), 'en');
    assert.equal(primaryLanguage('*'), undefined);
    assert.equal(primaryLanguage(undefined), undefined);
});
