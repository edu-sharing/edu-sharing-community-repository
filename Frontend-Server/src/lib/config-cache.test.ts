import assert from 'node:assert/strict';
import { test } from 'node:test';
import Fastify from 'fastify';
import type { BackendResponse, JsonGetter } from './backend-client.ts';
import { ConfigCache, isInternalConfig, parseRepositoryConfig } from './config-cache.ts';

const ABOUT = '/edu-sharing/rest/_about';
const VALUES = '/edu-sharing/rest/config/v1/values';

function configBody(branding: boolean) {
    return {
        current: { branding },
        currentBackend: { exporter: { lrmi: { properties: { name: 'cclom:title,cm:name' }, vcard: { creator: 'ccm:lifecyclecontributer_author' } } } },
    };
}

/** Mutable fake backend: `about` and `values` can be changed between calls. */
function fakeBackend() {
    const state = {
        lastCacheUpdate: 1 as number | undefined,
        branding: true,
        failValues: false,
        calls: [] as { path: string; host: unknown }[],
    };
    const client: JsonGetter = {
        getJson: async (path, headers = {}) => {
            state.calls.push({ path, host: headers['host'] });
            if (path === ABOUT) {
                return ok({ lastCacheUpdate: state.lastCacheUpdate });
            }
            if (path === VALUES) {
                if (state.failValues) {
                    throw new Error('backend down');
                }
                return ok(configBody(state.branding));
            }
            return { status: 404, headers: {}, body: undefined };
        },
    };
    const valueCalls = () => state.calls.filter((call) => call.path === VALUES).length;
    return { client, state, valueCalls };
}

function ok(body: unknown): BackendResponse {
    return { status: 200, headers: {}, body };
}

function cache(client: JsonGetter, now = () => 0) {
    return new ConfigCache(client, { contextPath: '/edu-sharing', pollIntervalMs: 0, maxAgeMs: 1000, log: Fastify().log, now });
}

test('parses branding and the lrmi mapping from the config values', () => {
    const config = parseRepositoryConfig(configBody(true));
    assert.equal(config.branding, true);
    assert.deepEqual(config.lrmi.properties, { name: 'cclom:title,cm:name' });
    assert.deepEqual(config.lrmi.vcard, { creator: 'ccm:lifecyclecontributer_author' });
});

test('an older backend without the mapping yields an empty one, not an error', () => {
    const empty = { branding: false, lrmi: { properties: {}, vcard: {} }, angularHeaders: {}, customHtmlHeaders: '' };
    assert.deepEqual(parseRepositoryConfig({ current: {} }), empty);
    assert.deepEqual(parseRepositoryConfig('garbage'), empty);
});

test('parses the internal angular block: security headers and custom html headers', () => {
    const config = parseRepositoryConfig({
        currentBackend: {
            angular: {
                headers: { 'X-Frame-Options': 'sameorigin', 'Content-Security-Policy': { 'default-src': "'self'" } },
                customHtmlHeaders: '<meta name="x">',
            },
        },
    });
    assert.deepEqual(config.angularHeaders, { 'X-Frame-Options': 'sameorigin', 'Content-Security-Policy': { 'default-src': "'self'" } });
    assert.equal(config.customHtmlHeaders, '<meta name="x">');
});

test('tells the internal config from the public one', () => {
    assert.equal(isInternalConfig({ currentBackend: { angular: {} } }), true);
    assert.equal(isInternalConfig({ currentBackend: { repository: {} } }), false);
});

test('caches per domain and sends the domain as Host', async () => {
    const { client, state, valueCalls } = fakeBackend();
    const subject = cache(client);
    await subject.get('a.example.org', { host: 'a.example.org' });
    await subject.get('a.example.org', { host: 'a.example.org' });
    await subject.get('b.example.org', { host: 'b.example.org' });
    assert.equal(valueCalls(), 2);
    assert.deepEqual(
        state.calls.map((call) => call.host),
        ['a.example.org', 'b.example.org'],
    );
});

test('concurrent requests for one domain share a single backend call', async () => {
    const { client, valueCalls } = fakeBackend();
    const subject = cache(client);
    await Promise.all([subject.get('a', {}), subject.get('a', {}), subject.get('a', {})]);
    assert.equal(valueCalls(), 1);
});

test('flushes when lastCacheUpdate increases', async () => {
    const { client, state, valueCalls } = fakeBackend();
    const subject = cache(client);
    await subject.poll();
    assert.equal((await subject.get('a', {}))?.branding, true);

    state.branding = false;
    await subject.poll(); // unchanged -> still cached
    assert.equal((await subject.get('a', {}))?.branding, true);

    state.lastCacheUpdate = 2;
    await subject.poll();
    assert.equal((await subject.get('a', {}))?.branding, false);
    assert.equal(valueCalls(), 2);
});

test('a lower lastCacheUpdate - another cluster pod - does not flush', async () => {
    const { client, state, valueCalls } = fakeBackend();
    const subject = cache(client);
    state.lastCacheUpdate = 5;
    await subject.poll();
    await subject.get('a', {});
    state.lastCacheUpdate = 3;
    await subject.poll();
    state.lastCacheUpdate = 5;
    await subject.poll();
    await subject.get('a', {});
    assert.equal(valueCalls(), 1);
});

test('refetches after maxAge even without a refresh signal', async () => {
    const { client, valueCalls } = fakeBackend();
    let now = 0;
    const subject = cache(client, () => now);
    await subject.get('a', {});
    now = 999;
    await subject.get('a', {});
    now = 1000;
    await subject.get('a', {});
    assert.equal(valueCalls(), 2);
});

test('a failing backend keeps the stale entry, or yields null without one', async () => {
    const { client, state } = fakeBackend();
    let now = 0;
    const subject = cache(client, () => now);
    state.failValues = true;
    assert.equal(await subject.get('a', {}), null);

    state.failValues = false;
    assert.equal((await subject.get('a', {}))?.branding, true);

    state.failValues = true;
    now = 5000;
    assert.equal((await subject.get('a', {}))?.branding, true);
});

test('a failing _about poll is swallowed', async () => {
    const client: JsonGetter = {
        getJson: async () => {
            throw new Error('down');
        },
    };
    await cache(client).poll();
});

test('ready once the backend answered _about, and stays ready when it fails later', async () => {
    const { client, state } = fakeBackend();
    let failing = false;
    const flaky: JsonGetter = {
        getJson: async (path, headers) => {
            if (failing) {
                throw new Error('down');
            }
            return client.getJson(path, headers);
        },
    };
    const subject = new ConfigCache(flaky, { contextPath: '/edu-sharing', pollIntervalMs: 30_000, maxAgeMs: 1000, log: Fastify().log });
    failing = true;
    await subject.poll();
    assert.equal(subject.isReady(), false);
    failing = false;
    await subject.poll();
    assert.equal(subject.isReady(), true);
    failing = true;
    state.lastCacheUpdate = 2;
    await subject.poll();
    assert.equal(subject.isReady(), true);
});

test('always ready without polling', () => {
    const { client } = fakeBackend();
    assert.equal(cache(client).isReady(), true);
});
