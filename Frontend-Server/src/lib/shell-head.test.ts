import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { RepositoryConfig } from './config-cache.ts';
import type { LoadedNode } from './node-metadata.ts';
import { enrichShell, jsonForScript, pageDescription, pageTitle, setTitle, type ShellContext } from './shell-head.ts';

const SHELL = '<html><head><base href="/edu-sharing/"><title>edu-sharing</title></head><body></body></html>';

const CONFIG: RepositoryConfig = {
    branding: true,
    lrmi: { properties: { name: 'cclom:title,cm:name' }, vcard: {} },
};

function loaded(properties: LoadedNode['properties'], licenseUrl?: string): LoadedNode {
    return {
        node: { ref: { id: 'n1' }, properties, ...(licenseUrl === undefined ? {} : { license: { url: licenseUrl } }) },
        properties,
        setCookie: undefined,
    };
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
