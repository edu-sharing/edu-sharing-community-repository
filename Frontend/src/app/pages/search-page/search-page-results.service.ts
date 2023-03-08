import { Injectable, Injector, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MdsDefinition, MdsQueryCriteria, MdsService, SearchService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    share,
    switchMap,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { ListItem, ListItemSort, Node, RestConstants } from '../../core-module/core.module';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { ListSortConfig } from '../../features/node-entries/entries-model';
import {
    fromSearchResults,
    NodeDataSourceRemote,
    NodeRemote,
    NodeRequestParams,
} from '../../features/node-entries/node-data-source-remote';
import { notNull } from '../../util/functions';
import { SearchPageService, SearchRequestParams } from './search-page.service';

export interface SearchPageResults {
    totalResults?: Observable<number>;
    loadingProgress: Observable<number>;
}

@Injectable()
export class SearchPageResultsService implements SearchPageResults, OnDestroy {
    readonly resultsDataSource = new NodeDataSourceRemote(this._injector);
    readonly totalResults = this.resultsDataSource.observeTotal();
    readonly collectionsDataSource = new NodeDataSourceRemote(this._injector);
    readonly resultColumns = new BehaviorSubject<ListItem[]>([]);
    readonly collectionColumns = new BehaviorSubject<ListItem[]>([]);
    readonly sortConfig = new BehaviorSubject<ListSortConfig>(null);
    readonly loadingParams = new BehaviorSubject<boolean>(true);
    readonly loadingContent = new BehaviorSubject<boolean>(true);
    readonly loadingCollections = new BehaviorSubject<boolean>(true);
    readonly loadingProgress = new BehaviorSubject<number>(0);

    private readonly _destroyed = new Subject<void>();

    constructor(
        private _injector: Injector,
        private _search: SearchService,
        private _searchPage: SearchPageService,
        private _mds: MdsService,
        private _translate: TranslateService,
    ) {
        this._registerSearchObservables();
        this._registerColumnsAndSortConfig();
        this._registerLoadingProgress();
    }

    ngOnDestroy(): void {
        this._destroyed.next();
        this._destroyed.complete();
    }

    private _registerSearchObservables() {
        const searchRequestParams: Observable<SearchRequestParams> = rxjs
            .combineLatest([
                this._searchPage.activeRepository.observeValue(),
                // .pipe(tap((value) => console.log('activeRepository changed', value))),
                this._searchPage.activeMetadataSet.observeValue(),
                // .pipe(tap((value) => console.log('activeMetadataSet changed', value))),
                this._searchPage.searchFilters.observeValue(),
                // .pipe(tap((value) => console.log('searchFilters changed', value))),
                this._searchPage.searchString.observeValue(),
                // .pipe(tap((value) => console.log('searchString changed', value))),
            ])
            .pipe(
                takeUntil(this._destroyed),
                tap(() => this.loadingParams.next(true)),
                filter(
                    ([repository, metadataSet, searchFilters]) =>
                        notNull(repository) && notNull(metadataSet) && notNull(searchFilters),
                ),
                tap(() => this.loadingParams.next(false)),
                map(
                    ([repository, metadataSet, searchFilters, searchString]) =>
                        new SearchRequestParams(
                            repository,
                            metadataSet,
                            searchFilters,
                            searchString,
                        ),
                ),
                distinctUntilChanged((x, y) => x.equals(y)),
                share(),
            );
        const collectionRequestParams: Observable<SearchRequestParams> = searchRequestParams.pipe(
            // Omit searchFilters.
            map(
                ({ repository, metadataSet, searchString }) =>
                    new SearchRequestParams(repository, metadataSet, {}, searchString),
            ),
            distinctUntilChanged((x, y) => x.equals(y)),
        );
        searchRequestParams
            .pipe(map((params) => this._getSearchRemote(params)))
            .subscribe((remote) => this.resultsDataSource.setRemote(remote));
        collectionRequestParams
            .pipe(map((params) => this._getCollectionsSearchRemote(params)))
            .subscribe((remote) => this.collectionsDataSource.setRemote(remote));
    }

    private _registerColumnsAndSortConfig(): void {
        // Get MDS definition.
        const mds: Observable<MdsDefinition> = rxjs
            .combineLatest([
                this._searchPage.activeRepository.observeValue(),
                this._searchPage.activeMetadataSet.observeValue(),
            ])
            .pipe(
                takeUntil(this._destroyed),
                filter(([repository, metadataSet]) => notNull(repository) && notNull(metadataSet)),
                switchMap(([repository, metadataSet]) =>
                    this._mds.getMetadataSet({ repository, metadataSet }),
                ),
            );
        // Register columns.
        mds.pipe(map((mds) => MdsHelper.getColumns(this._translate, mds, 'search'))).subscribe(
            this.resultColumns,
        );
        mds.pipe(
            map((mds) => MdsHelper.getColumns(this._translate, mds, 'searchCollections')),
        ).subscribe(this.collectionColumns);
        // Register sort.
        mds.pipe(map((mds) => MdsHelper.getSortInfo(mds, 'search'))).subscribe((sortInfo) => {
            this.sortConfig.next({
                allowed: true,
                active: sortInfo.default.sortBy,
                direction: sortInfo.default.sortAscending ? 'asc' : 'desc',
                columns: sortInfo.columns.map(
                    ({ id, mode }) =>
                        new ListItemSort('NODE', id, mode as 'ascending' | 'descending'),
                ),
            });
        });
    }

    private _getSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        // console.log('%cgetSearchRemote', 'font-weight: bold', params);
        this.loadingContent.next(true);
        return (request: NodeRequestParams) => {
            this.loadingContent.next(true);
            // console.log('search', request);
            return this._search
                .search({
                    body: {
                        criteria: this._getSearchCriteria(params),
                        facetLimit: 5,
                        facetMinCount: 1,
                        permissions: this._searchPage.reUrl.value
                            ? [RestConstants.ACCESS_CC_PUBLISH]
                            : [],
                    },
                    maxItems: request.range.endIndex - request.range.startIndex,
                    skipCount: request.range.startIndex,
                    sortAscending: request.sort ? [request.sort.direction === 'asc'] : null,
                    sortProperties: request.sort ? [request.sort.active] : null,
                    contentType: 'FILES',
                    repository: params.repository,
                    metadataset: params.metadataSet,
                    query: RestConstants.DEFAULT_QUERY_NAME,
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(
                    map(fromSearchResults),
                    tap(() => this.loadingContent.next(false)),
                );
        };
    }

    private _getCollectionsSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        const repository = this._searchPage.availableRepositories.value.find(
            ({ id }) => id === params.repository,
        );
        if (
            // We cannot show collections for another repository.
            !repository.isHomeRepo ||
            // We don't show other collections when searching for material to add to a collection.
            this._searchPage.addToCollectionMode.value
        ) {
            this.loadingCollections.next(false);
            return () => rxjs.of({ data: [], total: 0 });
        }
        this.loadingCollections.next(true);
        return (request: NodeRequestParams) => {
            this.loadingCollections.next(true);
            return this._search
                .requestSearch({
                    body: {
                        criteria: this._getSearchCriteria(params),
                        facets: [],
                    },
                    sortProperties: [
                        RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                        RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                        RestConstants.CM_MODIFIED_DATE,
                    ],
                    sortAscending: [false, true, false],
                    maxItems: request.range.endIndex - request.range.startIndex,
                    skipCount: request.range.startIndex,
                    // sortAscending: request.sort ? [request.sort.direction === 'asc'] : null,
                    // sortProperties: request.sort ? [request.sort.active] : null,
                    contentType: 'ALL', // This is now handled via mds queries
                    repository: params.repository,
                    metadataset: params.metadataSet,
                    query: RestConstants.QUERY_NAME_COLLECTIONS,
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(
                    map(fromSearchResults),
                    tap(() => this.loadingCollections.next(false)),
                );
        };
    }

    private _getSearchCriteria(params: SearchRequestParams): MdsQueryCriteria[] {
        const criteria: MdsQueryCriteria[] = Object.entries(params.searchFilters ?? {}).map(
            ([property, values]) => ({ property, values }),
        );
        if (params.searchString) {
            criteria.push({ property: 'ngsearchword', values: [params.searchString] });
        }
        return criteria;
    }

    private _registerLoadingProgress(): void {
        rxjs.combineLatest([this.loadingParams, this.loadingContent, this.loadingCollections])
            .pipe(
                map(([loadingParams, loadingContent, loadingCollections]) => {
                    if (loadingParams) {
                        return 10;
                    } else {
                        return 40 + (loadingContent ? 0 : 30) + (loadingCollections ? 0 : 30);
                    }
                }),
                debounceTime(0),
                distinctUntilChanged(),
                // tap((progress) => console.log('progress', progress)),
            )
            .subscribe((progress) => this.loadingProgress.next(progress));
    }
}
