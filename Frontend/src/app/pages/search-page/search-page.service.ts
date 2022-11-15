import { Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
    distinctUntilChanged,
    filter,
    map,
    share,
    switchMap,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { Node, Repository, RestConstants } from '../../core-module/core.module';
import {
    fromSearchResults,
    NodeDataSourceRemote,
    NodeRemote,
    NodeRequestParams,
} from '../../features/node-entries/node-data-source-remote';
import { SearchFieldService } from '../../main/navigation/search-field/search-field.service';
import { notNull } from '../../util/functions';
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
    readonly resultsDataSource = new NodeDataSourceRemote();
    readonly collectionsDataSource = new NodeDataSourceRemote();
    readonly availableRepositories = new BehaviorSubject<Repository[]>(null);
    readonly activeRepository = this.userModifiableValues.createString();
    readonly availableMetadataSets = new BehaviorSubject<MetadataSetInfo[]>(null);
    readonly activeMetadataSet = this.userModifiableValues.createString();
    readonly searchFilters = this.userModifiableValues.createDict();
    readonly searchString = this.userModifiableValues.createString();

    private destroyed = new Subject<void>();

    constructor(
        private config: ConfigService,
        private searchField: SearchFieldService,
        private mds: MdsService,
        private network: NetworkService,
        private route: ActivatedRoute,
        private search: SearchService,
        private userModifiableValues: UserModifiableValuesService,
    ) {
        this.activeRepository.registerQueryParameter('repo', this.route);
        this.activeMetadataSet.registerQueryParameter('mds', this.route);
        this.searchFilters.registerQueryParameter('filters', this.route);
        this.searchString.registerQueryParameter('q', this.route);
        this.registerRepositories();
        this.registerAvailableMetadataSets();
        this.registerActiveMetadataSet();
        this.registerSearchField();
        this.registerSearchObservables();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerRepositories() {
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

    private registerSearchField() {
        this.searchField
            .onSearchTriggered(this.destroyed)
            .subscribe(({ searchString }) => this.searchString.setUserValue(searchString || null));
        this.searchString
            .observeUserValue()
            .subscribe((searchString) => this.searchField.setSearchString(searchString));
        rxjs.combineLatest([
            this.activeRepository.observeValue().pipe(filter(notNull)),
            this.activeMetadataSet.observeValue().pipe(filter(notNull)),
        ])
            .pipe(
                takeUntil(this.destroyed),
                map(([repository, metadataSet]) => ({ repository, metadataSet })),
            )
            .subscribe((mdsInfo) => this.searchField.setMdsInfo(mdsInfo));
        this.searchField.onFilterValuesChanged(this.destroyed).subscribe((filterValues) => {
            // console.log('onFilterValuesChanged', filterValues);
            this.searchFilters.setUserValue(filterValues);
        });
        this.searchFilters.observeUserValue().subscribe((searchFilters) => {
            // console.log('searchFilters.userValue', searchFilters);
            this.searchField.setFilterValues(searchFilters);
        });
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
        // TODO: sync up the two remotes to keep the content of both until both have finished
        // loading.
        searchRequestParams
            .pipe(map((params) => this.getSearchRemote(params)))
            .subscribe((remote) => this.resultsDataSource.setRemote(remote));
        collectionRequestParams
            .pipe(map((params) => this.getCollectionsSearchRemote(params)))
            .subscribe((remote) => this.collectionsDataSource.setRemote(remote));
    }

    private getSearchRemote(params: SearchRequestParams): NodeRemote<Node> {
        // console.log('%cgetSearchRemote', 'font-weight: bold', params);
        return (request: NodeRequestParams) => {
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
                .pipe(map(fromSearchResults));
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
                .pipe(map(fromSearchResults));
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
