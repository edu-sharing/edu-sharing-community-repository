import { getLicenseUrl } from '../shared/license-url.ts';
import type { ElasticSearcher, SearchRequest } from './elastic-client.ts';
import type { LoadedNode, NodeProperties, ShellTarget } from './node-metadata.ts';

/** `StoreRef.STORE_REF_WORKSPACE_SPACESSTORE` */
const STORE_PROTOCOL = 'workspace';
const STORE_ID = 'SpacesStore';

/**
 * The query. Both security filters are part of it on purpose - a node that is not
 * public, or that has been deleted, must not even be returned, so no later code can
 * leak it by omission.
 *
 * * `permissions.read: GROUP_EVERYONE` - only publicly readable elements. This server
 *   forwards no identity (see backend-client.ts), so public is all it may show.
 * * `nodeRef.storeRef.protocol: workspace` - a deleted node stays in the index under
 *   the SAME `_id` and only switches to `archive`, keeping its permissions. Without
 *   this filter deleted content would be handed to crawlers.
 */
export function publicNodeQuery(index: string, nodeId: string): SearchRequest {
    return {
        index,
        size: 1,
        _source: ['properties'],
        query: {
            bool: {
                filter: [
                    { ids: { values: [nodeId] } },
                    { term: { 'permissions.read': 'GROUP_EVERYONE' } },
                    { term: { 'nodeRef.storeRef.protocol': STORE_PROTOCOL } },
                ],
            },
        },
    };
}

export interface ElasticNodeContext {
    readonly index: string;
    /** `https://host/edu-sharing`, for the preview url */
    readonly appBaseUrl: string;
    /** `{{language}}` of the license url, from the browser's Accept-Language */
    readonly language: string | undefined;
}

/**
 * Loads the properties of a publicly readable node from the search index.
 *
 * Replaces the former `/rest/node/v1/nodes/-home-/<id>/metadata` call: since this
 * server stopped forwarding the browser identity, that call could only ever see
 * public nodes anyway - the index holds the same data without the Tomcat round trip.
 *
 * Returns undefined for anything not public, not in the index or archived; the shell
 * then goes out without enrichment.
 */
export async function loadNode(
    client: ElasticSearcher,
    target: ShellTarget,
    context: ElasticNodeContext,
): Promise<LoadedNode | undefined> {
    if (target.kind === 'render' && target.version !== undefined) {
        // A versioned render url is not enriched. The index holds one document per
        // node, in its current state - there are no version documents. Answering with
        // the current metadata under a url that asks for a specific version would be
        // wrong, so nothing is sent rather than something false. No query is issued.
        return undefined;
    }
    const source = await client.firstSource(publicNodeQuery(context.index, target.nodeId));
    if (source === undefined) {
        return undefined;
    }
    const properties = normalizeProperties(source['properties']);
    return {
        properties,
        mimetype: first(properties['cclom:format']),
        previewUrl: previewUrl(context.appBaseUrl, target.nodeId),
        licenseUrl:
            getLicenseUrl(
                first(properties['ccm:commonlicense_key']) ?? '',
                first(properties['ccm:commonlicense_cc_version']) ?? '',
                first(properties['ccm:commonlicense_cc_locale']) ?? '',
                context.language ?? '',
            ) ?? undefined,
    };
}

/**
 * `NodeServiceImpl.getPreviewUrl`, minus its `dontcache` timestamp: this url ends up
 * in the LRMI `thumbnailUrl`, where a cache buster that changes on every page view is
 * harmful. Built from the requested origin, like the LRMI `url` and the oEmbed links.
 */
export function previewUrl(appBaseUrl: string, nodeId: string): string {
    return `${appBaseUrl}/preview?nodeId=${encodeURIComponent(nodeId)}&storeProtocol=${STORE_PROTOCOL}&storeId=${STORE_ID}`;
}

/**
 * The primary tag of an Accept-Language header, e.g. "de-DE,de;q=0.9" -> "de".
 * The app passes `TranslateService.currentLang` here; this server has no session, so
 * it takes what the browser asks for.
 */
export function primaryLanguage(acceptLanguage: string | undefined): string | undefined {
    const preferred = acceptLanguage?.split(',')[0]?.split(';')[0]?.trim();
    if (preferred === undefined || preferred === '' || preferred === '*') {
        return undefined;
    }
    return preferred.split('-')[0]?.toLowerCase();
}

/**
 * The index stores property values as they were indexed: `cclom:title` is a string,
 * `ccm:educontextname` an array, `cm:modified` a number. Everything downstream expects
 * the REST shape, i.e. an array of strings per property - and reads it with
 * `properties[key]?.[0]`, which on a bare string would silently yield its FIRST
 * CHARACTER. So the shape is normalized here, at the boundary, once.
 *
 * `<prop>_LONG` is aliased onto `<prop>`: the REST API exposed the epoch millis under
 * that name while the index stores them under the plain one. This keeps both the built
 * in date fields and a custom `exporter.lrmi` mapping working.
 */
export function normalizeProperties(raw: unknown): NodeProperties {
    if (raw === null || typeof raw !== 'object') {
        return {};
    }
    const properties: Record<string, string[]> = {};
    for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
        const values = toStringArray(value);
        if (values === undefined) {
            continue;
        }
        properties[key] = values;
        if (!key.endsWith('_LONG')) {
            properties[`${key}_LONG`] ??= values;
        }
    }
    return properties;
}

function toStringArray(value: unknown): string[] | undefined {
    if (value === null || value === undefined) {
        return undefined;
    }
    const values = (Array.isArray(value) ? value : [value])
        .filter((entry) => entry !== null && entry !== undefined && typeof entry !== 'object')
        .map((entry) => String(entry));
    return values.length === 0 ? undefined : values;
}

function first(values: string[] | undefined): string | undefined {
    return values?.[0];
}
