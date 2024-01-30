import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { NodeV1Service, SearchV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node, NodeEntries, NodePermissions } from '../models';
import { NodeStats } from '../api/models/node-stats';

export class NodeConstants {
    public static SPACES_STORE_REF = 'workspace://SpacesStore/';
}
export class NodeTools {
    static createSpacesStoreRef(id: string) {
        return NodeConstants.SPACES_STORE_REF + id;
    }
    static removeSpacesStoreRef(id: string) {
        if (id.startsWith(NodeConstants.SPACES_STORE_REF)) {
            return id.substr(NodeConstants.SPACES_STORE_REF.length);
        }
        return id;
    }
}
@Injectable({
    providedIn: 'root',
})
export class NodeService {
    private readonly _nodesChanged = new Subject<void>();
    readonly nodesChanged = this._nodesChanged.asObservable();

    constructor(private nodeV1: NodeV1Service, private searchV1: SearchV1Service) {}

    getNode(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node> {
        return this.nodeV1
            .getMetadata({
                repository,
                node: id,
                propertyFilter: ['-all-'],
            })
            .pipe(map((nodeEntry) => nodeEntry.node));
    }

    deleteNode(
        id: string,
        { recycle = true, repository = HOME_REPOSITORY } = {},
    ): Observable<void> {
        return this.nodeV1
            .delete({ repository, recycle, node: id })
            .pipe(tap(() => this._nodesChanged.next()));
    }

    getChildren(
        parent: string,
        {
            repository = HOME_REPOSITORY,
            ...params
        }: Partial<Omit<Parameters<NodeV1Service['getChildren']>[0], 'node'>> = {},
    ) {
        return this.nodeV1.getChildren({
            repository,
            node: parent,
            propertyFilter: ['-all-'],
            ...params,
        });
    }

    /**
     * return the forked childs (variants) of this node
     * @returns
     */
    getForkedChilds(node: Node, { repository = HOME_REPOSITORY } = {}) {
        let id = node.ref.id;
        // if it's a published copy, use the original node id
        // since variants are always forked from their
        if (node.properties?.['ccm:published_original']) {
            id = NodeTools.removeSpacesStoreRef(node.properties['ccm:published_original'][0]);
        }
        return this.searchV1.searchByProperty({
            repository,
            comparator: ['='],
            contentType: 'FILES',
            property: ['ccm:forked_origin'],
            propertyFilter: ['-all-'],
            value: [NodeTools.createSpacesStoreRef(id)],
        });
    }

    getPublishedCopies(id: string, { repository = HOME_REPOSITORY } = {}) {
        return this.nodeV1.getPublishedCopies({
            repository,
            node: id,
        });
    }

    /**
     * Changes the properties of the given node.
     *
     * If `versionComment` is provided, creates a new version, otherwise changes the properties
     * in-place.
     */
    editNodeMetadata(
        id: string,
        properties: { [key: string]: string[] },
        {
            versionComment,
            repository = HOME_REPOSITORY,
        }: { versionComment?: string; repository?: string } = {},
    ): Observable<Node> {
        if (versionComment) {
            return this.nodeV1
                .changeMetadataWithVersioning({
                    repository,
                    node: id,
                    versionComment,
                    body: properties,
                })
                .pipe(
                    tap(() => this._nodesChanged.next()),
                    map(({ node }) => node),
                );
        } else {
            return this.nodeV1.changeMetadata({ repository, node: id, body: properties }).pipe(
                tap(() => this._nodesChanged.next()),
                map(({ node }) => node),
            );
        }
    }

    /**
     * Change content of node.
     */
    changeContent(
        /**
         * ID of repository (or &quot;-home-&quot; for home repository)
         */
        repository: string,

        /**
         * ID of node
         */
        node: string,

        /**
         * MIME-Type
         */
        mimetype: string,

        /**
         * comment, leave empty &#x3D; no new version, otherwise new version is generated
         */
        versionComment?: string,

        body?: {
            /**
             * file upload
             */
            file?: Blob;
        },
    ): Observable<Node> {
        return this.nodeV1
            .changeContent1({
                repository,
                node,
                versionComment,
                mimetype,
                body,
            })
            .pipe(map((nEntry) => nEntry.node));
    }

    copyMetadata(targetId: string, sourceId: string, { repository = HOME_REPOSITORY }) {
        return this.nodeV1.copyMetadata({
            node: targetId,
            from: sourceId,
            repository,
        });
    }

    setPermissions(
        id: string,
        permissions: NodePermissions,
        { sendMail = false, mailText = '', sendCopy = false, repository = HOME_REPOSITORY } = {},
    ): Observable<void> {
        return this.nodeV1.setPermission({
            node: id,
            sendMail,
            mailtext: mailText,
            sendCopy,
            repository,
            body: permissions,
        });
    }

    /**
     * Fetches the metadata as indexed for search.
     *
     * The search index includes some additional information like collections in which the node is
     * used, however, it may not always be in sync with the database and requested nodes might not
     * be included in the response.
     */
    getNodeFromSearchIndex(
        nodeId: string,
        { repository = HOME_REPOSITORY } = {},
    ): Observable<Node | null> {
        return this.getNodesFromSearchIndex([nodeId], { repository }).pipe(
            map((nodeEntries) => nodeEntries.nodes[0] ?? null),
        );
    }

    /**
     * Like `getNodeFromSearchIndex`, but fetches multiple nodes.
     */
    getNodesFromSearchIndex(
        nodeIds: string[],
        { repository = HOME_REPOSITORY } = {},
    ): Observable<NodeEntries> {
        return this.searchV1.getMetdata({ repository, nodeIds, propertyFilter: ['-all-'] });
    }
    /**
     * Like `getNodeFromSearchIndex`, but fetches multiple nodes.
     */
    getStats(nodeId: string, { repository = HOME_REPOSITORY } = {}): Observable<NodeStats> {
        return this.nodeV1.getStats({ repository, node: nodeId });
    }
}
