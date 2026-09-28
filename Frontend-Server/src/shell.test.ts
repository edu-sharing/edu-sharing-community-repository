import assert from 'node:assert/strict';
import fs from 'node:fs';
import http from 'node:http';
import type { AddressInfo } from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { after, before, beforeEach, test } from 'node:test';
import type { FastifyInstance } from 'fastify';
import type { Config } from './config.ts';
import { buildServer } from './server.ts';

/**
 * Shell delivery end to end, against a fake Tomcat with both connectors: the internal
 * one answers _about and the internal config (ValuesBackendInternal), the external
 * one node metadata and everything proxied. "private" is only readable with the
 * session cookie of its owner - which this server never sends, so it serves to show
 * that the backend calls really do go out anonymously.
 */

const OWNER_SESSION = 'JSESSIONID=owner';

const NODES: Record<string, { owner: boolean; node: unknown }> = {
    public: {
        owner: false,
        node: {
            ref: { id: 'public' },
            mimetype: 'application/pdf',
            properties: {
                'cclom:title': ['Photosynthesis'],
                'cclom:general_description': ['How plants eat light'],
                'ccm:lifecyclecontributer_author': ['BEGIN:VCARD\nVERSION:3.0\nN:Miller;Mike\nEND:VCARD'],
            },
            license: { url: 'https://creativecommons.org/licenses/by/4.0/' },
            preview: { url: 'http://repo.example.org/edu-sharing/preview?nodeId=public' },
        },
    },
    private: {
        owner: true,
        node: { ref: { id: 'private' }, properties: { 'cclom:title': ['Secret draft'] } },
    },
};

interface Hit {
    port: 'internal' | 'external';
    url: string;
    headers: http.IncomingHttpHeaders;
}

const CUSTOM_HTML = '<script nonce="{{ngCspNonce}}">window.tracking = true;</script>';

const hits: Hit[] = [];
let external: http.Server;
let internal: http.Server;
let app: FastifyInstance;
let distDir: string;

function json(response: http.ServerResponse, status: number, body: unknown, headers: http.OutgoingHttpHeaders = {}): void {
    response.writeHead(status, { 'content-type': 'application/json', ...headers });
    response.end(JSON.stringify(body));
}

before(async () => {
    distDir = fs.mkdtempSync(path.join(os.tmpdir(), 'es-frontend-server-shell-'));
    fs.writeFileSync(
        path.join(distDir, 'index.html'),
        '<html><head><base href="/edu-sharing/" /><title>edu-sharing</title></head><body><es-app></es-app></body></html>',
    );

    internal = http.createServer((request, response) => {
        const url = request.url ?? '';
        hits.push({ port: 'internal', url, headers: request.headers });
        if (url === '/edu-sharing/rest/_about') {
            return json(response, 200, { lastCacheUpdate: 1 });
        }
        if (url === '/edu-sharing/rest/config/v1/values') {
            return json(response, 200, {
                current: {
                    branding: true,
                    defaultLocation: request.headers.host === 'portal.example.org' ? 'https://portal.example.org/start' : 'workspace',
                },
                currentBackend: {
                    angular: {
                        headers: {
                            'X-Frame-Options': 'sameorigin',
                            'Content-Security-Policy': { 'script-src': "'self' 'nonce-{{ngCspNonce}}'" },
                        },
                        customHtmlHeaders: CUSTOM_HTML,
                    },
                    exporter: {
                        lrmi: {
                            properties: { name: 'cclom:title,cm:name', encodingFormat: 'AlfrescoMimeType' },
                            vcard: { creator: 'ccm:lifecyclecontributer_author' },
                        },
                    },
                },
            });
        }
        response.writeHead(404);
        response.end();
    });
    external = http.createServer((request, response) => {
        const url = request.url ?? '';
        hits.push({ port: 'external', url, headers: request.headers });
        const match = /^\/edu-sharing\/rest\/node\/v1\/nodes\/-home-\/([^/]+)\/metadata\?propertyFilter=-all-$/.exec(url);
        if (match?.[1] !== undefined) {
            const entry = NODES[match[1]];
            if (entry === undefined) {
                return json(response, 404, { error: '404' });
            }
            if (entry.owner && request.headers.cookie !== OWNER_SESSION) {
                return json(response, 403, { error: '403' });
            }
            // a guest without session gets one, as Tomcat's guest filter does
            const setCookie = request.headers.cookie === undefined ? { 'set-cookie': 'JSESSIONID=guest; Path=/edu-sharing' } : {};
            return json(response, 200, { node: entry.node }, setCookie);
        }
        if (url === '/edu-sharing/unknown-to-tomcat') {
            // what ErrorFilter / ErrorPageServlet render: a relative redirect
            if (String(request.headers.accept).includes('text/html')) {
                response.writeHead(404, { 'content-type': 'text/html;charset=UTF-8' });
                response.end('<head><meta http-equiv="refresh" content="0;url=/edu-sharing/components/error/404"></head>');
                return;
            }
            return json(response, 404, { error: '404', message: 'The requested resource is not available' });
        }
        response.writeHead(200, { 'content-type': 'text/plain' });
        response.end(`backend:${url}`);
    });
    await new Promise<void>((resolve) => external.listen(0, '127.0.0.1', resolve));
    await new Promise<void>((resolve) => internal.listen(0, '127.0.0.1', resolve));

    const config: Config = {
        host: '127.0.0.1',
        port: 0,
        contextPath: '/edu-sharing',
        backendUrl: `http://127.0.0.1:${(external.address() as AddressInfo).port}`,
        backendInternalUrl: `http://127.0.0.1:${(internal.address() as AddressInfo).port}`,
        distDir,
        proxyUnmatched: true,
        proxyTimeoutMs: 5000,
        bodyLimitBytes: 1024 * 1024,
        logLevel: 'silent',
        dev: false,
        trustProxy: true,
        enrichTimeoutMs: 2000,
        configTimeoutMs: 10_000,
        configPollIntervalMs: 0,
        configMaxAgeMs: 60_000,
        backendProbeTimeoutMs: 5000,
        staticCacheMaxBytes: 16 * 1024 * 1024,
        staticCacheMaxFileBytes: 4 * 1024 * 1024,
    };
    app = await buildServer(config);
    await app.ready();
});

after(async () => {
    await app.close();
    await new Promise<void>((resolve) => external.close(() => resolve()));
    await new Promise<void>((resolve) => internal.close(() => resolve()));
    fs.rmSync(distDir, { recursive: true, force: true });
});

beforeEach(() => {
    hits.length = 0;
});

function shell(url: string, headers: Record<string, string> = {}) {
    return app.inject({ method: 'GET', url, headers: { host: 'repo.example.org', ...headers } });
}

test('render view of a public node carries title, description, license, LRMI and oEmbed', async () => {
    const response = await shell('/edu-sharing/components/render/public');
    assert.equal(response.statusCode, 200);
    const html = response.body;
    assert.match(html, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.match(html, /<meta name="description" content="How plants eat light">/);
    assert.match(html, /<link rel="license" href="https:\/\/creativecommons.org\/licenses\/by\/4.0\/">/);
    assert.match(html, /eduservlet\/oembed\?format=json&amp;url=http%3A%2F%2Frepo.example.org%2Fedu-sharing%2Fcomponents%2Frender%2Fpublic/);

    const lrmi = JSON.parse(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/.exec(html)?.[1] ?? 'null');
    assert.equal(lrmi.name, 'Photosynthesis');
    assert.equal(lrmi.encodingFormat, 'application/pdf');
    assert.equal(lrmi.url, 'http://repo.example.org/edu-sharing/components/render/public');
    assert.deepEqual(lrmi.creator, { '@type': 'Person', givenName: 'Mike', familyName: 'Miller' });
});

test('the metadata call carries no browser identity - even the owner gets no enrichment', async () => {
    const response = await shell('/edu-sharing/components/render/private', { cookie: OWNER_SESSION });
    const metadataHit = hits.find((hit) => hit.url.includes('/private/metadata'));
    assert.equal(metadataHit?.headers.cookie, undefined);
    assert.equal(metadataHit?.headers.authorization, undefined);
    // the price of anonymous forwarding: a node only its owner may read stays unenriched
    assert.doesNotMatch(response.body, /Secret draft/);
    assert.match(response.body, /<title>edu-sharing<\/title>/);
});

test('without the session a private node stays hidden - plain shell plus oEmbed', async () => {
    const response = await shell('/edu-sharing/components/render/private');
    assert.equal(response.statusCode, 200);
    assert.doesNotMatch(response.body, /Secret draft/);
    assert.match(response.body, /<title>edu-sharing<\/title>/);
    assert.match(response.body, /json\+oembed/);
});

test('the config is fetched without the browser identity', async () => {
    // fresh domain, so the config is not served from the cache
    await shell('/edu-sharing/components/render/private', { cookie: OWNER_SESSION, host: 'other.example.org' });
    const configHit = hits.find((hit) => hit.url === '/edu-sharing/rest/config/v1/values');
    assert.ok(configHit);
    assert.equal(configHit.headers.cookie, undefined);
    assert.equal(configHit.headers.host, 'other.example.org');
});

test('a session the metadata call opened is not handed to the browser', async () => {
    const response = await shell('/edu-sharing/components/render/public');
    // the fake backend answers the anonymous call with a guest cookie; handing it on
    // would pin the browser to a session that, without an ingress, is not sticky
    assert.equal(response.headers['set-cookie'], undefined);
    // enrichment itself still works, the node is public
    assert.match(response.body, /<title>Photosynthesis – edu-sharing<\/title>/);
});

test('collection view gets title and description only', async () => {
    const response = await shell('/edu-sharing/components/collections?id=public');
    assert.match(response.body, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.doesNotMatch(response.body, /oembed|ld\+json/);
});

test('other shell urls load no node metadata', async () => {
    const response = await shell('/edu-sharing/components/workspace');
    assert.equal(response.statusCode, 200);
    assert.equal(
        hits.some((hit) => hit.url.includes('/metadata')),
        false,
    );
});

test('every shell page carries the security headers and custom html headers, sharing one nonce', async () => {
    const response = await shell('/edu-sharing/components/workspace');
    assert.equal(response.headers['x-frame-options'], 'sameorigin');
    const nonce = /'nonce-([^']+)'/.exec(String(response.headers['content-security-policy']))?.[1];
    assert.ok(nonce);
    assert.ok(response.body.includes(`<script nonce="${nonce}">window.tracking = true;</script></head>`));
    assert.doesNotMatch(response.body, /\{\{ngCspNonce\}\}/);
});

test('each shell response gets a fresh nonce', async () => {
    const nonceOf = async () => (await shell('/edu-sharing/components/workspace')).headers['content-security-policy'];
    assert.notEqual(await nonceOf(), await nonceOf());
});

test('config and _about go to the internal port, metadata and proxying to the external one', async () => {
    await shell('/edu-sharing/components/render/public', { host: 'ports.example.org' });
    await shell('/edu-sharing/rest/ping', { host: 'ports.example.org' });
    const portOf = (fragment: string) => hits.find((hit) => hit.url.includes(fragment))?.port;
    assert.equal(portOf('/config/v1/values'), 'internal');
    assert.equal(portOf('/metadata'), 'external');
    assert.equal(portOf('/rest/ping'), 'external');
});

test('components/error/<code> answers with that status', async () => {
    assert.equal((await shell('/edu-sharing/components/error/404')).statusCode, 404);
    assert.equal((await shell('/edu-sharing/components/error/403')).statusCode, 403);
    assert.equal((await shell('/edu-sharing/components/error/nonsense')).statusCode, 500);
});

test('proxied requests keep the browser Host - the backend resolves the config context from it', async () => {
    await shell('/edu-sharing/rest/ping', { host: 'tenant.example.org' });
    const hit = hits.find((entry) => entry.url === '/edu-sharing/rest/ping');
    assert.equal(hit?.headers.host, 'tenant.example.org');
});

test("an unmatched path tomcat does not know either keeps tomcat's error answer", async () => {
    const html = await shell('/edu-sharing/unknown-to-tomcat', { accept: 'text/html' });
    assert.equal(html.statusCode, 404);
    assert.match(html.body, /<meta http-equiv="refresh" content="0;url=\/edu-sharing\/components\/error\/404">/);

    const api = await shell('/edu-sharing/unknown-to-tomcat', { accept: 'application/json' });
    assert.equal(api.statusCode, 404);
    assert.equal(api.json().error, '404');
});

test('the bare entry point goes to the configured default location, like AuthenticationFilter did', async () => {
    const root = await shell('/edu-sharing/');
    assert.equal(root.statusCode, 302);
    assert.equal(root.headers.location, '/edu-sharing/components/workspace');
    assert.equal(root.headers['cache-control'], 'no-store');

    // after the login the app returns to the entry point it came from
    const fromLogin = await shell('/edu-sharing/?fromLogin=true');
    assert.equal(fromLogin.headers.location, '/edu-sharing/components/workspace');

    const absolute = await shell('/edu-sharing/', { host: 'portal.example.org' });
    assert.equal(absolute.headers.location, 'https://portal.example.org/start');
});

