import { computed, inject, Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DEFAULT, Node } from 'ngx-edu-sharing-api';
import { firstValueFrom, from, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Tree, TreeNode } from '../tree';
import { TreeSource, TreeSourceValue } from '../tree-source';
import {
    CollectionScope,
    CollectionScopesService,
} from '../../../../../../services/collection-scopes.service';
import {
    COLLECTION_TREE_MAX_SEARCH_HITS,
    COLLECTION_TREE_ROOT_QUERY_PARAM,
    CollectionTreeConfig,
    DEFAULT_COLLECTION_TREE_PAGE_SIZE,
    parseCollectionTreeConfig,
} from './collection-tree-config';
import {
    CollectionTreeService,
    getCollectionTitle,
    PlacedCollection,
} from './collection-tree.service';

/** Guards against a cycle in the parent links of corrupt data. */
const MAX_DEPTH = 50;
/** The row loading the next page of search hits. */
const SEARCH_MORE_ID = 'more:search';

/**
 * Lazily loaded, searchable tree of the collections the user can read, for `collectionTree`
 * widgets. It starts at the collection named by the page URL (`collectionRoot`), else at the one
 * configured, else at the categories of the collections page.
 */
@Injectable()
export class CollectionTreeSource implements TreeSource {
    private readonly collectionScopesService = inject(CollectionScopesService);
    private readonly collectionTreeService = inject(CollectionTreeService);
    private readonly translate = inject(TranslateService);
    private readonly urlRootId = toSignal(
        inject(ActivatedRoute).queryParamMap.pipe(
            map((params) => params.get(COLLECTION_TREE_ROOT_QUERY_PARAM)),
        ),
        { initialValue: null },
    );
    private readonly config = signal<CollectionTreeConfig>({});
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
        { tree: Tree; collection?: Node; scope?: CollectionScope }
    >();
    private browseTree: { key: string; tree: Promise<Tree> } | null = null;
    /** The categories the browse tree lists, none below a start collection. */
    private categories: CollectionScope[] = [];
    private searchTree: Tree | null = null;
    /** The hits of the latest search, extended page by page. */
    private search: SearchState | null = null;

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

    /** @param configuration the widget's configuration, see `CollectionTreeConfig` */
    configure(configuration: string | undefined): this {
        this.config.set(parseCollectionTreeConfig(configuration));
        return this;
    }

    getTree(filter: string | null): Observable<Tree> {
        if (!filter) {
            return from(this.getBrowseTree());
        }
        // the browse tree loads the start collection the search is limited to; the default metadata
        // set searches all collections the browse tree lists, whatever metadata set they were made in
        return from(this.getBrowseTree()).pipe(
            switchMap(() => {
                const rootId = this.withinRoot() ? this.rootNode()?.ref.id ?? null : null;
                const key = `${rootId}|${filter}`;
                // the latest search is listed again with all pages loaded so far
                const search =
                    this.search?.key === key
                        ? of(this.search)
                        : this.loadSearchPage({
                              key,
                              filter,
                              rootId,
                              hits: [],
                              placed: new Map(),
                              loaded: 0,
                              total: 0,
                          });
                return search.pipe(map((state) => this.createSearchTree(state)));
            }),
        );
    }

    async loadChildren(node: TreeNode): Promise<void> {
        if (node.id !== SEARCH_MORE_ID) {
            return this.appendPage(node);
        }
        if (this.search) {
            await firstValueFrom(this.loadSearchPage(this.search));
        }
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
        this.categories = [];
        const root = this.withinRoot() ? await this.loadRoot() : null;
        if (root) {
            const rootNode = this.addCollection(tree, root);
            // the start collection is listed with its first page of collections, and can be
            // expanded only when that page holds any
            await this.appendPage(rootNode);
            rootNode.children ??= [];
            rootNode.hasChildren = rootNode.children.length > 0;
        } else {
            this.categories = await firstValueFrom(
                this.collectionScopesService.getScopes({
                    scopes: this.config().scopes,
                    hideEmpty: true,
                }),
            );
            for (const scope of this.categories) {
                const category = this.addCategory(tree, scope);
                category.hasChildren = true;
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

    private addCategory(tree: Tree, scope: CollectionScope): TreeNode {
        return tree.addNode({
            id: `category:${scope}`,
            caption: this.translate.instant(`COLLECTIONS.TAB.${scope}`),
            type: 'group',
        });
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

    /** Loads the next page of hits of a search, which becomes the latest one. */
    private loadSearchPage(search: SearchState): Observable<SearchState> {
        return this.collectionTreeService
            .searchCollectionsPage(search.filter, {
                rootId: search.rootId,
                mdsId: DEFAULT,
                maxItems: COLLECTION_TREE_MAX_SEARCH_HITS,
                skipCount: search.loaded,
            })
            .pipe(
                switchMap((page) => {
                    const hits = page.nodes.filter((hit) => matchesTitle(hit, search.filter));
                    // the search covers the collections below the start collection, not itself
                    const root = this.rootNode();
                    if (
                        !search.loaded &&
                        root?.ref.id === search.rootId &&
                        matchesTitle(root, search.filter)
                    ) {
                        hits.unshift(root);
                    }
                    return this.collectionTreeService.getWithAncestors(hits, DEFAULT).pipe(
                        map((placed) => {
                            const loaded = search.loaded + page.nodes.length;
                            this.search = {
                                ...search,
                                hits: [...search.hits, ...hits],
                                placed: new Map([...search.placed, ...placed]),
                                loaded,
                                // an empty page ends the list, e.g. when hits became unreadable
                                total: page.nodes.length ? page.total : loaded,
                            };
                            return this.search;
                        }),
                    );
                }),
            );
    }

    /**
     * Lists search hits below the collections leading to them from the start collection, else from
     * the category of their top-level collection. A hit's own sub-collections are loaded once it
     * is expanded.
     */
    private createSearchTree({ hits, placed, rootId, loaded, total }: SearchState): Tree {
        for (const [node, source] of this.lazyNodes) {
            if (source.tree === this.searchTree) {
                this.lazyNodes.delete(node);
            }
        }
        const tree = Tree.generateTree([]);
        this.searchTree = tree;
        for (const hit of hits) {
            const path = getPath(hit.ref.id, placed);
            const offset = path && rootId ? path.indexOf(rootId) : 0;
            // only a repository that cannot search below a collection returns hits outside of it
            if (offset < 0) {
                continue;
            }
            const top = (path && placed.get(path[0])?.node) ?? hit;
            const scope = rootId ? undefined : this.collectionScopesService.getScopeOf(top);
            // another user's private collection is readable to admins, but no category lists it
            if (scope === null) {
                continue;
            }
            let parent = scope ? this.getSearchCategory(tree, scope) : undefined;
            for (const id of path?.slice(offset) ?? [hit.ref.id]) {
                const siblings = parent ? parent.children : tree.rootNodes;
                parent =
                    siblings?.find((node) => node.id === id) ??
                    tree.addNode(
                        { id, caption: getCollectionTitle(placed.get(id)?.node ?? hit) },
                        parent,
                    );
            }
            // a hit leading to other hits lists only those
            if (parent && !parent.children && (hit.collection?.childCollectionsCount ?? 0) > 0) {
                parent.hasChildren = true;
                this.lazyNodes.set(parent, { tree, collection: hit });
            }
        }
        const categories = this.categories.map((scope) => `category:${scope}`);
        const rank = (node: TreeNode) =>
            node.type === 'group' ? categories.indexOf(node.id) : categories.length;
        tree.rootNodes.sort((a, b) => rank(a) - rank(b));
        if (loaded < total) {
            tree.addNode({ id: SEARCH_MORE_ID, caption: '', type: 'more' });
        }
        return tree;
    }

    /** The category node listing hits of `scope`, none for a category the widget does not list. */
    private getSearchCategory(tree: Tree, scope: CollectionScope): TreeNode | undefined {
        if (!this.categories.includes(scope)) {
            return undefined;
        }
        return (
            tree.rootNodes.find((node) => node.id === `category:${scope}`) ??
            this.addCategory(tree, scope)
        );
    }
}

/** A search with the hits of the pages loaded so far and the collections leading to them. */
interface SearchState {
    key: string;
    filter: string;
    rootId: string | null;
    hits: Node[];
    placed: Map<string, PlacedCollection>;
    /** Hits loaded so far, including those dropped for not matching the title. */
    loaded: number;
    total: number;
}

/**
 * Whether the title of a collection holds the text searched for, or the filter is its id, since the
 * search also matches single words, keywords and descriptions, which the tree does not show.
 */
function matchesTitle(collection: Node, filter: string): boolean {
    const text = normalize(filter);
    return collection.ref.id === text || normalize(getCollectionTitle(collection)).includes(text);
}

function normalize(text: string): string {
    return text.trim().replace(/\s+/g, ' ').toLocaleLowerCase();
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
