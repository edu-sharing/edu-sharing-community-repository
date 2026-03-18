import { EventEmitter, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    CollectionService,
    HOME_REPOSITORY,
    Node,
    NodeEntries,
    NodeService,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { firstValueFrom, map, tap } from 'rxjs';
import { LocalEventsService } from '../../services/local-events.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { DynamicFlatNode } from './dynamic-flat-node';

@Injectable({
    providedIn: 'root',
})
export class TreeNodeService {
    // holds the already requested nodes
    private dataMap: Map<string, Partial<Node>[]> = new Map<string, Partial<Node>[]>();
    // holds the IDs of already clicked, but empty folders
    private emptyFolders: string[] = [];
    // node types with children
    private readonly folderTypes: string[] = [
        RestConstants.CM_TYPE_FOLDER,
        RestConstants.CCM_TYPE_MAP,
    ];
    // holds information on the initial data
    private initialData: DynamicFlatNode[] = [];
    // holds information on the last loaded children node ID, which is used for pagination
    private parentIdToLastLoadedNodeId = new Map<string, string>();
    // avoid empty (faked) parents from being toggled, as the IDs do not exist
    private emptyParentIds: string[] = [];
    private baseSearchParams = {
        maxItems: 11,
        sortAscending: [true],
        sortProperties: [RestConstants.LOM_PROP_TITLE],
    };
    // whether files should be requested and shown as well
    private showFiles: boolean = true;
    // whether multiple selection is allowed
    private multipleSelection: boolean = false;
    // holds initially selected nodes to allow later selections
    private initiallySelectedNodes: Node[] = [];
    private selectionMode: 'source' | 'target' = 'source';
    readonly nodesChanged = new EventEmitter<Node[]>();

    constructor(
        private collectionService: CollectionService,
        private localEventsService: LocalEventsService,
        private nodeHelperService: NodeHelperService,
        private nodeService: NodeService,
    ) {
        this.localEventsService.nodesChanged
            .pipe(takeUntilDestroyed())
            .subscribe((nodes: Node[]) => this.refreshTree(nodes));
        this.localEventsService.nodesDeleted
            .pipe(takeUntilDestroyed())
            .subscribe((nodes: Node[]) => {
                void this.refreshTree(nodes, true);
            });
    }

    /**
     * Initializes the data by iterating the nodes and ordering them into a tree structure.
     * Stores the initial data.
     */
    async initializeTreeData(nodes: Node[]): Promise<void> {
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
                const childIsCollection = this.nodeHelperService.isNodeCollection(child as Node);
                const childKey = child.ref.id;
                const treeNode = initialData.find((n) => n.item.ref.id === childKey);
                const subChildren = this.dataMap.get(childKey) ?? [];
                if (subChildren.length && childIsCollection) {
                    // we just want to add the number of sub-collections, not the total number of sub-nodes
                    const numberOfSubCollections = subChildren.filter((c) =>
                        this.nodeHelperService.isNodeCollection(c as Node),
                    ).length;
                    if (!child.collection) {
                        child.collection = {
                            fromUser: false,
                            level0: false,
                            scope: '',
                            title: '',
                            type: '',
                        };
                    }
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
                } else if (!subChildren.length && treeNode && treeNode.level === 0) {
                    // fix directories do not store information on whether they contain sub-nodes
                    const isDirectory: boolean =
                        !childIsCollection && child.type === RestConstants.CCM_TYPE_MAP;
                    if (isDirectory) {
                        treeNode.expandable = true;
                    } else {
                        // store information about empty root elements
                        this.emptyParentIds.push(treeNode.item.ref.id);
                    }
                }
                if (treeNode && this.isExpandable(child)) {
                    treeNode.expandable = true;
                }
            });
        });
        this.initialData = initialData;
    }

    /**
     * Retrieves the initial data for the tree.
     */
    getInitialData(): DynamicFlatNode[] {
        return this.initialData;
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
            let nodeEntries: NodeEntries;
            if (this.nodeHelperService.isNodeCollection(node as Node)) {
                nodeEntries = await firstValueFrom(
                    this.collectionService
                        .getSubcollections({
                            collection: nodeId,
                            scope: 'MY',
                            repository: node.ref.repo,
                            ...this.baseSearchParams,
                            sortProperties: [
                                this.nodeHelperService.getSortByForCollection(node as Node).active,
                            ],
                            sortAscending: [
                                this.nodeHelperService.getSortByForCollection(node as Node)
                                    .direction === 'asc',
                            ],
                        })
                        .pipe(
                            map((s) => {
                                return {
                                    pagination: s.pagination,
                                    nodes: s.collections,
                                };
                            }),
                        ),
                );
                if (this.showFiles) {
                    const nodeEntriesRef = await firstValueFrom(
                        this.collectionService
                            .getReferences({
                                collection: nodeId,
                                repository: node.ref.repo,
                                ...this.baseSearchParams,
                                sortProperties: [
                                    this.nodeHelperService.getSortByForCollectionReferences(
                                        node as Node,
                                    ).active,
                                ],
                                sortAscending: [
                                    this.nodeHelperService.getSortByForCollectionReferences(
                                        node as Node,
                                    ).direction === 'asc',
                                ],
                            })
                            .pipe(
                                map((s) => {
                                    return {
                                        pagination: s.pagination,
                                        nodes: s.references,
                                    };
                                }),
                            ),
                    );
                    nodeEntries.nodes = nodeEntries.nodes.concat(nodeEntriesRef.nodes);
                    nodeEntriesRef.pagination.count += nodeEntries.pagination.count;
                    nodeEntriesRef.pagination.total += nodeEntries.pagination.total;
                }
            } else {
                // regular file/folders
                nodeEntries = await firstValueFrom(
                    this.nodeService.getChildren(nodeId, this.baseSearchParams),
                );
                // filter out files if not requested and update the pagination
                if (!this.showFiles) {
                    let filteredNodesCount: number = nodeEntries.nodes.length;
                    nodeEntries.nodes = nodeEntries.nodes.filter(
                        (n) => n.type !== RestConstants.CCM_TYPE_IO,
                    );
                    filteredNodesCount -= nodeEntries.nodes.length;
                    nodeEntries.pagination.count -= filteredNodesCount;
                    nodeEntries.pagination.total -= filteredNodesCount;
                }
            }
            children = this.replaceNodeReferences(nodeEntries?.nodes ?? []);
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
        const children = this.replaceNodeReferences(
            existingChildren.concat(requestedChildren) as Node[],
        );
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
        const atLeastOneChild: boolean =
            this.nodeHelperService.isNodeCollection(node as Node) &&
            (node.collection?.childCollectionsCount > 0 ||
                node.collection?.childReferencesCount > 0);
        const unclickedFolder: boolean =
            !this.nodeHelperService.isNodeCollection(node as Node) &&
            this.folderTypes.includes(node.type) &&
            !this.emptyFolders.includes(node.ref.id) &&
            !this.emptyParentIds.includes(node.ref.id);
        return atLeastOneChild || unclickedFolder;
    }

    /**
     * Returns the data map.
     */
    getDataMap(): Map<string, Partial<Node>[]> {
        return this.dataMap;
    }

    /**
     * Returns the empty folders array.
     */
    getEmptyFolders(): string[] {
        return this.emptyFolders;
    }

    /**
     * Returns the parent ID to last loaded node ID mapping.
     */
    getParentIdToLastLoadedNodeId(): Map<string, string> {
        return this.parentIdToLastLoadedNodeId;
    }

    /**
     * Returns the selection mode.
     */
    getSelectionMode(): 'source' | 'target' {
        return this.selectionMode;
    }

    /**
     * Sets a given mode as the selection mode.
     *
     * @param mode
     */
    setSelectionMode(mode: 'source' | 'target') {
        this.selectionMode = mode;
    }

    /**
     * Helper function to refresh the tree if updates were made to given nodes.
     *
     * @param nodes
     * @param deleted
     */
    private async refreshTree(nodes: Node[], deleted: boolean = false): Promise<void> {
        const updatedNodes: Node[] = [];
        for (const node of nodes) {
            const nodeId: string = node.ref.id;

            if (this.nodeHelperService.isNodeCollection(node)) {
                // if a collection was updated, reload its children
                if (!deleted) {
                    // remove node ID from helper structures
                    this.parentIdToLastLoadedNodeId.delete(nodeId);
                    this.emptyFolders = this.emptyFolders.filter((id) => id !== nodeId);
                    // retrieve updated children (also updates the helper structures)
                    this.dataMap.delete(nodeId);
                    await this.getChildren(node);
                    // workaround for updating the number of references
                    // note: update is not done automatically, so reloading the references is necessary
                    const references = await firstValueFrom(
                        this.collectionService.getReferences({
                            repository: HOME_REPOSITORY,
                            collection: nodeId,
                        }),
                    );
                    node.collection.childReferencesCount = references.pagination.total;
                    updatedNodes.push(node);
                }
                // if a collection was deleted, reload its parents' children
                else {
                    // iterate over dataMap and find the parent node
                    const parentId = node.parent.id;
                    let parentNode: Partial<Node> | undefined;
                    for (const [mapKey, children] of this.dataMap.entries()) {
                        const foundParent = children.find((child) => child.ref.id === parentId);
                        if (foundParent) {
                            parentNode = foundParent;
                            break;
                        }
                    }
                    if (parentNode) {
                        // remove node ID from helper structures
                        this.parentIdToLastLoadedNodeId.delete(parentId);
                        this.emptyFolders = this.emptyFolders.filter((id) => id !== parentId);
                        // retrieve updated children (also updates the helper structures)
                        this.dataMap.delete(parentId);
                        await this.getChildren(parentNode);
                        // workaround for updating the number of references
                        // note: update is not done automatically, so reloading the references is necessary
                        const references = await firstValueFrom(
                            this.collectionService.getReferences({
                                repository: HOME_REPOSITORY,
                                collection: parentId,
                            }),
                        );
                        parentNode.collection.childReferencesCount = references.pagination.total;
                        // also update the number of child collections
                        parentNode.collection.childCollectionsCount =
                            this.dataMap
                                .get(parentId)
                                ?.filter((child) =>
                                    this.nodeHelperService.isNodeCollection(child as Node),
                                ).length ?? 0;
                        updatedNodes.push(parentNode as Node);
                    } else {
                        updatedNodes.push(node);
                    }
                }
            } else {
                if (deleted) {
                    console.log('d', node, nodeId, this.dataMap);
                    for (let entry of this.dataMap.entries()) {
                        this.dataMap.set(
                            entry[0],
                            entry[1].filter((v) => v.ref.id !== nodeId),
                        );
                        console.log(this.dataMap, entry[0]);
                    }
                }
                updatedNodes.push(node);
            }
        }
        // emit the changed nodes
        this.nodesChanged.emit(updatedNodes);
    }

    /**
     * Resets all data loaded for the tree.
     */
    resetData(): void {
        this.dataMap = new Map<string, Partial<Node>[]>();
        this.emptyFolders = [];
        this.initialData = [];
        this.parentIdToLastLoadedNodeId = new Map<string, string>();
        this.emptyParentIds = [];
    }

    /**
     * Updates the show files flag to a given value.
     *
     * @param showFiles
     */
    updateShowFiles(showFiles: boolean) {
        this.showFiles = showFiles;
    }

    /**
     * Updates the multiple selection flag to a given value.
     *
     * @param multipleSelection
     */
    updateMultipleSelectionAllowed(multipleSelection: boolean) {
        this.multipleSelection = multipleSelection;
    }

    /**
     * Returns whether multiple selection is allowed.
     */
    isMultipleSelectionAllowed(): boolean {
        return this.multipleSelection;
    }

    /**
     * Updates the initially selected nodes.
     *
     * @param nodes
     */
    updateInitiallySelectedNodes(nodes: Node[]) {
        this.initiallySelectedNodes = nodes;
    }

    /**
     * Helper function to replace node references of already selected nodes in a given array of nodes.
     * This is necessary as the comparison of the SelectionModel uses reference equality (===) by default,
     * so the selection is not detected properly, even when both JSON objects are identically.
     *
     * @param nodes
     */
    private replaceNodeReferences(nodes: Node[]) {
        if (this.initiallySelectedNodes?.length) {
            nodes = nodes.map((child) => {
                // check if a child exists that was initially selected and replace it
                const match = this.initiallySelectedNodes.find(
                    (initialNode) => child.ref.id === initialNode.ref.id,
                );
                // remove match as already added before
                if (match) {
                    this.initiallySelectedNodes = this.initiallySelectedNodes.filter(
                        (initialNode) => initialNode.ref.id !== match.ref.id,
                    );
                }
                return match ?? child;
            });
        }
        return nodes;
    }
}
