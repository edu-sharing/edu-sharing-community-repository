import { Injectable } from '@angular/core';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    Node,
    RestConstants,
    RestMdsService,
    RestNodeService,
} from '../../../core-module/core.module';
import { MdsDefinition, MdsType, Values } from '../types/types';
import {Metadata, BaseMetadataFragment} from 'ngx-edu-sharing-graphql';
import {Meta} from '@angular/platform-browser';
import {Apollo} from 'apollo-angular';
import {DocumentNode} from '@apollo/client/link/core/types';

/** Error with a translatable message that is suitable to be shown to the user. */
export class UserPresentableError extends Error {
    constructor(message: string) {
        super(message);
        // Apparently, this is what it's gonna be for now...
        // https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
        Object.setPrototypeOf(this, UserPresentableError.prototype);
        this.name = 'UserPresentableError';
    }
}

/**
 * Common functions used by the metadata-set editor.
 *
 * Usable by native MDS editor and legacy `<mds>` component.
 */
@Injectable({
    providedIn: 'root',
})
export class MdsEditorCommonService {
    constructor(
        private restNode: RestNodeService,
        private mdsService: RestMdsService,
        private apollo: Apollo,
    ) {}

    /**
     * Fetches the up-to-date and complete metadata from the server.
     */
    @memoizeSingle
    async fetchNodesMetadata(nodes: Node[]): Promise<Node[]> {
        return forkJoin(
            nodes.map((node) =>
                this.restNode
                    .getNodeMetadata(node.ref.id, [RestConstants.ALL])
                    .pipe(map((nodeWrapper) => nodeWrapper.node)),
            ),
        ).toPromise();
    }

    async saveNodesMetadata(
        pairs: Array<{ id?: string; node?: Node; values: Values }>,
        versionComment?: string,
    ): Promise<Node[]> {
        console.log(pairs);
        return forkJoin(
            pairs.map(({id, node, values }) => {
                if (versionComment) {
                    return this.restNode
                        .editNodeMetadataNewVersion(node?.ref?.id || id, versionComment, values)
                        .pipe(map((nodeWrapper) => nodeWrapper.node));
                } else {
                    return this.restNode
                        .editNodeMetadata(node?.ref?.id || id, values)
                        .pipe(map((nodeWrapper) => nodeWrapper.node));
                }
            }),
        ).toPromise();
    }
    async saveGraphqlMetadata(
        pairs: Metadata[],
        versionComment?: string,
    ): Promise<Node[]> {
        return forkJoin(
            pairs.map((metadata) => {
                console.log('graphql target metadata', metadata);
                /*this.apollo.mutate({
                    variables: {
                        id
                    },
                    mutation: DocumentNode
                })*/
            }),
        ).toPromise();
    }

    async saveNodeContent(node: Node, file: File, versionComment?: string): Promise<void> {
        return this.restNode
            .uploadNodeContent(
                node.ref.id,
                file,
                versionComment || RestConstants.COMMENT_CONTENT_UPDATE,
            )
            .pipe(map((r) => null))
            .toPromise();
    }

    async saveNodeProperty(
        node: Node,
        property: string,
        values: string[]
        //versionComment?: string,
    ): Promise<void> {
        this.restNode.editNodeProperty(node.ref.id, property, values).toPromise();

    }

    /**
     * Gets the relevant MDS ID for the given nodes.
     *
     * Throws with a translatable error message if the given nodes cannot be handled by an MDS.
     */
    getMdsId(nodes: Node[]|BaseMetadataFragment[]): string {
        const types = nodes.map((node) => (node as Node).type || (node as Metadata).nodeType);
        if (!areAllEqual(types)) {
            throw new UserPresentableError('MDS.ERROR_INVALID_TYPE_COMBINATION');
        }
        const requestedMdsIds = nodes.map((node) => (node as Node).metadataset || (node as Metadata).info.metadataSet || RestConstants.DEFAULT);
        if (!areAllEqual(requestedMdsIds)) {
            throw new UserPresentableError('MDS.ERROR_INVALID_MDS_COMBINATION');
        }
        if (nodes[0] instanceof Node &&
            (nodes as Node[]).filter(
                (n) => !!n.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]).length >0
        ) {
            throw new UserPresentableError('MDS.ERROR_ELEMENT_TYPE_UNSUPPORTED');
        }
        return requestedMdsIds[0];
    }
    getGroupIdGraphql(nodes: BaseMetadataFragment[]): MdsType {
        const node = nodes[0];
        let nodeGroup: MdsType = node.nodeType === RestConstants.CCM_TYPE_MAP ? MdsType.Map : MdsType.Io;
        if (node.info.objectType?.id === 'folder-link') {
            nodeGroup = MdsType.MapRef;
        }
        if (node.info.aspects?.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) !== -1) {
            nodeGroup = MdsType.IoChildObject;
        }
        if (node.info.aspects?.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1) {
            nodeGroup = MdsType.Collection;
        }
        if (node.info.aspects?.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION) !== -1) {
            nodeGroup = MdsType.ToolDefinition;
        }
        if (node.nodeType === RestConstants.CCM_TYPE_TOOL_INSTANCE) {
            nodeGroup = MdsType.ToolInstance;
        }
        if (node.nodeType === RestConstants.CCM_TYPE_SAVED_SEARCH) {
            nodeGroup = MdsType.SavedSearch;
        }
        if (nodes.length > 1) {
            if (nodeGroup !== MdsType.Io) {
                throw new UserPresentableError('MDS.ERROR_INVALID_TYPE_BULK');
            }
            nodeGroup = MdsType.IoBulk;
        }
        return nodeGroup;
    }
    getGroupId(nodes: Node[]): MdsType {
        const node = nodes[0];
        let nodeGroup: MdsType = node.isDirectory ? MdsType.Map : MdsType.Io;
        if (node.mediatype == 'folder-link') {
            nodeGroup = MdsType.MapRef;
        }
        if (node.aspects?.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) !== -1) {
            nodeGroup = MdsType.IoChildObject;
        }
        if (node.aspects?.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1) {
            nodeGroup = MdsType.Collection;
        }
        if (node.aspects?.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION) !== -1) {
            nodeGroup = MdsType.ToolDefinition;
        }
        if (node.type === RestConstants.CCM_TYPE_TOOL_INSTANCE) {
            nodeGroup = MdsType.ToolInstance;
        }
        if (node.type === RestConstants.CCM_TYPE_SAVED_SEARCH) {
            nodeGroup = MdsType.SavedSearch;
        }
        if (nodes.length > 1) {
            if (nodeGroup !== MdsType.Io) {
                throw new UserPresentableError('MDS.ERROR_INVALID_TYPE_BULK');
            }
            nodeGroup = MdsType.IoBulk;
        }
        return nodeGroup;
    }
}

function areAllEqual<T>(elements: T[]): boolean {
    return elements.every((element) => element === elements[0]);
}

/**
 * Caches a single result for a limited time.
 *
 * All arguments must be equal for a cache hit.
 */
function memoizeSingle(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const TIMEOUT = 500;
    const originalMethod = descriptor.value;
    let cachedArgs: any[];
    let cachedResult: any;
    descriptor.value = function (...args: any[]) {
        if (
            cachedResult &&
            args.length === cachedArgs.length &&
            args.every((value, index) => value === cachedArgs[index])
        ) {
            return cachedResult;
        } else {
            setTimeout(() => {
                cachedResult = null;
            }, TIMEOUT);
            const result = originalMethod.apply(this, args);
            cachedArgs = args;
            cachedResult = result;
            return result;
        }
    };
}

/**
 * Caches results based on arguments.
 *
 * Arguments are serialized to identify cache hits.
 */
function memoize(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    const originalMethod = descriptor.value;
    const cache: { [argsSerialized: string]: any } = {};
    descriptor.value = function (...args: any[]) {
        const argsSerialized = JSON.stringify(args);
        if (!(argsSerialized in cache)) {
            cache[argsSerialized] = originalMethod.apply(this, args);
        }
        return cache[argsSerialized];
    };
}
