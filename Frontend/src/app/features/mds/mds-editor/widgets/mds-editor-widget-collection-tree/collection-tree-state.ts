import { computed, Signal, signal, WritableSignal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { EMPTY, forkJoin, Observable, of, Subject, Subscription } from 'rxjs';
import {
    catchError,
    debounceTime,
    filter,
    map,
    startWith,
    switchMap,
    take,
    tap,
} from 'rxjs/operators';
import { CollectionTreeScope } from './collection-tree-config';
import { CollectionTreeService, PARENT_ID_PROPERTY } from './collection-tree.service';

const MIN_SEARCH_LENGTH = 2;
const SEARCH_DEBOUNCE_MS = 300;
/** Guards against a cycle in the parent links of corrupt data. */
const MAX_DEPTH = 50;
export const PATH_SEPARATOR = ' › ';

type LoadStatus = 'loading' | 'ready' | 'error';

/** A loaded collection inside the tree or the search results. */
export interface CollectionTreeItem {
    readonly node: Node;
    /** Ids of the collections above this one, top-most first, `null` while unknown. */
    readonly ancestorIds: Signal<string[] | null>;
    /** Set when the repository reported that the collection has no sub-collections. */
    readonly leaf: boolean;
    readonly expanded: WritableSignal<boolean>;
    /** Created when the item is expanded for the first time. */
    readonly children: WritableSignal<CollectionTreeLevel | null>;
    /** Search hits and the collections leading to them, listed instead of the children. */
    readonly branches: WritableSignal<CollectionTreeItem[]>;
}

/** The children of one collection, or a top level, loaded page by page. */
export interface CollectionTreeLevel {
    readonly parent: CollectionTreeItem | null;
    /** Category a top level lists the collections of, `null` below a collection. */
    readonly scope: CollectionTreeScope | null;
    readonly items: WritableSignal<CollectionTreeItem[]>;
    readonly total: WritableSignal<number>;
    readonly status: WritableSignal<LoadStatus>;
}

export interface CollectionNodeRow {
    readonly kind: 'node';
    /** Unique per row, a collection can be listed in the tree and in the search results. */
    readonly key: string;
    /** Key of the row this one is listed below, `null` on the first level. */
    readonly parentKey: string | null;
    readonly node: Node;
    readonly level: number;
    /** `null` when the location of a search hit could not be loaded. */
    readonly ancestorIds: string[] | null;
    readonly item: CollectionTreeItem;
    readonly expandable: boolean;
    readonly expanded: boolean;
}

/** A category of the top level, like a tab of the collections page. */
export interface CollectionTreeCategory {
    readonly scope: CollectionTreeScope;
    readonly expanded: WritableSignal<boolean>;
    /** Created when the category is loaded for the first time. */
    readonly level: WritableSignal<CollectionTreeLevel | null>;
}

/** Header of a category, grouping collections without being selectable itself. */
export interface CollectionCategoryRow {
    readonly kind: 'category';
    readonly key: string;
    readonly level: number;
    readonly category: CollectionTreeCategory;
    readonly expanded: boolean;
}

export interface CollectionStatusRow {
    readonly kind: 'more' | 'loading' | 'error' | 'empty';
    readonly key: string;
    readonly level: number;
    /** The tree level the row acts on, `null` for the search results. */
    readonly target: CollectionTreeLevel | null;
    readonly loaded?: number;
    readonly total?: number;
}

export type CollectionTreeRow = CollectionNodeRow | CollectionCategoryRow | CollectionStatusRow;

export interface CollectionTreeStateOptions {
    /** Collections to start at, tried in order until one can be read. */
    rootCandidates: readonly string[];
    /** Categories of the top level; empty ones are left out, a single one has no header. */
    scopes: readonly CollectionTreeScope[];
    pageSize: number;
    mdsId: string;
}

/**
 * Tree and search state of one `collectionTree` widget. The tree starts at the first readable
 * root candidate, listed as the only top node, unless the user lists the top level by category.
 */
export class CollectionTreeState {
    /** The first readable root candidate, `null` while loading or when there is none. */
    readonly rootNode = signal<Node | null>(null);
    /** Set when a root candidate could not be read and a fallback is listed instead. */
    readonly rootUnavailable = signal(false);
    /** Whether tree and search are limited to the root collection, chosen by the user. */
    readonly withinRoot = signal(true);
    readonly searchTerm = signal('');
    readonly searchActive = computed(() => this.searchTerm().trim().length >= MIN_SEARCH_LENGTH);
    readonly searchStatus = signal<LoadStatus>('ready');
    readonly searchHits = signal<Node[]>([]);
    readonly searchTotal = signal(0);

    /** The collection tree and search start at, `null` for the top level. */
    private readonly startNode = computed(() => (this.withinRoot() ? this.rootNode() : null));
    private readonly rootLevel = signal<CollectionTreeLevel | null>(null);
    /** Categories of the top level that hold collections, `null` until they are known. */
    private readonly categories = signal<CollectionTreeCategory[] | null>(null);
    private categoriesRequested = false;
    /** Number of search hits requested so far, including hits that are not listed. */
    private readonly searchFetched = signal(0);
    private readonly searchRoots = signal<CollectionTreeItem[]>([]);
    /** Items listed in the current search results, by collection id. */
    private readonly searchItems = new Map<string, CollectionTreeItem>();
    /** Known collections by id with their parent's id, `null` for a top-level collection. */
    private readonly known = new Map<string, { node: Node; parentId: string | null }>();
    /** Collections that were looked up but cannot be read, no path leads through them. */
    private readonly unreachable = new Set<string>();
    private readonly searchRequests = new Subject<void>();
    /** Emits whenever the top level is set or a tree level finished loading. */
    private readonly treeLoaded = new Subject<void>();
    private readonly subscriptions = new Subscription();

    readonly treeRows = computed((): CollectionTreeRow[] => {
        const rows: CollectionTreeRow[] = [];
        const categories = this.categories();
        if (!this.startNode() && categories && categories.length !== 1) {
            if (!categories.length) {
                rows.push({ kind: 'empty', key: 'top-empty', level: 0, target: null });
            }
            for (const category of categories) {
                const key = getCategoryKey(category);
                const expanded = category.expanded();
                rows.push({ kind: 'category', key, level: 0, category, expanded });
                const level = category.level();
                if (expanded && level) {
                    this.appendLevel(rows, level, 1, key);
                }
            }
            return rows;
        }
        // the root, or a single category, is listed without a header
        const top = this.startNode() ? this.rootLevel() : categories?.[0]?.level();
        if (top) {
            this.appendLevel(rows, top, 0, null);
        } else {
            rows.push({ kind: 'loading', key: 'top-loading', level: 0, target: null });
        }
        return rows;
    });

    /** The search hits below the collections they are located in, like a filtered tree. */
    readonly searchRows = computed((): CollectionTreeRow[] => {
        const rows: CollectionTreeRow[] = [];
        for (const item of this.searchRoots()) {
            this.appendBranch(rows, item, 0, null);
        }
        const status = this.searchStatus();
        const loaded = this.searchFetched();
        const total = this.searchTotal();
        if (status === 'loading') {
            rows.push({ kind: 'loading', key: 'search-loading', level: 0, target: null });
        } else if (status === 'error') {
            rows.push({ kind: 'error', key: 'search-error', level: 0, target: null });
        } else if (loaded < total) {
            rows.push({ kind: 'more', key: 'search-more', level: 0, target: null, loaded, total });
        } else if (!rows.length) {
            rows.push({ kind: 'empty', key: 'search-empty', level: 0, target: null });
        }
        return rows;
    });

    constructor(
        private readonly service: CollectionTreeService,
        private readonly options: CollectionTreeStateOptions,
    ) {
        this.subscriptions.add(
            this.searchRequests
                .pipe(
                    debounceTime(SEARCH_DEBOUNCE_MS),
                    switchMap(() => this.fetchSearchPage(0)),
                )
                .subscribe(),
        );
    }

    init(): void {
        this.subscriptions.add(
            this.resolveRoot(this.options.rootCandidates).subscribe((path) => {
                if (!path?.length) {
                    this.loadCategories();
                    return;
                }
                const root = path[path.length - 1];
                // only the root itself is read with the user's rights
                path.forEach((node, index) =>
                    this.remember(
                        node === root ? node : withoutAccess(node),
                        index ? path[index - 1].ref.id : null,
                    ),
                );
                // the path request reports no sub-collection counts
                const item = createItem(
                    root,
                    signal(path.slice(0, -1).map((n) => n.ref.id)),
                    false,
                );
                this.rootNode.set(root);
                this.rootLevel.set({
                    parent: null,
                    scope: null,
                    items: signal([item]),
                    total: signal(1),
                    status: signal('ready'),
                });
                this.expand(item);
                this.treeLoaded.next();
            }),
        );
    }

    destroy(): void {
        this.subscriptions.unsubscribe();
        this.searchRequests.complete();
        this.treeLoaded.complete();
    }

    /** Limits tree and search to the root collection, or lists the top level by category. */
    setWithinRoot(withinRoot: boolean): void {
        this.withinRoot.set(withinRoot);
        if (!withinRoot) {
            this.loadCategories();
        }
        if (this.searchActive()) {
            this.startSearch();
        }
    }

    /**
     * Expands the tree along `path`, ids from the top-most collection down, as far as it is
     * listed, and emits the row key of the deepest listed collection or `null`.
     */
    reveal(path: readonly string[]): Observable<string | null> {
        return this.treeLoaded.pipe(
            startWith(undefined),
            map(() => this.expandAlong(path)),
            filter((result): result is string | null => result !== PENDING),
            take(1),
        );
    }

    expand(item: CollectionTreeItem): void {
        item.expanded.set(true);
        // a collection leading to search hits lists those instead of all of its children
        if (!item.branches().length && !item.children()) {
            const level = createLevel(item);
            item.children.set(level);
            this.loadPage(level);
        }
    }

    collapse(item: CollectionTreeItem): void {
        item.expanded.set(false);
    }

    /** Lists the collections of a category, loading them the first time. */
    expandCategory(category: CollectionTreeCategory): void {
        category.expanded.set(true);
        this.loadCategory(category);
    }

    collapseCategory(category: CollectionTreeCategory): void {
        category.expanded.set(false);
    }

    toggleCategory(category: CollectionTreeCategory): void {
        if (category.expanded()) {
            this.collapseCategory(category);
        } else {
            this.expandCategory(category);
        }
    }

    toggleExpanded(item: CollectionTreeItem): void {
        if (item.expanded()) {
            this.collapse(item);
        } else {
            this.expand(item);
        }
    }

    /** Loads the next page of the given tree level, or of the search results for `null`. */
    loadMore(level: CollectionTreeLevel | null): void {
        if (level) {
            this.loadPage(level);
        } else {
            this.searchStatus.set('loading');
            this.subscriptions.add(this.fetchSearchPage(this.searchFetched()).subscribe());
        }
    }

    retry(level: CollectionTreeLevel | null): void {
        if (level) {
            this.loadPage(level);
        } else {
            this.startSearch();
        }
    }

    setSearchTerm(term: string): void {
        this.searchTerm.set(term);
        if (this.searchActive()) {
            this.startSearch();
        }
    }

    private startSearch(): void {
        this.searchStatus.set('loading');
        this.searchHits.set([]);
        this.searchItems.clear();
        this.searchRoots.set([]);
        this.searchFetched.set(0);
        this.searchTotal.set(0);
        this.searchRequests.next();
    }

    private resolveRoot(candidates: readonly string[]): Observable<Node[] | null> {
        if (!candidates.length) {
            return of(null);
        }
        return this.service.getAncestors(candidates[0]).pipe(
            catchError(() => {
                this.rootUnavailable.set(true);
                return this.resolveRoot(candidates.slice(1));
            }),
        );
    }

    /**
     * Finds the categories that hold collections, with one single-entry request each. A category
     * that cannot be checked is kept, its error shows once it is expanded.
     */
    private loadCategories(): void {
        if (this.categoriesRequested) {
            return;
        }
        this.categoriesRequested = true;
        const scopes = this.options.scopes;
        if (scopes.length < 2) {
            this.showCategories(scopes.map(createCategory));
            return;
        }
        const probes = scopes.map((scope) =>
            this.service.getChildren(null, { scope, skipCount: 0, maxItems: 1 }).pipe(
                map((page) => page.total > 0),
                catchError(() => of(true)),
            ),
        );
        this.subscriptions.add(
            forkJoin(probes).subscribe((filled) =>
                this.showCategories(scopes.filter((_, i) => filled[i]).map(createCategory)),
            ),
        );
    }

    private showCategories(categories: CollectionTreeCategory[]): void {
        this.categories.set(categories);
        // a single category is listed without its header, so its collections are shown at once
        if (categories.length === 1) {
            this.loadCategory(categories[0]);
        }
        this.treeLoaded.next();
    }

    private loadCategory(category: CollectionTreeCategory): CollectionTreeLevel {
        let level = category.level();
        if (!level) {
            level = createLevel(null, category.scope);
            category.level.set(level);
            this.loadPage(level);
        }
        return level;
    }

    private loadPage(level: CollectionTreeLevel): void {
        level.status.set('loading');
        const parent = level.parent;
        const ancestorIds: Signal<string[] | null> = parent
            ? computed(() => [...(parent.ancestorIds() ?? []), parent.node.ref.id])
            : signal([]);
        const skipCount = level.items().length;
        this.subscriptions.add(
            this.service
                .getChildren(parent?.node ?? null, {
                    scope: level.scope ?? undefined,
                    skipCount,
                    maxItems: this.options.pageSize,
                })
                .subscribe({
                    next: (page) => {
                        const known = new Set(level.items().map((item) => item.node.ref.id));
                        const added = page.nodes
                            .filter((node) => !known.has(node.ref.id))
                            .map((node) => createItem(node, ancestorIds, true));
                        level.items.update((items) => [...items, ...added]);
                        // the total also counts collections the user cannot read, so a request
                        // that covered it or brought nothing new ends the level
                        const complete =
                            !added.length || skipCount + this.options.pageSize >= page.total;
                        level.total.set(complete ? level.items().length : page.total);
                        level.status.set('ready');
                        for (const node of page.nodes) {
                            this.remember(node, parent?.node.ref.id ?? null);
                        }
                        this.treeLoaded.next();
                    },
                    error: () => {
                        level.status.set('error');
                        this.treeLoaded.next();
                    },
                }),
        );
    }

    /**
     * Expands the loaded part of the tree along `path`, loading categories to find its start.
     * @returns the key `reveal` emits, or `PENDING` while a level on the way is still loading
     */
    private expandAlong(path: readonly string[]): string | null | typeof PENDING {
        const start = this.startNode();
        if (start) {
            const root = this.rootLevel();
            if (!root) {
                return PENDING;
            }
            // the tree starts at the start node, collections above it are not listed
            const offset = path.indexOf(start.ref.id);
            return offset < 0 ? null : this.expandLevelAlong(root, path.slice(offset), null);
        }
        const categories = this.categories();
        if (!categories) {
            return PENDING;
        }
        if (categories.length === 1) {
            return this.expandLevelAlong(this.loadCategory(categories[0]), path, null);
        }
        for (const category of categories) {
            // categories are loaded without expanding them, only the one holding `path` opens
            const level = this.loadCategory(category);
            if (level.status() === 'loading') {
                return PENDING;
            }
            if (level.items().some((item) => item.node.ref.id === path[0])) {
                category.expanded.set(true);
                return this.expandLevelAlong(level, path, getCategoryKey(category));
            }
        }
        return null;
    }

    /** Expands `level` and the levels below it along `path`, which starts in `level`. */
    private expandLevelAlong(
        level: CollectionTreeLevel,
        path: readonly string[],
        parentKey: string | null,
    ): string | null | typeof PENDING {
        let current: CollectionTreeLevel | null = level;
        let key = parentKey;
        let found: string | null = null;
        for (const id of path) {
            const item = current?.items().find((candidate) => candidate.node.ref.id === id);
            if (!item) {
                return current?.status() === 'loading' ? PENDING : found;
            }
            key = key ? `${key}/${id}` : id;
            found = key;
            if (id === path[path.length - 1] || item.leaf) {
                break;
            }
            this.expand(item);
            current = item.children();
        }
        return found;
    }

    private fetchSearchPage(skipCount: number): Observable<void> {
        const term = this.searchTerm().trim();
        if (term.length < MIN_SEARCH_LENGTH) {
            return EMPTY;
        }
        const start = this.startNode();
        const isCurrent = () => term === this.searchTerm().trim() && start === this.startNode();
        return this.service
            .searchCollections(term, {
                rootId: start?.ref.id ?? null,
                mdsId: this.options.mdsId,
                skipCount,
                maxItems: this.options.pageSize,
            })
            .pipe(
                switchMap((page) =>
                    isCurrent() ? this.loadHitPaths(page.nodes).pipe(map(() => page)) : EMPTY,
                ),
                map((page) => {
                    if (!isCurrent()) {
                        return;
                    }
                    const known = new Set(this.searchHits().map((hit) => hit.ref.id));
                    const added = page.nodes.filter((node) => !known.has(node.ref.id));
                    this.searchHits.update((hits) => [...hits, ...added]);
                    this.searchFetched.set(skipCount + page.nodes.length);
                    // a page without new hits ends the results even if the total says otherwise
                    this.searchTotal.set(
                        added.length || !skipCount ? page.total : this.searchFetched(),
                    );
                    this.placeSearchHits();
                    this.searchStatus.set('ready');
                }),
                catchError(() => {
                    if (isCurrent()) {
                        this.searchStatus.set('error');
                    }
                    return EMPTY;
                }),
            );
    }

    /** Looks up the collections above `hits` that are not known yet, one request per level. */
    private loadHitPaths(hits: Node[]): Observable<void> {
        for (const hit of hits) {
            this.remember(hit, getParentIdOf(hit));
        }
        return this.lookUpMissing(hits.map((hit) => hit.ref.id));
    }

    /**
     * Looks up the first unknown collection above each of `ids`, all of them in one request, and
     * repeats this until every path is known or leads through a collection that cannot be read.
     */
    private lookUpMissing(ids: string[]): Observable<void> {
        const missing = [
            ...new Set(ids.map((id) => this.getFirstUnknown(id)).filter((id) => !!id)),
        ];
        if (!missing.length) {
            return of(undefined);
        }
        return this.service.getCollections(missing, this.options.mdsId).pipe(
            catchError(() => of([] as Node[])),
            tap((found) => {
                for (const node of found) {
                    this.remember(node, getParentIdOf(node));
                }
                // collections the user cannot read are not returned
                missing
                    .filter((id) => !this.known.has(id))
                    .forEach((id) => this.unreachable.add(id));
            }),
            switchMap(() => this.lookUpMissing(ids)),
        );
    }

    /**
     * Remembers a loaded collection. A node read with the user's rights replaces one read with
     * other rights, which comes without `access`.
     */
    private remember(node: Node, parentId: string | null): void {
        const known = this.known.get(node.ref.id);
        if (!known || (!known.node.access && node.access)) {
            this.known.set(node.ref.id, { node, parentId: parentId ?? known?.parentId ?? null });
        }
    }

    /** The first collection on the way up from `id` that is neither known nor unreachable. */
    private getFirstUnknown(id: string): string | null {
        let current: string | null = id;
        for (let depth = 0; current && depth < MAX_DEPTH; depth++) {
            if (this.unreachable.has(current)) {
                return null;
            }
            const known = this.known.get(current);
            if (!known) {
                return current;
            }
            current = known.parentId;
        }
        return null;
    }

    /** The collections from the top-most one down to `id`, `null` while the path is unknown. */
    private getPath(id: string): Node[] | null {
        const path: Node[] = [];
        let current: string | null = id;
        while (current) {
            const known = this.known.get(current);
            if (!known || path.length >= MAX_DEPTH) {
                return null;
            }
            path.unshift(known.node);
            current = known.parentId;
        }
        return path;
    }

    /** Arranges the search hits below the collections leading to them from the start node. */
    private placeSearchHits(): void {
        const start = this.startNode();
        const hits = this.searchHits();
        const roots: CollectionTreeItem[] = [];
        const branches = new Map<CollectionTreeItem, CollectionTreeItem[]>();
        // hits come with the user's access rights, so they are used where they lead to other hits
        for (const hit of hits) {
            this.getSearchItem(hit, this.getPath(hit.ref.id)?.slice(0, -1) ?? null, false);
        }
        for (const hit of hits) {
            const path = this.getPath(hit.ref.id);
            const offset =
                path && start ? path.findIndex((node) => node.ref.id === start.ref.id) : 0;
            // only a repository without the search below a collection returns hits outside of it
            if (offset < 0) {
                continue;
            }
            const listed = path ? path.slice(offset) : [hit];
            let siblings = roots;
            listed.forEach((node, index) => {
                const leadsToHit = index < listed.length - 1;
                const ancestors = path ? path.slice(0, offset + index) : null;
                const item = this.getSearchItem(node, ancestors, leadsToHit);
                if (!siblings.includes(item)) {
                    siblings.push(item);
                }
                if (leadsToHit) {
                    siblings = branches.get(item) ?? [];
                    branches.set(item, siblings);
                }
            });
        }
        for (const item of this.searchItems.values()) {
            const itemBranches = branches.get(item) ?? [];
            // a collection is expanded once it leads to hits, afterwards the user decides
            if (itemBranches.length && !item.branches().length) {
                item.expanded.set(true);
            }
            item.branches.set(itemBranches);
        }
        this.searchRoots.set(roots);
    }

    private getSearchItem(
        node: Node,
        ancestors: Node[] | null,
        leadsToHit: boolean,
    ): CollectionTreeItem {
        let item = this.searchItems.get(node.ref.id);
        if (!item) {
            const ancestorIds = ancestors?.map((ancestor) => ancestor.ref.id) ?? null;
            // hits report their sub-collection counts, collections taken from a path do not
            item = createItem(node, signal(ancestorIds), !leadsToHit);
            this.searchItems.set(node.ref.id, item);
        }
        return item;
    }

    private appendBranch(
        rows: CollectionTreeRow[],
        item: CollectionTreeItem,
        depth: number,
        parentKey: string | null,
    ): void {
        const key = `${parentKey ?? 'search'}/${item.node.ref.id}`;
        rows.push(toRow(item, key, parentKey, depth));
        if (!item.expanded()) {
            return;
        }
        const branches = item.branches();
        const children = item.children();
        if (branches.length) {
            branches.forEach((branch) => this.appendBranch(rows, branch, depth + 1, key));
        } else if (children) {
            this.appendLevel(rows, children, depth + 1, key);
        }
    }

    private appendLevel(
        rows: CollectionTreeRow[],
        level: CollectionTreeLevel,
        depth: number,
        parentKey: string | null,
    ) {
        const items = level.items();
        for (const item of items) {
            const itemKey = parentKey ? `${parentKey}/${item.node.ref.id}` : item.node.ref.id;
            rows.push(toRow(item, itemKey, parentKey, depth));
            const children = item.children();
            if (item.expanded() && children) {
                this.appendLevel(rows, children, depth + 1, itemKey);
            }
        }
        const key = parentKey ?? 'top';
        const status = level.status();
        const total = level.total();
        if (status === 'loading') {
            rows.push({ kind: 'loading', key: key + '-loading', level: depth, target: level });
        } else if (status === 'error') {
            rows.push({ kind: 'error', key: key + '-error', level: depth, target: level });
        } else if (items.length < total) {
            const loaded = items.length;
            rows.push({
                kind: 'more',
                key: key + '-more',
                level: depth,
                target: level,
                loaded,
                total,
            });
        } else if (!items.length && !level.parent) {
            rows.push({ kind: 'empty', key: key + '-empty', level: depth, target: level });
        }
    }
}

const PENDING = Symbol('pending');

function createLevel(
    parent: CollectionTreeItem | null,
    scope: CollectionTreeScope | null = null,
): CollectionTreeLevel {
    return { parent, scope, items: signal([]), total: signal(0), status: signal('loading') };
}

function createCategory(scope: CollectionTreeScope): CollectionTreeCategory {
    return { scope, expanded: signal(false), level: signal(null) };
}

function getCategoryKey(category: CollectionTreeCategory): string {
    return `category:${category.scope}`;
}

/**
 * @param countsKnown whether the request that returned `node` filled in its sub-collection
 * counts, which are reported as 0 otherwise
 */
function createItem(
    node: Node,
    ancestorIds: Signal<string[] | null>,
    countsKnown: boolean,
): CollectionTreeItem {
    const leaf = countsKnown && node.collection?.childCollectionsCount === 0;
    return {
        node,
        ancestorIds,
        leaf,
        expanded: signal(false),
        children: signal(null),
        branches: signal([]),
    };
}

function toRow(
    item: CollectionTreeItem,
    key: string,
    parentKey: string | null,
    level: number,
): CollectionNodeRow {
    const children = item.children();
    const withoutChildren = children?.status() === 'ready' && children.items().length === 0;
    const expandable = item.branches().length > 0 || (!item.leaf && !withoutChildren);
    return {
        kind: 'node',
        key,
        parentKey,
        node: item.node,
        level,
        ancestorIds: item.ancestorIds(),
        item,
        expandable,
        expanded: expandable && item.expanded(),
    };
}

export function getCollectionTitle(node: Node): string {
    return node.title || node.name;
}

/** The id of the collection above `node`, `null` for a top-level collection. */
function getParentIdOf(node: Node): string | null {
    return node.collection?.level0 ? null : node.properties?.[PARENT_ID_PROPERTY]?.[0] ?? null;
}

/** A copy of `node` whose access rights are unknown to the user. */
function withoutAccess(node: Node): Node {
    return { ...node, access: undefined };
}
