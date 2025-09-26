import { Injectable } from '@angular/core';
import { Node, NodeEntries, NodeService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { DynamicFlatNode } from './dynamic-flat-node';

@Injectable({
    providedIn: 'root',
})
export class TreeNodeService {
    // holds the already requested nodes
    dataMap = new Map<string, Partial<Node>[]>();
    // holds the IDs of already clicked, but empty folders
    emptyFolders: string[] = [];
    // node types with children
    private folderTypes: string[] = ['cm:folder', 'ccm:map'];
    // holds information on the last loaded children node ID, which is used for pagination
    parentIdToLastLoadedNodeId = new Map<string, string>();
    private baseSearchParams = {
        maxItems: 11,
        sortAscending: [true],
        sortProperties: ['cm:title'],
    };

    constructor(private nodeService: NodeService) {}

    /**
     * Retrieves the initial data by iterating the nodes and ordering them into a tree structure.
     * Returns the first level of dynamic flat nodes.
     */
    async getInitialData(nodes: Node[]): Promise<DynamicFlatNode[]> {
        // initial data
        const initialData: DynamicFlatNode[] = [];

        // index for quick lookup
        // ID -> node
        const nodeMap = new Map<string, Node>();
        nodes.forEach((n) => nodeMap.set(n.ref.id, n));

        // helper to compute level by walking up parents
        function getLevel(node: Node): number {
            let level = 0;
            let current = node;
            while (current.parent?.id && nodeMap.has(current.parent.id)) {
                level++;
                current = nodeMap.get(current.parent.id)!;
            }
            return level;
        }

        // first pass: create DynamicFlatNodes and insert them into dataMap
        nodes.forEach((node) => {
            const level = getLevel(node);
            if (!node.collection) {
                node.collection = {
                    fromUser: false,
                    level0: false,
                    scope: '',
                    title: '',
                    type: '',
                };
            }
            const treeNode: DynamicFlatNode = new DynamicFlatNode(node, level, false);
            // only push first-level nodes into initialData
            if (level === 0) {
                initialData.push(treeNode);
            }

            // add to map
            const parentKey = node.parent?.id ?? '__root__';
            if (!this.dataMap.has(parentKey)) {
                this.dataMap.set(parentKey, []);
            }
            this.dataMap.get(parentKey)!.push(node);
        });

        // second pass: update numberOfChildren
        this.dataMap.forEach((children) => {
            children.forEach((child) => {
                const childKey = child.ref.id;
                const subChildren = this.dataMap.get(childKey) ?? [];
                if (subChildren.length) {
                    // we just want to add the number of sub-collections, not the total number of sub-nodes
                    const numberOfSubCollections = subChildren.filter(
                        (child) =>
                            child.mediatype === 'collection' ||
                            this.folderTypes.includes(child.type),
                    ).length;
                    child.collection.childCollectionsCount =
                        numberOfSubCollections +
                        subChildren.reduce(
                            (sum, node) => sum + (node.collection?.childCollectionsCount || 0),
                            0,
                        );
                    child.collection.childReferencesCount = subChildren.reduce(
                        (sum, node) => sum + (node.collection?.childReferencesCount || 0),
                        0,
                    );
                }
                const treeNode = initialData.find((n) => n.item.ref.id === childKey);
                if (treeNode && child.collection.childCollectionsCount > 0) {
                    treeNode.expandable = true;
                }
            });
        });
        return initialData;
    }

    /**
     * Retrieves the nodes children either from the dataMap or by requesting them from the backend.
     *
     * @param node
     */
    async getChildren(node: Partial<Node>): Promise<Partial<Node>[] | undefined> {
        const nodeId = node.ref.id;
        let children: Partial<Node>[] = this.dataMap.get(nodeId) || [];
        if (!children?.length) {
            const nodeEntries: NodeEntries = await firstValueFrom(
                this.nodeService.getChildren(nodeId, this.baseSearchParams),
            );
            children = nodeEntries?.nodes ?? [];
            // hold the last loaded node ID to load the next elements
            if (children.length < nodeEntries.pagination.total) {
                this.parentIdToLastLoadedNodeId.set(
                    node.ref.id,
                    children[children.length - 1].ref.id,
                );
            } else if (this.parentIdToLastLoadedNodeId.has(node.ref.id)) {
                this.parentIdToLastLoadedNodeId.delete(node.ref.id);
            }

            // workaround for holding information about empty folders
            if (!children.length && !this.emptyFolders.includes(nodeId)) {
                this.emptyFolders.push(nodeId);
            }
            this.dataMap.set(nodeId, children);
        }
        return children;
    }

    /**
     * Retrieves the next children of a node.
     *
     * @param nodeId
     */
    async getFurtherChildren(nodeId: string): Promise<Partial<Node>[] | undefined> {
        // delete the map entry first
        this.parentIdToLastLoadedNodeId.delete(nodeId);
        // request both existing and further children and concat those
        const existingChildren: Partial<Node>[] = this.dataMap.get(nodeId) || [];
        const extendedCriteria = JSON.parse(JSON.stringify(this.baseSearchParams));
        extendedCriteria.skipCount = existingChildren.length;
        const nodeEntries: NodeEntries = await firstValueFrom(
            this.nodeService.getChildren(nodeId, {
                ...this.baseSearchParams,
                skipCount: existingChildren.length,
            }),
        );
        const requestedChildren = nodeEntries?.nodes ?? [];
        const children = existingChildren.concat(requestedChildren);
        this.dataMap.set(nodeId, children);
        // update the last loaded node ID to load the next elements
        if (children.length < nodeEntries.pagination.total) {
            this.parentIdToLastLoadedNodeId.set(nodeId, children[children.length - 1].ref.id);
        }
        return children;
    }

    /**
     * Returns whether a node is expandable.
     *
     * @param node
     */
    isExpandable(node: Partial<Node>): boolean {
        const atLeastOneChild: boolean = node.collection?.childCollectionsCount > 0;
        const unclickedFolder: boolean =
            this.folderTypes.includes(node.type) && !this.emptyFolders.includes(node.ref.id);
        return atLeastOneChild || unclickedFolder;
    }
}
