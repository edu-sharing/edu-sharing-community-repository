import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import zlib from 'node:zlib';
import { after, before, test } from 'node:test';
import { HASHED_ASSET } from '../server.ts';
import { negotiateEncoding, StaticCache, type CachedFile } from './static-cache.ts';

let root: string;
const BUNDLE = 'console.log("edu-sharing");\n'.repeat(2000);

before(() => {
    root = fs.mkdtempSync(path.join(os.tmpdir(), 'es-static-cache-'));
    fs.writeFileSync(path.join(root, 'main-A1B2C3D4.js'), BUNDLE);
    fs.writeFileSync(path.join(root, 'styles-ABCDEFGH.css'), 'body { color: red; }\n'.repeat(500));
    fs.writeFileSync(path.join(root, 'font.woff2'), Buffer.alloc(4096, 7));
    fs.writeFileSync(path.join(root, 'tiny.js'), 'x');
    fs.writeFileSync(path.join(root, 'big.js'), 'y'.repeat(256 * 1024));
    fs.mkdirSync(path.join(root, 'assets'));
    fs.writeFileSync(path.join(root, 'assets', 'config.json'), JSON.stringify({ a: 1 }));
    fs.writeFileSync(path.join(os.tmpdir(), 'es-static-cache-outside.js'), 'secret');
});

after(() => {
    fs.rmSync(root, { recursive: true, force: true });
    fs.rmSync(path.join(os.tmpdir(), 'es-static-cache-outside.js'), { force: true });
});

function cache(maxBytes = 16 * 1024 * 1024, maxFileBytes = 128 * 1024) {
    return new StaticCache({ root, maxBytes, maxFileBytes, revalidateMs: 60_000, isHashed: (name) => HASHED_ASSET.test(name) });
}

test('reads and compresses a file once, then answers it from memory', async () => {
    const subject = cache();
    const first = await subject.get('main-A1B2C3D4.js');
    assert.equal(first.result, 'miss');
    const second = await subject.get('main-A1B2C3D4.js');
    assert.equal(second.result, 'hit');
    assert.equal(second.file, first.file, 'handed out by reference, not copied');
    assert.equal(second.file?.identity.toString(), BUNDLE);
    assert.equal(zlib.gunzipSync(second.file!.gzip!).toString(), BUNDLE);
    assert.equal(zlib.brotliDecompressSync(second.file!.br!).toString(), BUNDLE);
    assert.equal(second.file?.contentType, 'application/javascript; charset=utf-8', 'the same type @fastify/send sets');
    assert.match(second.file!.etag, /^W\/"[0-9a-f]+-[0-9a-f]+"$/);
});

test('concurrent requests for a cold file share one read', async () => {
    const subject = cache();
    const results = await Promise.all([subject.get('styles-ABCDEFGH.css'), subject.get('styles-ABCDEFGH.css'), subject.get('styles-ABCDEFGH.css')]);
    assert.ok(results.every((r) => r.file === results[0]!.file));
});

test('every type is cached - already compressed ones as they are', async () => {
    const subject = cache();
    const font = await subject.get('font.woff2');
    assert.equal(font.result, 'miss');
    assert.equal(font.file?.contentType, 'font/woff2');
    assert.equal(font.file?.br, undefined, 'woff2 is not compressed again');
    assert.equal(font.file?.gzip, undefined);
    assert.equal((await subject.get('font.woff2')).result, 'hit');
    assert.equal((await subject.get('assets/config.json')).file?.contentType, 'application/json; charset=utf-8');
});

test('too large, missing and outside files bypass the cache', async () => {
    const subject = cache();
    assert.equal((await subject.get('big.js')).result, 'bypass');
    assert.equal((await subject.get('missing-12345678.js')).result, 'bypass');
    assert.equal((await subject.get('../es-static-cache-outside.js')).result, 'bypass');
    assert.equal((await subject.get('../../../../etc/passwd.js')).result, 'bypass');
});

test('a variant that saves nothing is not kept', async () => {
    const tiny = (await cache().get('tiny.js')).file!;
    assert.equal(tiny.gzip, undefined);
    assert.equal(tiny.br, undefined);
    assert.equal(tiny.bytes, 1);
});

test('stays within its byte budget, least recently used first', async () => {
    const one = (await cache().get('main-A1B2C3D4.js')).file!.bytes;
    const subject = cache(one + 100);
    await subject.get('main-A1B2C3D4.js');
    await subject.get('styles-ABCDEFGH.css');
    assert.ok(subject.size().bytes <= one + 100);
    assert.equal((await subject.get('main-A1B2C3D4.js')).result, 'miss', 'evicted for the css');
});

test('a budget of 0 disables the cache', async () => {
    const subject = cache(0);
    assert.equal(subject.enabled, false);
    assert.equal((await subject.get('main-A1B2C3D4.js')).result, 'bypass');
});

test('negotiates brotli before gzip and honours q=0', () => {
    const file = { br: Buffer.from('b'), gzip: Buffer.from('g') } as CachedFile;
    assert.equal(negotiateEncoding('gzip, deflate, br, zstd', file), 'br');
    assert.equal(negotiateEncoding('gzip', file), 'gzip');
    assert.equal(negotiateEncoding('br;q=0, gzip', file), 'gzip');
    assert.equal(negotiateEncoding('*', file), 'br');
    assert.equal(negotiateEncoding('identity', file), 'identity');
    assert.equal(negotiateEncoding(undefined, file), 'identity');
    assert.equal(negotiateEncoding('br', { gzip: Buffer.from('g') } as CachedFile), 'identity');
});
