import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { RepositoryConfig } from './config-cache.ts';
import type { LoadedNode } from './node-metadata.ts';
import { enrichPage, enrichShell, jsonForScript, pageDescription, pageTexts, pageTitle, setTitle, type PageContext, type ShellContext } from './shell-head.ts';

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

// --- pages that are no node view ------------------------------------------------------

const APP_SHELL =
    '<html><head><base href="/edu-sharing/"><title>edu-sharing</title></head><body><es-app><div class="ng-loading"></div></es-app></body></html>';

function pageContext(overrides: Partial<PageContext> = {}): PageContext {
    return { page: 'start', texts: undefined, appUrl: 'https://repo.example.org/edu-sharing/', ...overrides };
}

const START_TEXTS = {
    title: 'ByCS-Mediathek | Unterrichtsmaterialien für Schulen in Bayern',
    description: 'Die ByCS-Mediathek bietet Lehrkräften Unterrichtsmaterialien.',
    heading: 'ByCS-Mediathek',
    intro: 'Das zentrale Medienportal.\n\nMit Videos, Audios und Arbeitsblättern.',
};

function bodyOf(html: string): string {
    return html.slice(html.indexOf('<body>'));
}

test('pageTexts: the entry of the page, blank texts are none, - becomes _', () => {
    const seo = {
        START: { TITLE: ' T ', DESCRIPTION: '  ', HEADING: 'H', INTRO: 5 },
        MY_PAGE: { TITLE: 'M' },
        SEARCH: 'not an object',
    };
    assert.deepEqual(pageTexts(seo, 'start'), { title: 'T', description: undefined, heading: 'H', intro: undefined });
    assert.deepEqual(pageTexts(seo, 'my-page'), { title: 'M', description: undefined, heading: undefined, intro: undefined });
    assert.equal(pageTexts(seo, 'search'), undefined);
    assert.equal(pageTexts(seo, 'collections'), undefined);
    assert.equal(pageTexts(undefined, 'start'), undefined);
});

test('page with all texts: title, description, canonical, Open Graph, WebSite with search, heading and text', () => {
    const html = enrichPage(APP_SHELL, pageContext({ texts: START_TEXTS }));
    const head = headOf(html);
    assert.match(head, /<title>ByCS-Mediathek \| Unterrichtsmaterialien für Schulen in Bayern<\/title>/);
    assert.equal(html.match(/<title>/g)?.length, 1);
    assert.match(head, /<meta name="description" content="Die ByCS-Mediathek bietet Lehrkräften Unterrichtsmaterialien.">/);
    assert.match(head, /<link rel="canonical" href="https:\/\/repo.example.org\/edu-sharing\/components\/start">/);
    assert.match(head, /<meta property="og:title" content="ByCS-Mediathek \| /);
    assert.match(head, /<meta property="og:description" content="Die ByCS-Mediathek/);
    assert.match(head, /<meta property="og:type" content="website">/);
    assert.match(head, /<meta property="og:url" content="https:\/\/repo.example.org\/edu-sharing\/components\/start">/);
    assert.match(head, /<meta name="twitter:card" content="summary">/);
    const ld = /<script type="application\/ld\+json">([\s\S]*?)<\/script>/.exec(head);
    assert.ok(ld, 'JSON-LD');
    assert.deepEqual(JSON.parse(ld[1] as string), {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: 'ByCS-Mediathek',
        url: 'https://repo.example.org/edu-sharing/components/start',
        potentialAction: {
            '@type': 'SearchAction',
            target: 'https://repo.example.org/edu-sharing/components/search?q={search_term_string}',
            'query-input': 'required name=search_term_string',
        },
    });
    const body = bodyOf(html);
    assert.match(body, /<div id="seo-content"><h1>ByCS-Mediathek<\/h1><p>Das zentrale Medienportal.<\/p><p>Mit Videos, Audios und Arbeitsblättern.<\/p><\/div><\/es-app>/);
});

test('page text goes below the loading animation, inside the app element', () => {
    const body = bodyOf(enrichPage(APP_SHELL, pageContext({ texts: START_TEXTS })));
    assert.ok(body.indexOf('ng-loading') < body.indexOf('seo-content'));
    assert.ok(body.indexOf('seo-content') < body.indexOf('</es-app>'));
});

test('start, search and collections get a canonical url without any texts', () => {
    for (const page of ['start', 'search', 'collections']) {
        const html = enrichPage(APP_SHELL, pageContext({ page }));
        assert.match(headOf(html), new RegExp(`<link rel="canonical" href="https://repo.example.org/edu-sharing/components/${page}">`));
        assert.match(html, /<title>edu-sharing<\/title>/);
        assert.doesNotMatch(html, /og:|name="description"|ld\+json|seo-content/);
    }
});

test('a page nobody wrote texts for stays as it is', () => {
    assert.equal(enrichPage(APP_SHELL, pageContext({ page: 'login' })), APP_SHELL);
    assert.equal(enrichPage(APP_SHELL, pageContext({ page: 'workspace', texts: undefined })), APP_SHELL);
});

test('a page with texts is enriched even when it is no entry page of the core', () => {
    const html = enrichPage(APP_SHELL, pageContext({ page: 'my-page', texts: { title: 'Mine' } }));
    assert.match(headOf(html), /rel="canonical" href="https:\/\/repo.example.org\/edu-sharing\/components\/my-page"/);
    assert.match(html, /<title>Mine<\/title>/);
    // no WebSite data: that is for the start page
    assert.doesNotMatch(html, /ld\+json/);
});

test('every part is left out when its text is missing', () => {
    const titleOnly = enrichPage(APP_SHELL, pageContext({ page: 'search', texts: { title: 'Suche' } }));
    assert.match(titleOnly, /<title>Suche<\/title>/);
    assert.match(titleOnly, /og:title/);
    assert.doesNotMatch(titleOnly, /name="description"|og:description|seo-content/);
    const headingOnly = enrichPage(APP_SHELL, pageContext({ texts: { heading: 'Mediathek' } }));
    assert.match(headingOnly, /<h1>Mediathek<\/h1>/);
    // the WebSite data take the heading as name when there is no title
    assert.match(headingOnly, /"name": "Mediathek"/);
    assert.match(headingOnly, /<title>edu-sharing<\/title>/);
    assert.doesNotMatch(headingOnly, /og:/);
});

test('the WebSite data are for the start page only', () => {
    const html = enrichPage(APP_SHELL, pageContext({ page: 'search', texts: START_TEXTS }));
    assert.doesNotMatch(html, /ld\+json/);
});

test('page texts are escaped, a text cannot inject markup', () => {
    const evil = { title: '</title><script>alert(1)</script>', description: '"><img src=x>', heading: '<b>x</b>', intro: '<script>alert(2)</script>' };
    const html = enrichPage(APP_SHELL, pageContext({ texts: evil }));
    assert.doesNotMatch(html, /<script>alert|<img src=x>|<b>x<\/b>/);
    assert.match(html, /&lt;script&gt;alert\(2\)&lt;\/script&gt;/);
    // JSON-LD cannot close its script element
    assert.equal(html.match(/<script/g)?.length, 1);
});

test('a shell without the app element only gets the head', () => {
    const html = enrichPage('<html><head></head><body></body></html>', pageContext({ texts: START_TEXTS }));
    assert.match(html, /rel="canonical"/);
    assert.doesNotMatch(html, /seo-content/);
});
