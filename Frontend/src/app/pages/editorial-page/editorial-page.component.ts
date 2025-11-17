import { AfterViewInit, Component, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import {
    Assignment,
    AuthenticationService,
    ConfigService,
    DEFAULT,
    HOME_REPOSITORY,
    InviteEvent,
    LoginInfo,
    MdsDefinition,
    MdsService,
    Node,
    NodeEvent,
    NodeShare,
    SearchResults,
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
    ColumnType,
    Constrain,
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
    OptionItem,
    OptionItemToggle,
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
import { debounceTime, delay, distinctUntilChanged, first, startWith, tap } from 'rxjs/operators';
import { BreakpointObserver } from '@angular/cdk/layout';
import { SelectionModel } from '@angular/cdk/collections';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { OptionsHelperService } from '../../services/options-helper.service';
import { EditorialSidebarService } from './editorial-sidebar/editorial-sidebar.service';
import { EditorialBreadcrumbService } from './editorial-breadcrumb/editorial-breadcrumb.service';

export type PrimaryMode = 'activity' | 'share' | 'assignment';
export type MainComponentType = 'manageAssignment' | 'assignmentSubmission' | 'submitAssignment';
type RouteConfig = {
    primaryMode: PrimaryMode;
};

@Component({
    selector: 'es-editorial-page',
    templateUrl: 'editorial-page.component.html',
    styleUrls: ['editorial-page.component.scss'],
    providers: [OptionsHelperService],
    standalone: false,
})
export class EditorialPageComponent implements OnInit, AfterViewInit, OnDestroy {
    readonly HOME_REPOSITORY = HOME_REPOSITORY;
    readonly PageCount = 25;
    /**
     * list of fields which are processed before being sent into the criteria list
     */
    readonly IgnoredSearchFields = ['virtual:shareMaxAge'];
    readonly TabWidgetActivities = 'virtual:activityType';
    readonly TabWidgetShares = 'virtual:shareDirection';
    readonly TabWidgetAssignment = 'virtual:assignmentType';
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
    /**
     * mds group, used to fetch the template group AND search query id!
     */
    mdsGroup = signal<string>(null);

    params$ = new BehaviorSubject<RouteConfig>(null);
    loginInfo$ = new BehaviorSubject<LoginInfo>(null);
    queryParams$ = new BehaviorSubject<Params>(null);
    tabSelection$ = new BehaviorSubject<number>(0);

    /**
     * primary component to show in the center
     */
    mainComponent$ = new BehaviorSubject<MainComponentType>(null);
    searchValues$ = new BehaviorSubject<Values>({});
    mdsLoaded$ = new BehaviorSubject(false);
    searchEvent$: Observable<SearchEvent>;
    /**
     * called when the first init was done (all fields have been parsed and initalized)
     */
    init$ = new BehaviorSubject<boolean>(false);
    /**
     * holds state if this was the first navigation to use replaceUrl for the first param init
     */
    firstNavigation$ = new BehaviorSubject<boolean>(false);
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    readonly dataSource = new NodeDataSource<Node | NodeShare | NodeEvent | Assignment>();
    columns = signal<ColumnType>(null);
    selection = signal<SelectionModel<Node | null>>(null);
    private sidebarOptionToggle: OptionItemToggle;
    private pagination$ = new BehaviorSubject<{
        skipCount: number;
        maxItems: number;
    }>(null);

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private breakpointObserver: BreakpointObserver,
        private dialogs: DialogsService,
        private mdsService: MdsService,
        private mainNav: MainNavService,
        private searchFieldService: SearchFieldService,
        private searchService: SearchService,
        public editorialSidebarService: EditorialSidebarService,
        private searchServiceUnwrapped: SearchServiceUnwrapped,
        private configService: ConfigService,
        private searchHelperService: SearchHelperService,
        private optionsHelperService: OptionsHelperService,
        private ui: UIService,
        private authenticationService: AuthenticationService,
        public editorialPageService: EditorialPageService,
        public editorialBreadcrumbService: EditorialBreadcrumbService,
    ) {
        this.isMobile$.pipe(first()).subscribe((mobile) => {
            this.editorialSidebarService.sidebarOpened.set(!mobile);
        });
        this.authenticationService
            .observeLoginInfo()
            .subscribe((loginInfo) => this.loginInfo$.next(loginInfo));
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
                first(),
            )
            .subscribe((instance) => {
                instance
                    .onFiltersButtonClicked()
                    .subscribe(() => this.sidenavLeft.set(!this.sidenavLeft()));
                this.searchEvent$ = instance.onSearchTriggered();
                this.initSubscription();
            });
    }

    ngAfterViewInit(): void {
        this.prepareOptions();
    }

    private prepareOptions() {
        this.sidebarOptionToggle = this.optionsHelperService.getOptionItemToggleSidebar(
            this.editorialSidebarService.sidebarOpened,
        );
        const reject = new OptionItem(
            'EDITORIAL.OPTION.REJECT_SHARE',
            'cancel_schedule_send',
            (element: InviteEvent[]) => {
                const elements = this.optionsHelperService.getObjects(
                    element,
                    this.nodeEntriesRef.optionsHelper.getData(),
                );
                void this.dialogs.openRejectShareDialog(elements);
            },
        );
        reject.elementType = [ElementType.Node, ElementType.SavedSearch];
        reject.constrains = [Constrain.User];
        reject.group = DefaultGroups.Delete;
        reject.showAsAction = true;
        reject.customShowCallback = async (nodes) => {
            return (
                this.params$.value.primaryMode === 'share' &&
                nodes.every(
                    (n) =>
                        (n as unknown as { share: InviteEvent }).share?.sharedBy.authorityName !==
                        this.loginInfo$.value?.authorityName,
                )
            );
        };
        void this.nodeEntriesRef?.initOptionsGenerator({
            actionbar: this.actionbarRef,
            customOptions: {
                useDefaultOptions: true,
                addOptions: [this.sidebarOptionToggle, reject],
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
            this.editorialBreadcrumbService.mode.set(p.primaryMode);
            if (p.primaryMode === 'activity') {
                this.columns.set({
                    Default: [
                        new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
                        new ListItem('EVENT', 'eventType'),
                        new ListItem('EVENT', 'timestamp'),
                    ],
                });
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
            } else if (p.primaryMode === 'share') {
                this.columns.set({
                    Default: [
                        new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
                        new ListItem('SHARE', 'sharedBy'),
                        new ListItem('SHARE', 'timestamp'),
                    ],
                });
                this.mdsDefinition$.next(
                    await firstValueFrom(
                        this.mdsService.getMetadataSet({ repository: HOME_REPOSITORY }),
                    ),
                );
                const widget = MdsHelperService.getWidget(
                    this.TabWidgetShares,
                    null,
                    this.mdsDefinition$.value.widgets,
                );
                this.mdsGroup.set('editorial_share');
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetShares,
                    );
                } else {
                    this.editorialPageService.registerTabsFromWidget(widget);
                }
            } else if (p.primaryMode === 'assignment') {
                this.columns.set({
                    Default: [
                        new ListItem('ASSIGNMENT', 'title'),
                        new ListItem('ASSIGNMENT', 'type'),
                        new ListItem('ASSIGNMENT', 'status'),
                        new ListItem('ASSIGNMENT', 'summary'),
                    ],
                });
                this.mdsDefinition$.next(
                    await firstValueFrom(
                        this.mdsService.getMetadataSet({ repository: HOME_REPOSITORY }),
                    ),
                );
                const widget = MdsHelperService.getWidget(
                    this.TabWidgetAssignment,
                    null,
                    this.mdsDefinition$.value.widgets,
                );
                const createAssigment = new OptionItem(
                    'EDITORIAL.OPTIONS.CREATE_ASSIGNMENT',
                    'task',
                    () => this.mainComponent$.next('manageAssignment'),
                );
                // @TODO: Define toolpermission for create assignment!
                createAssigment.toolpermissions = [];
                this.updateCreateOptions([createAssigment]);
                this.mdsGroup.set('editorial_assignment');
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetAssignment,
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
        combineLatest([
            this.init$.pipe(
                filter((i) => i),
                first(),
            ),
            this.searchEvent$.pipe(
                startWith({
                    searchString: this.searchFieldService.getCurrentInstance()?.getSearchString(),
                    cleared: false,
                }),
                distinctUntilChanged(),
            ),
            this.tabSelection$.pipe(distinctUntilChanged()),
            this.pagination$.pipe(distinctUntilChanged((a, b) => Helper.objectEquals(a, b))),
            this.mainComponent$.pipe(distinctUntilChanged()),
            // first one will be the init of the set
            this.searchValues$.pipe(
                distinctUntilChanged((a, b) => Helper.objectEquals(a, b)),
                tap((a) => console.log('values', a)),
            ),
        ])
            .pipe(
                filter(([init]) => init),
                distinctUntilChanged(),
                debounceTime(50),
            )
            .subscribe(([_, search, tab, pagination, mainComponent, values]) => {
                console.log('THIS MUST BE SHOWN ONCE', search, tab, pagination, values);

                const queryParams = {
                    q: search?.searchString,
                    offset: pagination?.skipCount || null,
                    size: pagination?.maxItems || null,
                    mainComponent,
                    filters: JSON.stringify({
                        ...values,
                        ...this.editorialPageService.buildSearchCriteria(tab),
                    }),
                };
                // console.log(this.editorialPageService.buildSearchCriteria(tab));
                void this.router.navigate(['./'], {
                    relativeTo: this.route,
                    replaceUrl: !this.firstNavigation$.value,
                    queryParams,
                    queryParamsHandling: 'merge',
                });
                this.firstNavigation$.next(true);
            });
    }

    private async processCurrentValues(params: Params, routeConfig: RouteConfig) {
        this.searchFieldService.getCurrentInstance().patchConfig({
            placeholder: 'EDITORIAL.SEARCH_PLACEHOLDER.' + routeConfig.primaryMode.toUpperCase(),
        });
        const mds = await firstValueFrom(this.mdsDefinition$.pipe(filter((m) => !!m)));
        const criteria = JSON.parse(params.filters || '{}') as Values;
        const originalCriteria = Helper.deepCopy(criteria);
        this.mainComponent$.next(params.mainComponent || null);
        const pagination = {
            skipCount: parseInt(params.offset) || 0,
            maxItems: parseInt(params.size) || this.PageCount,
        };
        this.pagination$.next(pagination);
        this.tabSelection$.next(this.editorialPageService.resolveTabForCriteria(criteria));
        this.searchValues$.next(criteria);
        let ngsearchword = '';
        if (params.q) {
            ngsearchword = params.q;
            this.searchFieldService.getCurrentInstance().setSearchString(params.q);
            console.log('search string', params.q);
        }

        this.IgnoredSearchFields.forEach((f) => delete criteria[f]);
        const searchCriteria = this.searchHelperService.convertCritieria(
            {
                ...criteria,
                ...this.editorialPageService.buildSearchCriteria(this.tabSelection$.value),
                ...(ngsearchword
                    ? { [RestConstants.PRIMARY_SEARCH_CRITERIA]: [ngsearchword] }
                    : {}),
            },
            mds.widgets,
            true,
        );

        // this is the first call. In this case, we wait to get a new event with the default uri parameters before loading
        await firstValueFrom(
            this.mdsLoaded$.pipe(
                filter((v) => v),
                first(),
                delay(1),
            ),
        );
        this.init$.next(true);
        if (Object.keys(params).length === 0) {
            return;
        }

        console.log('processCurrentValues', params);
        this.dataSource.isLoading = true;
        this.dataSource.reset();

        this.nodeEntriesRef?.setPaginator(pagination);
        // wait for mds and delay to make sure the facets are registered

        if (routeConfig.primaryMode === 'activity') {
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
            this.searchService
                .search({
                    type: 'recentActivity',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    ...pagination,
                    // @TODO: we might need to consider eventType instead?
                    contentType: this.editorialPageService.buildSearchCriteria(
                        this.tabSelection$.value,
                    )[this.TabWidgetActivities] as any,
                    body: {
                        facetLimit: 5,
                        facetMinCount: 1,
                        criteria: searchCriteria,
                    },
                })
                .subscribe((events) => {
                    this.dataSource.isLoading = false;
                    this.dataSource.setData(events.nodes, events.pagination);
                });
        } else if (routeConfig.primaryMode === 'share') {
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
            const maxAge = originalCriteria['virtual:shareMaxAge']?.[0];
            this.searchService
                .search({
                    type: 'shares',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    contentType: 'ALL',
                    direction: this.editorialPageService.buildSearchCriteria(
                        this.tabSelection$.value,
                    )[this.TabWidgetShares] as any,
                    maxAge: !maxAge || maxAge === 'unlimited' ? null : parseInt(maxAge),
                    ...pagination,
                    body: {
                        facetLimit: 5,
                        facetMinCount: 1,
                        criteria: searchCriteria,
                    },
                })
                .subscribe((events) => {
                    this.dataSource.isLoading = false;
                    this.dataSource.setData(events.nodes, events.pagination);
                });
        } else if (routeConfig.primaryMode === 'assignment') {
            this.searchService
                .search({
                    type: 'assignments',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    contentType: 'ALL',
                    direction: this.editorialPageService.buildSearchCriteria(
                        this.tabSelection$.value,
                    )[this.TabWidgetShares] as any,
                    ...pagination,
                    body: {
                        facetLimit: 5,
                        facetMinCount: 1,
                        criteria: searchCriteria,
                    },
                })
                .subscribe((events) => {
                    this.dataSource.isLoading = false;
                    this.dataSource.setData(events.nodes, events.pagination);
                });
        } else {
            this.searchService
                .search<SearchResults>({
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
        this.pagination$.next({
            skipCount: event.offset,
            maxItems: event.amount,
        });
    }

    updateMdsFilter(values: Values) {
        this.searchValues$.next(
            Object.fromEntries(
                Object.entries(values).filter(
                    ([_, value]) => !(Array.isArray(value) && value.length === 0),
                ),
            ),
        );
    }

    private updateCreateOptions(options: OptionItem[]) {
        options.map((o) => {
            o.elementType = [ElementType.NoneOrUnknown];
            return o;
        });
        this.mainNav.patchMainNavConfig({
            customCreateOptions: options,
        });
    }
}
