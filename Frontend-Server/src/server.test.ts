import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import zlib from 'node:zlib';
import { after, before, test } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { FastifyInstance } from 'fastify';
import { buildManagementServer } from './lib/management-server.ts';
import { buildServer } from './server.ts';
import type { Config } from './config.ts';

/** Records what the fake Tomcat saw, so header tunnelling can be asserted. */
interface BackendHit {
    url: string;
    method: string;
    headers: http.IncomingHttpHeaders;
    body: string;
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
    fs.mkdirSync(path.join(distDir, 'web-components', 'app'), { recursive: true });
    fs.writeFileSync(path.join(distDir, 'web-components', 'app', 'main.js'), 'console.log(2);');

    backend = http.createServer((request, response) => {
        const chunks: Buffer[] = [];
        request.on('data', (chunk: Buffer) => chunks.push(chunk));
        request.on('end', () => {
            hits.push({
                url: request.url ?? '',
                method: request.method ?? '',
                headers: request.headers,
                body: Buffer.concat(chunks).toString(),
            });
            const url = request.url ?? '';
            response.writeHead(200, {
                'content-type': 'text/plain',
                // as tomcat answers a login: the proxy has to hand this to the browser
                ...(url.includes('/rest/authentication') ? { 'set-cookie': 'JSESSIONID=abc; Path=/edu-sharing; HttpOnly' } : {}),
            });
            response.end(`backend:${request.url}`);
        });
    });
    await new Promise<void>((resolve) => backend.listen(0, '127.0.0.1', resolve));
    const backendPort = (backend.address() as AddressInfo).port;

    const config: Config = {
        host: '127.0.0.1',
        port: 0,
        contextPath: '/edu-sharing',
        backendUrl: `http://127.0.0.1:${backendPort}`,
        backendInternalUrl: `http://127.0.0.1:${backendPort}`,
        // these tests do not exercise the shell enrichment; buildServer gets a fake
        elasticUrl: 'http://127.0.0.1:9200',
        elasticIndex: 'workspace_test',
        distDir,
        proxyUnmatched: true,
        proxyTimeoutMs: 5000,
        bodyLimitBytes: 1024 * 1024,
        logLevel: 'silent',
        dev: false,
        trustProxy: true,
        enrichTimeoutMs: 2000,
        configTimeoutMs: 10_000,
        // no background polling - tests count backend hits
        configPollIntervalMs: 0,
        configMaxAgeMs: 60_000,
        backendProbeTimeoutMs: 5000,
        staticCacheMaxBytes: 16 * 1024 * 1024,
        staticCacheMaxFileBytes: 4 * 1024 * 1024,
    };
    app = await buildServer(config, { elastic: { firstSource: async () => undefined } });
    await app.ready();
});

after(async () => {
    await app.close();
    await new Promise<void>((resolve) => backend.close(() => resolve()));
    fs.rmSync(distDir, { recursive: true, force: true });
});

test('serves the SPA shell on the angular routes and substitutes the nonce', async () => {
    for (const url of ['/edu-sharing/index.html', '/edu-sharing/components/workspace', '/edu-sharing/v/abc']) {
        const response = await app.inject({ method: 'GET', url });
        assert.equal(response.statusCode, 200, url);
        assert.match(response.headers['content-type'] as string, /text\/html/);
        assert.equal(response.body.includes('{{ngCspNonce}}'), false, `nonce not substituted for ${url}`);
        assert.match(response.headers['cache-control'] as string, /no-store/);
    }
});

test('redirects the bare root and the context path to the app', async () => {
    for (const url of ['/', '/edu-sharing']) {
        const response = await app.inject({ method: 'GET', url });
        assert.equal(response.statusCode, 302, url);
        assert.equal(response.headers.location, '/edu-sharing/', url);
    }
});

test('gives each shell request its own nonce', async () => {
    const nonceOf = (body: string) => /ngcspnonce="([^"]+)"/i.exec(body)?.[1];
    const first = nonceOf((await app.inject({ method: 'GET', url: '/edu-sharing/components/workspace' })).body);
    const second = nonceOf((await app.inject({ method: 'GET', url: '/edu-sharing/components/workspace' })).body);
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

test('web components carry Access-Control-Allow-Origin: *, as WebComponentsFilter sets it', async () => {
    const component = await app.inject({ method: 'GET', url: '/edu-sharing/web-components/app/main.js' });
    assert.equal(component.statusCode, 200);
    assert.equal(component.headers['access-control-allow-origin'], '*');

    const other = await app.inject({ method: 'GET', url: '/edu-sharing/assets/logo.svg' });
    assert.equal(other.headers['access-control-allow-origin'], undefined);
});

test('/assets/i18n wins over the static /assets delivery', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/assets/i18n/de.json' });
    assert.equal(response.body, 'backend:/edu-sharing/assets/i18n/de.json');
});

test('proxies unmatched paths instead of answering with the shell', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/some-unknown-servlet' });
    assert.equal(response.body, 'backend:/edu-sharing/some-unknown-servlet');
});

const REQUEST_ID = '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b';

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
            'x-request-id': REQUEST_ID,
        },
    });
    const hit = hits.at(-1);
    assert.ok(hit);
    assert.equal(hit.headers['x-b3-traceid'], 'trace-1');
    assert.equal(hit.headers['x-b3-spanid'], 'span-1');
    assert.equal(hit.headers['x-b3-parentspanid'], 'parent-1');
    assert.equal(hit.headers['x-b3-sampled'], '1');
    assert.equal(hit.headers['x-ot-span-context'], 'ot-1');
    assert.equal(hit.headers['x-request-id'], REQUEST_ID);
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

test('liveness and readiness never touch the backend', async () => {
    hits.length = 0;
    const management = buildManagementServer(app, { backendUrl: 'x' });
    const liveness = await management.inject({ method: 'GET', url: '/health/liveness' });
    assert.equal(liveness.statusCode, 200);
    assert.equal(liveness.json().status, 'ok');
    // polling is off in this test, so ready right away
    const readiness = await management.inject({ method: 'GET', url: '/health/readiness' });
    assert.equal(readiness.statusCode, 200);
    assert.equal(hits.length, 0);
});

test('the proxy forwards the browser identity in full - unlike this server\'s own calls', async () => {
    hits.length = 0;
    await app.inject({
        method: 'GET',
        url: '/edu-sharing/rest/node/v1/nodes',
        headers: {
            cookie: 'JSESSIONID=abc; XSRF-TOKEN=t',
            authorization: 'Basic dXNlcjpwYXNz',
            'x-xsrf-token': 't',
            'accept-language': 'de-DE',
        },
    });
    const hit = hits.at(-1);
    assert.ok(hit);
    // stripping any of these would break login, CSRF and the UI language. Only the
    // server's OWN backend calls go out anonymously - see lib/backend-client.ts
    assert.equal(hit.headers.cookie, 'JSESSIONID=abc; XSRF-TOKEN=t');
    assert.equal(hit.headers.authorization, 'Basic dXNlcjpwYXNz');
    assert.equal(hit.headers['x-xsrf-token'], 't');
    assert.equal(hit.headers['accept-language'], 'de-DE');
});

test('the proxy hands Set-Cookie back to the browser untouched', async () => {
    const response = await app.inject({ method: 'POST', url: '/edu-sharing/rest/authentication/v1/validateSession' });
    // same path prefix as tomcat, so the cookie needs no rewriting
    assert.equal(String(response.headers['set-cookie']), 'JSESSIONID=abc; Path=/edu-sharing; HttpOnly');
});

test('proxies non-GET methods', async () => {
    const response = await app.inject({ method: 'POST', url: '/edu-sharing/rest/node/v1/nodes', payload: 'x' });
    assert.equal(response.statusCode, 200);
    assert.equal(hits.at(-1)?.method, 'POST');
});

test('proxies the WebDAV methods on /webdav with their body', async () => {
    for (const method of ['PROPFIND', 'PROPPATCH', 'MKCOL', 'COPY', 'MOVE', 'LOCK', 'UNLOCK']) {
        const response = await app.inject({
            method: method as 'GET',
            url: '/edu-sharing/webdav/folder/file.txt',
            headers: { 'content-type': 'application/xml', depth: '1' },
            payload: '<propfind xmlns="DAV:"/>',
        });
        assert.equal(response.statusCode, 200, method);
        assert.equal(response.body, 'backend:/edu-sharing/webdav/folder/file.txt', method);
        const hit = hits.at(-1);
        assert.equal(hit?.method, method);
        assert.equal(hit?.body, '<propfind xmlns="DAV:"/>', method);
        assert.equal(hit?.headers.depth, '1', method);
    }
});

test('metrics are served on the management server only', async () => {
    await app.inject({ method: 'GET', url: '/edu-sharing/rest/ping' });
    const metrics = await buildManagementServer(app, { backendUrl: 'x' }).inject({ method: 'GET', url: '/metrics' });
    assert.equal(metrics.statusCode, 200);
    assert.match(metrics.headers['content-type'] as string, /text\/plain/);
    assert.match(metrics.body, /http_request_duration_seconds_count\{[^}]*route="\/edu-sharing\/rest\/\*"/);
    assert.match(metrics.body, /frontend_server_proxy_duration_seconds_count\{reason="backend-path",status_code="200"\}/);
    assert.match(metrics.body, /nodejs_eventloop_lag_seconds/);

    // never on the public port
    const onApp = await app.inject({ method: 'GET', url: '/metrics' });
    assert.doesNotMatch(onApp.body, /http_request_duration_seconds/);
});

test('the bare entry point goes to the login without a backend config', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/' });
    assert.equal(response.statusCode, 302);
    assert.equal(response.headers.location, '/edu-sharing/components/login');
});

test('static files come from the memory cache, compressed as the client accepts', async () => {
    const plain = await app.inject({ method: 'GET', url: '/edu-sharing/main-A1B2C3D4.js' });
    assert.equal(plain.statusCode, 200);
    assert.equal(plain.body, 'console.log(1);');
    assert.equal(plain.headers['cache-control'], 'public, max-age=3600, immutable');
    assert.equal(plain.headers.vary, 'Accept-Encoding');
    assert.equal(plain.headers['content-type'], 'application/javascript; charset=utf-8');

    const big = 'console.log("x");\n'.repeat(500);
    fs.writeFileSync(path.join(distDir, 'chunk-ZZZZZZZZ.js'), big);
    const gzipped = await app.inject({ method: 'GET', url: '/edu-sharing/chunk-ZZZZZZZZ.js', headers: { 'accept-encoding': 'gzip' } });
    assert.equal(gzipped.headers['content-encoding'], 'gzip');
    assert.equal(zlib.gunzipSync(gzipped.rawPayload).toString(), big);
    const brotli = await app.inject({ method: 'GET', url: '/edu-sharing/chunk-ZZZZZZZZ.js', headers: { 'accept-encoding': 'gzip, deflate, br' } });
    assert.equal(brotli.headers['content-encoding'], 'br');
    assert.equal(zlib.brotliDecompressSync(brotli.rawPayload).toString(), big);

    const revalidated = await app.inject({ method: 'GET', url: '/edu-sharing/chunk-ZZZZZZZZ.js', headers: { 'if-none-match': String(brotli.headers.etag) } });
    assert.equal(revalidated.statusCode, 304);
    assert.equal(revalidated.body, '');

    const head = await app.inject({ method: 'HEAD', url: '/edu-sharing/chunk-ZZZZZZZZ.js', headers: { 'accept-encoding': 'br' } });
    assert.equal(head.statusCode, 200);
    assert.equal(head.body, '');
});

test('ranges are streamed from disk, other files keep their cache headers', async () => {
    const range = await app.inject({ method: 'GET', url: '/edu-sharing/main-A1B2C3D4.js', headers: { range: 'bytes=0-6' } });
    assert.equal(range.statusCode, 206);
    assert.equal(range.body, 'console');
    const svg = await app.inject({ method: 'GET', url: '/edu-sharing/assets/logo.svg' });
    assert.equal(svg.statusCode, 200);
    assert.equal(svg.headers['cache-control'], 'public, max-age=300');
});

test('a missing static file still falls through to the backend', async () => {
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/missing-00000000.js' });
    assert.equal(response.body, 'backend:/edu-sharing/missing-00000000.js');
});

