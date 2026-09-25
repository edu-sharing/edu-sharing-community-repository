import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { after, before, test } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { FastifyInstance } from 'fastify';
import { buildManagementServer } from './lib/management-server.ts';
import { buildServer } from './server.ts';
import type { Config } from './config.ts';

/**
 * The backend goes away and comes back: a real tomcat stand-in that is closed (connection
 * refused) and restarted on the same port, with the background `_about` polling on.
 */
let backend: http.Server | undefined;
let backendPort = 0;
let app: FastifyInstance;
let distDir: string;

async function startBackend(): Promise<void> {
    backend = http.createServer((request, response) => {
        if (request.url === '/edu-sharing/rest/_about') {
            response.writeHead(200, { 'content-type': 'application/json' });
            response.end(JSON.stringify({ lastCacheUpdate: 1 }));
            return;
        }
        response.writeHead(200, { 'content-type': 'text/plain' });
        response.end(`backend:${request.url}`);
    });
    await new Promise<void>((resolve) => backend!.listen(backendPort, '127.0.0.1', resolve));
    backendPort = (backend.address() as AddressInfo).port;
}

async function stopBackend(): Promise<void> {
    const server = backend;
    backend = undefined;
    server?.closeAllConnections();
    await new Promise<void>((resolve) => (server ? server.close(() => resolve()) : resolve()));
}

async function eventually(check: () => Promise<boolean>): Promise<void> {
    for (let attempt = 0; attempt < 100; attempt++) {
        if (await check()) {
            return;
        }
        await new Promise((resolve) => setTimeout(resolve, 20));
    }
    assert.fail('condition not reached');
}

before(async () => {
    distDir = fs.mkdtempSync(path.join(os.tmpdir(), 'es-frontend-server-unavailable-'));
    fs.writeFileSync(path.join(distDir, 'index.html'), '<html><head><base href="/edu-sharing/" /></head><body><es-app></es-app></body></html>');
    fs.writeFileSync(path.join(distDir, 'main-A1B2C3D4.js'), 'console.log(1);');
    await startBackend();
    const config: Config = {
        host: '127.0.0.1',
        port: 0,
        contextPath: '/edu-sharing',
        backendUrl: `http://127.0.0.1:${backendPort}`,
        backendInternalUrl: `http://127.0.0.1:${backendPort}`,
        distDir,
        proxyUnmatched: true,
        proxyTimeoutMs: 5000,
        bodyLimitBytes: 1024 * 1024,
        logLevel: 'silent',
        dev: false,
        trustProxy: true,
        enrichTimeoutMs: 2000,
        configTimeoutMs: 10_000,
        // the regular polling brings the backend back quickly in this test
        configPollIntervalMs: 50,
        configMaxAgeMs: 60_000,
        backendProbeTimeoutMs: 5000,
    };
    app = await buildServer(config);
    await app.ready();
});

after(async () => {
    await app.close();
    await stopBackend();
    fs.rmSync(distDir, { recursive: true, force: true });
});

const browser = { accept: 'text/html,application/xhtml+xml' };

async function metrics(): Promise<string> {
    const management = buildManagementServer(app, { backendUrl: `http://127.0.0.1:${backendPort}` });
    try {
        return (await management.inject({ method: 'GET', url: '/metrics' })).body;
    } finally {
        await management.close();
    }
}

test('with the backend gone, browsers are sent to the unavailable page and come back afterwards', async () => {
    const shell = await app.inject({ method: 'GET', url: '/edu-sharing/components/workspace?id=1', headers: browser });
    assert.equal(shell.statusCode, 200);

    await stopBackend();

    // a proxied request finds the backend gone - no waiting for the next poll
    const api = await app.inject({ method: 'GET', url: '/edu-sharing/rest/node/v1/nodes', headers: { accept: 'application/json' } });
    assert.equal(api.statusCode, 503);
    assert.equal(api.headers['retry-after'], '10');
    assert.equal(api.json().error, '503');

    const page = await app.inject({ method: 'GET', url: '/edu-sharing/components/workspace?id=1', headers: browser });
    assert.equal(page.statusCode, 503);
    assert.equal(page.headers['retry-after'], '10');
    assert.equal(
        page.body,
        '<head><meta http-equiv="refresh" content="0;url=/edu-sharing/components/error/503?next=%2Fedu-sharing%2Fcomponents%2Fworkspace%3Fid%3D1"></head>',
    );

    // the unavailable page itself is the shell - no loop, no waiting for the backend
    const started = Date.now();
    const unavailable = await app.inject({ method: 'GET', url: '/edu-sharing/components/error/503?next=%2Fedu-sharing%2F', headers: browser });
    assert.equal(unavailable.statusCode, 503);
    assert.match(unavailable.body, /<es-app>/);
    assert.ok(Date.now() - started < 1000, 'the shell must not wait for the backend');

    // static files are served regardless
    const bundle = await app.inject({ method: 'GET', url: '/edu-sharing/main-A1B2C3D4.js' });
    assert.equal(bundle.statusCode, 200);

    const down = await metrics();
    assert.match(down, /^frontend_server_backend_reachable 0$/m);
    // whichever notices first - the proxied request or the 50 ms poll - counts the one outage
    assert.match(down, /^frontend_server_backend_unreachable_total\{source="(proxy|poll)"\} 1$/m);
    assert.match(down, /^frontend_server_unavailable_responses_total\{kind="proxy"\} 1$/m);
    assert.match(down, /^frontend_server_unavailable_responses_total\{kind="page"\} 1$/m);

    await startBackend();
    await eventually(async () => {
        const back = await app.inject({ method: 'GET', url: '/edu-sharing/components/workspace?id=1', headers: browser });
        return back.statusCode === 200 && back.body.includes('<es-app>');
    });
    assert.match(await metrics(), /^frontend_server_backend_reachable 1$/m);
});

test('a proxied browser request to a gone backend is sent to the unavailable page with its own url', async () => {
    await stopBackend();
    const response = await app.inject({ method: 'GET', url: '/edu-sharing/eduservlet/render?node_id=x', headers: browser });
    assert.equal(response.statusCode, 503);
    assert.match(response.body, /components\/error\/503\?next=%2Fedu-sharing%2Feduservlet%2Frender%3Fnode_id%3Dx/);
    await startBackend();
    await eventually(async () => (await app.inject({ method: 'GET', url: '/edu-sharing/', headers: browser })).statusCode === 200);
});
