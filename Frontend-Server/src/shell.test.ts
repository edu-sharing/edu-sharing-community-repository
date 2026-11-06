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
 * Shell delivery end to end, against a fake Tomcat with both connectors (internal:
 * _about and the internal config; external: everything proxied) and a fake search
 * index for the node metadata.
 *
 * The index double answers the way the real one does: raw property values, mixed
 * scalars and arrays, and only for a document the public-read filter matches - so
 * "private" is simply not found, whatever the browser sends.
 */

const OWNER_SESSION = 'JSESSIONID=owner';

/**
 * `_source` documents as the index holds them: property values are raw - `cclom:title`
 * a scalar, `ccm:commonlicense_key` an array - and there is no ready made preview url,
 * mimetype or license url; the server derives those.
 */
const DOCUMENTS: Record<string, { public: boolean; source: Record<string, unknown> }> = {
    public: {
        public: true,
        source: {
            properties: {
                'cclom:title': 'Photosynthesis',
                'cclom:general_description': 'How plants eat light',
                'cclom:format': 'application/pdf',
                'ccm:lifecyclecontributer_author': ['BEGIN:VCARD\nVERSION:3.0\nN:Miller;Mike\nEND:VCARD'],
                'ccm:commonlicense_key': ['CC_BY'],
                'ccm:commonlicense_cc_version': '4.0',
            },
        },
    },
    private: {
        public: false,
        source: { properties: { 'cclom:title': 'Secret draft' } },
    },
};

/** Records the queries, and answers only for a document the public filter matches. */
const elasticQueries: { index: string; nodeId: string }[] = [];
const fakeElastic = {
    firstSource: async (request: { index?: unknown; query?: any }) => {
        const filters = request.query?.bool?.filter ?? [];
        const nodeId = String(filters[0]?.ids?.values?.[0] ?? '');
        elasticQueries.push({ index: String(request.index), nodeId });
        // the real index applies these as part of the query - mirror that here, so a
        // server that dropped a filter would fail this suite
        const everyone = filters.some((f: any) => f?.term?.['permissions.read'] === 'GROUP_EVERYONE');
        const workspace = filters.some((f: any) => f?.term?.['nodeRef.storeRef.protocol'] === 'workspace');
        const entry = DOCUMENTS[nodeId];
        if (entry === undefined || !everyone || !workspace || !entry.public) {
            return undefined;
        }
        return entry.source;
    },
};

interface Hit {
    port: 'internal' | 'external';
    url: string;
    headers: http.IncomingHttpHeaders;
}

const CUSTOM_HTML = '<script nonce="{{ngCspNonce}}">window.tracking = true;</script>';

/** repository.guest.enabled of the fake backend - flipped by the guest gate test */
let guestEnabled = true;

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
                    repository: { guest: { enabled: guestEnabled } },
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
        // the index is faked through buildServer's seam, the url stays unused
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
        configPollIntervalMs: 0,
        configMaxAgeMs: 60_000,
        backendProbeTimeoutMs: 5000,
        staticCacheMaxBytes: 16 * 1024 * 1024,
        staticCacheMaxFileBytes: 4 * 1024 * 1024,
    };
    app = await buildServer(config, { elastic: fakeElastic });
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
    elasticQueries.length = 0;
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
    // derived from ccm:commonlicense_key + _cc_version, language from Accept-Language
    assert.match(html, /<link rel="license" href="https:\/\/creativecommons.org\/licenses\/by\/4.0\/deed.en">/);
    assert.match(html, /eduservlet\/oembed\?format=json&amp;url=http%3A%2F%2Frepo.example.org%2Fedu-sharing%2Fcomponents%2Frender%2Fpublic/);

    const lrmi = JSON.parse(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/.exec(html)?.[1] ?? 'null');
    assert.equal(lrmi.name, 'Photosynthesis');
    assert.equal(lrmi.encodingFormat, 'application/pdf');
    assert.equal(lrmi.url, 'http://repo.example.org/edu-sharing/components/render/public');
    assert.deepEqual(lrmi.creator, { '@type': 'Person', givenName: 'Mike', familyName: 'Miller' });
    // built from the requested origin, no dontcache buster
    assert.equal(
        lrmi.thumbnailUrl,
        'http://repo.example.org/edu-sharing/preview?nodeId=public&storeProtocol=workspace&storeId=SpacesStore',
    );
});

test('a node that is not public stays unenriched, whatever the browser sends', async () => {
    const response = await shell('/edu-sharing/components/render/private', { cookie: OWNER_SESSION });
    // the browser identity plays no part: the query filters on permissions.read, so
    // even the owner's session buys nothing here
    assert.doesNotMatch(response.body, /Secret draft/);
    assert.match(response.body, /<title>edu-sharing<\/title>/);
    assert.equal(hits.some((hit) => hit.url.includes('/metadata')), false);
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

test('the shell request opens no session at all', async () => {
    const response = await shell('/edu-sharing/components/render/public');
    // metadata comes from the index, so tomcat never sees the shell request and cannot
    // hand out a (guest) session the browser would then be pinned to
    assert.equal(response.headers['set-cookie'], undefined);
    assert.match(response.body, /<title>Photosynthesis – edu-sharing<\/title>/);
});

test('with guest access off the shell carries no node metadata at all', async () => {
    guestEnabled = false;
    try {
        // a fresh domain, so the cache does not serve the guest-enabled entry
        const response = await shell('/edu-sharing/components/render/public', { host: 'noguest.example.org' });
        assert.equal(response.statusCode, 200);
        // the repository hands an anonymous caller nothing, so neither do we - even
        // though the index would happily answer, and did
        assert.doesNotMatch(response.body, /Photosynthesis/);
        assert.doesNotMatch(response.body, /ld\+json/);
        assert.doesNotMatch(response.body, /rel="license"/);
        // the parts that do not depend on the node stay
        assert.match(response.body, /json\+oembed/);
        assert.match(response.body, /<title>edu-sharing<\/title>/);
    } finally {
        guestEnabled = true;
    }
});

test('with guest access off in the cached config the index is not even asked', async () => {
    guestEnabled = false;
    try {
        // the first request finds no config yet and queries in parallel to its fetch
        await shell('/edu-sharing/components/render/public', { host: 'noguest-cached.example.org' });
        elasticQueries.length = 0;
        const response = await shell('/edu-sharing/components/render/public', { host: 'noguest-cached.example.org' });
        assert.equal(response.statusCode, 200);
        assert.doesNotMatch(response.body, /Photosynthesis/);
        assert.deepEqual(elasticQueries, []);
    } finally {
        guestEnabled = true;
    }
});

test('collection view gets title and description only', async () => {
    const response = await shell('/edu-sharing/components/collections?id=public');
    assert.match(response.body, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.doesNotMatch(response.body, /oembed|ld\+json/);
});

test('other shell urls query the index not at all', async () => {
    const response = await shell('/edu-sharing/components/workspace');
    assert.equal(response.statusCode, 200);
    assert.deepEqual(elasticQueries, []);
});

test('a versioned render url is not enriched and spares the query', async () => {
    const response = await shell('/edu-sharing/components/render/public/1.0');
    assert.equal(response.statusCode, 200);
    // the index holds no version documents - current metadata under a versioned url
    // would be wrong, so nothing is sent
    assert.doesNotMatch(response.body, /Photosynthesis/);
    assert.deepEqual(elasticQueries, []);
    // the oEmbed links do not depend on the node and stay
    assert.match(response.body, /json\+oembed/);
});

test('the query goes to the configured index', async () => {
    await shell('/edu-sharing/components/render/public');
    assert.deepEqual(elasticQueries, [{ index: 'workspace_test', nodeId: 'public' }]);
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

test('config goes to the internal port, proxying to the external one, metadata to neither', async () => {
    await shell('/edu-sharing/components/render/public', { host: 'ports.example.org' });
    await shell('/edu-sharing/rest/ping', { host: 'ports.example.org' });
    const portOf = (fragment: string) => hits.find((hit) => hit.url.includes(fragment))?.port;
    assert.equal(portOf('/config/v1/values'), 'internal');
    assert.equal(portOf('/rest/ping'), 'external');
    // node metadata no longer touches tomcat at all - it comes from the index
    assert.equal(portOf('/metadata'), undefined);
    assert.equal(elasticQueries.length, 1);
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

