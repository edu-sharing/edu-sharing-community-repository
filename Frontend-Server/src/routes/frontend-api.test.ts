import assert from 'node:assert/strict';
import zlib from 'node:zlib';
import { after, before, test } from 'node:test';
import Fastify, { type FastifyInstance } from 'fastify';
import type { TranslationBundle } from '../lib/translations.ts';
import { FRONTEND_API_PATH, frontendApi } from './frontend-api.ts';

const PREFIX = `/edu-sharing${FRONTEND_API_PATH}`;
const DE = Buffer.from(JSON.stringify({ SAVE: 'Speichern', NESTED: { A: 'a' } }));
const bundle: TranslationBundle = { identity: DE, br: zlib.brotliCompressSync(DE), gzip: zlib.gzipSync(DE), etag: '"abc"', complete: true };
const requested: string[] = [];
const contexts: (string | undefined)[] = [];

let app: FastifyInstance;

before(async () => {
    app = Fastify();
    await app.register(frontendApi, {
        prefix: PREFIX,
        translations: {
            get: async (language: string, _domain: string, contextId?: string) => {
                requested.push(language);
                contexts.push(contextId);
                return language === 'de' ? bundle : undefined;
            },
        },
        overrides: {
            resolve: async (_language: string, _domain: string, contextId?: string) => ({
                overrides: contextId === 'ctx' ? { SAVE: 'Sichern' } : {},
                version: '1',
                complete: true,
            }),
        },
    });
    await app.ready();
});

after(() => app.close());

test('translations: the bundle as json, cacheable, any origin', async () => {
    const response = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de`, headers: { 'accept-encoding': 'identity' } });
    assert.equal(response.statusCode, 200);
    assert.match(String(response.headers['content-type']), /^application\/json/);
    assert.deepEqual(response.json(), { SAVE: 'Speichern', NESTED: { A: 'a' } });
    assert.equal(response.headers['cache-control'], 'public, max-age=60');
    assert.equal(response.headers.etag, '"abc"');
    assert.equal(response.headers['access-control-allow-origin'], '*');
    assert.equal(response.headers['content-encoding'], undefined);
});

test('translations: brotli and gzip by Accept-Encoding', async () => {
    const br = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de`, headers: { 'accept-encoding': 'gzip, br' } });
    assert.equal(br.headers['content-encoding'], 'br');
    assert.deepEqual(zlib.brotliDecompressSync(br.rawPayload), DE);
    const gz = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de`, headers: { 'accept-encoding': 'gzip' } });
    assert.equal(gz.headers['content-encoding'], 'gzip');
    assert.deepEqual(zlib.gunzipSync(gz.rawPayload), DE);
});

test('translations: unknown language -> 404 json', async () => {
    const response = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/xx` });
    assert.equal(response.statusCode, 404);
    assert.equal(response.json().error, 'Not Found');
});

test('translations: invalid language -> 400, never reaches the file system', async () => {
    requested.length = 0;
    for (const language of ['..%2Fde', 'DE', 'de_DE', 'd']) {
        const response = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/${language}` });
        assert.equal(response.statusCode, 400, language);
    }
    assert.deepEqual(requested, []);
});

test('unknown path below the prefix -> 404 json, also for other methods', async () => {
    for (const method of ['GET', 'POST', 'DELETE'] as const) {
        const response = await app.inject({ method, url: `${PREFIX}/v2/whatever` });
        assert.equal(response.statusCode, 404, method);
        assert.equal(response.json().error, 'Not Found');
    }
});

test('OPTIONS answers the CORS preflight', async () => {
    const response = await app.inject({
        method: 'OPTIONS',
        url: `${PREFIX}/v1/i18n/de`,
        headers: { origin: 'https://example.org', 'access-control-request-method': 'GET', 'access-control-request-headers': 'x-foo' },
    });
    assert.equal(response.statusCode, 204);
    assert.equal(response.headers['access-control-allow-origin'], '*');
    assert.equal(response.headers['access-control-allow-headers'], 'x-foo');
    assert.match(String(response.headers['access-control-allow-methods']), /GET/);
});

test('translations: If-None-Match -> 304', async () => {
    const response = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de`, headers: { 'if-none-match': 'W/"abc"' } });
    assert.equal(response.statusCode, 304);
    assert.equal(response.body, '');
});

test('translations: ?context is handed on, an invalid one -> 400', async () => {
    contexts.length = 0;
    await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de?context=ctx-1` });
    assert.deepEqual(contexts, ['ctx-1']);
    const invalid = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de?context=a%2Fb` });
    assert.equal(invalid.statusCode, 400);
});

test('overrides: the flat map of the context', async () => {
    const response = await app.inject({ method: 'GET', url: `${PREFIX}/v1/i18n/de/overrides?context=ctx` });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { SAVE: 'Sichern' });
    assert.equal(response.headers['access-control-allow-origin'], '*');
});
