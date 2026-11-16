import assert from 'node:assert/strict';
import { test } from 'node:test';
import Fastify from 'fastify';
import type { Defaults } from './language-defaults.ts';
import type { ResolvedOverrides } from './translation-overrides.ts';
import { Translations } from './translations.ts';

const log = Fastify().log;

function setup(options: { overrides?: Record<string, string>; complete?: boolean; version?: string } = {}) {
    const calls = { defaults: 0, overrides: 0 };
    const state = { overrides: options.overrides ?? {}, complete: options.complete ?? true, version: options.version ?? '1' };
    const translations = new Translations({
        defaults: {
            get: async (language: string): Promise<Defaults | undefined> => {
                calls.defaults++;
                return language === 'de'
                    ? { translations: { SAVE: 'Speichern', SEO: { START: { TITLE: 'Start', DESCRIPTION: 'Beschreibung' } } }, version: 'd1' }
                    : undefined;
            },
        },
        overrides: {
            resolve: async (): Promise<ResolvedOverrides> => {
                calls.overrides++;
                return { overrides: state.overrides, version: state.version, complete: state.complete };
            },
        },
        log,
    });
    return { translations, calls, state };
}

test('section: the entry of the defaults, only that', async () => {
    const { translations } = setup();
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), { START: { TITLE: 'Start', DESCRIPTION: 'Beschreibung' } });
});

test('section: an override wins and adds, those of other entries stay out', async () => {
    const { translations } = setup({
        overrides: { 'SEO.START.TITLE': 'Mediathek', 'SEO.SEARCH.TITLE': 'Suche', SAVE: 'Sichern' },
    });
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), {
        START: { TITLE: 'Mediathek', DESCRIPTION: 'Beschreibung' },
        SEARCH: { TITLE: 'Suche' },
    });
});

test('section: the defaults stay untouched by an override', async () => {
    const { translations, state } = setup({ overrides: { 'SEO.START.TITLE': 'A' }, version: '1' });
    await translations.section('de', 'repo.example.org', 'SEO');
    state.overrides = {};
    state.version = '2';
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), { START: { TITLE: 'Start', DESCRIPTION: 'Beschreibung' } });
});

test('section: an entry nobody has is empty, a language without files is undefined', async () => {
    const { translations } = setup();
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'NOTHING'), {});
    assert.equal(await translations.section('fr', 'repo.example.org', 'SEO'), undefined);
});

test('section: kept per version of the overrides - same version the same object, a new one is rebuilt', async () => {
    const { translations, state } = setup({ overrides: { 'SEO.START.TITLE': 'A' }, version: '1' });
    const first = await translations.section('de', 'repo.example.org', 'SEO');
    assert.equal(await translations.section('de', 'repo.example.org', 'SEO'), first);
    state.overrides = { 'SEO.START.TITLE': 'B' };
    state.version = '2';
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), { START: { TITLE: 'B', DESCRIPTION: 'Beschreibung' } });
});

test('section: without the overrides (backend unreachable) the defaults alone, and not kept', async () => {
    const { translations, state } = setup({ complete: false, version: 'none' });
    const first = await translations.section('de', 'repo.example.org', 'SEO');
    assert.deepEqual(first, { START: { TITLE: 'Start', DESCRIPTION: 'Beschreibung' } });
    // the backend is back: the next request must not get the section without the overrides
    state.complete = true;
    state.overrides = { 'SEO.START.TITLE': 'Mediathek' };
    state.version = '3';
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), { START: { TITLE: 'Mediathek', DESCRIPTION: 'Beschreibung' } });
});

test('section: an override that would turn a text into a subtree is skipped', async () => {
    const { translations } = setup({ overrides: { 'SEO.START.TITLE.MORE': 'x', 'SEO.START.DESCRIPTION': 'ok' } });
    assert.deepEqual(await translations.section('de', 'repo.example.org', 'SEO'), { START: { TITLE: 'Start', DESCRIPTION: 'ok' } });
});
