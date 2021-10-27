import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { LabeledValue, MdsIdentifier, SearchResults } from '../../public-api';
import * as apiModels from '../api/models';
import { SearchV1Service } from '../api/services';
import { LabeledValuesDict, MdsLabelService, RawValuesDict } from './mds-label.service';

/** Configuration for `SearchService`. */
export class SearchConfig {
    /** Repository to provide to search API. */
    repository = '-home-';
    /** Metadata set to provide to search API. */
    metadataSet = '-default-';
    /** Query to provide to search API. */
    query = 'ngsearch';
    /** Number of results per page. */
    pageSize = 10;
    /** Properties for which to request facet values. */
    facets: string[] = [];
}

/** A singe entry of a given facet, including its localized display name. */
export interface FacetValue extends LabeledValue {
    count: number;
}

/** The aggregation of all facet values of a given facet. */
export interface FacetAggregation {
    hasMore: boolean;
    values: FacetValue[];
}

/** Values of all facets by property. */
export type FacetsDict = {
    [property: string]: FacetAggregation;
};

/** Parameters to be provided to `search`. */
export type SearchRequestParams = Parameters<SearchV1Service['searchV2']>[0];

@Injectable({
    providedIn: 'root',
})
export class SearchService {
    private readonly resultsSubject = new BehaviorSubject<SearchResults | null>(null);
    private readonly facetsSubject = new BehaviorSubject<FacetsDict>({});
    private readonly searchParamsSubject = new BehaviorSubject<SearchRequestParams | null>(null);
    private readonly subscribedFacetsSubject = new BehaviorSubject<string[][]>([]);

    constructor(private searchV1: SearchV1Service, private mdsLabel: MdsLabelService) {
        this.registerFacetsSubject();
    }

    /**
     * Sends a search request with new parameters.
     *
     * Facets will be updated and search parameters will be kept for future calls to `getPage` and
     * `getAsYouTypeFacetSuggestions`.
     */
    search(params: SearchRequestParams): Observable<SearchResults> {
        this.searchParamsSubject.next(params);
        return this.requestSearch({
            ...params,
            body: {
                ...params.body,
                // Additionally fetch facets that have been subscribed to via `getFacets`.
                facettes: this.getSubscribedFacets(params.body.facettes),
            },
        }).pipe(tap((results) => this.resultsSubject.next(results)));
    }

    /**
     * Sends a plain search request without updating any global state.
     */
    requestSearch(params: SearchRequestParams): Observable<SearchResults> {
        return this.searchV1.searchV2(params);
    }

    /**
     * Gets another page of search results with parameters last provided to `search`.
     */
    getPage(pageIndex: number): Observable<SearchResults> {
        const searchParams = this.getSearchParams();
        return this.requestSearch({
            ...searchParams,
            skipCount: (searchParams.maxItems ?? 10) * pageIndex,
            body: {
                ...searchParams.body,
                // Do not fetch facets again. These don't change for equal search criteria.
                facettes: [],
            },
        });
    }

    /**
     * Gets facets from search requests.
     *
     * The given properties will be registered to be fetched with search requests while the
     * observable is being subscribed to. The observable is updated by new search requests.
     *
     * Subscribing to the observable does *not* trigger a new search request. Therefore, requested
     * properties might not be present in the provided facets until a search request has been
     * triggered.
     *
     * Facets are mapped to include translated labels taken from the respective MDS widget.
     *
     * @param includeActiveFilters - whether to always include active filters in the facet
     * list---even if the facet does not have any results (`count` is 0).
     */
    getFacets(properties: string[], { includeActiveFilters = false } = {}): Observable<FacetsDict> {
        return new Observable((subscriber) => {
            this.subscribeFacets(properties);
            const destroyed$ = new Subject<void>();
            this.facetsSubject
                .pipe(
                    takeUntil(destroyed$),
                    includeActiveFilters
                        ? switchMap((facets) => this.includeActiveFilters(facets))
                        : rxjs.identity,
                )
                .subscribe((value) => subscriber.next(value));
            return () => {
                this.unsubscribeFacets(properties);
                destroyed$.next();
                destroyed$.complete();
            };
        });
    }

    /**
     * Like `getFacets`, but gets facets for a single property.
     *
     * Might still return `null` when the requested facet was not part of the last search request
     * (see `getFacets`).
     */
    getFacet(
        property: string,
        options?: Parameters<SearchService['getFacets']>[1],
    ): Observable<FacetAggregation | null> {
        return this.getFacets([property], options).pipe(map((facets) => facets[property] ?? null));
    }

    /**
     * Loads more facets of the given property.
     *
     * Updates the observable returned by `getFacets`.
     *
     * @param size number of new items to load.
     */
    loadMoreFacets(property: string, size: number): Promise<void> {
        const searchParams = this.getSearchParams();
        const currentFacetSize = this.facetsSubject.value[property].values.length;
        return this.searchV1
            .searchV2({
                ...searchParams,
                maxItems: 0,
                body: {
                    ...searchParams.body,
                    facettes: [property],
                    facetLimit: currentFacetSize + size,
                },
            })
            .pipe(
                map((results) => results.facettes.find((facet) => facet.property === property)),
                switchMap((facet) =>
                    facet
                        ? this.mapFacet(facet)
                        : rxjs.throwError(`Did not receive requested facet for "${property}"`),
                ),
                tap((facet) =>
                    this.facetsSubject.next({ ...this.facetsSubject.value, [property]: facet }),
                ),
                map(() => {}),
            )
            .toPromise();
    }

    /**
     * Fetches facet suggestions as the user is typing.
     *
     * @param inputString Incomplete search string while the user is typing
     * @param facets Properties for which to fetch suggestions
     * @param size Number of results for each facet to fetch
     * @returns Dictionary of labeled suggestions
     */
    getAsYouTypeFacetSuggestions(
        inputString: string,
        facets: string[],
        size: number,
    ): Observable<FacetsDict> {
        const searchParams = this.getSearchParams();
        return this.searchV1
            .searchFacets({
                repository: searchParams.repository,
                metadataset: searchParams.metadataset,
                query: searchParams.query,
                body: {
                    criterias: this.getFilterCriteria(searchParams.body.criterias),
                    facettes: facets,
                    facetLimit: size,
                    facetMinCount: 1,
                    facetSuggest: inputString,
                },
            })
            .pipe(switchMap((response) => this.mapFacets(response.facettes)));
    }

    /**
     * Returns the filters provided as search params.
     *
     * Filters are mapped to include translated labels taken from the respective MDS widget.
     */
    getFilters(): Observable<LabeledValuesDict> {
        const filterCriteria = this.getFilterCriteria();
        if (filterCriteria.length === 0) {
            // If we don't have any filters yet, search params might not be available and
            // `getMdsIdentifier` would fail.
            return rxjs.of({});
        }
        const filterRawValues = this.criteriaToRawValues(filterCriteria);
        return this.mdsLabel.labelValuesDict(this.getMdsIdentifier(), filterRawValues);
    }

    /** Registers the internal `facetsSubject` to be updated with search responses. */
    private registerFacetsSubject(): void {
        this.resultsSubject
            .pipe(
                switchMap((results) => {
                    if (results?.facettes) {
                        return this.mapFacets(results.facettes);
                    } else {
                        return rxjs.of({});
                    }
                }),
            )
            .subscribe((facets) => this.facetsSubject.next(facets));
    }

    /** Adds given properties to the list of facets to be fetched with search requests. */
    private subscribeFacets(properties: string[]) {
        this.subscribedFacetsSubject.next([...this.subscribedFacetsSubject.value, properties]);
    }

    /**
     * Removes the given array from the subscribed-facets list.
     *
     * The given array has to be the exact object that has been passed to `subscribeFacets`.
     */
    private unsubscribeFacets(properties: string[]) {
        const subscribedFacets = [...this.subscribedFacetsSubject.value];
        const index = subscribedFacets.indexOf(properties);
        if (index === -1) {
            throw new Error('Could not remove properties from subscribedFacets: ' + properties);
        }
        subscribedFacets.splice(index, 1);
        this.subscribedFacetsSubject.next(subscribedFacets);
    }

    /**
     * Gets a list of all facets that should be fetched with search request right now.
     *
     * @param additionalFacets - facets that should be fetched in addition to the ones on the
     * subscribed-facets list.
     */
    private getSubscribedFacets(additionalFacets: string[] = []): string[] {
        return additionalFacets
            .concat(...this.subscribedFacetsSubject.value) // Flatten array
            .filter((value, index, array) => array.indexOf(value) === index); // Remove duplicates
    }

    /** Gets parameters of the last call to `search`. */
    private getSearchParams(): SearchRequestParams {
        if (this.searchParamsSubject.value) {
            return this.searchParamsSubject.value;
        } else {
            throw new Error('Missing search parameters');
        }
    }

    /** Gets the mds identifier taken from the last call to `search`. */
    private getMdsIdentifier(): MdsIdentifier {
        const searchParams = this.getSearchParams();
        return {
            repository: searchParams.repository,
            metadataSet: searchParams.metadataset,
        };
    }

    /**
     * Extracts property filters from the search parameters "criterias" property.
     *
     * This includes everything but the free-text search string.
     */
    private getFilterCriteria(
        criteria: apiModels.SearchParameters['criterias'] = this.searchParamsSubject.value?.body
            .criterias ?? [],
    ): apiModels.SearchParameters['criterias'] {
        return criteria.filter((criterion) => criterion.property !== 'ngsearchword');
    }

    /**
     * Adds filters as facets to the given facets dictionary.
     *
     * Filters are taken from the last call to `search` and updated with new requests.
     *
     * If a filter is not already included as facet in the facets dictionary, it is appended with a
     * `count` of 0 to the respective list.
     */
    private includeActiveFilters(facets: FacetsDict): Observable<FacetsDict> {
        return this.getFilters().pipe(
            map((activeFilters) => {
                for (const [property, filters] of Object.entries(activeFilters)) {
                    for (const filter of filters) {
                        if (
                            facets[property]?.values.every((facet) => facet.value !== filter.value)
                        ) {
                            facets = {
                                ...facets,
                                [property]: {
                                    ...facets[property],
                                    values: [...facets[property].values, { ...filter, count: 0 }],
                                },
                            };
                        }
                    }
                }
                return facets;
            }),
        );
    }

    /**
     * Maps the `facets` part of a search response to a facets dictionary with labeled facet values.
     */
    private mapFacets(results: SearchResults['facettes']): Observable<FacetsDict> {
        if (results.length === 0) {
            return rxjs.of({});
        }
        return rxjs.forkJoin(
            results.reduce((acc, facet) => {
                acc[facet.property] = this.mapFacet(facet);
                return acc;
            }, {} as { [property: string]: Observable<FacetAggregation> }),
        );
    }

    /**
     * Maps a facets list of a single property from a search response to a facet aggregation with
     * labeled facet values.
     */
    private mapFacet(facet: apiModels.Facette): Observable<FacetAggregation> {
        if (facet.values.length === 0) {
            return rxjs.of({ values: [], hasMore: false });
        }
        return rxjs
            .forkJoin(facet.values.map((value) => this.mapFacetValue(facet.property, value)))
            .pipe(
                map((values) => ({
                    values,
                    hasMore: !!facet.sumOtherDocCount && facet.sumOtherDocCount > 0,
                })),
            );
    }

    /**
     * Maps a single facet value from a search response to a labeled facet value.
     */
    private mapFacetValue(
        property: string,
        { count, value }: apiModels.Value,
    ): Observable<FacetValue> {
        return this.mdsLabel
            .getLabel(this.getMdsIdentifier(), property, value)
            .pipe(map((label) => ({ count, value, label })));
    }

    /**
     * Maps the `criteria` array of a search request to a simple dictionary of values indexed by
     * properties.
     */
    private criteriaToRawValues(criteria: apiModels.SearchParameters['criterias']): RawValuesDict {
        return criteria.reduce((acc, criterion) => {
            acc[criterion.property] = criterion.values;
            return acc;
        }, {} as RawValuesDict);
    }
}
