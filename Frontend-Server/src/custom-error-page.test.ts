import assert from 'node:assert/strict';
import http from 'node:http';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { after, before, test } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { FastifyInstance } from 'fastify';
import { buildManagementServer } from './lib/management-server.ts';
import { parseStatuses } from './lib/custom-error-page.ts';
import { buildServer } from './server.ts';
import type { Config } from './config.ts';

/**
 * ERROR_PAGE_FILE / ERROR_PAGE_URL: browsers get the operator's page instead of the
 * angular one - for this server's own errors, for an unreachable backend and where
 * tomcat's errors land (components/error/<code>). API clients are never affected.
 */
let backend: http.Server | undefined;
let backendPort = 0;
let distDir: string;
const apps: FastifyInstance[] = [];

const PAGE =
    '<!doctype html><title>{{status}} {{message}}</title><p id="id">{{requestId}}</p>' +
    '<a href="{{next}}">back</a><script nonce="{{nonce}}">1</script>';

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

function config(errorPage: Config['errorPage'], proxyUnmatched = true): Config {
    return {
        host: '127.0.0.1',
        port: 0,
        contextPath: '/edu-sharing',
        backendUrl: `http://127.0.0.1:${backendPort}`,
        backendInternalUrl: `http://127.0.0.1:${backendPort}`,
        elasticUrl: 'http://127.0.0.1:9200',
        elasticIndex: 'workspace_test',
        distDir,
        proxyUnmatched,
        proxyTimeoutMs: 5000,
        bodyLimitBytes: 1024 * 1024,
        logLevel: 'silent',
        dev: false,
        trustProxy: true,
        enrichTimeoutMs: 2000,
        configTimeoutMs: 10_000,
        configPollIntervalMs: 50,
        configMaxAgeMs: 60_000,
        backendProbeTimeoutMs: 5000,
        staticCacheMaxBytes: 16 * 1024 * 1024,
        staticCacheMaxFileBytes: 4 * 1024 * 1024,
        errorPage,
    };
}

async function app(errorPage: Config['errorPage'], proxyUnmatched = true): Promise<FastifyInstance> {
    const instance = await buildServer(config(errorPage, proxyUnmatched), { elastic: { firstSource: async () => undefined } });
    await instance.ready();
    apps.push(instance);
    return instance;
}

before(async () => {
    distDir = fs.mkdtempSync(path.join(os.tmpdir(), 'es-frontend-server-error-page-'));
    fs.writeFileSync(path.join(distDir, 'index.html'), '<html><head><base href="/edu-sharing/" /></head><body><es-app></es-app></body></html>');
    await startBackend();
});

after(async () => {
    await Promise.all(apps.map((instance) => instance.close()));
    await stopBackend();
    fs.rmSync(distDir, { recursive: true, force: true });
});

const browser = { accept: 'text/html,application/xhtml+xml' };
const json = { accept: 'application/json' };

test('statuses: exact codes, classes and all', () => {
    const matcher = parseStatuses('502, 503,4xx');
    assert.equal(matcher(502), true);
    assert.equal(matcher(404), true);
    assert.equal(matcher(500), false);
    assert.equal(parseStatuses('all')(500), true);
    assert.throws(() => parseStatuses('503,oops'), /ERROR_PAGE_STATUSES/);
    assert.throws(() => parseStatuses('200'), /ERROR_PAGE_STATUSES/);
});

test('an invalid url fails the startup', async () => {
    await assert.rejects(buildServer(config({ statuses: '503', url: '/relative' })), /ERROR_PAGE_URL/);
    await assert.rejects(buildServer(config({ statuses: '503', url: 'javascript:alert(1)' })), /ERROR_PAGE_URL/);
});

test('html: where tomcat errors land, with placeholders, own CSP and no caching', async () => {
    const server = await app({ statuses: '5xx', html: PAGE });
    const response = await server.inject({ method: 'GET', url: '/edu-sharing/components/error/500', headers: browser });
    assert.equal(response.statusCode, 500);
    assert.match(response.body, /<title>500 Internal Server Error<\/title>/);
    assert.match(response.body, /<a href="\/edu-sharing\/">back<\/a>/);
    assert.equal(response.headers['cache-control'], 'no-store');
    const requestId = String(response.headers['x-request-id']);
    assert.ok(response.body.includes(`<p id="id">${requestId}</p>`));
    const nonce = /<script nonce="([^"]+)">/.exec(response.body)?.[1];
    assert.ok(nonce);
    assert.match(String(response.headers['content-security-policy']), new RegExp(`script-src 'nonce-${nonce.replaceAll('+', '\\+')}'`));
    assert.doesNotMatch(response.body, /\{\{/);
});

test('html: statuses not listed keep the angular page and the meta refresh', async () => {
    const server = await app({ statuses: '503', html: PAGE }, false);
    const shell = await server.inject({ method: 'GET', url: '/edu-sharing/components/error/404', headers: browser });
    assert.equal(shell.statusCode, 404);
    assert.match(shell.body, /<es-app>/);
    const notFound = await server.inject({ method: 'GET', url: '/edu-sharing/nope', headers: browser });
    assert.equal(notFound.statusCode, 404);
    assert.match(notFound.body, /components\/error\/404/);
});

test('html: an incoming next is only taken when it stays on this host', async () => {
    const server = await app({ statuses: '503', html: PAGE });
    const local = await server.inject({ method: 'GET', url: '/edu-sharing/components/error/503?next=%2Fedu-sharing%2Fcomponents%2Fworkspace%3Fa%3D1%26b%3D2', headers: browser });
    assert.equal(local.statusCode, 503);
    assert.equal(local.headers['retry-after'], '10');
    assert.match(local.body, /<a href="\/edu-sharing\/components\/workspace\?a=1&amp;b=2">/);
    for (const evil of ['%2F%2Fevil.example', 'https%3A%2F%2Fevil.example', '%2F%5Cevil.example']) {
        const response = await server.inject({ method: 'GET', url: `/edu-sharing/components/error/503?next=${evil}`, headers: browser });
        assert.match(response.body, /<a href="\/edu-sharing\/">back<\/a>/, evil);
    }
});

test('url: meta refresh to the url with status and the absolute url to return to, status kept', async () => {
    const server = await app({ statuses: '502,503', url: 'https://status.example.org/?code={status}&back={next}' });
    const response = await server.inject({
        method: 'GET',
        url: '/edu-sharing/components/error/502',
        headers: { ...browser, host: 'repo.example.org', 'x-forwarded-proto': 'https' },
    });
    assert.equal(response.statusCode, 502);
    assert.equal(response.headers['cache-control'], 'no-store');
    assert.equal(
        response.body,
        '<head><meta http-equiv="refresh" content="0;url=https://status.example.org/?code=502&amp;back=https%3A%2F%2Frepo.example.org%2Fedu-sharing%2F"></head>',
    );
});

test('backend gone: browsers get the custom page with their own url as next, API clients json', async () => {
    const server = await app({ statuses: '503', html: PAGE });
    await stopBackend();
    try {
        await eventually(async () => (await server.inject({ method: 'GET', url: '/edu-sharing/components/workspace', headers: browser })).statusCode === 503);
        const page = await server.inject({ method: 'GET', url: '/edu-sharing/components/workspace?id=1', headers: browser });
        assert.equal(page.headers['retry-after'], '10');
        assert.match(page.body, /<title>503 Service Unavailable<\/title>/);
        assert.match(page.body, /<a href="\/edu-sharing\/components\/workspace\?id=1">/);

        const api = await server.inject({ method: 'GET', url: '/edu-sharing/rest/node/v1/nodes', headers: json });
        assert.equal(api.statusCode, 503);
        assert.deepEqual(api.json(), { error: '503', message: 'Service Unavailable' });

        const management = buildManagementServer(server, { backendUrl: `http://127.0.0.1:${backendPort}` });
        try {
            const metrics = (await management.inject({ method: 'GET', url: '/metrics' })).body;
            assert.match(metrics, /^frontend_server_custom_error_pages_total\{kind="html",status_code="503"\} [1-9]\d*$/m);
        } finally {
            await management.close();
        }
    } finally {
        await startBackend();
    }
});
