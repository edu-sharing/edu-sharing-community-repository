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
    // guest.enabled false without a value: the reference config defaults
    // repository.guest.enabled to false, so an older backend must not unlock it
    const empty = {
        branding: false,
        lrmi: { properties: {}, vcard: {} },
        angular: { headers: {}, customHtmlHeaders: '' },
        guest: { enabled: false },
    };
    assert.deepEqual(parseRepositoryConfig({ current: {} }), empty);
    assert.deepEqual(parseRepositoryConfig('garbage'), empty);
});

test('guest access is read from repository.guest.enabled, and only true unlocks it', () => {
    const guestOf = (currentBackend: unknown) => parseRepositoryConfig({ currentBackend }).guest.enabled;
    assert.equal(guestOf({ repository: { guest: { enabled: true } } }), true);
    assert.equal(guestOf({ repository: { guest: { enabled: false } } }), false);
    assert.equal(guestOf({ repository: { guest: {} } }), false);
    assert.equal(guestOf({ repository: {} }), false);
    assert.equal(guestOf({}), false);
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
    assert.deepEqual(config.angular.headers, { 'X-Frame-Options': 'sameorigin', 'Content-Security-Policy': { 'default-src': "'self'" } });
    assert.equal(config.angular.customHtmlHeaders, '<meta name="x">');
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

test('refetches when lastCacheUpdate increases', async () => {
    const { client, state, valueCalls } = fakeBackend();
    const subject = cache(client);
    await subject.poll();
    assert.equal((await subject.get('a', {}))?.branding, true);

    state.branding = false;
    await subject.poll(); // unchanged -> still cached
    assert.equal((await subject.get('a', {}))?.branding, true);

    state.lastCacheUpdate = 2;
    await subject.poll();
    // stale-while-revalidate: the old one once more, the new one after the background fetch
    assert.equal((await subject.get('a', {}))?.branding, true);
    await subject.idle();
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
    await subject.idle();
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

test('unreachable after a failed or 5xx _about poll, reachable again after a good one', async () => {
    let answer: 'ok' | 'down' | 'starting' = 'ok';
    const client: JsonGetter = {
        getJson: async () => {
            if (answer === 'down') {
                throw Object.assign(new Error('connect ECONNREFUSED'), { code: 'ECONNREFUSED' });
            }
            return answer === 'starting' ? { status: 503, headers: {}, body: undefined } : ok({ lastCacheUpdate: 1 });
        },
    };
    const subject = cache(client);
    assert.equal(subject.isBackendReachable(), true);
    answer = 'down';
    await subject.poll();
    assert.equal(subject.isBackendReachable(), false);
    answer = 'ok';
    await subject.poll();
    assert.equal(subject.isBackendReachable(), true);
    answer = 'starting';
    await subject.poll();
    assert.equal(subject.isBackendReachable(), false);
});

test('a config request that cannot connect marks the backend unreachable, a slow one does not', async () => {
    let error: Error = Object.assign(new Error('connect ECONNREFUSED'), { code: 'ECONNREFUSED' });
    const client: JsonGetter = {
        getJson: async () => {
            throw error;
        },
    };
    const subject = cache(client);
    error = new Error('backend call /edu-sharing/rest/config/v1/values timed out');
    assert.equal(await subject.get('a.example.org', {}), null);
    assert.equal(subject.isBackendReachable(), true);
    error = Object.assign(new Error('connect ECONNREFUSED'), { code: 'ECONNREFUSED' });
    assert.equal(await subject.get('b.example.org', {}), null);
    assert.equal(subject.isBackendReachable(), false);
});

test('while unreachable, the background polling retries faster', async () => {
    let down = true;
    let polls = 0;
    const client: JsonGetter = {
        getJson: async () => {
            polls++;
            if (down) {
                throw Object.assign(new Error('connect ECONNREFUSED'), { code: 'ECONNREFUSED' });
            }
            return ok({ lastCacheUpdate: 1 });
        },
    };
    const subject = new ConfigCache(client, {
        contextPath: '/edu-sharing',
        pollIntervalMs: 60_000,
        unreachableRetryMs: 10,
        maxAgeMs: 1000,
        log: Fastify().log,
    });
    subject.start();
    try {
        for (let attempt = 0; attempt < 100 && polls < 3; attempt++) {
            await new Promise((resolve) => setTimeout(resolve, 10));
        }
        assert.ok(polls >= 3, `expected fast retries, got ${polls} polls`);
        down = false;
        for (let attempt = 0; attempt < 100 && !subject.isBackendReachable(); attempt++) {
            await new Promise((resolve) => setTimeout(resolve, 10));
        }
        assert.equal(subject.isBackendReachable(), true);
    } finally {
        subject.stop();
    }
});

test('peek hands out the cached config without asking the backend', async () => {
    const { client, valueCalls } = fakeBackend();
    const subject = cache(client);
    assert.equal(subject.peek('repo.example.org'), null);
    await subject.get('repo.example.org', {});
    assert.equal(subject.peek('repo.example.org')?.branding, true);
    assert.equal(valueCalls(), 1);
});

test('a single slow _about poll keeps the backend reachable, repeated ones do not', async () => {
    const reasons: string[] = [];
    const client: JsonGetter = {
        getJson: async () => {
            throw new Error('backend call /edu-sharing/rest/_about timed out');
        },
    };
    const subject = cache(client);
    subject.onUnreachable((source) => reasons.push(source));
    await subject.poll();
    await subject.poll();
    assert.equal(subject.isBackendReachable(), true, 'two timeouts are not an outage');
    await subject.poll();
    assert.equal(subject.isBackendReachable(), false, 'the third one is');
    assert.deepEqual(reasons, ['poll']);
});

test('a good poll in between resets the count of failed polls', async () => {
    let slow = true;
    const client: JsonGetter = {
        getJson: async () => {
            if (slow) {
                throw new Error('timed out');
            }
            return ok({ lastCacheUpdate: 1 });
        },
    };
    const subject = cache(client);
    await subject.poll();
    await subject.poll();
    slow = false;
    await subject.poll();
    slow = true;
    await subject.poll();
    await subject.poll();
    assert.equal(subject.isBackendReachable(), true);
});

test('the listener hears each turn to unreachable once, with its source', async () => {
    const { client } = fakeBackend();
    const subject = cache(client);
    const sources: string[] = [];
    subject.onUnreachable((source) => sources.push(source));
    subject.markUnreachable('proxy', 'ECONNREFUSED');
    subject.markUnreachable('proxy', 'ECONNREFUSED');
    await subject.poll();
    subject.markUnreachable('config', 'ECONNREFUSED');
    assert.deepEqual(sources, ['proxy', 'config']);
});

test('the _about poll uses the probe client, the config fetch the regular one', async () => {
    const { client, state } = fakeBackend();
    const probed: string[] = [];
    const probe: JsonGetter = {
        getJson: async (path, headers) => {
            probed.push(path);
            return client.getJson(path, headers);
        },
    };
    const subject = new ConfigCache(client, { contextPath: '/edu-sharing', pollIntervalMs: 0, maxAgeMs: 1000, log: Fastify().log, probe });
    await subject.poll();
    await subject.get('repo.example.org', {});
    assert.deepEqual(probed, [ABOUT]);
    assert.deepEqual(
        state.calls.map((call) => call.path),
        [ABOUT, VALUES],
    );
});

test('an outdated entry is handed out without waiting for its refetch', async () => {
    let release: (() => void) | undefined;
    let slow = false;
    const { client, state } = fakeBackend();
    const gated: JsonGetter = {
        getJson: async (path, headers) => {
            if (slow) {
                await new Promise<void>((resolve) => (release = resolve));
            }
            return client.getJson(path, headers);
        },
    };
    let now = 0;
    const subject = cache(gated, () => now);
    assert.equal((await subject.get('a', {}))?.branding, true);

    slow = true;
    state.branding = false;
    now = 5000;
    // resolves although the backend has not answered yet
    assert.equal((await subject.get('a', {}))?.branding, true);
    assert.equal((await subject.get('a', {}))?.branding, true, 'a second request joins the running fetch');

    release?.();
    await subject.idle();
    assert.equal((await subject.get('a', {}))?.branding, false);
});

test('without any entry the first request waits for the fetch', async () => {
    let release: (() => void) | undefined;
    const { client } = fakeBackend();
    const gated: JsonGetter = {
        getJson: async (path, headers) => {
            await new Promise<void>((resolve) => (release = resolve));
            return client.getJson(path, headers);
        },
    };
    const subject = cache(gated);
    let settled = false;
    const first = subject.get('a', {}).then((config) => {
        settled = true;
        return config;
    });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(settled, false);
    release?.();
    assert.equal((await first)?.branding, true);
});

test('host names are case insensitive - one entry and one backend call for every spelling', async () => {
    const { client, valueCalls } = fakeBackend();
    const subject = cache(client);
    await subject.get('Repo.Example.ORG', {});
    await subject.get('repo.example.org', {});
    await subject.get('repo.example.org.', {});
    assert.equal(valueCalls(), 1);
    assert.deepEqual(subject.domains(), ['repo.example.org']);
    assert.equal(subject.peek('REPO.example.org')?.branding, true);
});

test('keeps at most maxDomains entries, least recently used first out', async () => {
    const { client } = fakeBackend();
    const subject = new ConfigCache(client, { contextPath: '/edu-sharing', pollIntervalMs: 0, maxAgeMs: 60_000, maxDomains: 2, log: Fastify().log });
    await subject.get('a.example.org', {});
    await subject.get('b.example.org', {});
    await subject.get('a.example.org', {});
    await subject.get('c.example.org', {});
    assert.deepEqual(subject.domains().sort(), ['a.example.org', 'c.example.org']);
});
