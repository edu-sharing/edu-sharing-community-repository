import {
    AfterViewInit,
    Component,
    effect,
    OnDestroy,
    OnInit,
    signal,
    ViewChild,
} from '@angular/core';
import {
    AuthenticationService,
    ConfigService,
    DEFAULT,
    HOME_REPOSITORY,
    MdsDefinition,
    MdsService,
    Node,
    SearchService,
    SearchServiceUnwrapped,
} from 'ngx-edu-sharing-api';
import {
    BehaviorSubject,
    combineLatest,
    filter,
    firstValueFrom,
    map,
    Observable,
    Subject,
} from 'rxjs';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    ActionbarComponent,
    DefaultGroups,
    ElementType,
    FetchEvent,
    Helper,
    InteractionType,
    ListItem,
    MdsHelperService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItemToggle,
    OptionsHelperDataService,
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
import { debounceTime, distinctUntilChanged, first, skip, startWith, tap } from 'rxjs/operators';
import { BreakpointObserver } from '@angular/cdk/layout';
import { SelectionModel } from '@angular/cdk/collections';

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
export class EditorialPageComponent implements OnInit, AfterViewInit, OnDestroy {
    readonly HOME_REPOSITORY = HOME_REPOSITORY;
    readonly PageCount = 5;
    readonly TabWidgetActivities = 'virtual:activityType';
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Scope = Scope;
    @ViewChild(ActionbarComponent) actionbarRef: ActionbarComponent;
    @ViewChild(NodeEntriesWrapperComponent) nodeEntriesRef: NodeEntriesWrapperComponent<Node>;
    private destroyed$ = new Subject<void>();
    isMobile$ = this.breakpointObserver
        .observe(['(max-width: 900px)'])
        .pipe(map(({ matches }) => matches));
    sidenavLeft = signal(true);
    sidenavRight = signal(false);
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
    selection = signal<SelectionModel<Node | null>>(null);
    private sidebarOptionToggle: OptionItemToggle;
    private pagination$ = new BehaviorSubject<FetchEvent>(null);

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private breakpointObserver: BreakpointObserver,
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
        this.isMobile$.pipe(first()).subscribe((mobile) => {
            this.sidenavRight.set(!mobile);
        });
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
        effect(() => {
            const open = this.sidenavRight();
            if (this.sidebarOptionToggle) {
                this.sidebarOptionToggle.toggleState = open;
            }
        });
    }

    ngAfterViewInit(): void {
        this.sidebarOptionToggle = new OptionItemToggle(
            {
                enabled: 'EDITORIAL.OPTION.TOGGLE_SIDEBAR',
                disabled: 'EDITORIAL.OPTION.TOGGLE_SIDEBAR',
            },
            {
                enabled: 'splitscreen_right',
                disabled: 'view_column_2',
            },
            this.sidenavRight(),
            () => this.sidenavRight.set(!this.sidenavRight()),
        );
        this.sidebarOptionToggle.elementType = [ElementType.NoneOrUnknown];
        void this.nodeEntriesRef?.initOptionsGenerator({
            actionbar: this.actionbarRef,
            customOptions: {
                useDefaultOptions: true,
                addOptions: [this.sidebarOptionToggle],
            },
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
                this.mdsGroup.set('editorial_activity');
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetActivities,
                    );
                } else {
                    this.editorialPageService.registerTabsFromWidget(widget);
                }
            }
        });
    }

    private initSubscription() {
        combineLatest([
            this.queryParams$.pipe(startWith(this.queryParams$.value)),
            this.params$.pipe(startWith(this.params$.value)),
        ])
            .pipe(debounceTime(10))
            .subscribe(([params, primary]) => {
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
                this.pagination$.pipe(distinctUntilChanged()),
                // first one will be the init of the set
                this.searchValues$.pipe(
                    skip(1),
                    distinctUntilChanged((a, b) => Helper.objectEquals(a, b)),
                    tap((a) => console.log('values', a)),
                ),
            ])
                .pipe(distinctUntilChanged(), debounceTime(50))
                .subscribe(([search, tab, pagination, values]) => {
                    console.log('THIS MUST BE SHOWN ONCE', search, tab, pagination, values);
                    const queryParams = {
                        q: search?.searchString,
                        offset: pagination?.offset || null,
                        size: pagination?.amount || null,
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
        // this is the first call. In this case, we wait to get a new event with the default uri parameters before loading
        if (Object.keys(params).length === 0) {
            return;
        }
        console.log('processCurrentValues', params);
        this.dataSource.isLoading = true;
        this.dataSource.reset();

        const pagination = {
            skipCount: params.offset || 0,
            maxItems: params.size || this.PageCount,
        };
        this.nodeEntriesRef.setPaginator(pagination);
        if (routeConfig.primaryMode === 'activity') {
            this.searchServiceUnwrapped
                .getRecentUserEvents({
                    repository: HOME_REPOSITORY,
                    contentType: 'ALL',
                    ...pagination,
                })
                .subscribe((events) => {
                    this.dataSource.isLoading = false;
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
    select(event: NodeClickEvent<NodeEntriesDataType>) {
        if (
            !(
                this.nodeEntriesRef?.getSelection()?.selected.length === 1 &&
                this.nodeEntriesRef?.getSelection()?.selected[0] === event.element
            )
        ) {
            this.nodeEntriesRef?.getSelection()?.clear();
        }
        this.nodeEntriesRef?.getSelection()?.toggle(event.element as Node);
    }

    fetchEvent(event: FetchEvent) {
        this.pagination$.next(event);
    }
}
