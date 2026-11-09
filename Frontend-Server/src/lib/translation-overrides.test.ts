import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { FastifyBaseLogger } from 'fastify';
import type { BackendResponse, JsonGetter } from './backend-client.ts';
import { applyOverrides, TranslationOverrides, UnknownContextError } from './translation-overrides.ts';

const log = { debug() {}, info() {}, warn() {}, error() {} } as unknown as FastifyBaseLogger;

const values = {
    currentBackend: {
        language: [
            { language: 'de', string: [{ key: 'SAVE', value: 'Sichern' }, { key: 'WORKSPACE.TITLE', value: 'Global' }] },
            { language: 'de', string: [{ key: 'SAVE', value: 'ignored, the first block wins' }] },
            { language: 'en', string: [{ key: 'SAVE', value: 'Store' }] },
        ],
    },
};
const contexts = [
    {
        id: 'ctx-a',
        domain: ['A.example.org'],
        language: [
            { language: 'de', string: [{ key: 'WORKSPACE.TITLE', value: 'Kontext A' }] },
            // a language the global config has no block for - the backend drops these
            { language: 'fr', string: [{ key: 'SAVE', value: 'Enregistrer' }] },
        ],
    },
    { id: 'ctx-b', domain: ['b.example.org'] },
    null,
];

function backend(): JsonGetter & { calls: string[] } {
    const calls: string[] = [];
    return {
        calls,
        getJson: async (path: string): Promise<BackendResponse> => {
            calls.push(path);
            const body = path.includes('/values') ? values : path.includes('/context/') ? contexts : undefined;
            return { status: body === undefined ? 404 : 200, headers: {}, body };
        },
    };
}

function overrides(client: JsonGetter, version = { value: 1 }, now = { value: 0 }) {
    return new TranslationOverrides(client, {
        contextPath: '/edu-sharing',
        configVersion: () => version.value,
        maxAgeMs: 1000,
        log,
        now: () => now.value,
    });
}

test('global overrides of the base language, first block wins', async () => {
    const resolved = await overrides(backend()).resolve('de-informal', 'other.example.org');
    assert.deepEqual(resolved.overrides, { SAVE: 'Sichern', 'WORKSPACE.TITLE': 'Global' });
    assert.equal(resolved.contextId, undefined);
    assert.equal(resolved.complete, true);
});

test('the context of the domain on top of the global ones, domain case insensitive', async () => {
    const resolved = await overrides(backend()).resolve('de', 'a.EXAMPLE.org');
    assert.deepEqual(resolved.overrides, { SAVE: 'Sichern', 'WORKSPACE.TITLE': 'Kontext A' });
    assert.equal(resolved.contextId, 'ctx-a');
});

test('a context given by id wins over the domain', async () => {
    const source = overrides(backend());
    assert.equal((await source.resolve('de', 'b.example.org', 'ctx-a')).contextId, 'ctx-a');
});

test('an unknown id: one refetch once the snapshot is old enough, then UnknownContextError', async () => {
    const client = backend();
    const now = { value: 0 };
    const source = overrides(client, { value: 1 }, now);
    // young snapshot: no refetch
    await assert.rejects(source.resolve('de', 'b.example.org', 'nope'), UnknownContextError);
    assert.equal(client.calls.length, 2);
    // old enough (maxAge of the test is 1s, the refetch window 10s - the maxAge refetch comes first)
    now.value = 500;
    await assert.rejects(source.resolve('de', 'b.example.org', 'nope'), UnknownContextError);
    assert.equal(client.calls.length, 2);
});

test('a language only the context overrides counts', async () => {
    const resolved = await overrides(backend()).resolve('fr', 'a.example.org');
    assert.deepEqual(resolved.overrides, { SAVE: 'Enregistrer' });
});

test('one snapshot for all requests, refetched after a config reload or maxAge', async () => {
    const client = backend();
    const version = { value: 1 };
    const now = { value: 0 };
    const source = overrides(client, version, now);
    const first = await source.resolve('de', 'x');
    await source.resolve('en', 'y');
    assert.equal(client.calls.length, 2);

    version.value = 2;
    // stale while revalidate: the old snapshot right away
    assert.equal((await source.resolve('de', 'x')).version, first.version);
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(client.calls.length, 4);
    assert.notEqual((await source.resolve('de', 'x')).version, first.version);

    now.value = 5000;
    await source.resolve('de', 'x');
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(client.calls.length, 6);
});

test('backend unreachable without a snapshot: no overrides, marked incomplete', async () => {
    const source = overrides({
        getJson: async () => {
            throw new Error('ECONNREFUSED');
        },
    });
    const resolved = await source.resolve('de', 'x');
    assert.deepEqual(resolved.overrides, {});
    assert.equal(resolved.complete, false);
});

test('applyOverrides: dotted keys into the tree, copy on write', () => {
    const defaults = Object.freeze({
        SAVE: 'Speichern',
        WORKSPACE: Object.freeze({ TITLE: 'Arbeitsbereich', TOAST: Object.freeze({ SAVED: 'Gespeichert' }) }),
        OTHER: Object.freeze({ A: 'a' }),
    });
    const merged = applyOverrides(defaults, {
        'WORKSPACE.TOAST.SAVED': 'Gesichert',
        'NEW.KEY': 'neu',
        SAVE: 'Sichern',
    });
    assert.deepEqual(merged, {
        SAVE: 'Sichern',
        WORKSPACE: { TITLE: 'Arbeitsbereich', TOAST: { SAVED: 'Gesichert' } },
        OTHER: { A: 'a' },
        NEW: { KEY: 'neu' },
    });
    // untouched subtrees are shared, the defaults unchanged
    assert.equal(merged.OTHER, defaults.OTHER);
    assert.equal(defaults.WORKSPACE.TOAST.SAVED, 'Gespeichert');
});

test('applyOverrides: an override turning a text into a subtree is skipped', () => {
    const merged = applyOverrides({ SAVE: 'Speichern' }, { 'SAVE.SUB': 'x', OK: 'ok' });
    assert.deepEqual(merged, { SAVE: 'Speichern', OK: 'ok' });
});
