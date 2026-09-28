import type { OutgoingHttpHeaders } from 'node:http';
import type { components } from '../shared/api-types.ts';
import type { JsonGetter } from './backend-client.ts';

/** REST models, generated from openapi.json by scripts/generate-api-types.mjs */
export type RestNode = components['schemas']['Node'];
type NodeEntry = components['schemas']['NodeEntry'];
type NodeVersionEntry = components['schemas']['NodeVersionEntry'];

export type NodeProperties = NonNullable<RestNode['properties']>;

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

export interface LoadedNode {
    readonly node: RestNode;
    /** of the requested version - the node's own when no version was requested */
    readonly properties: NodeProperties;
}

/**
 * `GET /node/v1/nodes/-home-/<id>/metadata`, plus `/versions/<major>/<minor>/metadata`
 * for a versioned render url: the version endpoint only carries the properties, while
 * preview, license and render url come from the node itself - as in LRMITool, which
 * also takes the preview of the current node.
 *
 * Runs anonymously (see backend-client.ts): a node the public may not read yields
 * undefined, and the shell then goes out without enrichment.
 */
export async function loadNode(
    client: JsonGetter,
    contextPath: string,
    target: ShellTarget,
    headers: OutgoingHttpHeaders,
): Promise<LoadedNode | undefined> {
    const base = `${contextPath}/rest/node/v1/nodes/-home-/${encodeURIComponent(target.nodeId)}`;
    const version = target.kind === 'render' ? parseVersion(target.version) : undefined;
    if (target.kind === 'render' && target.version !== undefined && version === undefined) {
        // an unparsable version fails in NgServlet as well - no enrichment
        return undefined;
    }
    const [current, versioned] = await Promise.all([
        client.getJson(`${base}/metadata?propertyFilter=-all-`, headers),
        version === undefined
            ? undefined
            : client.getJson(`${base}/versions/${version.major}/${version.minor}/metadata?propertyFilter=-all-`, headers),
    ]);
    const node = (current.body as Partial<NodeEntry> | undefined)?.node;
    if (node === undefined) {
        return undefined;
    }
    let properties: NodeProperties = node.properties ?? {};
    if (versioned !== undefined) {
        const versionProperties = (versioned.body as Partial<NodeVersionEntry> | undefined)?.version?.properties;
        if (versionProperties === undefined) {
            return undefined;
        }
        properties = versionProperties;
    }
    return { node, properties };
}

function parseVersion(version: string | undefined): { major: number; minor: number } | undefined {
    const match = version === undefined ? null : /^(\d+)\.(\d+)$/.exec(version);
    return match === null ? undefined : { major: Number(match[1]), minor: Number(match[2]) };
}
