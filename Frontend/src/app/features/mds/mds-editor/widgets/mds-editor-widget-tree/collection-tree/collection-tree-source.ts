import { computed, inject, Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService, Node } from 'ngx-edu-sharing-api';
import { firstValueFrom, forkJoin, from, Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Tree, TreeNode } from '../tree';
import { TreeSource, TreeSourceValue } from '../tree-source';
import {
    COLLECTION_TREE_MAX_SEARCH_HITS,
    COLLECTION_TREE_ROOT_QUERY_PARAM,
    CollectionTreeConfig,
    CollectionTreeScope,
    DEFAULT_COLLECTION_TREE_PAGE_SIZE,
    DEFAULT_COLLECTION_TREE_SCOPES,
    parseCollectionTreeConfig,
    PERSONAL_COLLECTION_TREE_SCOPES,
} from './collection-tree-config';
import {
    CollectionTreeService,
    getCollectionTitle,
    PlacedCollection,
} from './collection-tree.service';

/** Guards against a cycle in the parent links of corrupt data. */
const MAX_DEPTH = 50;

/**
 * Lazily loaded, searchable tree of the collections the user can read, for `collectionTree`
 * widgets. It starts at the collection named by the page URL (`collectionRoot`), else at the one
 * configured, else at the categories of the collections page.
 */
@Injectable()
export class CollectionTreeSource implements TreeSource {
    private readonly collectionTreeService = inject(CollectionTreeService);
    private readonly translate = inject(TranslateService);
    private readonly isGuest = toSignal(
        inject(AuthenticationService)
            .observeLoginInfo()
            .pipe(map((login) => !!login?.isGuest)),
        { initialValue: false },
    );
    private readonly urlRootId = toSignal(
        inject(ActivatedRoute).queryParamMap.pipe(
            map((params) => params.get(COLLECTION_TREE_ROOT_QUERY_PARAM)),
        ),
        { initialValue: null },
    );
    private readonly config = signal<CollectionTreeConfig>({});
    /** Metadata set whose `collections` query is used for searching. */
    private mdsId: string;
    private readonly rootId = computed(
        () => this.urlRootId() || this.config().rootCollectionId || null,
    );
    /** The start collection once it is loaded, `null` without one or when it cannot be read. */
    private readonly rootNode = signal<Node | null>(null);
    private readonly withinRoot = signal(true);
    private readonly values = signal<ReadonlyMap<string, TreeSourceValue>>(new Map());
    private readonly requestedValues = new Set<string>();
    /** What the children of a lazily loaded node are loaded from, and the tree it belongs to. */
    private readonly lazyNodes = new Map<
        TreeNode,
        { tree: Tree; collection?: Node; scope?: CollectionTreeScope }
    >();
    private browseTree: { key: string; tree: Promise<Tree> } | null = null;

    /**
     * Listed only where the page URL names a start collection, like a search opened from a topic
     * page; the configured start collection does not count.
     */
    readonly showWithoutValues = computed(() => !!this.urlRootId());

    readonly headerToggle = computed(() => {
        const root = this.rootNode();
        return root
            ? {
                  label: this.translate.instant('MDS.COLLECTION_TREE.WITHIN_ROOT', {
                      root: getCollectionTitle(root),
                  }),
                  checked: this.withinRoot(),
              }
            : null;
    });

    /**
     * @param configuration the widget's configuration, see `CollectionTreeConfig`
     * @param mdsId metadata set whose `collections` query is used for searching
     */
    configure(configuration: string | undefined, mdsId: string): this {
        this.config.set(parseCollectionTreeConfig(configuration));
        this.mdsId = mdsId;
        return this;
    }

    getTree(filter: string | null): Observable<Tree> {
        if (!filter) {
            return from(this.getBrowseTree());
        }
        // the browse tree loads the start collection the search is limited to
        return from(this.getBrowseTree()).pipe(
            switchMap(() => {
                const rootId = this.withinRoot() ? this.rootNode()?.ref.id ?? null : null;
                return this.collectionTreeService
                    .searchCollections(filter, {
                        rootId,
                        mdsId: this.mdsId,
                        maxItems: COLLECTION_TREE_MAX_SEARCH_HITS,
                    })
                    .pipe(
                        switchMap((hits) =>
                            this.collectionTreeService
                                .getWithAncestors(hits, this.mdsId)
                                .pipe(map((placed) => createSearchTree(hits, placed, rootId))),
                        ),
                    );
            }),
        );
    }

    loadChildren(node: TreeNode): Promise<void> {
        return this.appendPage(node);
    }

    loadValues(ids: string[]): void {
        for (const id of ids) {
            if (this.requestedValues.has(id)) {
                continue;
            }
            this.requestedValues.add(id);
            this.collectionTreeService.getAncestors(id).subscribe({
                next: (path) =>
                    this.setValue(id, {
                        label: getCollectionTitle(path[path.length - 1]),
                        ancestorIds: path.slice(0, -1).map((node) => node.ref.id),
                    }),
                error: () =>
                    this.setValue(id, {
                        label: this.translate.instant('MDS.COLLECTION_TREE.NOT_ACCESSIBLE'),
                        ancestorIds: [],
                    }),
            });
        }
    }

    getValue(id: string): TreeSourceValue | undefined {
        return this.values().get(id);
    }

    setHeaderToggle(checked: boolean): void {
        this.withinRoot.set(checked);
    }

    private setValue(id: string, value: TreeSourceValue): void {
        this.values.update((values) => new Map(values).set(id, value));
    }

    /** The tree listed without a filter, built on first use for the current start collection. */
    private getBrowseTree(): Promise<Tree> {
        const key = `${this.rootId()}|${this.withinRoot()}`;
        if (this.browseTree?.key !== key) {
            // the nodes of an earlier browse tree are no longer listed
            this.lazyNodes.clear();
            this.browseTree = { key, tree: this.createBrowseTree() };
        }
        return this.browseTree.tree;
    }

    private async createBrowseTree(): Promise<Tree> {
        const tree = Tree.generateTree([]);
        const root = this.withinRoot() ? await this.loadRoot() : null;
        if (root) {
            const rootNode = this.addCollection(tree, root);
            // the start collection is listed with its first page of collections
            rootNode.hasChildren = true;
            await this.appendPage(rootNode);
            rootNode.children ??= [];
        } else {
            for (const scope of await this.getFilledCategories()) {
                const category = tree.addNode({
                    id: `category:${scope}`,
                    caption: this.translate.instant(`COLLECTIONS.TAB.${scope}`),
                    type: 'group',
                    hasChildren: true,
                });
                this.lazyNodes.set(category, { tree, scope });
            }
        }
        return tree;
    }

    private async loadRoot(): Promise<Node | null> {
        const id = this.rootId();
        if (!id) {
            return null;
        }
        try {
            const path = await firstValueFrom(this.collectionTreeService.getAncestors(id));
            this.rootNode.set(path[path.length - 1] ?? null);
        } catch {
            // without a readable start collection, the categories are listed instead
            this.rootNode.set(null);
        }
        return this.rootNode();
    }

    /** Categories of the top level that hold collections, guests having none of their own. */
    private async getFilledCategories(): Promise<CollectionTreeScope[]> {
        const scopes = (this.config().scopes ?? DEFAULT_COLLECTION_TREE_SCOPES).filter(
            (scope) => !this.isGuest() || !PERSONAL_COLLECTION_TREE_SCOPES.includes(scope),
        );
        if (!scopes.length) {
            return [];
        }
        const filled = await firstValueFrom(
            forkJoin(scopes.map((scope) => this.collectionTreeService.hasCollections(scope))),
        );
        return scopes.filter((_, index) => filled[index]);
    }

    /** Appends the next page of collections below `node`, with a `more` row if any are left. */
    private async appendPage(node: TreeNode): Promise<void> {
        const source = this.lazyNodes.get(node);
        if (!source) {
            return;
        }
        const { tree, collection, scope } = source;
        const more = node.children?.find((child) => child.type === 'more');
        const skipCount = (node.children?.length ?? 0) - (more ? 1 : 0);
        const pageSize = this.config().pageSize;
        const maxItems = (pageSize ?? 0) > 0 ? pageSize : DEFAULT_COLLECTION_TREE_PAGE_SIZE;
        const page = await firstValueFrom(
            this.collectionTreeService.getChildren(collection ?? null, {
                scope,
                skipCount,
                maxItems,
            }),
        );
        if (more) {
            tree.removeNode(more);
        }
        for (const child of page.nodes) {
            this.addCollection(tree, child, node);
        }
        // the total also counts collections the user cannot read, so an empty page ends the list
        if (page.nodes.length && skipCount + maxItems < page.total) {
            tree.addNode({ id: `more:${node.uid}`, caption: '', type: 'more' }, node);
        }
    }

    private addCollection(tree: Tree, collection: Node, parent?: TreeNode): TreeNode {
        const node = tree.addNode(
            {
                id: collection.ref.id,
                caption: getCollectionTitle(collection),
                hasChildren: (collection.collection?.childCollectionsCount ?? 0) > 0,
            },
            parent,
        );
        this.lazyNodes.set(node, { tree, collection });
        return node;
    }
}

/** Lists search hits below the collections leading to them from the start collection. */
function createSearchTree(
    hits: Node[],
    placed: Map<string, PlacedCollection>,
    rootId: string | null,
): Tree {
    const tree = Tree.generateTree([]);
    for (const hit of hits) {
        const path = getPath(hit.ref.id, placed);
        const offset = path && rootId ? path.indexOf(rootId) : 0;
        // only a repository that cannot search below a collection returns hits outside of it
        if (offset < 0) {
            continue;
        }
        let parent: TreeNode | undefined;
        for (const id of path?.slice(offset) ?? [hit.ref.id]) {
            const siblings = parent ? parent.children : tree.rootNodes;
            parent =
                siblings?.find((node) => node.id === id) ??
                tree.addNode(
                    { id, caption: getCollectionTitle(placed.get(id)?.node ?? hit) },
                    parent,
                );
        }
    }
    return tree;
}

/** The ids from the top-most collection down to `id`, `null` while a collection is unknown. */
function getPath(id: string, placed: Map<string, PlacedCollection>): string[] | null {
    const path: string[] = [];
    let current: string | null = id;
    while (current) {
        const collection = placed.get(current);
        if (!collection || path.length >= MAX_DEPTH) {
            return null;
        }
        path.unshift(current);
        current = collection.parentId;
    }
    return path;
}
