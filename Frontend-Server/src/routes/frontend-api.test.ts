import assert from 'node:assert/strict';
import zlib from 'node:zlib';
import { after, before, test } from 'node:test';
import Fastify, { type FastifyInstance } from 'fastify';
import type { Bundle } from '../lib/language-defaults.ts';
import { FRONTEND_API_PATH, frontendApi } from './frontend-api.ts';

const PREFIX = `/edu-sharing${FRONTEND_API_PATH}`;
const DE = Buffer.from(JSON.stringify({ SAVE: 'Speichern', NESTED: { A: 'a' } }));
const bundle: Bundle = { identity: DE, br: zlib.brotliCompressSync(DE), gzip: zlib.gzipSync(DE) };
const requested: string[] = [];

let app: FastifyInstance;

before(async () => {
    app = Fastify();
    await app.register(frontendApi, {
        prefix: PREFIX,
        languageDefaults: {
            get: async (language: string) => {
                requested.push(language);
                return language === 'de' ? bundle : undefined;
            },
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
    assert.equal(response.headers['cache-control'], 'public, max-age=300');
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
