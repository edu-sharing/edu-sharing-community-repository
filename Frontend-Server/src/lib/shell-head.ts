import escapeHtml from 'escape-html';
import type { RepositoryConfig } from './config-cache.ts';
import { buildLrmi } from './lrmi.ts';
import type { LoadedNode, NodeProperties, ShellTarget } from './node-metadata.ts';

/** `NgServlet.MAX_TITLE_LENGTH` / `MAX_DESCRIPTION_LENGTH` */
const MAX_TITLE_LENGTH = 45;
const MAX_DESCRIPTION_LENGTH = 160;
const BRANDING_SUFFIX = ' – edu-sharing';

const TITLE_PROPERTIES = ['cclom:title', 'cm:title', 'cm:name'] as const;
const DESCRIPTION_PROPERTIES = ['cclom:general_description', 'cm:description'] as const;

export interface ShellContext {
    readonly target: ShellTarget;
    /** undefined when the node could not be read - missing, forbidden or backend down */
    readonly loaded: LoadedNode | undefined;
    /** null when the backend config is not available */
    readonly config: RepositoryConfig | null;
    /** the page url as the browser requested it, the oEmbed `url` */
    readonly pageUrl: string;
    /** public base of the app, `https://host/edu-sharing/` */
    readonly appUrl: string;
}

/**
 * The `<head>` enrichment of `NgServlet.doGet`, in its order:
 * render view -> license link, LRMI JSON-LD (unless `exporter.lrmi.enrichment` is off), oEmbed links; render and collection
 * view -> title and description. Every part is skipped on its own when its data is
 * missing, so a failing backend degrades to the plain shell, never to an error.
 */
export function enrichShell(html: string, context: ShellContext): string {
    const { target, loaded, config } = context;
    const head: string[] = [];
    if (target.kind === 'render') {
        const licenseUrl = loaded?.licenseUrl;
        if (licenseUrl) {
            head.push(`<link rel="license" href="${escapeHtml(licenseUrl)}">`);
        }
        if (loaded !== undefined && config !== null && config.lrmi.enrichment) {
            const lrmi = buildLrmi(loaded, config.lrmi, renderUrl(context.appUrl, target.nodeId, target.version));
            head.push(`<script type="application/ld+json">${jsonForScript(lrmi)}</script>`);
        }
        const oembed = `${context.appUrl}eduservlet/oembed`;
        const url = encodeURIComponent(context.pageUrl);
        head.push(`<link rel="alternate" type="application/json+oembed" href="${escapeHtml(`${oembed}?format=json&url=${url}`)}"/>`);
        head.push(`<link rel="alternate" type="text/xml+oembed" href="${escapeHtml(`${oembed}?format=xml&url=${url}`)}"/>`);
    }
    let result = insertIntoHead(html, head.join(''));
    if (loaded !== undefined) {
        const title = pageTitle(loaded.properties, config?.branding ?? false);
        if (title !== undefined) {
            result = setTitle(result, title);
            const description = pageDescription(loaded.properties);
            if (description !== undefined) {
                result = insertIntoHead(result, `<meta name="description" content="${escapeHtml(description)}">`);
            }
        }
        result = insertIntoHead(result, socialHead(context, loaded));
    }
    return result;
}

/**
 * `canonical`, Open Graph and the Twitter card of a render or collection view.
 *
 * The canonical is the url of the node without a version: an old version is the same content
 * for a search engine, the current one should collect the ranking. The Open Graph title is the
 * title without the branding suffix, the suffix is for the browser tab.
 *
 * No title (same as for the `<title>`): the canonical stays, the Open Graph tags are left out -
 * `og:title` is required, the others mean nothing without it.
 */
export function socialHead(context: ShellContext, loaded: LoadedNode): string {
    const { target } = context;
    const url = target.kind === 'render' ? renderUrl(context.appUrl, target.nodeId, undefined) : collectionUrl(context.appUrl, target.nodeId);
    const tags = [`<link rel="canonical" href="${escapeHtml(url)}">`];
    const title = pageTitle(loaded.properties, false);
    if (title !== undefined) {
        tags.push(metaProperty('og:type', target.kind === 'render' ? 'article' : 'website'));
        tags.push(metaProperty('og:title', title));
        tags.push(metaProperty('og:url', url));
        const description = pageDescription(loaded.properties);
        if (description !== undefined) {
            tags.push(metaProperty('og:description', description));
        }
        if (loaded.previewUrl !== undefined) {
            tags.push(metaProperty('og:image', loaded.previewUrl));
        }
        tags.push(`<meta name="twitter:card" content="${loaded.previewUrl !== undefined ? 'summary_large_image' : 'summary'}">`);
    }
    return tags.join('');
}

function metaProperty(property: string, content: string): string {
    return `<meta property="${property}" content="${escapeHtml(content)}">`;
}

/** `URLTool.getNgCollectionUrl` */
export function collectionUrl(appUrl: string, nodeId: string): string {
    return `${appUrl}components/collections?id=${nodeId}`;
}

/**
 * The SEO texts of a page, the entries `SEO.<PAGE>.TITLE`, `.DESCRIPTION`, `.HEADING` and `.INTRO` of
 * the translations. The defaults are part of the build, a client overrides them in its
 * `client.config.xml` like any other text (per language, per context) - nothing is configured twice.
 */
export interface PageTexts {
    readonly title?: string | undefined;
    readonly description?: string | undefined;
    /** the visible `<h1>` of the page for a crawler that does not run the app */
    readonly heading?: string | undefined;
    /** the text below the heading, paragraphs separated by an empty line */
    readonly intro?: string | undefined;
}

/** the pages every instance has a public entry for - they get a canonical url even without own texts */
const CANONICAL_PAGES: ReadonlySet<string> = new Set(['start', 'search', 'collections']);

/**
 * @param seo the `SEO` entry of the translations (`Translations.section`)
 * @param page the page name of the url (`search`), its entry is `SEO.SEARCH`, `my-page` is `SEO.MY_PAGE`
 * @returns undefined if nobody wrote an entry for the page
 */
export function pageTexts(seo: Readonly<Record<string, unknown>> | undefined, page: string): PageTexts | undefined {
    const entry = seo?.[page.toUpperCase().replace(/-/g, '_')];
    if (entry === null || typeof entry !== 'object' || Array.isArray(entry)) {
        return undefined;
    }
    const text = (key: string): string | undefined => {
        const value = (entry as Record<string, unknown>)[key];
        return typeof value === 'string' && value.trim() !== '' ? value.trim() : undefined;
    };
    return { title: text('TITLE'), description: text('DESCRIPTION'), heading: text('HEADING'), intro: text('INTRO') };
}

export interface PageContext {
    /** the page name of the url, `search` for `components/search` */
    readonly page: string;
    /** undefined: no entry for the page, or no translations at all (backend down, unknown language) */
    readonly texts: PageTexts | undefined;
    /** public base of the app, `https://host/edu-sharing/` */
    readonly appUrl: string;
}

/**
 * The `<head>` and the content of a page that is no node view (start page, search, collections
 * overview ...) for a crawler that does not run the app: canonical, title, description, Open
 * Graph, for the start page the `WebSite` data with the search, and the heading and text.
 *
 * The start page, the search and the collections get a canonical url without any texts - it
 * costs nothing and keeps the variants of the url (`?q=`) from competing. Every other page only
 * gets enriched when somebody wrote texts for it, so a page like the login stays as it is.
 * Every part is skipped on its own when its text is missing.
 */
export function enrichPage(html: string, context: PageContext): string {
    const { page, texts, appUrl } = context;
    if (texts === undefined && !CANONICAL_PAGES.has(page)) {
        return html;
    }
    const url = `${appUrl}components/${page}`;
    const head = [`<link rel="canonical" href="${escapeHtml(url)}">`];
    let result = html;
    if (texts?.title !== undefined) {
        result = setTitle(result, texts.title);
        head.push(metaProperty('og:title', texts.title));
    }
    if (texts?.description !== undefined) {
        head.push(`<meta name="description" content="${escapeHtml(texts.description)}">`);
        head.push(metaProperty('og:description', texts.description));
    }
    if (texts?.title !== undefined || texts?.description !== undefined) {
        head.push(metaProperty('og:type', 'website'));
        head.push(metaProperty('og:url', url));
        head.push('<meta name="twitter:card" content="summary">');
    }
    const name = texts?.heading ?? texts?.title;
    if (page === 'start' && name !== undefined) {
        const search = `${appUrl}components/search?q={search_term_string}`;
        const website = {
            '@context': 'https://schema.org',
            '@type': 'WebSite',
            name,
            url,
            potentialAction: { '@type': 'SearchAction', target: search, 'query-input': 'required name=search_term_string' },
        };
        head.push(`<script type="application/ld+json">${jsonForScript(website)}</script>`);
    }
    result = insertIntoHead(result, head.join(''));
    return insertIntoApp(result, pageContent(texts));
}

/** the heading and the paragraphs of a page, empty without both */
function pageContent(texts: PageTexts | undefined): string {
    const parts: string[] = [];
    if (texts?.heading !== undefined) {
        parts.push(`<h1>${escapeHtml(texts.heading)}</h1>`);
    }
    for (const paragraph of (texts?.intro ?? '').split(/\n{2,}/)) {
        if (paragraph.trim() !== '') {
            parts.push(`<p>${escapeHtml(paragraph.trim())}</p>`);
        }
    }
    return parts.length === 0 ? '' : `<div id="seo-content">${parts.join('')}</div>`;
}

/**
 * Appends to the content of the app element, below the loading animation (it fills the window,
 * the text is a scroll away). The app replaces everything in its element when it starts, so
 * nothing of this stays next to the real page.
 */
function insertIntoApp(html: string, fragment: string): string {
    if (fragment === '') {
        return html;
    }
    const position = html.indexOf('</es-app>');
    return position === -1 ? html : html.slice(0, position) + fragment + html.slice(position);
}

/**
 * `NgServlet.addSEO`: first non blank of lom title / cm:title / cm:name, truncated.
 * NgServlet fails with a NullPointerException without any title and then adds
 * neither title nor description - hence undefined.
 */
export function pageTitle(properties: NodeProperties, branding: boolean): string | undefined {
    const title = firstFilled(properties, TITLE_PROPERTIES);
    if (title === undefined) {
        return undefined;
    }
    return truncate(title, MAX_TITLE_LENGTH) + (branding ? BRANDING_SUFFIX : '');
}

export function pageDescription(properties: NodeProperties): string | undefined {
    const description = firstFilled(properties, DESCRIPTION_PROPERTIES);
    return description === undefined ? undefined : truncate(description, MAX_DESCRIPTION_LENGTH);
}

/** `URLHelper.getNgRenderNodeUrl` */
export function renderUrl(appUrl: string, nodeId: string, version: string | undefined): string {
    return `${appUrl}components/render/${nodeId}${version === undefined ? '' : `/${version}`}`;
}

function firstFilled(properties: NodeProperties, keys: readonly string[]): string | undefined {
    for (const key of keys) {
        const value = properties[key]?.[0];
        if (value !== undefined && value.trim() !== '') {
            return value;
        }
    }
    return undefined;
}

function truncate(value: string, max: number): string {
    return value.length > max ? `${value.substring(0, max - 3)}...` : value;
}

/**
 * JSON inside `<script>` must not contain `</script>` or `<!--` - escaping every `<`
 * keeps it valid JSON and inert HTML.
 */
export function jsonForScript(value: unknown): string {
    return JSON.stringify(value, null, 2).replace(/</g, '\\u003c');
}

/** `NgServlet.addToHead`: right before the first `</head>`. */
export function insertIntoHead(html: string, fragment: string): string {
    if (fragment === '') {
        return html;
    }
    const position = html.indexOf('</head>');
    return position === -1 ? html : html.slice(0, position) + fragment + html.slice(position);
}

/** Replaces the existing `<title>`, or adds one - as Jsoup's `Document.title` does. */
export function setTitle(html: string, title: string): string {
    const element = `<title>${escapeHtml(title)}</title>`;
    const existing = /<title>[\s\S]*?<\/title>/i;
    return existing.test(html) ? html.replace(existing, () => element) : insertIntoHead(html, element);
}
