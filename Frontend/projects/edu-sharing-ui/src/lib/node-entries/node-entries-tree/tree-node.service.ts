import { Injectable } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { DynamicFlatNode } from './dynamic-flat-node';

@Injectable({
    providedIn: 'root',
})
export class TreeNodeService {
    // holds the already requested nodes
    dataMap = new Map<string, Partial<Node>[]>();

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
                        (child) => child.mediatype === 'collection',
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
     * @param nodeId
     */
    async getChildren(nodeId: string): Promise<Partial<Node>[] | undefined> {
        let children: Partial<Node>[] = this.dataMap.get(nodeId) || [];
        if (!children?.length) {
            children = (
                await firstValueFrom(
                    this.nodeService.getChildren(nodeId, {
                        sortAscending: [true],
                        sortProperties: ['cm:title'],
                    }),
                )
            )?.nodes.sort((a, b) => a.title.localeCompare(b.title));
            this.dataMap.set(nodeId, children);
        }
        return children;
    }

    /**
     * Returns whether a node is expandable.
     *
     * @param node
     */
    isExpandable(node: Partial<Node>): boolean {
        return node.collection?.childCollectionsCount > 0;
    }
}
