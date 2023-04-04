import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, startWith, switchMap, take, tap } from 'rxjs/operators';
import { MdsQueryCriteria, Node } from '../api/models';
import { SearchV1Service } from '../api/services';
import { CONTENT_TYPE_ALL, DEFAULT, HOME_REPOSITORY, PROPERTY_FILTER_ALL } from '../constants';
import { Sort } from '../models';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';
import { RawValuesDict } from './mds-label.service';
import { NodeService } from './node.service';
import { SearchService } from './search.service';

const SAVED_SEARCH = '-saved_search-';
const COUNT_UNLIMITED = 1247483647;
const CCM_TYPE_SAVED_SEARCH = 'ccm:saved_search';
const LOM_PROP_TITLE = 'cclom:title';
const CCM_PROP_SAVED_SEARCH_PARAMETERS = 'ccm:saved_search_parameters';
const PRIMARY_SEARCH_CRITERIA = 'ngsearchword';

export interface SavedSearch {
    title: string;
    repository: string;
    metadataSet: string;
    searchString: string | null;
    filters: RawValuesDict;
    node: Node;
}

@Injectable({
    providedIn: 'root',
})
export class SavedSearchesService {
    private readonly updateMySavedSearchesTrigger = new Subject<void>();
    private readonly mySavedSearches = this.getMySavedSearchesObservable();

    constructor(
        private search: SearchService,
        private node: NodeService,
        private searchV1: SearchV1Service,
    ) {
        node.nodesChanged.subscribe(() => this.updateMySavedSearchesTrigger.next());
    }

    /**
     * Saves the most recent search that was requested via `search`.
     *
     * @param replace Overrides any existing saved search with the same name. If false, a 409
     * "Conflict" response will be returned in case the name already exists.
     */
    saveCurrentSearch(name: string, { replace = false } = {}): Observable<SavedSearch> {
        const searchParams = this.search['getSearchParams']();
        return this.searchV1
            .saveSearch({
                name,
                replace,
                repository: searchParams.repository,
                metadataset: searchParams.metadataset,
                query: searchParams.query,
                body: searchParams.body.criteria,
            })
            .pipe(
                tap(() => this.updateMySavedSearchesTrigger.next()),
                switchMap(({ node }) =>
                    this.mySavedSearches.pipe(
                        take(1),
                        map(
                            (savedSearches) =>
                                savedSearches.find(({ node: n }) => n.ref.id === node.ref.id)!,
                        ),
                    ),
                ),
            );
    }

    observeMySavedSearches(): Observable<SavedSearch[]> {
        return this.mySavedSearches;
    }

    /**
     * Fetches the saved searches of the current user from the backend.
     */
    getMySavedSearches({
        startIndex,
        endIndex,
    }: {
        startIndex: number;
        endIndex: number;
    }): Observable<{
        data: Node[];
        total: number;
    }> {
        // Since results have to be filtered for `CCM_TYPE_SAVED_SEARCH`, we cannot send paginated
        // requests. So we fetch all results and simulate pagination here.
        //
        // FIXME: Can we filter for `CCM_TYPE_SAVED_SEARCH` in the backend?
        return this.observeMySavedSearches().pipe(
            take(1),
            map((savedSearches) => ({
                data: savedSearches.slice(startIndex, endIndex).map(({ node }) => node),
                total: savedSearches.length,
            })),
        );
    }

    /**
     * Fetches all saved searches that the current user has access to from the backend.
     */
    getSharedSavedSearches({
        searchString,
        startIndex,
        endIndex,
    }: // sort,
    {
        searchString?: string;
        startIndex: number;
        endIndex: number;
        // sort?: Sort;
    }): Observable<{
        data: Node[];
        total: number;
    }> {
        const criteria: MdsQueryCriteria[] = [];
        if (searchString) {
            criteria.push({ property: PRIMARY_SEARCH_CRITERIA, values: [searchString] });
        }
        return this.search
            .requestSearch({
                repository: HOME_REPOSITORY,
                query: 'saved_search',
                metadataset: DEFAULT,
                contentType: CONTENT_TYPE_ALL,
                propertyFilter: [PROPERTY_FILTER_ALL],
                skipCount: startIndex,
                maxItems: endIndex - startIndex,
                sortProperties: [LOM_PROP_TITLE],
                sortAscending: [true],
                // ...getSortParameters(sort),
                body: {
                    criteria,
                },
            })
            .pipe(map(({ nodes, pagination }) => ({ data: nodes, total: pagination.total })));
    }

    /**
     * Constructs an observable, that fetches the user's saved searches from the backend.
     *
     * The observable updates whenever `updateMySavedSearchesTrigger` fires.
     */
    private getMySavedSearchesObservable(): Observable<SavedSearch[]> {
        return this.updateMySavedSearchesTrigger.pipe(
            startWith(void 0 as void),
            switchReplay(() =>
                this.node
                    .getChildren(SAVED_SEARCH, {
                        maxItems: COUNT_UNLIMITED,
                        sortProperties: [LOM_PROP_TITLE],
                        sortAscending: [true],
                    })
                    .pipe(
                        map((entries) =>
                            entries.nodes
                                .filter((node) => node.type === CCM_TYPE_SAVED_SEARCH)
                                .map((node) => this.savedSearchNodeToSavedSearch(node)),
                        ),
                    ),
            ),
        );
    }

    private savedSearchNodeToSavedSearch(node: Node): SavedSearch {
        const properties = node.properties!;
        const criteria = JSON.parse(properties[CCM_PROP_SAVED_SEARCH_PARAMETERS][0]);
        const values = this.search['criteriaToRawValues'](criteria);
        const { [PRIMARY_SEARCH_CRITERIA]: searchValue, ...filters } = values;
        return {
            title: properties['cclom:title'][0],
            repository: properties['ccm:saved_search_repository'][0],
            metadataSet: properties['ccm:saved_search_mds'][0],
            searchString: searchValue && searchValue[0] !== '*' ? searchValue[0] : null,
            filters,
            node,
        };
    }
}

function getSortParameters(sort: Sort) {
    if (!sort?.active || !sort?.direction) {
        return {
            sortProperties: [sort.active],
            sortAscending: [sort.direction === 'asc'],
        };
    } else {
        return {};
    }
}
