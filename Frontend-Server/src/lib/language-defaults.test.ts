import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import zlib from 'node:zlib';
import { after, before, test } from 'node:test';
import { isLanguage, LanguageDefaults, scopeOrder } from './language-defaults.ts';

let root: string;

function write(scope: string, language: string, content: unknown): void {
    fs.mkdirSync(path.join(root, scope), { recursive: true });
    fs.writeFileSync(path.join(root, scope, `${language}.json`), typeof content === 'string' ? content : JSON.stringify(content));
}

before(() => {
    root = fs.mkdtempSync(path.join(os.tmpdir(), 'es-language-defaults-'));
    write('admin', 'de', { ADMIN: 'Admin', SAVE: 'Speichern (admin)' });
    write('common', 'de', { SAVE: 'Speichern', NESTED: { A: 'a' } });
    write('workspace', 'de', { SAVE: 'Speichern (workspace)', WORKSPACE: 'Arbeitsbereich' });
    write('admin', 'fr', { ADMIN: 'Admin' });
    write('common', 'fr', '');
    write('admin', 'de-informal', { ADMIN: 'Admin du' });
    fs.writeFileSync(path.join(root, 'README.md'), 'not a scope');
});

after(() => {
    fs.rmSync(root, { recursive: true, force: true });
});

test('scopeOrder: alphabetical', () => {
    assert.deepEqual(scopeOrder(['workspace', 'common', 'admin']), ['admin', 'common', 'workspace']);
});

test('isLanguage: only plain language tags', () => {
    for (const ok of ['de', 'en', 'de-informal', 'de-no-binnen-i']) {
        assert.equal(isLanguage(ok), true, ok);
    }
    for (const bad of ['', 'x', '../de', 'de/../../etc', 'DE', 'de.json', 'de_DE']) {
        assert.equal(isLanguage(bad), false, bad);
    }
});

test('merge: top level keys of every scope, a later scope wins', async () => {
    const merged = await new LanguageDefaults(root, { revalidate: false }).merge('de');
    assert.deepEqual(merged, {
        ADMIN: 'Admin',
        WORKSPACE: 'Arbeitsbereich',
        SAVE: 'Speichern (workspace)',
        NESTED: { A: 'a' },
    });
});

test('merge: scopes without the language and empty files are skipped', async () => {
    const defaults = new LanguageDefaults(root, { revalidate: false });
    assert.deepEqual(await defaults.merge('fr'), { ADMIN: 'Admin' });
    // no de fallback for a de-* variant - the app has it as its fallback language
    assert.deepEqual(await defaults.merge('de-informal'), { ADMIN: 'Admin du' });
});

test('get: unknown or invalid language -> undefined', async () => {
    const defaults = new LanguageDefaults(root, { revalidate: false });
    assert.equal(await defaults.get('it'), undefined);
    assert.equal(await defaults.get('../de'), undefined);
});

test('get: bundle in identity, brotli and gzip', async () => {
    const bundle = await new LanguageDefaults(root, { revalidate: false }).get('de');
    assert.ok(bundle);
    const expected = await new LanguageDefaults(root, { revalidate: true }).merge('de');
    assert.deepEqual(JSON.parse(bundle.identity.toString('utf8')), expected);
    assert.deepEqual(zlib.brotliDecompressSync(bundle.br), bundle.identity);
    assert.deepEqual(zlib.gunzipSync(bundle.gzip), bundle.identity);
});

test('get: cached, rebuilt on a changed file with revalidate', async () => {
    const cached = new LanguageDefaults(root, { revalidate: false });
    const uncached = new LanguageDefaults(root, { revalidate: true });
    assert.equal(await cached.get('de'), await cached.get('de'));
    const first = await uncached.get('de');
    // unchanged files: the same bundle, not rebuilt
    assert.equal(await uncached.get('de'), first);
    write('workspace', 'de', { WORKSPACE: 'geändert' });
    try {
        const second = await uncached.get('de');
        assert.notDeepEqual(first?.identity, second?.identity);
        assert.match(second!.identity.toString('utf8'), /geändert/);
    } finally {
        write('workspace', 'de', { SAVE: 'Speichern (workspace)', WORKSPACE: 'Arbeitsbereich' });
    }
});

test('get: invalid json rejects, the next call tries again', async () => {
    write('broken', 'en', '{');
    const defaults = new LanguageDefaults(root, { revalidate: false });
    try {
        await assert.rejects(defaults.get('en'));
        write('broken', 'en', { OK: 'ok' });
        assert.deepEqual(JSON.parse((await defaults.get('en'))!.identity.toString('utf8')), { OK: 'ok' });
    } finally {
        fs.rmSync(path.join(root, 'broken'), { recursive: true, force: true });
    }
});
