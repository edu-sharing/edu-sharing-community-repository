import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, startWith, switchMap, take, tap } from 'rxjs/operators';
import { Node } from '../api/models';
import { SearchV1Service } from '../api/services';
import { switchReplay } from '../utils/switch-replay';
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
    private readonly updateSavedSearchesTrigger = new Subject<void>();
    private readonly savedSearches = this.getSavedSearches();

    constructor(
        private search: SearchService,
        private node: NodeService,
        private searchV1: SearchV1Service,
    ) {}

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
                tap(() => this.updateSavedSearchesTrigger.next()),
                switchMap(({ node }) =>
                    this.savedSearches.pipe(
                        take(1),
                        map(
                            (savedSearches) =>
                                savedSearches.find(({ node: n }) => n.ref.id === node.ref.id)!,
                        ),
                    ),
                ),
            );
    }

    observeSavedSearches(): Observable<SavedSearch[]> {
        // TODO: Shared saved searches.
        // See https://scm.edu-sharing.com/edu-sharing/community/repository/edu-sharing-community-repository/-/blob/f150529668d32155486687766a703978952a3609/Frontend/src/app/modules/search/search.component.ts#L1411-1422
        //
        // TODO: Trigger update when saved-search nodes are edited / deleted.
        return this.savedSearches;
    }

    private getSavedSearches(): Observable<SavedSearch[]> {
        return this.updateSavedSearchesTrigger.pipe(
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
