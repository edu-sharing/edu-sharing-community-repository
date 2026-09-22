import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { after, before, test } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { FastifyInstance } from 'fastify';
import { buildServer } from './server.ts';
import type { Config } from './config.ts';

/** Records what the fake Tomcat saw, so header tunnelling can be asserted. */
interface BackendHit {
    url: string;
    method: string;
    headers: http.IncomingHttpHeaders;
}

const hits: BackendHit[] = [];
let backend: http.Server;
let app: FastifyInstance;
let distDir: string;

before(async () => {
    distDir = fs.mkdtempSync(path.join(os.tmpdir(), 'es-frontend-server-'));
    fs.writeFileSync(
        path.join(distDir, 'index.html'),
        '<html><head><base href="/edu-sharing/" /></head><body>' +
            '<es-app ngcspnonce="{{ngCspNonce}}"><style nonce="{{ngCspNonce}}"></style></es-app></body></html>',
    );
    fs.writeFileSync(path.join(distDir, 'main-A1B2C3D4.js'), 'console.log(1);');
    fs.mkdirSync(path.join(distDir, 'assets'));
    fs.writeFileSync(path.join(distDir, 'assets', 'logo.svg'), '<svg/>');

    backend = http.createServer((request, response) => {
        hits.push({ url: request.url ?? '', method: request.method ?? '', headers: request.headers });
        response.writeHead(200, { 'content-type': 'text/plain' });
        response.end(`backend:${request.url}`);
    });
    await new Promise<void>((resolve) => backend.listen(0, '127.0.0.1', resolve));
    const backendPort = (backend.address() as AddressInfo).port;

    const config: Config = {
        host: '127.0.0.1',
        port: 0,
        contextPath: '/edu-sharing',
        backendUrl: `http://127.0.0.1:${backendPort}`,
        distDir,
        proxyUnmatched: true,
        proxyTimeoutMs: 5000,
        bodyLimitBytes: 1024 * 1024,
        logLevel: 'silent',
        dev: false,
        trustProxy: true,
    };
    app = await buildServer(config);
    await app.ready();
});

after(async () => {
    await app.close();
    await new Promise<void>((resolve) => backend.close(() => resolve()));
    fs.rmSync(distDir, { recursive: true, force: true });
});

test('serves the SPA shell on the angular routes and substitutes the nonce', async () => {
    for (const url of ['/edu-sharing/', '/edu-sharing/index.html', '/edu-sharing/components/workspace', '/edu-sharing/v/abc']) {
        const response = await app.inject({ method: 'GET', url });
        assert.equal(response.statusCode, 200, url);
        assert.match(response.headers['content-type'] as string, /text\/html/);
        assert.equal(response.body.includes('{{ngCspNonce}}'), false, `nonce not substituted for ${url}`);
        assert.match(response.headers['cache-control'] as string, /no-store/);
    }
});

test('gives each shell request its own nonce', async () => {
    const nonceOf = (body: string) => /ngcspnonce="([^"]+)"/i.exec(body)?.[1];
    const first = nonceOf((await app.inject({ method: 'GET', url: '/edu-sharing/' })).body);
    const second = nonceOf((await app.inject({ method: 'GET', url: '/edu-sharing/' })).body);
    assert.ok(first);
    assert.notEqual(first, second);
});

test('serves static files, marking hashed bundles immutable', async () => {
    const bundle = await app.inject({ method: 'GET', url: '/edu-sharing/main-A1B2C3D4.js' });
    assert.equal(bundle.statusCode, 200);
    assert.match(bundle.headers['cache-control'] as string, /immutable/);

    const asset = await app.inject({ method: 'GET', url: '/edu-sharing/assets/logo.svg' });
    assert.equal(asset.statusCode, 200);
    assert.doesNotMatch(asset.headers['cache-control'] as string, /immutable/);
});

test('proxies the backend prefixes', async () => {
    for (const url of ['/edu-sharing/rest/node/v1/nodes', '/edu-sharing/eduservlet/download?id=1', '/edu-sharing/search']) {
        const response = await app.inject({ method: 'GET', url });
        assert.equal(response.statusCode, 200, url);
        assert.equal(response.body, `backend:${url}`);
    }
});

test('/assets/i18n wins over the static /assets delivery', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/assets/i18n/de.json' });
    assert.equal(response.body, 'backend:/edu-sharing/assets/i18n/de.json');
});

test('proxies unmatched paths instead of answering with the shell', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/some-unknown-servlet' });
    assert.equal(response.body, 'backend:/edu-sharing/some-unknown-servlet');
});

test('tunnels every trace header to the backend untouched', async () => {
    hits.length = 0;
    await app.inject({
        method: 'GET',
        url: '/edu-sharing/rest/ping',
        headers: {
            'x-b3-traceid': 'trace-1',
            'x-b3-spanid': 'span-1',
            'x-b3-parentspanid': 'parent-1',
            'x-b3-sampled': '1',
            'x-ot-span-context': 'ot-1',
            'x-client-trace-id': 'client-1',
            'x-request-id': 'req-1',
        },
    });
    const hit = hits.at(-1);
    assert.ok(hit);
    assert.equal(hit.headers['x-b3-traceid'], 'trace-1');
    assert.equal(hit.headers['x-b3-spanid'], 'span-1');
    assert.equal(hit.headers['x-b3-parentspanid'], 'parent-1');
    assert.equal(hit.headers['x-b3-sampled'], '1');
    assert.equal(hit.headers['x-ot-span-context'], 'ot-1');
    assert.equal(hit.headers['x-client-trace-id'], 'client-1');
    assert.equal(hit.headers['x-request-id'], 'req-1');
});

test('forwards the proxy headers tomcat needs for absolute urls', async () => {
    hits.length = 0;
    await app.inject({ method: 'GET', url: '/edu-sharing/rest/ping', headers: { host: 'repo.example.org' } });
    const hit = hits.at(-1);
    assert.ok(hit);
    assert.equal(hit.headers['x-forwarded-host'], 'repo.example.org');
    assert.equal(hit.headers['x-forwarded-proto'], 'http');
    assert.ok(hit.headers['x-request-id']);
});

test('answers health without touching the backend', async () => {
    hits.length = 0;
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/_health' });
    assert.equal(response.statusCode, 200);
    assert.equal(response.json().status, 'ok');
    assert.equal(hits.length, 0);
});

test('proxies non-GET methods', async () => {
    const response = await app.inject({ method: 'POST', url: '/edu-sharing/rest/node/v1/nodes', payload: 'x' });
    assert.equal(response.statusCode, 200);
    assert.equal(hits.at(-1)?.method, 'POST');
});
