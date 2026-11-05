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
    }
    return result;
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

export function escapeHtml(value: string): string {
    return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
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
