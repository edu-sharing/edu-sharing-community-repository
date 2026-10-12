import { inject, Injectable } from '@angular/core';
import {
    CollectionService,
    DEFAULT,
    HOME_REPOSITORY,
    Node,
    NodeServiceUnwrapped,
    ROOT,
    SearchService,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from 'ngx-edu-sharing-ui';
import { from, MonoTypeOperatorFunction, Observable, of, throwError } from 'rxjs';
import { catchError, map, mergeMap, shareReplay, switchMap, tap, toArray } from 'rxjs/operators';
import { RestConstants } from '../../../../../../core-module/rest/rest-constants';
import { CollectionTreeScope } from './collection-tree-config';

/** A page of collections together with the total number available. */
export interface CollectionPage {
    nodes: Node[];
    total: number;
}

/** A collection with the id of the collection above it, `null` for a top-level collection. */
export interface PlacedCollection {
    node: Node;
    parentId: string | null;
}

/** Property holding the id of a collection's parent, requested with each search hit. */
export const PARENT_ID_PROPERTY = 'virtual:primaryparent_nodeid';

/** Properties requested for every collection: the order of its children and its parent. */
const PROPERTY_FILTER = [
    RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
    PARENT_ID_PROPERTY,
];
/** Parameters of the collections query limiting it to a subtree and to given collections. */
const PARENT_RECURSIVE_PARAMETER = 'virtual:parent_recursive';
const NODE_ID_PARAMETER = 'virtual:node_id';
const ANCESTORS_CACHE_DURATION_MS = 5 * 60 * 1000;
const PARALLEL_PATH_REQUESTS = 4;
/** Guards against a cycle in the parent links of corrupt data. */
const MAX_DEPTH = 50;

export function getCollectionTitle(node: Node): string {
    return node.title || node.name;
}

/** The id of the collection above `node`, `null` for a top-level collection. */
export function getParentIdOf(node: Node): string | null {
    return node.collection?.level0 ? null : node.properties?.[PARENT_ID_PROPERTY]?.[0] ?? null;
}

/** Marks a failed request as handled, so the application shows no error message for it. */
function handledByCaller<T>(): MonoTypeOperatorFunction<T> {
    return catchError((error: unknown) => {
        markAsHandled(error);
        return throwError(() => error);
    });
}

function markAsHandled(error: unknown): void {
    (error as { preventDefault?: () => void })?.preventDefault?.();
}

/**
 * Loads collections for the `collectionTree` widget: tree levels, name search and the path of a
 * collection. Only collections the user can read are returned by the repository.
 */
@Injectable({ providedIn: 'root' })
export class CollectionTreeService {
    private readonly collectionService = inject(CollectionService);
    // requested without the shared cache, which holds nodes with all of their properties
    private readonly nodeService = inject(NodeServiceUnwrapped);
    private readonly nodeHelper = inject(NodeHelperService);
    private readonly searchService = inject(SearchService);

    private readonly ancestorsCache = new Map<
        string,
        { created: number; ancestors: Observable<Node[]> }
    >();
    /** Query parameters a metadata set rejected, as `mdsId|parameter`. */
    private readonly unsupportedParameters = new Set<string>();

    /**
     * The collection itself and all collections above it, top-most first. A failure is left to
     * the caller, e.g. to show the collection as not accessible.
     */
    getAncestors(id: string): Observable<Node[]> {
        const cached = this.ancestorsCache.get(id);
        if (cached && Date.now() - cached.created < ANCESTORS_CACHE_DURATION_MS) {
            return cached.ancestors;
        }
        const ancestors = this.nodeService
            .getParents({ repository: HOME_REPOSITORY, node: id, propertyFilter: PROPERTY_FILTER })
            .pipe(
                handledByCaller(),
                map((entries) => [...entries.nodes].reverse()),
                catchError((error: unknown) => {
                    this.ancestorsCache.delete(id);
                    return throwError(() => error);
                }),
                shareReplay(1),
            );
        this.ancestorsCache.set(id, { created: Date.now(), ancestors });
        return ancestors;
    }

    /**
     * The collections with the given ids the user can read, each carrying its parent's id. A
     * metadata set that cannot look them up by id makes this load the path of each one instead.
     */
    getCollections(ids: string[], mdsId: string): Observable<Node[]> {
        const metadataset = mdsId || DEFAULT;
        if (!ids.length) {
            return of([]);
        }
        if (this.unsupportedParameters.has(`${metadataset}|${NODE_ID_PARAMETER}`)) {
            return this.getCollectionsWithPaths(ids);
        }
        return this.searchService
            .requestSearch({
                repository: HOME_REPOSITORY,
                metadataset,
                query: RestConstants.QUERY_NAME_COLLECTIONS,
                contentType: 'COLLECTIONS',
                propertyFilter: PROPERTY_FILTER,
                skipCount: 0,
                maxItems: ids.length,
                body: { criteria: [{ property: NODE_ID_PARAMETER, values: ids }] },
            })
            .pipe(
                map((result) => (result.nodes ?? []) as Node[]),
                catchError((error: unknown) => {
                    markAsHandled(error);
                    console.warn('Collection lookup failed, loading paths instead', error);
                    this.rememberUnsupported(error, metadataset, NODE_ID_PARAMETER);
                    return this.getCollectionsWithPaths(ids);
                }),
            );
    }

    /**
     * The given collections and all readable collections above them, by id. The collections
     * above are looked up level by level, all of one level in one request.
     */
    getWithAncestors(nodes: Node[], mdsId: string): Observable<Map<string, PlacedCollection>> {
        const known = new Map<string, PlacedCollection>();
        // collections the user cannot read are not returned, which ends the way up
        const unreachable = new Set<string>();
        const remember = (node: Node) => {
            if (!known.has(node.ref.id)) {
                known.set(node.ref.id, { node, parentId: getParentIdOf(node) });
            }
        };
        const getFirstUnknown = (id: string): string | null => {
            let current: string | null = id;
            for (let depth = 0; current && depth < MAX_DEPTH; depth++) {
                if (unreachable.has(current)) {
                    return null;
                }
                const placed = known.get(current);
                if (!placed) {
                    return current;
                }
                current = placed.parentId;
            }
            return null;
        };
        const lookUpMissing = (): Observable<Map<string, PlacedCollection>> => {
            const missing = [
                ...new Set(nodes.map((node) => getFirstUnknown(node.ref.id)).filter((id) => !!id)),
            ];
            if (!missing.length) {
                return of(known);
            }
            return this.getCollections(missing, mdsId).pipe(
                catchError(() => of([] as Node[])),
                tap((found) => {
                    found.forEach(remember);
                    missing.filter((id) => !known.has(id)).forEach((id) => unreachable.add(id));
                }),
                switchMap(() => lookUpMissing()),
            );
        };
        nodes.forEach(remember);
        return lookUpMissing();
    }

    /** Whether the top level of `scope` holds collections; one that cannot be checked counts. */
    hasCollections(scope: CollectionTreeScope): Observable<boolean> {
        return this.getChildren(null, { scope, skipCount: 0, maxItems: 1 }).pipe(
            map((page) => page.total > 0),
            catchError((error: unknown) => {
                markAsHandled(error);
                return of(true);
            }),
        );
    }

    /**
     * Child collections of `parent` in the order configured for it, or the top-level
     * collections of `scope` (pinned ones first, like the collections page) for `null`.
     */
    getChildren(
        parent: Node | null,
        options: { scope?: CollectionTreeScope; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
        const sort = this.nodeHelper.getSortByForCollection(parent ?? ROOT);
        const ascending = sort.direction === 'asc';
        return this.collectionService
            .getSubcollections({
                repository: HOME_REPOSITORY,
                collection: parent?.ref.id ?? ROOT,
                // the scope only selects among the top-level collections, the API requires one
                scope: options.scope ?? 'EDU_ALL',
                // the counts tell which collections can be expanded at all
                fetchCounts: true,
                skipCount: options.skipCount,
                maxItems: options.maxItems,
                sortProperties: parent
                    ? [sort.active]
                    : [
                          RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                          RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                          sort.active,
                      ],
                sortAscending: parent ? [ascending] : [false, true, ascending],
                // the order mode is needed to sort the next level
                propertyFilter: [RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE],
            })
            .pipe(
                map((entries) => ({
                    nodes: entries.collections ?? [],
                    total: entries.pagination?.total ?? entries.collections?.length ?? 0,
                })),
            );
    }

    /**
     * Collections whose name matches `term`, limited to the descendants of `rootId` if given.
     * A metadata set lacking the subtree parameter or the collections query falls back to an
     * unrestricted search in the default set.
     */
    searchCollections(
        term: string,
        options: { rootId: string | null; mdsId: string; maxItems: number },
    ): Observable<Node[]> {
        const mdsId = options.mdsId || DEFAULT;
        const unrestricted = { ...options, rootId: null as string | null, mdsId: DEFAULT };
        if (
            options.rootId &&
            this.unsupportedParameters.has(`${mdsId}|${PARENT_RECURSIVE_PARAMETER}`)
        ) {
            return this.requestSearch(term, unrestricted);
        }
        return this.requestSearch(term, { ...options, mdsId }).pipe(
            catchError((error: unknown) => {
                if (!options.rootId && mdsId === DEFAULT) {
                    return throwError(() => error);
                }
                markAsHandled(error);
                console.warn('Collection search failed, retrying unrestricted', error);
                if (options.rootId) {
                    this.rememberUnsupported(error, mdsId, PARENT_RECURSIVE_PARAMETER);
                }
                return this.requestSearch(term, unrestricted);
            }),
        );
    }

    /** Loads the path of each collection, which brings the collections above it along. */
    private getCollectionsWithPaths(ids: string[]): Observable<Node[]> {
        return from(ids).pipe(
            mergeMap(
                (id) =>
                    this.getAncestors(id).pipe(
                        map((path) =>
                            path.filter((node) =>
                                node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION),
                            ),
                        ),
                        catchError(() => of([] as Node[])),
                    ),
                PARALLEL_PATH_REQUESTS,
            ),
            toArray(),
            map((paths) => paths.flat()),
        );
    }

    /** A rejected request (400) means the metadata set lacks the parameter. */
    private rememberUnsupported(error: unknown, mdsId: string, parameter: string): void {
        if ((error as { status?: number })?.status === 400) {
            this.unsupportedParameters.add(`${mdsId}|${parameter}`);
        }
    }

    private requestSearch(
        term: string,
        options: { rootId: string | null; mdsId: string; maxItems: number },
    ): Observable<Node[]> {
        const criteria = [{ property: RestConstants.PRIMARY_SEARCH_CRITERIA, values: [term] }];
        if (options.rootId) {
            criteria.push({ property: PARENT_RECURSIVE_PARAMETER, values: [options.rootId] });
        }
        // `requestSearch` leaves the search state of the hosting search page untouched
        return this.searchService
            .requestSearch({
                repository: HOME_REPOSITORY,
                metadataset: options.mdsId,
                query: RestConstants.QUERY_NAME_COLLECTIONS,
                contentType: 'COLLECTIONS',
                propertyFilter: PROPERTY_FILTER,
                skipCount: 0,
                maxItems: options.maxItems,
                body: { criteria },
            })
            .pipe(map((result) => (result.nodes ?? []) as Node[]));
    }
}
