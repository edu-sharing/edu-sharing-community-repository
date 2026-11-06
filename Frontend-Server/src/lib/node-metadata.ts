import type { components } from '../shared/api-types.ts';

/**
 * The property map shape, still taken from the REST model so it stays the one
 * definition of "a node's properties" - the values now come from the search index
 * and are normalized into this shape by `normalizeProperties` in elastic-node.ts.
 */
export type NodeProperties = NonNullable<components['schemas']['Node']['properties']>;

/**
 * What `NgServlet.getNodeFromURL` extracts from a shell url:
 * `components/render/<id>[/<version>]` or `components/collections?id=<id>`.
 */
export type ShellTarget =
    | { readonly kind: 'render'; readonly nodeId: string; readonly version: string | undefined }
    | { readonly kind: 'collection'; readonly nodeId: string };

const NODE_ID = /^[A-Za-z0-9-]+$/;

/** @param url request url below the context path, e.g. `/components/render/abc/1.0?x=y` */
export function parseShellTarget(url: string): ShellTarget | undefined {
    const parsed = new URL(url, 'http://localhost');
    const segments = parsed.pathname.split('/').filter((segment) => segment !== '');
    if (segments[0] !== 'components') {
        return undefined;
    }
    if (segments[1] === 'render') {
        const nodeId = segments[2];
        if (nodeId === undefined || !NODE_ID.test(nodeId)) {
            return undefined;
        }
        const version = segments[3];
        // URLHelper.getNgRenderNodeUrl treats "-1" as "latest"
        return { kind: 'render', nodeId, version: version === undefined || version === '-1' ? undefined : version };
    }
    if (segments[1] === 'collections') {
        const nodeId = parsed.searchParams.get('id');
        return nodeId !== null && NODE_ID.test(nodeId) ? { kind: 'collection', nodeId } : undefined;
    }
    return undefined;
}

/**
 * What the shell enrichment needs about a node. Deliberately not the REST `Node`
 * model any more: the data comes from the search index, and only these four things
 * are read (license link, LRMI thumbnailUrl / encodingFormat, title, description).
 * A nodeshaped stand-in would claim fields nobody fills.
 */
export interface LoadedNode {
    readonly properties: NodeProperties;
    /** `AlfrescoMimeType` of the LRMI mapping, from `cclom:format` */
    readonly mimetype: string | undefined;
    readonly previewUrl: string | undefined;
    readonly licenseUrl: string | undefined;
}
