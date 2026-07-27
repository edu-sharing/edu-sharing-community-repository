import { PlatformLocation } from '@angular/common';
import {
    AfterViewInit,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    OnDestroy,
    signal,
    ViewChild,
    viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ConnectedPosition } from '@angular/cdk/overlay';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import {
    Assignment,
    AuthenticationService,
    ConfigService,
    DEFAULT,
    GenericSearchResults,
    HOME_REPOSITORY,
    InviteEvent,
    LoginInfo,
    MdsDefinition,
    MdsService,
    Node,
    NodeEvent,
    NodeShare,
    NodeSuggestion,
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
    NodeHelperService,
    OptionItem,
    Scope,
    SearchHelperService,
    ToolpermissionPipe,
    UIConstants,
    Values,
} from 'ngx-edu-sharing-ui';
import { MainNavService } from '../../main/navigation/main-nav.service';
import {
    SearchEvent,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { EditorialPageService, RECENT_ACTIVITY_EVENT_TYPES } from './editorial-page.service';
import {
    debounceTime,
    delay,
    distinctUntilChanged,
    first,
    startWith,
    takeUntil,
} from 'rxjs/operators';
import { BreakpointObserver } from '@angular/cdk/layout';
import { SelectionChange, SelectionModel } from '@angular/cdk/collections';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { OptionsHelperService } from '../../services/options-helper.service';
import { EditorialBreadcrumbService } from './editorial-breadcrumb/editorial-breadcrumb.service';
import {
    MainComponentType,
    PrimaryMode,
} from '../../features/editorial-sidebar/editorial-sidebar.component';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { ConfigurationService } from '../../core-module/core.module';
import { RestConnectorService } from '../../core-module/rest/services/rest-connector.service';
import { UIService } from '../../core-module/rest/services/ui.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { SearchFieldInternalService } from '../../main/navigation/search-field/search-field-internal.service';

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
export class EditorialPageComponent implements AfterViewInit, OnDestroy {
    private router = inject(Router);
    private route = inject(ActivatedRoute);
    private breakpointObserver = inject(BreakpointObserver);
    private dialogs = inject(DialogsService);
    private mdsService = inject(MdsService);
    private mainNav = inject(MainNavService);
    private searchFieldService = inject(SearchFieldService);
    private searchFieldInternalService = inject(SearchFieldInternalService);
    private searchService = inject(SearchService);
    editorialSidebarService = inject(EditorialSidebarService);
    private searchServiceUnwrapped = inject(SearchServiceUnwrapped);
    private configService = inject(ConfigService);
    private searchHelperService = inject(SearchHelperService);
    private optionsHelperService = inject(OptionsHelperService);
    private ui = inject(UIService);
    private connector = inject(RestConnectorService);
    private platformLocation = inject(PlatformLocation);
    private configurationService = inject(ConfigurationService);
    private authenticationService = inject(AuthenticationService);
    editorialPageService = inject(EditorialPageService);
    editorialBreadcrumbService = inject(EditorialBreadcrumbService);
    private nodeHelperService = inject(NodeHelperService);
    private toolpermissionPipe = inject(ToolpermissionPipe);
    private loadingScreenService = inject(LoadingScreenService);
    /** hide the filter toggle tab while the global loading screen covers the app */
    readonly isLoading = toSignal(this.loadingScreenService.observeIsLoading(), {
        initialValue: true,
    });

    readonly HOME_REPOSITORY = HOME_REPOSITORY;
    readonly PageCount = 25;
    /**
     * list of fields which are processed before being sent into the criteria list
     */
    readonly IgnoredSearchFields = ['virtual:shareMaxAge'];
    readonly TabWidgetActivities = 'virtual:activityType';
    readonly TabWidgetShares = 'virtual:shareDirection';
    readonly TabWidgetAssignment = 'virtual:assignmentType';
    readonly TabWidgetSuggestions = 'virtual:suggestionType';
    readonly InteractionType = InteractionType;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly Scope = Scope;
    @ViewChild(ActionbarComponent) actionbarRef: ActionbarComponent;
    /** Bottom selection bar (actions-only); node actions render here, toggles stay in the toolbar. */
    readonly selectionActionbar = viewChild<ActionbarComponent>('selectionActionbarRef');
    readonly nodeEntriesRef = viewChild<NodeEntriesWrapperComponent<NodeEntriesDataType>>(
        NodeEntriesWrapperComponent,
    );
    /** Whether the selected-nodes overlay above the selection bar is open. */
    selectionOverlayOpen = false;
    /** Open the selection overlay upward (its bottom edge aligned to the bar's top edge). */
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'start',
            originY: 'top',
            overlayX: 'start',
            overlayY: 'bottom',
            offsetY: 0,
        },
    ];
    private destroyed$ = new Subject<void>();
    isMobile$ = this.breakpointObserver
        .observe(['(max-width: 900px)'])
        .pipe(map(({ matches }) => matches));
    private isMobile = toSignal(this.isMobile$);
    /** Guards the one-time auto-open of the filter bar on desktop page load. */
    private filterBarAutoOpened = false;
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
    searchValues$ = new BehaviorSubject<Values>(null);
    mdsLoaded$ = new BehaviorSubject(false);
    searchEvent$: Observable<SearchEvent>;
    /**
     * called when the first init was done (all fields have been parsed and initialized)
     */
    init$ = new BehaviorSubject<boolean>(false);
    /**
     * holds state if this was the first navigation to use replaceUrl for the first param init
     */
    firstNavigation$ = new BehaviorSubject<boolean>(false);
    mdsDefinition$ = new BehaviorSubject<MdsDefinition>(null);
    private mdsDefinition = toSignal(this.mdsDefinition$);
    /**
     * whether the current mds group renders any filter widgets
     */
    readonly filtersAvailable = computed(() => {
        const group = this.mdsGroup();
        const mds = this.mdsDefinition();
        return !!group && !!mds && MdsHelperService.groupHasWidgets(mds, group);
    });
    readonly dataSource = new NodeDataSource<
        Node | NodeShare | NodeEvent | Assignment | NodeSuggestion
    >();
    columns = signal<ColumnType>(null);
    selection = signal<SelectionModel<NodeEntriesDataType | null>>(null);
    private pagination$ = new BehaviorSubject<{
        skipCount: number;
        maxItems: number;
    }>(null);

    readonly filtersButtonClicked = this.searchFieldInternalService.filtersButtonClicked;
    readonly filterBarVisible = this.searchFieldInternalService.filterBarVisible;

    // Always-visible tab ("Lasche") on the left edge that opens/closes the filter drawer,
    // mirroring the editorial sidebar's toggle (see es-edge-toggle in the template).
    readonly leftSidenav = viewChild('leftSidenavEl', { read: ElementRef });
    private readonly filterBarVisibleSig = toSignal(this.filterBarVisible);
    private readonly mainComponentSig = toSignal(this.mainComponent$);
    /** whether the left filter drawer is currently open (same condition as the mat-sidenav) */
    readonly filterBarOpen = computed(
        () => !!this.filterBarVisibleSig() && !this.mainComponentSig() && this.filtersAvailable(),
    );

    constructor() {
        /*this.isMobile$.pipe(first()).subscribe((mobile) => {
            this.editorialSidebarService.sidebarOpened.set(!mobile);
        });*/
        effect(() => {
            if (this.selection()?.selected.length !== 1) {
                this.editorialSidebarService.sidebarOpened.set(false);
            }
            // Inject pending virtual nodes as soon as the node-entries wrapper exists. When returning
            // from a main component the wrapper is re-created after change detection, so it may be
            // absent at the moment the search result arrives — the signal query re-fires this then.
            if (this.nodeEntriesRef()) {
                this.injectVirtualNodes();
            }
        });
        // Editorial page requires a valid, non-guest login; redirect to login otherwise.
        // Init is gated behind the check so an unauthorized session never starts the page's flow.
        this.connector.isLoggedIn().subscribe({
            next: (login) => {
                if (login.isValidLogin && !login.isGuest && !login.currentScope) {
                    this.initEditorial();
                } else if (!login.isValidLogin) {
                    // No live session -> reconnect (the login page has no session to bounce back).
                    this.ui.goToLogin();
                } else {
                    // A *valid* guest session: editorial is not available to guests, and /login
                    // would just bounce the live session straight back here. Inform via toast and
                    // leave to the configured default location instead.
                    UIHelper.goToDefaultLocation(
                        this.router,
                        this.platformLocation,
                        this.configurationService,
                    );
                }
            },
            error: () => this.ui.goToLogin(),
        });
    }

    /** Page setup; runs only once a valid, non-guest session is confirmed. */
    private initEditorial(): void {
        this.authenticationService
            .observeLoginInfo()
            .subscribe((loginInfo) => this.loginInfo$.next(loginInfo));
        this.mainNav.setMainNavConfig({
            showUser: true,
            showScope: true,
            currentScope: 'editorial',
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
                takeUntil(this.destroyed$),
                filter((i) => !!i),
                first(),
            )
            .subscribe((instance) => {
                this.searchEvent$ = instance.onSearchTriggered();
                this.initSubscription();
            });
        this.route.queryParams.subscribe(this.queryParams$);
        this.route.params.subscribe(this.params$);
        this.registerMode();
    }

    ngAfterViewInit(): void {
        this.prepareOptions();
    }

    private prepareOptions() {
        const reject = new OptionItem(
            'EDITORIAL.OPTION.REJECT_SHARE',
            'cancel_schedule_send',
            (element: InviteEvent[]) => {
                const elements = this.optionsHelperService.getObjects(
                    element,
                    this.nodeEntriesRef()!.optionsHelper.getData(),
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
        // Split actionbar (mirrors search/workspace): the toolbar actionbar shows toggle options
        // only; node actions move to the bottom selection bar.
        void this.nodeEntriesRef()?.initOptionsGenerator({
            actionbar: [this.actionbarRef, this.selectionActionbar?.()].filter(Boolean),
            customOptions: {
                useDefaultOptions: true,
                addOptions: [reject],
            },
        });
    }

    ngOnDestroy(): void {
        console.log('destroy');
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    /**
     * Informs the user that no elements can be created in the current editorial view and offers
     * a shortcut to the workspace (mirrors the workspace "create not allowed" dialog).
     */
    private async showCreateNotAllowed(): Promise<void> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'EDITORIAL.CREATE_NOT_ALLOWED.TITLE',
            message: 'EDITORIAL.CREATE_NOT_ALLOWED.MESSAGE',
            buttons: [
                {
                    label: 'SIDEBAR.WORKSPACE',
                    config: { color: 'primary', position: 'opposite' },
                },
                { label: 'CLOSE', config: { color: 'standard' } },
            ],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'SIDEBAR.WORKSPACE') {
                void this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace']);
            }
        });
    }

    private registerMode() {
        this.params$.subscribe(async (p) => {
            // we disable here, assignment is the only component and will set it later
            this.mainNav.patchMainNavConfig({
                create: {
                    allowed: 'EMIT_EVENT',
                },
                onCreateNotAllowed: () => this.showCreateNotAllowed(),
            });
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
            } else if (p.primaryMode === 'suggestions') {
                this.mainNav.patchMainNavConfig({
                    currentScope: 'editorial_suggestions',
                    title: 'EDITORIAL.TITLE_SUGGESTIONS',
                });
                this.columns.set({ Default: ListItem.getSuggestionDefaults() });
                this.mdsDefinition$.next(
                    await firstValueFrom(
                        this.mdsService.getMetadataSet({ repository: HOME_REPOSITORY }),
                    ),
                );
                const widget = MdsHelperService.getWidget(
                    this.TabWidgetSuggestions,
                    null,
                    this.mdsDefinition$.value.widgets,
                );
                this.mdsGroup.set('editorial_suggestions');
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetSuggestions,
                    );
                } else {
                    this.editorialPageService.registerTabsFromWidget(widget);
                }
            } else if (p.primaryMode === 'assignment') {
                this.mainNav.patchMainNavConfig({
                    currentScope: 'editorial_assignment',
                    title: 'EDITORIAL.TITLE_ASSIGNMENT',
                });
                this.columns.set({
                    Default: [
                        new ListItem('ASSIGNMENT', 'title'),
                        // new ListItem('ASSIGNMENT', 'type'),
                        new ListItem('ASSIGNMENT', 'status'),
                        new ListItem('ASSIGNMENT', 'endTime'),
                        new ListItem('ASSIGNMENT', 'submissionStatus'),
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
                const createAssignment = new OptionItem(
                    'EDITORIAL.OPTIONS.CREATE_ASSIGNMENT',
                    'task',
                    () => this.goToComponent('manageAssignment'),
                );
                createAssignment.group = DefaultGroups.Create;
                createAssignment.toolpermissions = [
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_ASSIGNMENTS,
                ];
                this.updateCreateOptions([createAssignment]);
                this.mdsGroup.set('editorial_assignment');
                if (widget == null) {
                    console.warn(
                        'Can not register tabs since widget definition was not found',
                        this.TabWidgetAssignment,
                    );
                } else {
                    const canCreate = await this.toolpermissionPipe.transform(
                        RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_ASSIGNMENTS,
                    );
                    // hide created tab if user can not create assignments
                    this.editorialPageService.registerTabs(
                        this.editorialPageService
                            .mapWidgetToTabs(widget)
                            .filter((t) => canCreate || t.id !== 'created'),
                    );
                }
            }
        });
    }

    private initSubscription() {
        combineLatest([
            this.queryParams$,
            this.params$,
            this.editorialPageService.observeTabs().pipe(filter((t) => t?.length > 0)),
        ])
            .pipe(takeUntil(this.destroyed$), debounceTime(10))
            .subscribe(([params, primary]) => {
                void this.processCurrentValues(params, primary);
            });

        // when primary mode change -> trigger a full reinit
        this.params$
            .pipe(
                startWith(this.params$.value),
                debounceTime(0),
                distinctUntilChanged((a, b) => a?.primaryMode === b?.primaryMode),
            )
            .subscribe(() => this.init$.next(false));
        combineLatest([
            this.init$.pipe(
                takeUntil(this.destroyed$),
                distinctUntilChanged(),
                filter((i) => i),
            ),
            this.searchEvent$.pipe(
                startWith({
                    searchString: this.queryParams$.value.q || '',
                    cleared: false,
                }),
                distinctUntilChanged(),
            ),
            this.tabSelection$.pipe(distinctUntilChanged()),
            this.pagination$.pipe(distinctUntilChanged((a, b) => Helper.objectEquals(a, b))),
            this.mainComponent$.pipe(distinctUntilChanged()),
            // first one will be the init of the set
            this.searchValues$.pipe(distinctUntilChanged((a, b) => Helper.objectEquals(a, b))),
        ])
            .pipe(
                takeUntil(this.destroyed$),
                filter(([init]) => init),
                distinctUntilChanged(),
                debounceTime(50),
            )
            .subscribe(([_, search, tab, pagination, mainComponent, values]) => {
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
        this.clearSelection();
        const instance = this.searchFieldService.getCurrentInstance();
        instance.patchConfig({
            placeholder: 'EDITORIAL.SEARCH_PLACEHOLDER.' + routeConfig.primaryMode.toUpperCase(),
            showFiltersButton: !params.mainComponent && this.filtersAvailable(),
        });
        // Auto-open the filter bar once on desktop load, but only when the group definitely has
        // filter widgets; afterwards respect the user's manual open/close state.
        if (
            !this.filterBarAutoOpened &&
            this.isMobile() === false &&
            !params.mainComponent &&
            this.filtersAvailable()
        ) {
            this.filterBarAutoOpened = true;
            instance.filterBarIsVisible.setUserValue(true);
        }
        const mds = await firstValueFrom(this.mdsDefinition$.pipe(filter((m) => !!m)));
        const criteria = JSON.parse(params.filters || '{}') as Values;
        const originalCriteria = Helper.deepCopy(criteria);
        this.mainComponent$.next(params.mainComponent || null);
        if (!this.mainComponent$.value) {
            this.editorialBreadcrumbService.path.set([]);
        }
        const pagination = {
            skipCount: parseInt(params.offset) || 0,
            maxItems: parseInt(params.size) || this.PageCount,
        };
        this.pagination$.next(pagination);
        this.tabSelection$.next(this.editorialPageService.resolveTabForCriteria(criteria));
        // deep copy since it is modified via IgnoredSearchFields!
        this.searchValues$.next(Helper.deepCopy(criteria));
        let ngsearchword = '';
        if (params.q) {
            ngsearchword = params.q;
            this.searchFieldService.getCurrentInstance().setSearchString(params.q);
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

        this.prepareOptions();
        this.dataSource.isLoading = true;
        this.dataSource.reset();
        this.clearSelection();

        this.nodeEntriesRef()?.setPaginator(pagination);
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
            const activityTabKey = this.editorialPageService.buildSearchCriteria(
                this.tabSelection$.value,
            )[this.TabWidgetActivities]?.[0];
            this.searchService
                .search({
                    searchMode: 'recentActivity',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    ...pagination,
                    contentType: 'ALL',
                    eventType: RECENT_ACTIVITY_EVENT_TYPES[activityTabKey],
                    body: {
                        facetLimit: 5,
                        facetMinCount: 1,
                        criteria: searchCriteria,
                    },
                })
                .subscribe((event) => {
                    this.dataSource.isLoading = false;
                    this.setNewData(event);
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
                    searchMode: 'shares',
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
                .subscribe((event) => {
                    this.dataSource.isLoading = false;
                    this.setNewData(event);
                });
        } else if (routeConfig.primaryMode === 'suggestions') {
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
            const tabCriteria = this.editorialPageService.buildSearchCriteria(
                this.tabSelection$.value,
            );
            this.searchService
                .search<GenericSearchResults>({
                    searchMode: 'suggestions',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    type: tabCriteria[this.TabWidgetSuggestions] as any,
                    contentType: 'ALL',
                    ...pagination,
                    body: {
                        facetLimit: 5,
                        facetMinCount: 1,
                        criteria: searchCriteria,
                    },
                })
                .subscribe((event) => {
                    this.dataSource.isLoading = false;
                    this.setNewData(event);
                });
        } else if (routeConfig.primaryMode === 'assignment') {
            this.searchService
                .search({
                    searchMode: 'assignments',
                    metadataset: DEFAULT,
                    query: null,
                    repository: HOME_REPOSITORY,
                    contentType: 'ALL',
                    sortProperties: [RestConstants.CM_PROP_C_CREATED],
                    sortAscending: [false],
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
                .subscribe((event) => {
                    this.dataSource.isLoading = false;
                    this.setNewData(event);
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
    click(event: NodeClickEvent<NodeEntriesDataType>) {
        if (this.nodeHelperService.directActionOnSingleClick(event.element as Node)) {
            this.nodeHelperService.navigateToNode(event);
            return;
        }
        const previewConfig =
            this.params$.value?.primaryMode === 'suggestions'
                ? { groupId: 'preview_sidebar_edit', editorMode: 'nodes' as const }
                : undefined;
        this.editorialSidebarService.handleSelect(
            this.nodeEntriesRef(),
            event,
            Scope.EditorialPage,
            previewConfig,
        );
    }

    selectionChange(event: SelectionChange<NodeEntriesDataType>) {
        this.selection.set(event.source);
        if (!event.source.selected.length) {
            this.selectionOverlayOpen = false;
        }
        this.editorialSidebarService.handleSelection(event);
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
            create: {
                allowed: true,
                allowBinary: false,
                globalDrop: false, // this.params$.value.primaryMode === ''
            },
            customCreateOptions: {
                useDefaultOptions: false,
                addOptions: options,
            },
        });
    }

    goToComponent(component: MainComponentType) {
        void this.router.navigate(['./'], {
            relativeTo: this.route,
            replaceUrl: !this.firstNavigation$.value,
            queryParams: {
                mainComponent: component,
            },
            queryParamsHandling: 'replace',
        });
    }

    private setNewData(event: GenericSearchResults) {
        this.clearSelection();
        this.dataSource.setData(event.nodes, event.pagination);
        this.injectVirtualNodes();
    }

    /**
     * Injects the pending virtual nodes (e.g. a just-created/updated assignment) into the list.
     * Called both after a search result and from the `nodeEntriesRef` effect, so it also fires
     * once the node-entries wrapper is (re-)created after returning from a main component — when
     * the wrapper was still undefined at the moment the search result arrived.
     *
     * The nodes are not consumed here: leaving the editor triggers the list search more than once
     * and every reload replaces the data source, so we re-inject on each `setNewData`. They live
     * for the session and are cleared on logout (see `EditorialPageService`). `addVirtualNodes`
     * dedups (updates in place once the real search returns the node), so this never duplicates.
     */
    private injectVirtualNodes() {
        const ref = this.nodeEntriesRef();
        if (!ref) {
            return;
        }
        const virtualNodes = this.editorialPageService.getVirtualNodes(
            this.params$.value.primaryMode,
            this.editorialPageService.getTabId(this.tabSelection$.value),
        );
        if (virtualNodes?.length) {
            ref.addVirtualNodes(virtualNodes);
        }
    }
    openItem(element: NodeClickEvent<NodeEntriesDataType>) {
        void this.nodeHelperService.navigateToNode(element);
    }

    /** Selected elements as a node list, for the selection bar / overlay. */
    get selectedNodes(): Node[] {
        return (this.selection()?.selected ?? []) as Node[];
    }

    clearSelection() {
        this.nodeEntriesRef()?.getSelection()?.clear();
        this.selectionOverlayOpen = false;
        this.editorialSidebarService.sidebarOpened.set(false);
    }

    deselectNode(node: Node) {
        this.nodeEntriesRef()?.getSelection()?.deselect(node);
    }
}
