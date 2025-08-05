import { Component, computed, OnDestroy, OnInit, signal } from '@angular/core';
import {
    AuthenticationService,
    ConfigService,
    DEFAULT,
    HOME_REPOSITORY,
    IamV1Service,
    MdsDefinition,
    MdsService,
    NodeServiceUnwrapped,
    SearchService,
    SearchServiceUnwrapped,
} from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest, filter, firstValueFrom, Observable, Subject } from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    Helper,
    InteractionType,
    ListItem,
    MdsHelperService,
    MdsViewerService,
    NodeDataSource,
    NodeEntriesDisplayType,
    Scope,
    SearchHelperService,
    UIService,
    Values,
} from 'ngx-edu-sharing-ui';
import { MainNavService } from '../../main/navigation/main-nav.service';
import {
    SearchEvent,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EditorialPageService } from './editorial-page.service';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import {
    debounce,
    debounceTime,
    distinctUntilChanged,
    first,
    skip,
    startWith,
    tap,
} from 'rxjs/operators';

export type PrimaryMode = 'activity';
type RouteConfig = {
    primaryMode: PrimaryMode;
};

@Component({
    selector: 'es-editorial-page',
    templateUrl: 'editorial-page.component.html',
    styleUrls: ['editorial-page.component.scss'],
    standalone: false,
})
export class EditorialPageComponent implements OnInit, OnDestroy {
    readonly HOME_REPOSITORY = HOME_REPOSITORY;
    readonly PageCount = 25;
    readonly TabWidgetActivities = 'virtual:activityType';
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Scope = Scope;
    private destroyed$ = new Subject<void>();
    sidenavLeft = signal(true);
    sidenavRight = signal(true);
    /**
     * mds group, used to fetch the template group AND search query id!
     */
    mdsGroup = signal<string>(null);
    params$ = new BehaviorSubject<RouteConfig>(null);
    queryParams$ = new BehaviorSubject<Params>(null);
    tabSelection$ = new BehaviorSubject<number>(0);
    searchValues$ = new BehaviorSubject<Values>({});
    searchEvent$: Observable<SearchEvent>;
    /**
     * called when the first init was done (all fields have been parsed and initalized)
     */
    init = new Subject<void>();
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    dataSource = new NodeDataSource();
    columns = signal<ListItem[]>(null);
    displayType = signal(NodeEntriesDisplayType.Table);

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private mdsService: MdsService,
        private mainNav: MainNavService,
        private searchFieldService: SearchFieldService,
        private searchService: SearchService,
        private searchServiceUnwrapped: SearchServiceUnwrapped,
        private configService: ConfigService,
        private searchHelperService: SearchHelperService,
        private ui: UIService,
        private authenticationService: AuthenticationService,
        public editorialPageService: EditorialPageService,
    ) {
        this.mainNav.setMainNavConfig({
            showUser: true,
            showScope: true,
            currentScope: 'EDITORIAL',
            title: 'EDITORIAL.TITLE',
            show: true,
            hideSearchField: false,
            create: {
                allowed: true,
                allowBinary: true,
            },
            showNavigation: true,
        });
        this.searchFieldService.enable(
            {
                showFiltersButton: true,
                enableFiltersAndSuggestions: false,
            },
            this.destroyed$,
        );
        this.searchFieldService
            .observeCurrentInstance()
            .pipe(
                takeUntilDestroyed(),
                filter((i) => !!i),
            )
            .subscribe((instance) => {
                instance
                    .onFiltersButtonClicked()
                    .subscribe(() => this.sidenavLeft.set(!this.sidenavLeft()));
                this.searchEvent$ = instance.onSearchTriggered();
                this.initSubscription();
            });
    }

    async ngOnInit(): Promise<void> {
        this.route.queryParams.subscribe(this.queryParams$);
        this.route.params.subscribe(this.params$);
        this.registerMode();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private registerMode() {
        this.params$.subscribe(async (p) => {
            console.log(p);
            if (p.primaryMode === 'activity') {
                this.columns.set([
                    new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
                    new ListItem('EVENT', 'eventType'),
                    new ListItem('EVENT', 'timestamp'),
                ]);
                this.mdsDefinition$.next(
                    await firstValueFrom(
                        this.mdsService.getMetadataSet({ repository: HOME_REPOSITORY }),
                    ),
                );
                const widget = MdsHelperService.getWidget(
                    this.TabWidgetActivities,
                    null,
                    this.mdsDefinition$.value.widgets,
                );
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetActivities,
                    );
                } else {
                    this.mdsGroup.set('editorial_activity');
                    this.editorialPageService.registerTabsFromWidget(widget);
                }
            }
        });
    }

    private initSubscription() {
        combineLatest([
            this.queryParams$.pipe(startWith(this.queryParams$.value)),
            this.params$.pipe(startWith(this.params$.value)),
        ]).subscribe(([params, primary]) => {
            void this.processCurrentValues(params, primary);
        });
        this.init.pipe(first()).subscribe(() => {
            combineLatest([
                this.searchEvent$.pipe(
                    startWith({
                        searchString: this.searchFieldService
                            .getCurrentInstance()
                            ?.getSearchString(),
                        cleared: false,
                    }),
                    distinctUntilChanged(),
                ),
                this.tabSelection$.pipe(distinctUntilChanged()),
                // first one will be the init of the set
                this.searchValues$.pipe(
                    skip(1),
                    distinctUntilChanged((a, b) => Helper.objectEquals(a, b)),
                    tap((a) => console.log('values', a)),
                ),
            ])
                .pipe(distinctUntilChanged(), debounceTime(50))
                .subscribe(([search, tab, values]) => {
                    console.log('THIS MUST BE SHOWN ONCE', search, tab, values);
                    const queryParams = {
                        q: search?.searchString,
                        filters: JSON.stringify({
                            ...values,
                            ...this.editorialPageService.buildSearchCriteria(tab),
                        }),
                    };
                    // console.log(this.editorialPageService.buildSearchCriteria(tab));
                    void this.router.navigate(['./'], {
                        relativeTo: this.route,
                        replaceUrl: false,
                        queryParams,
                    });
                });
        });
    }

    private async processCurrentValues(params: Params, routeConfig: RouteConfig) {
        const mds = await firstValueFrom(this.mdsDefinition$.pipe(filter((m) => !!m)));
        const criteria = JSON.parse(params.filters || '{}');
        this.tabSelection$.next(this.editorialPageService.resolveTabForCriteria(criteria));
        this.searchValues$.next(criteria);
        let ngsearchword = '';
        if (params.q) {
            ngsearchword = params.q;
            this.searchFieldService.getCurrentInstance().setSearchString(params.q);
            console.log('search string', params.q);
        }
        const searchCriteria = this.searchHelperService.convertCritieria(
            {
                ...criteria,
                ...(ngsearchword
                    ? { [RestConstants.PRIMARY_SEARCH_CRITERIA]: [ngsearchword] }
                    : {}),
            },
            mds.widgets,
            true,
        );
        this.init.next();
        if (routeConfig.primaryMode === 'activity') {
            this.searchServiceUnwrapped
                .getRecentUserEvents({
                    repository: HOME_REPOSITORY,
                    contentType: 'ALL',
                    maxItems: this.PageCount,
                })
                .subscribe((events) => {
                    console.log(events);
                    this.dataSource.setData(
                        events.nodes.map((e) => {
                            return {
                                ...e.node,
                                event: {
                                    eventType: e.eventType,
                                    initiator: e.initiator,
                                    timestamp: e.timestamp,
                                },
                            };
                        }),
                        events.pagination,
                    );
                });
        } else {
            this.searchService
                .search({
                    repository: HOME_REPOSITORY,
                    metadataset: DEFAULT,
                    query: this.mdsGroup(),
                    body: {
                        criteria: searchCriteria,
                        resolveCollections: false,
                        resolveUsernames: true,
                    },
                })
                .subscribe((result) => {
                    this.dataSource.setData(result.nodes, result.pagination);
                });
        }
    }
}
