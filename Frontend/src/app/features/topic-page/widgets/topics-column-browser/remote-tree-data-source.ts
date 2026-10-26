import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { first, map, shareReplay, tap } from 'rxjs/operators';

export interface TreeNode<T> {
    id: string;
    level: number;
    data: T;
    isLoading?: boolean;
    isVisible?: boolean;
    children?: TreeNode<T>[];
}

/**
 * Function to (remotely) fetch child nodes of the given node.
 *
 * When called with `nodeId: null`, should return the root nodes.
 *
 * Returned tree nodes can contain nested children, but don't have to.
 */
export type FetchChildNodes<T> = (
    nodeId: string | null,
) => Observable<Omit<TreeNode<T>, 'level'>[]>;

/**
 * Provides access to tree-shaped data where nodes can be dynamically loaded remotely.
 */
export class RemoteTreeDataSource<T> {
    /**
     * An array of all root nodes of the tree.
     *
     * Nodes contain children as nested objects if already fetched, otherwise the `children` property
     * is not set. When children are fetched, the nodes are updated.
     *
     * All methods that perform changes on `_rootNodes` or `_nodesMap` must make sure that their data
     * is kept in sync.
     */
    private _rootNodes?: TreeNode<T>[];
    /**
     * A map, that contains all tree nodes indexed by id.
     *
     * Node objects are the same objects as in `_rootNodes` and their descendants.
     *
     * All methods that perform changes on `_rootNodes` or `_nodesMap` must make sure that their data
     * is kept in sync.
     */
    private _nodesMap?: { [id: string]: TreeNode<T> };
    /**
     * Externally provided function to fetch missing child nodes remotely.
     */
    private _fetchChildNodes?: FetchChildNodes<T>;
    /**
     * Map of currently in-flight requests to fetch missing child nodes.
     */
    private _inFlightFetchChildNodesRequests: {
        [nodeId: string]: Observable<TreeNode<T>[]>;
    } = {};
    /**
     * The state of this data source.
     *
     * 'ready' means, that the root nodes have been fetched.
     */
    private _state = new BehaviorSubject<'initializing' | 'ready'>('initializing');

    /**
     * Provides the function for fetching missing child nodes to this data source and initializes the
     * data source.
     */
    setFetchChildNodes(fetchChildNodes: FetchChildNodes<T>): void {
        this._fetchChildNodes = fetchChildNodes;
        this._initTreeRootNodes();
    }

    /**
     * Returns the root nodes of the tree.
     *
     * The nodes are fetches via `fetchChildNodes` initially.
     */
    getRootNodes(): Observable<TreeNode<T>[]> {
        return this._state.pipe(
            first((state) => state === 'ready'),
            map(() => this._rootNodes ?? []),
        );
    }

    /**
     * Returns the children of the given node.
     *
     * Children are fetched via `fetchChildNodes` when requested for the first time.
     */
    getChildren(node: TreeNode<T>): Observable<TreeNode<T>[]> {
        if (!node) {
            return rxjs.of([]);
        }
        if (!this._nodesMap) {
            throw Error('Called `getChildren` without calling `setFetchChildNodes` first.');
        }
        if (this._nodesMap[node.id] !== node) {
            throw Error('Called getChildren for a node that is not part of the tree.');
        }
        if (node.children) {
            return rxjs.of(node.children);
        } else {
            return this._fetchChildNodesToTree(node);
        }
    }

    /**
     * Resets the tree and fetches the tree's root nodes.
     */
    private _initTreeRootNodes(): void {
        this._rootNodes = [];
        this._nodesMap = {};
        this._inFlightFetchChildNodesRequests = {};
        this._state.next('initializing');
        this._fetchChildNodes!(null).subscribe({
            next: (nodes) => {
                const nodesWithLevel = nodes
                    .map((node) => ({ ...node, level: 0 }))
                    .sort(this._sortNodes);
                this._rootNodes = nodesWithLevel;
                this._addToNodesMap(nodesWithLevel);
                this._state.next('ready');
            },
            error: (error) => {
                this._state.error(error);
            },
        });
    }

    /**
     * Fetches child nodes for the given node and updates the tree.
     *
     * The given node must be part of the tree.
     */
    private _fetchChildNodesToTree(node: TreeNode<T>): Observable<TreeNode<T>[]> {
        if (!this._inFlightFetchChildNodesRequests[node.id]) {
            this._inFlightFetchChildNodesRequests[node.id] = this._fetchChildNodes!(node.id).pipe(
                map((childNodes) =>
                    childNodes
                        .map((childNode) => ({
                            ...childNode,
                            level: node.level + 1,
                        }))
                        .sort(this._sortNodes),
                ),
                tap((childNodes) => {
                    node.children = childNodes;
                    this._addToNodesMap(childNodes);
                    delete this._inFlightFetchChildNodesRequests[node.id];
                }),
                shareReplay(1),
            );
        }
        return this._inFlightFetchChildNodesRequests[node.id];
    }

    private _sortNodes(nodeA: any, nodeB: any) {
        return (nodeA.data as any).title.localeCompare((nodeB.data as any).title);
    }

    private _addToNodesMap(nodes: TreeNode<T>[]): void {
        for (const node of nodes) {
            this._nodesMap![node.id] = node;
            if (node.children) {
                this._addToNodesMap(node.children);
            }
        }
    }
}
