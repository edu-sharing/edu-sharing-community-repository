import { Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    ClientConfig,
    ConfigService,
    HOME_REPOSITORY,
    MdsQueryCriteria,
    MdsService,
    MetadataSetInfo,
    NetworkService,
    SearchService,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Subject } from 'rxjs';
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
import {
    ListItem,
    ListItemSort,
    Node,
    Repository,
    RestConstants,
    UIConstants,
} from '../../core-module/core.module';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { ListSortConfig } from '../../features/node-entries/entries-model';
import {
    fromSearchResults,
    NodeDataSourceRemoteService,
    NodeRemote,
    NodeRequestParams,
} from '../../features/node-entries/node-data-source-remote';
import {
    SearchFieldInstance,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { notNull } from '../../util/functions';
import { NavigationScheduler } from './navigation-scheduler';
import { UserModifiableValuesService } from './user-modifiable-values';

class SearchRequestParams {
    constructor(
        public readonly repository: string,
        public readonly metadataSet: string,
        public readonly searchFilters: { [key: string]: string[] },
        public readonly searchString: string,
    ) {}

    equals(other: SearchRequestParams) {
        return JSON.stringify(this) === JSON.stringify(other);
    }
}

@Injectable()
export class SearchPageService implements OnDestroy {
    readonly resultsDataSource = this.nodeDataSourceRemote.create();
    readonly collectionsDataSource = this.nodeDataSourceRemote.create();
    readonly availableRepositories = new BehaviorSubject<Repository[]>(null);
    readonly activeRepository = this.userModifiableValues.createString();
    readonly availableMetadataSets = new BehaviorSubject<MetadataSetInfo[]>(null);
    readonly activeMetadataSet = this.userModifiableValues.createString();
    readonly filterBarIsVisible = this.userModifiableValues.createBoolean(false);
    readonly searchFilters = this.userModifiableValues.createDict();
    readonly searchString = this.userModifiableValues.createString();
    readonly loadingProgress = new BehaviorSubject<number>(null);
    readonly resultColumns = new BehaviorSubject<ListItem[]>([]);
    readonly collectionColumns = new BehaviorSubject<ListItem[]>([]);
    readonly sortConfig = new BehaviorSubject<ListSortConfig>(null);

    private readonly destroyed = new Subject<void>();
    private readonly loadingContent = new BehaviorSubject<boolean>(false);
    private readonly loadingCollections = new BehaviorSubject<boolean>(false);

    constructor(
        private config: ConfigService,
        private searchField: SearchFieldService,
        private mds: MdsService,
        private navigationScheduler: NavigationScheduler,
        private network: NetworkService,
        private route: ActivatedRoute,
        private search: SearchService,
        private translate: TranslateService,
        private userModifiableValues: UserModifiableValuesService,
        private nodeDataSourceRemote: NodeDataSourceRemoteService,
    ) {}

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    /** Initializes the service for use on the search page. */
    init(): void {
        this.initBasicData();
        this.registerSearchField();
        this.initSearchPageUi();
        this.initQueryParams();
    }

    /**
     * Utilizes this service to provide facet search via a search field when not on the search page.
     *
     * When a search is triggered by either entering a search string and hitting the search button
     * or by selecting a facet, we jump to the search page.
     */
    initSearchFieldOnly(): void {
        this.initBasicData();
        const searchFieldInstance = this.registerSearchField();
        this.initQueryParams();
        searchFieldInstance.patchConfig({ showFiltersButton: false });
        // Jump to the search page when a search is triggered.
        rxjs.merge(
            searchFieldInstance.onSearchTriggered().pipe(
                // Don't trigger a search when the search field was cleared with the 'X' button.
                filter(({ cleared }) => !cleared),
            ),
            searchFieldInstance.onFilterValuesChanged(),
        ).subscribe(() => {
            this.navigationScheduler.scheduleNavigation({
                route: [UIConstants.ROUTER_PREFIX, 'search'],
            });
        });
    }

    private initBasicData(): void {
        this.registerRepositories();
        this.registerAvailableMetadataSets();
        this.registerActiveMetadataSet();
    }

    private initSearchPageUi(): void {
        this.registerSearchObservables();
        this.registerLoadingProgress();
        this.registerColumnsAndSortConfig();
    }

    private initQueryParams(): void {
        this.activeRepository.registerQueryParameter('repo', this.route);
        this.activeMetadataSet.registerQueryParameter('mds', this.route);
        this.searchFilters.registerQueryParameter('filters', this.route);
        this.searchString.registerQueryParameter('q', this.route);
        this.filterBarIsVisible.registerQueryParameter('filterBar', this.route, {
            replaceUrl: true,
        });
    }

    private registerRepositories(): void {
        rxjs.combineLatest([this.network.getRepositories(), this.config.observeConfig()]).subscribe(
            ([repositories, config]) =>
                this.availableRepositories.next(filterRepositories(repositories, config)),
        );
        this.availableRepositories.pipe(filter(notNull)).subscribe((availableRepositories) => {
            this.activeRepository.setSystemValue(availableRepositories[0].id);
            if (!availableRepositories.some(({ id }) => id === this.activeRepository.getValue())) {
                this.activeRepository.resetUserValue();
            }
        });
    }

    private registerAvailableMetadataSets(): void {
        this.activeRepository
            .observeValue()
            .pipe(
                filter(notNull),
                tap(() => this.availableMetadataSets.next(null)),
                switchMap((currentRepository) =>
                    rxjs.combineLatest([
                        this.mds.getAvailableMetadataSets(currentRepository),
                        this.config.observeConfig(),
                        this.availableRepositories.pipe(filter(notNull)),
                        rxjs.of(currentRepository),
                    ]),
                ),
            )
            .subscribe(([metadataSets, config, repositories, repository]) =>
                this.availableMetadataSets.next(
                    filterMetadataSets(
                        metadataSets,
                        config,
                        repositories.find(({ id }) => id === repository),
                    ),
                ),
            );
    }

    private registerActiveMetadataSet(): void {
        this.availableMetadataSets.subscribe((availableMetadataSets) => {
            if (!availableMetadataSets) {
                this.activeMetadataSet.setOverrideValue(null);
            } else {
                this.activeMetadataSet.unsetOverrideValue();
            }
            this.activeMetadataSet.setSystemValue(availableMetadataSets?.[0]?.id);
            if (
                availableMetadataSets &&
                !availableMetadataSets.some((mds) => mds.id === this.activeMetadataSet.getValue())
            ) {
                this.activeMetadataSet.resetUserValue();
            }
        });
    }

    private registerSearchField(): SearchFieldInstance {
        const searchFieldInstance = this.searchField.enable(
            {
                placeholder: 'SEARCH.SEARCH_STUFF',
                showFiltersButton: true,
                enableFiltersAndSuggestions: true,
            },
            this.destroyed,
        );
        searchFieldInstance
            .onFiltersButtonClicked()
            .subscribe(() =>
                this.filterBarIsVisible.setUserValue(!this.filterBarIsVisible.getValue()),
            );
        searchFieldInstance
            .onSearchTriggered()
            .subscribe(({ searchString }) => this.searchString.setUserValue(searchString || null));
        this.searchString
            .observeUserValue()
            .subscribe((searchString) => searchFieldInstance.setSearchString(searchString));
        rxjs.combineLatest([
            this.activeRepository.observeValue().pipe(filter(notNull)),
            this.activeMetadataSet.observeValue().pipe(filter(notNull)),
        ])
            .pipe(
                takeUntil(this.destroyed),
                map(([repository, metadataSet]) => ({ repository, metadataSet })),
            )
            .subscribe((mdsInfo) => searchFieldInstance.setMdsInfo(mdsInfo));
        searchFieldInstance.onFilterValuesChanged().subscribe((filterValues) => {
            // console.log('onFilterValuesChanged', filterValues);
            this.searchFilters.setUserValue(filterValues);
        });
        this.searchFilters.observeUserValue().subscribe((searchFilters) => {
            // console.log('searchFilters.userValue', searchFilters);
            searchFieldInstance.setFilterValues(searchFilters);
        });
        return searchFieldInstance;
    }

    private registerSearchObservables() {
        const searchRequestParams = rxjs
            .combineLatest([
                this.activeRepository.observeValue(),
                // .pipe(tap((value) => console.log('activeRepository changed', value))),
                this.activeMetadataSet.observeValue(),
                // .pipe(tap((value) => console.log('activeMetadataSet changed', value))),
                this.searchFilters.observeValue(),
                // .pipe(tap((value) => console.log('searchFilters changed', value))),
                this.searchString.observeValue(),
                // .pipe(tap((value) => console.log('searchString changed', value))),
            ])
            .pipe(
                filter(
                    ([repository, metadataSet, searchFilters]) =>
                        notNull(repository) && notNull(metadataSet) && notNull(searchFilters),
                ),
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
        const collectionRequestParams = searchRequestParams.pipe(
            // Omit searchFilters.
            map(
                ({ repository, metadataSet, searchString }) =>
                    new SearchRequestParams(repository, metadataSet, {}, searchString),
            ),
            distinctUntilChanged((x, y) => x.equals(y)),
        );
        searchRequestParams
            .pipe(map((params) => this.getSearchRemote(params)))
            .subscribe((remote) => this.resultsDataSource.setRemote(remote));
        collectionRequestParams
            .pipe(map((params) => this.getCollectionsSearchRemote(params)))
            .subscribe((remote) => this.collectionsDataSource.setRemote(remote));
    }

    private registerLoadingProgress(): void {
        rxjs.combineLatest([this.loadingContent, this.loadingCollections])
            .pipe(
                map(
                    ([loadingContent, loadingCollections]) =>
                        (loadingContent ? 0 : 0.5) + (loadingCollections ? 0 : 0.5),
                ),
                debounceTime(0),
                distinctUntilChanged(),
                // tap((progress) => console.log('progress', progress)),
                map((progress) => progress * 100),
                // map((progress) => progress >= 100 ? null : progress),
            )
            .subscribe((progress) => this.loadingProgress.next(progress));
    }

    private registerColumnsAndSortConfig(): void {
        // Get MDS definition.
        const mds = rxjs
            .combineLatest([
                this.activeRepository.observeValue().pipe(filter(notNull)),
                this.activeMetadataSet.observeValue().pipe(filter(notNull)),
            ])
            .pipe(
                switchMap(([repository, metadataSet]) =>
                    this.mds.getMetadataSet({ repository, metadataSet }),
                ),
            );
        // Register columns.
        mds.pipe(map((mds) => MdsHelper.getColumns(this.translate, mds, 'search'))).subscribe(
            this.resultColumns,
        );
        mds.pipe(
            map((mds) => MdsHelper.getColumns(this.translate, mds, 'searchCollections')),
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

    private getSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        // console.log('%cgetSearchRemote', 'font-weight: bold', params);
        return (request: NodeRequestParams) => {
            this.loadingContent.next(true);
            // console.log('search', request);
            return this.search
                .search({
                    body: {
                        criteria: this.getSearchCriteria(params),
                        facetLimit: 5,
                        facetMinCount: 1,
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

    private getCollectionsSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        const repository = this.availableRepositories.value.find(
            ({ id }) => id === params.repository,
        );
        // We cannot show collections for another repository.
        if (!repository.isHomeRepo) {
            return () => rxjs.of({ data: [], total: 0 });
        }
        return (request: NodeRequestParams) => {
            this.loadingCollections.next(true);
            return this.search
                .requestSearch({
                    body: {
                        criteria: this.getSearchCriteria(params),
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

    private getSearchCriteria(params: SearchRequestParams): MdsQueryCriteria[] {
        const criteria: MdsQueryCriteria[] = Object.entries(params.searchFilters ?? {}).map(
            ([property, values]) => ({ property, values }),
        );
        if (params.searchString) {
            criteria.push({ property: 'ngsearchword', values: [params.searchString] });
        }
        return criteria;
    }
}

function filterRepositories(repositories: Repository[], config: ClientConfig): Repository[] {
    const enabledRepositories = config.availableRepositories;
    if (enabledRepositories) {
        return repositories.filter(
            (repo) =>
                (repo.isHomeRepo && config.availableRepositories.includes(HOME_REPOSITORY)) ||
                config.availableRepositories.includes(repo.id),
        );
    } else {
        return repositories;
    }
}

function filterMetadataSets(
    metadataSets: MetadataSetInfo[],
    config: ClientConfig,
    repository: Repository,
): MetadataSetInfo[] {
    const enabledMetadataSets = config.availableMds?.find(
        (mdsConfig) =>
            mdsConfig.repository === repository.id ||
            (mdsConfig.repository === HOME_REPOSITORY && repository.isHomeRepo),
    )?.mds;
    if (enabledMetadataSets) {
        return metadataSets.filter((mds) => enabledMetadataSets.includes(mds.id));
    } else {
        return metadataSets;
    }
}
