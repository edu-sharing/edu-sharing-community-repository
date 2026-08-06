import { EventEmitter, Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    CollectionService,
    HOME_REPOSITORY,
    Node,
    NodeEntries,
    NodeService,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { firstValueFrom, map } from 'rxjs';
import { LocalEventsService } from '../../services/local-events.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { DynamicFlatNode } from './dynamic-flat-node';

@Injectable()
export class TreeNodeService {
    private collectionService = inject(CollectionService);
    private localEventsService = inject(LocalEventsService);
    private nodeHelperService = inject(NodeHelperService);
    private nodeService = inject(NodeService);

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
    private currentData: DynamicFlatNode[] = [];
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
    // whether includeResolveInheritedAccess should be added in requests
    private includeResolveInheritedAccess: boolean = false;
    // holds an attribute used to decide whether a node is initially selected
    private initialSelectionAttribute: string;
    // callback provided by the tree component to apply a selection to the UI layer
    private applySelectionCallback: ((nodes: Node[]) => void) | null = null;
    private selectionMode: 'source' | 'target' = 'source';
    readonly nodesChanged = new EventEmitter<Node[]>();
    // holds the currently expanded nodes
    private expandedNodes: string[] = [];
    // callback registered by components to define a custom isValidSource check
    private isValidSourceCallback: ((node: Node) => boolean) | null = null;
    // callback registered by components to define a custom isValidTarget check
    private isValidTargetCallback: ((node: Node) => boolean) | null = null;

    constructor() {
        this.localEventsService.nodesChanged
            .pipe(takeUntilDestroyed())
            .subscribe((nodes: Node[]) => this.refreshTree(nodes));
        this.localEventsService.nodesDeleted
            .pipe(takeUntilDestroyed())
            .subscribe((nodes: Node[]) => {
                void this.refreshTree(nodes, true);
            });
        this.localEventsService.nodesMoved
            .pipe(takeUntilDestroyed())
            .subscribe(({ nodes, source, target }) => this.handleNodesMoved(nodes, source, target));
    }

    /** Applies a move: drops the nodes from their former parent and reloads the target. */
    private handleNodesMoved(nodes: Node[], source: Partial<Node>, target: Node): void {
        const movedIds: string[] = nodes.map((node) => node.ref.id);
        for (const [parentId, children] of this.dataMap.entries()) {
            this.dataMap.set(
                parentId,
                children.filter((child) => !movedIds.includes(child.ref.id)),
            );
        }
        const updatedNodes: Node[] = [];
        const sourceId: string = source?.ref?.id ?? nodes[0]?.parent?.id;
        if (sourceId) {
            updatedNodes.push({ ref: { id: sourceId } } as Node);
        }
        if (target?.ref?.id) {
            this.invalidateChildren(target.ref.id, true);
            updatedNodes.push(target);
        }
        if (updatedNodes.length) {
            this.nodesChanged.emit(updatedNodes);
        }
    }

    /**
     * Drops the cached children of a node so they are re-requested on the next expand.
     * `gainedChildren` additionally marks the node as expandable.
     */
    private invalidateChildren(nodeId: string, gainedChildren: boolean = false): void {
        this.dataMap.delete(nodeId);
        this.parentIdToLastLoadedNodeId.delete(nodeId);
        this.emptyFolders = this.emptyFolders.filter((id) => id !== nodeId);
        if (gainedChildren) {
            this.emptyParentIds = this.emptyParentIds.filter((id) => id !== nodeId);
            const treeNode = this.currentData.find((data) => data.item.ref.id === nodeId);
            if (treeNode) {
                treeNode.expandable = true;
            }
        }
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
        this.setCurrentData(initialData);
    }

    /**
     * Retrieves the current data for the tree.
     */
    getCurrentData(): DynamicFlatNode[] {
        return this.currentData;
    }

    /**
     * Updates the current data of the tree.
     *
     * @param data
     */
    setCurrentData(data: DynamicFlatNode[]) {
        this.currentData = data;
    }

    /**
     * Retrieves the nodes children either from the dataMap or by requesting them from the backend.
     *
     * @param node
     */
    async getChildren(node: Partial<Node>): Promise<Partial<Node>[] | undefined> {
        const nodeId = node.ref.id;
        let children: Partial<Node>[] = this.dataMap.get(nodeId) || [];
        // early return when children do already exist
        if (children?.length) {
            return children;
        }
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
                        ...(this.includeResolveInheritedAccess
                            ? { resolveInheritedAccess: true }
                            : {}),
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
            const request = {
                ...this.baseSearchParams,
                ...(this.showFiles ? {} : { filter: ['folders'] }),
                ...(this.includeResolveInheritedAccess ? { resolveInheritedAccess: true } : {}),
            };
            nodeEntries = await firstValueFrom(this.nodeService.getChildren(nodeId, request));
        }
        // check whether an initial selection has to be made
        this.checkAndSetInitialSelection(nodeEntries);
        children = nodeEntries?.nodes ?? [];
        // hold the last loaded node ID to load the next elements
        if (children.length < nodeEntries.pagination.total) {
            this.parentIdToLastLoadedNodeId.set(node.ref.id, children[children.length - 1].ref.id);
        } else if (this.parentIdToLastLoadedNodeId.has(node.ref.id)) {
            this.parentIdToLastLoadedNodeId.delete(node.ref.id);
        }

        // workaround for holding information about empty folders
        if (!children.length && !this.emptyFolders.includes(nodeId)) {
            this.emptyFolders.push(nodeId);
        }
        this.dataMap.set(nodeId, children);
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
        const request = {
            ...this.baseSearchParams,
            skipCount: existingChildren.length,
            ...(this.showFiles ? {} : { filter: ['folders'] }),
            ...(this.includeResolveInheritedAccess ? { resolveInheritedAccess: true } : {}),
        };
        const nodeEntries: NodeEntries = await firstValueFrom(
            this.nodeService.getChildren(nodeId, request),
        );
        // check whether an initial selection has to be made
        this.checkAndSetInitialSelection(nodeEntries);
        const requestedChildren = nodeEntries?.nodes ?? [];
        const children = existingChildren.concat(requestedChildren) as Node[];
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
                    for (let entry of this.dataMap.entries()) {
                        this.dataMap.set(
                            entry[0],
                            entry[1].filter((v) => v.ref.id !== nodeId),
                        );
                    }
                } else if (this.folderTypes.includes(node.type)) {
                    // a changed folder may have gained children
                    this.invalidateChildren(nodeId, true);
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
        this.currentData = [];
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
     * Updates the include resolve inherited access flag to a given value.
     *
     * @param includeResolveInheritedAccess
     */
    updateIncludeResolveInheritedAccess(includeResolveInheritedAccess: boolean) {
        this.includeResolveInheritedAccess = includeResolveInheritedAccess;
    }

    /**
     * Updates the initial selection attribute to a given value.
     *
     * @param initialSelectionAttribute
     */
    updateInitialSelectionAttribute(selectionAttribute: string) {
        this.initialSelectionAttribute = selectionAttribute;
    }

    /**
     * Removes a given node from the currently expanded nodes array.
     *
     * @param node
     */
    collapseNode(node: Partial<Node>): void {
        if (!node?.ref.id) {
            return;
        }
        const index = this.expandedNodes.indexOf(node.ref.id);
        if (index > -1) {
            this.expandedNodes.splice(index, 1);
        }
    }

    /**
     * Add a given node to the currently expanded nodes array.
     *
     * @param node
     */
    expandNode(node: Partial<Node>): void {
        if (!node?.ref.id) {
            return;
        }
        const index = this.expandedNodes.indexOf(node.ref.id);
        if (index === -1) {
            this.expandedNodes.push(node.ref.id);
        }
    }

    /**
     * Retrieves the currently expanded nodes.
     */
    getExpandedNodes(): string[] {
        return this.expandedNodes;
    }

    /**
     * Sets the callback to apply a selection to the UI layer.
     *
     * @param callback
     */
    setApplySelectionCallback(callback: ((nodes: Node[]) => void) | null): void {
        this.applySelectionCallback = callback;
    }

    /**
     * Sets the callback to define a custom isValidSource check.
     */
    setCustomIsValidSourceCallback(callback: ((node: Node) => boolean) | null): void {
        this.isValidSourceCallback = callback;
    }

    /**
     * Retrieves the callback defining a custom isValidSource check.
     */
    getCustomIsValidSourceCallback(): ((node: Node) => boolean) | null {
        return this.isValidSourceCallback;
    }

    /**
     * Sets the callback to define a custom isValidTarget check.
     */
    setCustomIsValidTargetCallback(callback: ((node: Node) => boolean) | null): void {
        this.isValidTargetCallback = callback;
    }

    /**
     * Retrieves the callback defining a custom isValidTarget check.
     */
    getCustomIsValidTargetCallback(): ((node: Node) => boolean) | null {
        return this.isValidTargetCallback;
    }

    /**
     * Checks if an initial selection attribute is present and applies it to given node entries.
     *
     * @param nodeEntries
     */
    private checkAndSetInitialSelection(nodeEntries: NodeEntries) {
        if (this.initialSelectionAttribute && nodeEntries?.nodes?.length) {
            const nodesToSelect = nodeEntries.nodes.filter((n) => n.inherited);
            if (nodesToSelect.length) {
                this.applySelectionCallback?.(nodesToSelect);
            }
        }
    }
}
