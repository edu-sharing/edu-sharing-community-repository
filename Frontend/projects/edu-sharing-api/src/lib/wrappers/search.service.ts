import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { LabeledValue, MdsIdentifier } from '../../public-api';
import * as apiModels from '../api/models';
import { SearchV1Service } from '../api/services';
import { MdsLabelService } from './mds-label.service';

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

export type SearchResults = Pick<apiModels.SearchResultNode, 'nodes' | 'pagination'>;

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

type SearchParams = Parameters<SearchV1Service['searchV2']>[0];

@Injectable({
    providedIn: 'root',
})
export class SearchService {
    private readonly resultsSubject = new BehaviorSubject<apiModels.SearchResultNode | null>(null);
    private readonly facetsSubject = new BehaviorSubject<FacetsDict>({});
    private readonly searchParamsSubject = new BehaviorSubject<SearchParams | null>(null);
    private readonly subscribedFacetsSubject = new BehaviorSubject<string[][]>([]);

    constructor(private searchV1: SearchV1Service, private mdsLabel: MdsLabelService) {
        this.registerFacetsSubject();
    }

    /**
     * Send a search request with new parameters.
     *
     * Facets will be updated and search parameters will be kept for future calls to `getPage` and
     * `getAsYouTypeFacetSuggestions`.
     */
    search(params: SearchParams): Observable<SearchResults> {
        this.searchParamsSubject.next(params);
        return this.requestSearch({
            ...params,
            body: {
                ...params.body,
                // Additionally fetch facets that have been subscribed to via `getFacets`.
                facettes: this.getSubscribedFacets(params.body.facettes),
            },
        });
    }

    /**
     * Get another page of search results with parameters last provided to `search`.
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
     */
    getFacets(properties: string[]): Observable<FacetsDict> {
        this.subscribeFacets(properties);
        return new Observable((subscriber) => {
            const destroyed$ = new Subject<void>();
            this.facetsSubject
                .pipe(takeUntil(destroyed$))
                .subscribe((value) => subscriber.next(value));
            return () => {
                this.unsubscribeFacets(properties);
                destroyed$.next();
                destroyed$.complete();
            };
        });
    }

    // /**
    //  * Like `getFacets`, but does not map translated labels.
    //  */
    // getRawFacets(properties: string[]): Observable<apiModels.Facette[]> {
    //     this.subscribeFacets(properties);
    //     return new Observable((subscriber) => {
    //         const destroyed$ = new Subject<void>();
    //         this.resultsSubject
    //             .pipe(
    //                 takeUntil(destroyed$),
    //                 filter((results): results is apiModels.SearchResultNode => results !== null),
    //             )
    //             .subscribe((results) => subscriber.next(results.facettes));
    //         return () => {
    //             this.unsubscribeFacets(properties);
    //             destroyed$.next();
    //             destroyed$.complete();
    //         };
    //     });
    // }

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

    private subscribeFacets(properties: string[]) {
        this.subscribedFacetsSubject.next([...this.subscribedFacetsSubject.value, properties]);
    }

    private unsubscribeFacets(properties: string[]) {
        const subscribedFacets = [...this.subscribedFacetsSubject.value];
        const index = subscribedFacets.indexOf(properties);
        if (index === -1) {
            throw new Error('Could not remove properties from subscribedFacets: ' + properties);
        }
        subscribedFacets.splice(index, 1);
        this.subscribedFacetsSubject.next(subscribedFacets);
    }

    private getSubscribedFacets(additionalFacets: string[] = []): string[] {
        return additionalFacets
            .concat(...this.subscribedFacetsSubject.value) // Flatten array
            .filter((value, index, array) => array.indexOf(value) === index); // Remove duplicates
    }

    private getSearchParams(): SearchParams {
        if (this.searchParamsSubject.value) {
            return this.searchParamsSubject.value;
        } else {
            throw new Error('Missing search parameters');
        }
    }

    private getMdsIdentifier(): MdsIdentifier {
        const searchParams = this.getSearchParams();
        return {
            repository: searchParams.repository,
            metadataSet: searchParams.metadataset,
        };
    }

    private requestSearch(params: SearchParams): Observable<apiModels.SearchResultNode> {
        return this.searchV1
            .searchV2(params)
            .pipe(tap((results) => this.resultsSubject.next(results)));
    }

    /**
     * Extracts property filters from the search parameters "criterias" property.
     *
     * This includes everything but the free-text search string.
     */
    private getFilterCriteria(
        criteria: apiModels.SearchParameters['criterias'],
    ): apiModels.SearchParameters['criterias'] {
        return criteria.filter((criterion) => criterion.property !== 'ngsearchword');
    }

    private mapFacets(results: apiModels.SearchResultNode['facettes']): Observable<FacetsDict> {
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

    private mapFacetValue(
        property: string,
        { count, value }: apiModels.Value,
    ): Observable<FacetValue> {
        return this.mdsLabel
            .getLabel(this.getMdsIdentifier(), property, value)
            .pipe(map((label) => ({ count, value, label })));
    }
}
