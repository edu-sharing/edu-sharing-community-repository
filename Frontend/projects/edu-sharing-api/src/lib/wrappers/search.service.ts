import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, switchMap } from 'rxjs';
import * as apiModels from '../api/models';
import { SearchV1Service } from '../api/services';
import * as rxjs from 'rxjs';

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

/** Parameters for a search request. */
export interface SearchParams {
    /** Free text search string entered by a user. */
    searchString?: string;
    /** Property filters to narrow search results. */
    filters?: Filters;
}

export type Filters = { [property: string]: string[] };

export type SearchResults = Pick<apiModels.SearchResultNode, 'nodes' | 'pagination'>;

/** A singe entry of a given facet, including its localized display name. */
export interface FacetValue {
    count: number;
    id: string;
    displayName: string;
}

/** The aggregation of all facet values of a given facet. */
export interface FacetAggregation {
    // TODO: pagination information
    values: FacetValue[];
}

/** Values of all facets by property. */
export type Facets = {
    [property: string]: FacetAggregation;
};

@Injectable({
    providedIn: 'root',
})
export class SearchService {
    private readonly resultsSubject = new BehaviorSubject<apiModels.SearchResultNode | null>(null);
    private config: SearchConfig | null = null;
    private params: SearchParams = {};

    constructor(private searchV1: SearchV1Service) {}

    /**
     * Configure `SearchService`.
     *
     * Has to be called at least once before other methods can be used. Subsequent calls replace
     * previous configurations and reset the service's state.
     */
    configure(config: Partial<SearchConfig>): void {
        this.config = { ...new SearchConfig(), ...config };
        this.resultsSubject.next(null);
    }

    /**
     * Send a search request with new parameters.
     *
     * Facets will be updated and search parameters will be kept for future calls to `getPage` and
     * `getAsYouTypeFacetSuggestions`.
     */
    search(params: SearchParams): Observable<SearchResults> {
        this.params = params;
        return this.requestSearch();
    }

    /**
     * Get another page of search results with parameters last provided to `search`.
     */
    getPage(pageIndex: number): Observable<SearchResults> {
        return this.requestSearch({ pageIndex, requestFacets: false });
    }

    getAsYouTypeFacetSuggestions(inputString: string): Observable<Facets> {
        // This is a placeholder implementation with non-final results.
        const config = this.getConfig();
        return this.searchV1
            .searchV2({
                repository: config.repository,
                metadataset: config.metadataSet,
                query: config.query,
                skipCount: 0,
                maxItems: 0,
                body: {
                    criterias: this.getSearchCriteria(this.params),
                    facettes: config.facets,
                },
            })
            .pipe(switchMap((response) => this.mapFacets(response.facettes)));
    }

    private requestSearch({
        pageIndex = 0,
        requestFacets = true,
    } = {}): Observable<apiModels.SearchResultNode> {
        const config = this.getConfig();
        throw new Error('not implemented');
    }

    private getConfig(): SearchConfig {
        if (this.config) {
            return this.config;
        } else {
            throw new Error('Missing search configuration.');
        }
    }

    private getSearchCriteria(params: SearchParams): apiModels.SearchParameters['criterias'] {
        // TODO: implementation
        return [];
    }

    private mapFacets(results: apiModels.SearchResultNode['facettes']): Observable<Facets> {
        return rxjs.of(
            results.reduce((acc, facet) => {
                acc[facet.property] = this.mapFacet(facet);
                return acc;
            }, {} as Facets),
        );
    }

    private mapFacet(facet: apiModels.Facette): FacetAggregation {
        return {
            values: facet.values.map((value) => this.mapFacetValue(value)),
        };
    }

    private mapFacetValue(value: apiModels.Value): FacetValue {
        return {
            count: value.count,
            id: value.value,
            displayName: value.value, // TODO: correct mapping
        };
    }
}
