import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NodeV1Service, SearchV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node, NodeEntries } from '../models';

export class NodeConstants {
    public static SPACES_STORE_REF = 'workspace://SpacesStore/';
}
export class NodeTools {
    static createSpacesStoreRef(id: string) {
        return NodeConstants.SPACES_STORE_REF + id;
    }
}
@Injectable({
    providedIn: 'root',
})
export class NodeService {
    constructor(private nodeV1: NodeV1Service, private searchV1: SearchV1Service) {}

    getNode(repository: string, id: string): Observable<Node> {
        return this.nodeV1
            .getMetadata({
                repository,
                node: id,
                propertyFilter: ['-all-'],
            })
            .pipe(map((nodeEntry) => nodeEntry.node));
    }
    /**
     * return the forked childs (variants) of this node
     * @returns
     */
    getForkedChilds(id: string, { repository = HOME_REPOSITORY } = {}) {
        return this.searchV1.searchByProperty({
            repository,
            comparator: ['='],
            contentType: 'FILES',
            property: ['ccm:forked_origin'],
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
}
