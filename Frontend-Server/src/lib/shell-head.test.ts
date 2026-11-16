import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { RepositoryConfig } from './config-cache.ts';
import type { LoadedNode } from './node-metadata.ts';
import { enrichShell, jsonForScript, pageDescription, pageTitle, setTitle, type ShellContext } from './shell-head.ts';

const SHELL = '<html><head><base href="/edu-sharing/"><title>edu-sharing</title></head><body></body></html>';

const CONFIG: RepositoryConfig = {
    branding: true,
    lrmi: { enrichment: true, properties: { name: 'cclom:title,cm:name' }, vcard: {} },
    angular: { headers: {}, customHtmlHeaders: '' },
    guest: { enabled: true },
};

function loaded(properties: LoadedNode['properties'], licenseUrl?: string): LoadedNode {
    return { properties, mimetype: undefined, previewUrl: undefined, licenseUrl };
}

function renderContext(overrides: Partial<ShellContext> = {}): ShellContext {
    return {
        target: { kind: 'render', nodeId: 'n1', version: undefined },
        loaded: loaded({ 'cclom:title': ['Photosynthesis'], 'cclom:general_description': ['How plants eat light'] }, 'https://cc.org/by'),
        config: CONFIG,
        pageUrl: 'https://repo.example.org/edu-sharing/components/render/n1',
        appUrl: 'https://repo.example.org/edu-sharing/',
        ...overrides,
    };
}

function headOf(html: string): string {
    return html.slice(html.indexOf('<head>'), html.indexOf('</head>'));
}

test('title: lom title, then cm:title, then cm:name', () => {
    assert.equal(pageTitle({ 'cclom:title': ['A'], 'cm:title': ['B'], 'cm:name': ['C'] }, false), 'A');
    assert.equal(pageTitle({ 'cclom:title': [' '], 'cm:title': ['B'], 'cm:name': ['C'] }, false), 'B');
    assert.equal(pageTitle({ 'cm:name': ['C'] }, false), 'C');
    assert.equal(pageTitle({}, false), undefined);
});

test('title: truncated to 45 characters before the branding suffix', () => {
    const long = 'x'.repeat(60);
    assert.equal(pageTitle({ 'cm:name': [long] }, false), `${'x'.repeat(42)}...`);
    assert.equal(pageTitle({ 'cm:name': [long] }, true), `${'x'.repeat(42)}... – edu-sharing`);
    assert.equal(pageTitle({ 'cm:name': ['x'.repeat(45)] }, false), 'x'.repeat(45));
});

test('description: lom description, then cm:description, truncated to 160', () => {
    assert.equal(pageDescription({ 'cclom:general_description': ['A'], 'cm:description': ['B'] }), 'A');
    assert.equal(pageDescription({ 'cm:description': ['B'] }), 'B');
    assert.equal(pageDescription({ 'cm:description': ['y'.repeat(200)] }), `${'y'.repeat(157)}...`);
    assert.equal(pageDescription({}), undefined);
});

test('render view: license, LRMI, oEmbed, title and description', () => {
    const html = enrichShell(SHELL, renderContext());
    const head = headOf(html);
    assert.match(head, /<link rel="license" href="https:\/\/cc.org\/by">/);
    assert.match(head, /<script type="application\/ld\+json">[\s\S]*"name": "Photosynthesis"[\s\S]*<\/script>/);
    assert.match(
        head,
        /<link rel="alternate" type="application\/json\+oembed" href="https:\/\/repo.example.org\/edu-sharing\/eduservlet\/oembed\?format=json&amp;url=https%3A%2F%2Frepo.example.org%2Fedu-sharing%2Fcomponents%2Frender%2Fn1"\/>/,
    );
    assert.match(head, /type="text\/xml\+oembed" href="[^"]*format=xml/);
    assert.match(head, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.match(head, /<meta name="description" content="How plants eat light">/);
    assert.equal(html.match(/<title>/g)?.length, 1);
});

test('render view: keeps NgServlet order - license, LRMI, oEmbed, then description', () => {
    const head = headOf(enrichShell(SHELL, renderContext()));
    const order = ['rel="license"', 'application/ld+json', 'json+oembed', 'xml+oembed', 'name="description"'].map((marker) =>
        head.indexOf(marker),
    );
    assert.deepEqual(
        order,
        [...order].sort((a, b) => a - b),
    );
    assert.ok(order.every((position) => position >= 0));
});

test('render view with exporter.lrmi.enrichment off: no LRMI, the rest stays', () => {
    const config: RepositoryConfig = { ...CONFIG, lrmi: { ...CONFIG.lrmi, enrichment: false } };
    const head = headOf(enrichShell(SHELL, renderContext({ config })));
    assert.doesNotMatch(head, /ld\+json/);
    assert.match(head, /rel="license"/);
    assert.match(head, /json\+oembed/);
    assert.match(head, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.match(head, /<meta name="description" content="How plants eat light">/);
});

test('render view without readable node: only the oEmbed links, as NgServlet', () => {
    const html = enrichShell(SHELL, renderContext({ loaded: undefined }));
    assert.match(html, /json\+oembed/);
    assert.doesNotMatch(html, /ld\+json|rel="license"|name="description"/);
    assert.match(html, /<title>edu-sharing<\/title>/);
});

test('without backend config: no LRMI and no branding, the rest stays', () => {
    const html = enrichShell(SHELL, renderContext({ config: null }));
    assert.doesNotMatch(html, /ld\+json/);
    assert.match(html, /<title>Photosynthesis<\/title>/);
    assert.match(html, /rel="license"/);
});

test('collection view: title and description only', () => {
    const html = enrichShell(SHELL, renderContext({ target: { kind: 'collection', nodeId: 'c1' } }));
    assert.match(html, /<title>Photosynthesis – edu-sharing<\/title>/);
    assert.match(html, /name="description"/);
    assert.doesNotMatch(html, /oembed|ld\+json|rel="license"/);
});

test('render view: canonical, Open Graph and Twitter card', () => {
    const head = headOf(enrichShell(SHELL, renderContext({ loaded: { ...loaded({ 'cclom:title': ['Photosynthesis'], 'cclom:general_description': ['How plants eat light'] }), previewUrl: 'https://repo.example.org/edu-sharing/preview?nodeId=n1&storeProtocol=workspace&storeId=SpacesStore' } })));
    assert.match(head, /<link rel="canonical" href="https:\/\/repo.example.org\/edu-sharing\/components\/render\/n1">/);
    assert.match(head, /<meta property="og:type" content="article">/);
    // the browser tab has the branding suffix, the share preview does not
    assert.match(head, /<meta property="og:title" content="Photosynthesis">/);
    assert.match(head, /<meta property="og:description" content="How plants eat light">/);
    assert.match(head, /<meta property="og:url" content="https:\/\/repo.example.org\/edu-sharing\/components\/render\/n1">/);
    assert.match(head, /<meta property="og:image" content="https:\/\/repo.example.org\/edu-sharing\/preview\?nodeId=n1&amp;storeProtocol=workspace&amp;storeId=SpacesStore">/);
    assert.match(head, /<meta name="twitter:card" content="summary_large_image">/);
});

test('render view of a version: the canonical is the url without the version', () => {
    const head = headOf(
        enrichShell(SHELL, renderContext({ target: { kind: 'render', nodeId: 'n1', version: '1.2' }, pageUrl: 'https://repo.example.org/edu-sharing/components/render/n1/1.2' })),
    );
    assert.match(head, /<link rel="canonical" href="https:\/\/repo.example.org\/edu-sharing\/components\/render\/n1">/);
    assert.doesNotMatch(head, /rel="canonical"[^>]*n1\/1\.2/);
});

test('collection view: canonical on the collection, Open Graph type website', () => {
    const head = headOf(enrichShell(SHELL, renderContext({ target: { kind: 'collection', nodeId: 'c1' } })));
    assert.match(head, /<link rel="canonical" href="https:\/\/repo.example.org\/edu-sharing\/components\/collections\?id=c1">/);
    assert.match(head, /<meta property="og:type" content="website">/);
    assert.match(head, /<meta property="og:url" content="https:\/\/repo.example.org\/edu-sharing\/components\/collections\?id=c1">/);
});

test('without a preview: no og:image and a small Twitter card', () => {
    const head = headOf(enrichShell(SHELL, renderContext()));
    assert.doesNotMatch(head, /og:image/);
    assert.match(head, /<meta name="twitter:card" content="summary">/);
});

test('without a description: no og:description', () => {
    const head = headOf(enrichShell(SHELL, renderContext({ loaded: loaded({ 'cclom:title': ['Photosynthesis'] }) })));
    assert.match(head, /og:title/);
    assert.doesNotMatch(head, /og:description/);
});

test('without readable node: neither canonical nor Open Graph', () => {
    const html = enrichShell(SHELL, renderContext({ loaded: undefined }));
    assert.doesNotMatch(html, /rel="canonical"|og:|twitter:/);
});

test('no title: the canonical stays, the Open Graph tags are left out', () => {
    const head = headOf(enrichShell(SHELL, renderContext({ loaded: loaded({ 'cclom:general_description': ['D'] }) })));
    assert.match(head, /rel="canonical"/);
    assert.doesNotMatch(head, /og:|twitter:/);
});

test('Open Graph values are escaped', () => {
    const evil = { ...loaded({ 'cclom:title': ['"><script>alert(1)</script>'], 'cclom:general_description': ['"><img src=x>'] }), previewUrl: 'https://x/p?a=1&b="><i>' };
    const html = enrichShell(SHELL, renderContext({ loaded: evil }));
    assert.doesNotMatch(html, /<script>alert|<img src=x>|<i>/);
});

test('no title at all: neither title nor description, like the NPE path in NgServlet', () => {
    const html = enrichShell(SHELL, renderContext({ loaded: loaded({ 'cclom:general_description': ['D'] }) }));
    assert.match(html, /<title>edu-sharing<\/title>/);
    assert.doesNotMatch(html, /name="description"/);
});

test('escapes metadata - a node title cannot inject markup', () => {
    const evil = loaded({ 'cclom:title': ['</title><script>alert(1)</script>'], 'cclom:general_description': ['"><img src=x>'] });
    const html = enrichShell(SHELL, renderContext({ loaded: evil }));
    assert.doesNotMatch(html, /<script>alert/);
    assert.doesNotMatch(html, /<img src=x>/);
    assert.match(html, /&lt;\/title&gt;/);
});

test('JSON-LD cannot close its script element', () => {
    const json = jsonForScript({ name: '</script><script>alert(1)</script>' });
    assert.doesNotMatch(json, /<\/script>/);
    assert.deepEqual(JSON.parse(json), { name: '</script><script>alert(1)</script>' });
});

test('setTitle adds a title when the shell has none', () => {
    assert.equal(setTitle('<head></head>', 'T'), '<head><title>T</title></head>');
});

test('setTitle is safe against $ patterns in the title', () => {
    assert.equal(setTitle('<head><title>x</title></head>', "$& $' $1"), "<head><title>$&amp; $&#39; $1</title></head>");
});
