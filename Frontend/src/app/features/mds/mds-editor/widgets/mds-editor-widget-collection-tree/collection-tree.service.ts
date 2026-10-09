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
import { catchError, map, mergeMap, shareReplay, toArray } from 'rxjs/operators';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { CollectionTreeScope } from './collection-tree-config';

/** A page of collections together with the total number available. */
export interface CollectionPage {
    nodes: Node[];
    total: number;
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

/**
 * Marks failed requests as handled, so the application shows no error message for them: the
 * widget shows them itself, e.g. as an error row with a retry button.
 */
function shownByWidget<T>(): MonoTypeOperatorFunction<T> {
    return catchError((error: unknown) => {
        (error as { preventDefault?: () => void })?.preventDefault?.();
        return throwError(() => error);
    });
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
     * The collection itself and all collections above it, top-most first. Only the collection
     * itself carries the user's own access rights.
     */
    getAncestors(id: string): Observable<Node[]> {
        const cached = this.ancestorsCache.get(id);
        if (cached && Date.now() - cached.created < ANCESTORS_CACHE_DURATION_MS) {
            return cached.ancestors;
        }
        const ancestors = this.nodeService
            .getParents({ repository: HOME_REPOSITORY, node: id, propertyFilter: PROPERTY_FILTER })
            .pipe(
                shownByWidget(),
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
                shownByWidget(),
                map((result) => (result.nodes ?? []) as Node[]),
                catchError((error: unknown) => {
                    console.warn('Collection lookup failed, loading paths instead', error);
                    this.rememberUnsupported(error, metadataset, NODE_ID_PARAMETER);
                    return this.getCollectionsWithPaths(ids);
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
                shownByWidget(),
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
        options: { rootId: string | null; mdsId: string; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
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
                        // the path above `id` is read with other rights than the user's
                        map((path) =>
                            path
                                .filter((node) =>
                                    node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION),
                                )
                                .map((node) =>
                                    node.ref.id === id ? node : { ...node, access: undefined },
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
        options: { rootId: string | null; mdsId: string; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
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
                skipCount: options.skipCount,
                maxItems: options.maxItems,
                body: { criteria },
            })
            .pipe(
                shownByWidget(),
                map((result) => {
                    const nodes = (result.nodes ?? []) as Node[];
                    const total = result.pagination?.total ?? nodes.length;
                    return { nodes, total };
                }),
            );
    }
}
