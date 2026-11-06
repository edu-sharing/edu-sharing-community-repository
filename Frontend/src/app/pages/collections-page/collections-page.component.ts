import {
    AfterContentInit,
    afterNextRender,
    ChangeDetectorRef,
    Component,
    computed,
    ContentChild,
    effect,
    ElementRef,
    EventEmitter,
    inject,
    Injector,
    OnDestroy,
    signal,
    TemplateRef,
    viewChild,
    ViewChild,
    DestroyRef,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    ActionbarComponent,
    ColorHelper,
    LocalEventsService,
    NodeEntriesDisplayType,
    OptionsHelperDataService,
    PreferredColor,
    Scope,
    SortEvent,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { ViewToggleItem } from '../../shared/components/view-toggle-group/view-toggle-group.component';
import { ConnectedPosition } from '@angular/cdk/overlay';
import { combineLatest, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ConfigService, Mediacenter, Node } from 'ngx-edu-sharing-api';
import * as EduData from '../../core-module/core.module';
import {
    ConfigurationService,
    DialogButton,
    NodeRef,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMediacenterService,
    RestNetworkService,
    RestNodeService,
    TemporaryStorageService,
    UIService,
} from '../../core-module/core.module';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { NodeHelperService } from '../../services/node-helper.service';
import { Toast } from '../../services/toast';
import { BreadcrumbsService } from '../../shared/components/breadcrumbs/breadcrumbs.service';
import { ImprintPrivacyService } from '../../shared/components/imprint-privacy-footer/imprint-privacy-service';
import { CollectionContentComponent } from './collection-content/collection-content.component';
import { CollectionHeaderBarComponent } from './collection-header-bar/collection-header-bar.component';
import { CollectionViewStateService, CollectionViewType } from './collection-view-state.service';
import { GlobalCollectionsPageServiceInternal } from './global-collections-page.service';
import { OptionsHelperService } from 'src/app/services/options-helper.service';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { SEARCH_INPUT_QUERY_PARAM } from '../../features/topic-page/shared/types/custom-definitions';
import { CollectionScope, CollectionScopesService } from '../../services/collection-scopes.service';
import { CollectionDetailsService } from '../../features/collection-info/collection-details.service';
import {
    SearchFieldInstance,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';
import { CollectionSearchService } from './collection-search.service';
import {
    COLLECTION_SEARCH_FILTERS_PARAM,
    CollectionSearchStateService,
} from './collection-search-state.service';
import {
    parseSearchFilters,
    sameSearchFilters,
} from '../../features/topic-page/shared/utils/search-hit-util';
import { CollectionViewSlotsService } from './collection-view-slots.service';
import { CollectionTopicViewService } from './collection-topic-view/collection-topic-view.service';

// component class
@Component({
    selector: 'es-collections-page',
    templateUrl: 'collections-page.component.html',
    styleUrls: ['collections-page.component.scss'],
    // provide a new instance so to not get conflicts with other service instances
    providers: [
        OptionsHelperDataService,
        OptionsHelperService,
        CollectionDetailsService,
        CollectionViewStateService,
        CollectionSearchStateService,
        CollectionSearchService,
        CollectionViewSlotsService,
        CollectionTopicViewService,
    ],
    standalone: false,
})
export class CollectionsPageComponent implements AfterContentInit, OnDestroy {
    private breadcrumbsService = inject(BreadcrumbsService);
    readonly details = inject(CollectionDetailsService);
    readonly viewState = inject(CollectionViewStateService);
    readonly searchState = inject(CollectionSearchStateService);
    private readonly searchField = inject(SearchFieldService);
    /** the ways the user can present the content of a collection */
    readonly displayTypeItems: ViewToggleItem<CollectionViewType>[] = [
        {
            id: 'grid',
            icon: 'grid_view',
            label: 'EDITORIAL.OPTIONS.NODES_SELECTOR.COLLECTIONS.DISPLAY_TYPE.GRID',
        },
        {
            id: 'list',
            icon: 'list',
            label: 'EDITORIAL.OPTIONS.NODES_SELECTOR.COLLECTIONS.DISPLAY_TYPE.TABLE',
        },
        {
            id: 'tree',
            icon: 'account_tree',
            label: 'EDITORIAL.OPTIONS.NODES_SELECTOR.COLLECTIONS.DISPLAY_TYPE.TREE',
        },
    ];
    readonly topicView = inject(CollectionTopicViewService);
    /** the view types of a collection below the root level, which also offer its topic page */
    readonly collectionDisplayTypeItems = computed(() => [
        ...this.displayTypeItems,
        this.topicView.toggleItem(),
    ]);
    readonly breadcrumbEndSlot = inject(CollectionViewSlotsService).breadcrumbEnd;
    private collectionService = inject(RestCollectionService);
    private collectionScopesService = inject(CollectionScopesService);
    private config = inject(ConfigurationService);
    private configService = inject(ConfigService);
    private connector = inject(RestConnectorService);
    private iamService = inject(RestIamService);
    private imprintPrivacy = inject(ImprintPrivacyService);
    private destroyRef = inject(DestroyRef);
    private localEvents = inject(LocalEventsService);
    private mediacenterService = inject(RestMediacenterService);
    private nodeHelper = inject(NodeHelperService);
    private nodeService = inject(RestNodeService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private globalCollectionsPageServiceInternal = inject(GlobalCollectionsPageServiceInternal);
    private tempStorage = inject(TemporaryStorageService);
    private optionsService = inject(OptionsHelperDataService);
    private networkService = inject(RestNetworkService);
    private temporaryStorageService = inject(TemporaryStorageService);
    /** Cut/copied nodes; while not empty the bottom bar offers "paste" into the open collection. */
    readonly clipboard = this.temporaryStorageService.clipboard;
    /** Whether the clipboard-nodes overlay above the bottom bar is open. */
    clipboardOverlayOpen = false;
    editorialSidebarService = inject(EditorialSidebarService);
    private injector = inject(Injector);
    private changeDetectorRef = inject(ChangeDetectorRef);
    private toast = inject(Toast);
    private translations = inject(TranslationsService);
    private uiService = inject(UIService);

    /** How each category of collections is shown as a tab. */
    readonly SCOPE_TABS: Record<
        CollectionScope,
        { icon: string; iconContext?: string; labelClass: string }
    > = {
        MY: { icon: 'person', labelClass: 'mat-tab-my data-test-collections-tab-my' },
        EDU_GROUPS: {
            icon: 'group',
            labelClass: 'mat-tab-groups data-test-collections-tab-collaborative',
        },
        TYPE_EDITORIAL: {
            icon: 'star',
            iconContext: 'collection-scope',
            labelClass: 'mat-tab-editorial data-test-collections-tab-editorial',
        },
        TYPE_MEDIA_CENTER: {
            icon: 'domain',
            labelClass: 'mat-tab-media-center data-test-collections-tab-media-centre',
        },
        EDU_ALL: {
            icon: 'language',
            labelClass: 'mat-tab-edu-all data-test-collections-tab-public',
        },
    };
    readonly SCOPES = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;

    private readonly headerBar = viewChild(CollectionHeaderBarComponent);
    /** the actionbar in the header bar of the open collection */
    readonly headerActionbar = computed(() => this.headerBar()?.actionbar());
    @ViewChild('collectionContentComponent') collectionContentRef: CollectionContentComponent;
    readonly selectionActionbar = viewChild<ActionbarComponent>('selectionActionbarRef');
    @ContentChild('collectionContentTemplate') collectionContentTemplateRef: TemplateRef<any>;

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

    dialogTitle: string;
    dialogCancelable = false;
    dialogMessage: string;
    dialogButtons: DialogButton[];
    tabSelected: string = RestConstants.COLLECTIONSCOPE_MY;
    isLoading = true;
    isReady = false;
    private currentCollection: Node;
    get collection(): Node {
        return this.currentCollection;
    }
    set collection(collection: Node) {
        this.currentCollection = collection;
        this.details.collection.set(collection);
    }
    ngAfterContentInit(): void {
        // a host template receives the permissions of the collection, so they are requested
        // even though no sidebar asks for them
        if (this.collectionContentTemplateRef) {
            this.details.activate();
        }
    }
    collectionSortEmitter = new EventEmitter<SortEvent>();
    collectionCustomSortEmitter = new EventEmitter<boolean>();
    referenceSortEmitter = new EventEmitter<SortEvent>();
    referenceCustomSortEmitter = new EventEmitter<boolean>();
    mainnav = true;
    isGuest = true;
    addToOther: EduData.Node[];
    addPinning: string;
    tutorialElement: ElementRef;

    // FIXME: `collectionShare` is expected to be of type `Node[]` by `workspace-management` but is
    // of type `Node` here.
    private adminMediacenters: Mediacenter[];
    isRootLevel: boolean;
    set collectionShare(collectionShare: Node[]) {
        this._collectionShare = collectionShare as any as Node;
        this.refreshAll();
    }

    get collectionShare() {
        return this._collectionShare as any as Node[];
    }

    set tabSelectedIndex(pos: number) {
        this.selectTab(this.scopes()[pos]);
    }

    get tabSelectedIndex() {
        return this.scopes().findIndex((scope) => scope === this.tabSelected);
    }
    private contentDetailObject: any = null;
    // real parentCollectionId is only available, if user was browsing
    private parentCollectionId: EduData.Reference = new EduData.Reference(
        RestConstants.HOME_REPOSITORY,
        RestConstants.ROOT,
    );
    private person: EduData.User;
    /** Categories of collections listed as tabs, empty until they are known. */
    readonly scopes = signal<CollectionScope[]>([]);
    constructor() {
        // the topic view brings its own search field, the collection search takes it back after
        effect(() => {
            if (this.topicView.selected() || !this.isReady) {
                return;
            }
            afterNextRender(
                () => {
                    this.setupSearchField();
                    this.searchFieldInstance.setSearchString(this.searchState.query() ?? '');
                },
                { injector: this.injector },
            );
        });
        this.translations.waitForInit().subscribe(() => {
            combineLatest([
                this.connector.isLoggedIn(),
                // FIXME: The sub components should be obserable-aware!
                this.networkService.getRepositories(),
            ]).subscribe(
                ([data]) => {
                    if (data.isValidLogin && data.currentScope == null) {
                        this.isGuest = data.isGuest;
                        this.mediacenterService.getMediacenters().subscribe((mediacenters) => {
                            this.adminMediacenters = mediacenters.filter(
                                (m) => m.administrationAccess,
                            );
                        });
                        this.collectionScopesService.getScopes().subscribe((scopes) => {
                            this.scopes.set(scopes);
                            this.dropUnlistedScope();
                        });
                        this.initialize();
                    } else {
                        this.uiService.goToLogin();
                    }
                },
                () => this.uiService.goToLogin(),
            );
        });
        // New level0 collections (e.g. pasted on the start page) always belong to the user's own
        // collections and are only shown (as virtual nodes until indexed) in the "my" tab.
        this.localEvents.nodesCreated
            .pipe(
                takeUntil(this.destroyed),
                filter(
                    (nodes) =>
                        this.isRootLevelCollection() &&
                        this.tabSelected !== RestConstants.COLLECTIONSCOPE_MY &&
                        nodes.some((node) => this.nodeHelper.isNodeCollection(node)),
                ),
            )
            .subscribe(() => this.selectTab(RestConstants.COLLECTIONSCOPE_MY));
        // Navigate to parent collection when the current collection is deleted.
        this.localEvents.nodesDeleted
            .pipe(
                takeUntil(this.destroyed),
                filter((nodes) => nodes.some((node) => node.ref.id === this.collection.ref.id)),
            )
            .subscribe(() => this.navigate(this.parentCollectionId.id));
        // The header shows the collection, so it has to follow changes made in the sidebar.
        this.localEvents.nodesChanged
            .pipe(
                takeUntil(this.destroyed),
                filter((nodes) =>
                    nodes.some(
                        (node) =>
                            !this.isRootLevel &&
                            node?.ref?.id === this.collection?.ref?.id &&
                            !!node.collection,
                    ),
                ),
            )
            .subscribe(() => this.refreshAll());
    }
    reurl: any;
    private _collectionShare: Node;
    private params: Params;
    private destroyed = new Subject<void>();

    readonly customTemplates = this.globalCollectionsPageServiceInternal.customTemplates;
    readonly showLanding = this.globalCollectionsPageServiceInternal.showLanding.asReadonly();
    // inject services

    ngOnDestroy() {
        this.destroyed.next();
        this.destroyed.complete();
        this.tempStorage.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.collectionContentRef?.dataSourceReferences,
        );
    }

    isMobile() {
        return this.uiService.isMobile();
    }

    isMobileWidth() {
        return window.innerWidth < UIConstants.MOBILE_WIDTH;
    }

    navigate(id = '', addToOther = '', feedback = false) {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.scope = this.tabSelected;
            const query = this.searchState.query();
            if (query) {
                params[SEARCH_INPUT_QUERY_PARAM] = query;
            }
            if (!this.searchState.includeSubLevels()) {
                params.subLevels = false;
            }
            if (Object.keys(this.searchState.filters()).length) {
                params[COLLECTION_SEARCH_FILTERS_PARAM] = JSON.stringify(
                    this.searchState.filters(),
                );
            }
            if (id && id !== '-root-') {
                params.id = id;
            }
            if (feedback) {
                params.feedback = feedback;
            }
            if (addToOther) {
                params.addToOther = addToOther;
            }
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'collections'], {
                queryParams: params,
            });
        });
    }

    closeAddToOther() {
        this.navigate(this.collection.ref.id);
    }

    selectTab(tab: string) {
        if (this.tabSelected !== tab || this.getCollectionId() !== RestConstants.ROOT) {
            this.tabSelected = tab;
            this.parentCollectionId = new EduData.Reference(
                RestConstants.HOME_REPOSITORY,
                RestConstants.ROOT,
            );
            this.contentDetailObject = null;
            this.navigate();
        }
    }

    isRootLevelCollection(): boolean {
        return this.isRootLevel;
    }

    /**
     * Whether the bottom bar is shown without a selection, to offer pasting the clipboard into the
     * open collection (on the start page: as level0 collections).
     */
    showClipboardBar(): boolean {
        return !this.isGuest && !!this.clipboard()?.nodes?.length;
    }

    clearClipboard() {
        this.temporaryStorageService.remove(TemporaryStorageService.WORKSPACE_CLIPBOARD);
        this.clipboardOverlayOpen = false;
    }

    removeFromClipboard(node: Node) {
        this.temporaryStorageService.removeFromClipboard(node);
    }

    isAllowedToEditCollection(): boolean {
        if (this.isRootLevelCollection()) {
            return !this.isGuest; //this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_WRITE);
    }

    feedbackAllowed(): boolean {
        return (
            !this.isGuest &&
            RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_FEEDBACK)
        );
    }

    isUserAllowedToEdit(collection: Node): boolean {
        return RestHelper.isUserAllowedToEdit(collection, this.person);
    }

    pinCollection() {
        this.addPinning = this.collection.ref.id;
    }

    getPrivacyScope(collection: EduData.Collection): string {
        return collection.scope;
        //  return RestHelper.getPrivacyScope(collection);
    }

    navigateToSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    isBrightColor() {
        return (
            ColorHelper.getPreferredColor(this.collection?.collection?.color) ===
            PreferredColor.White
        );
    }

    getScopeInfo() {
        return this.nodeHelper.getCollectionScopeInfo(this.collection);
    }

    getScope() {
        return this.tabSelected ? this.tabSelected : RestConstants.COLLECTIONSCOPE_ALL;
    }

    onCreateCollection() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            void this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'new',
                    this.collection.ref.id,
                ],
                { queryParams: params },
            );
        });
    }

    onCollectionsClick(collection: Node): void {
        // remember actual collection as breadcrumb
        if (!this.isRootLevelCollection()) {
            this.parentCollectionId = this.collection.ref;
        }

        // set thru router so that browser back button can work
        this.navigate(collection.ref.id);
    }

    refreshAll() {
        void this.displayCollectionById(this.collection.ref.id);
    }

    async displayCollectionById(id: string) {
        if (!!id) {
            try {
                this.collection = (
                    await this.collectionService.getCollection(id).toPromise()
                ).collection;
            } catch (e) {
                if (e.status === RestConstants.HTTP_NOT_FOUND) {
                    // opened collection id does not exist -> inform user and go back to root
                    this.toast.error(null, 'COLLECTIONS.ERROR_NOT_FOUND');
                    this.navigate();
                    return;
                }
                if (e.status === RestConstants.HTTP_FORBIDDEN) {
                    const login = await this.connector.isLoggedIn().toPromise();
                    if (login.statusCode === RestConstants.STATUS_CODE_OK) {
                        this.toast.error(e);
                    } else {
                        this.uiService.goToLogin(null);
                    }
                }
            }
        } else {
            this.setCollectionId(RestConstants.ROOT);
        }
        this.imprintPrivacy.setContextNode(id ? this.collection : null, this.destroyRef);
        this.isLoading = false;
        this.renderBreadcrumbs();
        this.isRootLevel = !id;
        // the page is not checked on every change, but the collection is set from a request
        this.changeDetectorRef.markForCheck();
    }

    private searchFieldInstance: SearchFieldInstance;

    /** Shows the search field of the main navigation and applies what is searched for. */
    private setupSearchField() {
        this.searchFieldInstance = this.searchField.enable(
            {
                placeholder: 'COLLECTIONS.SEARCH',
                showFiltersButton: true,
                filterBarVisibleStorageKey: 'collections-page-filters',
            },
            this.destroyed,
        );
        this.searchFieldInstance.onSearchTriggered().subscribe((event) => {
            void this.router.navigate([], {
                relativeTo: this.route,
                queryParams: { [SEARCH_INPUT_QUERY_PARAM]: event.searchString?.trim() || null },
                queryParamsHandling: 'merge',
            });
            if (!event.cleared) {
                this.uiService.hideKeyboardIfMobile();
            }
        });
    }

    closeDialog() {
        this.dialogTitle = null;
    }

    showTabs() {
        return this.isRootLevelCollection() && this.scopes().length > 1;
    }

    /**
     * Opens the sidebar to create a collection in the current one and removes the request from
     * the url, so a reload does not open it again. The list sets the scope of the main navigation
     * while it is rendered, which closes the sidebar, so it opens after that render.
     */
    private openCreateCollectionSidebar(): void {
        afterNextRender(
            () =>
                this.editorialSidebarService.showOption({
                    option: 'CREATE_COLLECTION',
                    trap: false,
                }),
            { injector: this.injector },
        );
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { createCollection: null },
            queryParamsHandling: 'merge',
            replaceUrl: true,
        });
    }

    /**
     * Drops a scope from the URL that is not listed for the user, e.g. an editorial scope without
     * editorial collections, so the default category shows; going back does not return to it.
     */
    private dropUnlistedScope(): void {
        const scope = this.params?.scope;
        const scopes = this.scopes();
        if (
            scope &&
            this.isRootLevelCollection() &&
            scopes.length &&
            !scopes.some((listed) => listed === scope)
        ) {
            void this.router.navigate([], {
                relativeTo: this.route,
                queryParams: { scope: null },
                queryParamsHandling: 'merge',
                replaceUrl: true,
            });
        }
    }

    hasNonIconPreview(): boolean {
        const preview = this.collection?.preview;
        return preview && !preview.isIcon;
    }

    private renderBreadcrumbs() {
        this.topicView.reset();
        if (this.collection.ref.id === RestConstants.ROOT) {
            this.breadcrumbsService.setNodePath([]);
            void this.topicView.resolveCollectionViewType(this.collection, null);
            return;
        }
        // the path of the collection before stays until this one's arrives; the properties carry
        // the page configs parents propagate to their sub-collections
        this.nodeService
            .getNodeParents(this.collection.ref.id, false, [RestConstants.ALL])
            .subscribe({
                next: (data: EduData.ParentList) => {
                    const path = [...data.nodes].reverse();
                    if (path.length > 1) {
                        this.parentCollectionId = new EduData.Reference(
                            path[path.length - 2].ref.repo,
                            path[path.length - 2].ref.id,
                        );
                    }
                    this.breadcrumbsService.setNodePath(path);
                    void this.topicView.resolveCollectionViewType(this.collection, data);
                },
                error: () => this.breadcrumbsService.setNodePath([]),
            });
    }

    private initialize() {
        // load user profile
        this.iamService.getCurrentUserAsync().then(
            (iamUser) => {
                // WIN

                this.person = iamUser.person;

                // set app to ready state
                this.isReady = true;
                // subscribe to parameters of url
                this.setupSearchField();
                this.route.queryParams.subscribe(async (params) => {
                    this.topicView.applyParams(params);
                    this.searchState.query.set(params[SEARCH_INPUT_QUERY_PARAM] || null);
                    this.searchState.includeSubLevels.set(params.subLevels !== 'false');
                    const filters = parseSearchFilters(params[COLLECTION_SEARCH_FILTERS_PARAM]);
                    if (!sameSearchFilters(filters, this.searchState.filters())) {
                        this.searchState.filters.set(filters);
                    }
                    this.searchState.paramsApplied.set(true);
                    // the topic view fills the search field itself, from the same param
                    if (!this.topicView.selected()) {
                        this.searchFieldInstance?.setSearchString(
                            params[SEARCH_INPUT_QUERY_PARAM] ?? '',
                        );
                    }
                    // view type, search term and the topic view's state are applied by the views, the
                    // page stays as it is; a removed param counts as changed as well
                    const changed = Object.keys({ ...this.params, ...params }).filter(
                        (key) => this.params?.[key] !== params[key],
                    );
                    const keptKeys = [
                        SEARCH_INPUT_QUERY_PARAM,
                        'subLevels',
                        COLLECTION_SEARCH_FILTERS_PARAM,
                        'createCollection',
                        ...this.topicView.queryParamKeys,
                    ];
                    if (
                        this.params &&
                        changed.length > 0 &&
                        changed.every((key) => keptKeys.includes(key))
                    ) {
                        this.params = params;
                        return;
                    }
                    this.params = params;
                    if (params.scope) {
                        this.tabSelected = params.scope;
                    } else {
                        this.tabSelected = this.isGuest
                            ? RestConstants.COLLECTIONSCOPE_ALL
                            : RestConstants.COLLECTIONSCOPE_MY;
                    }
                    this.reurl = params.reurl;

                    if (params.mainnav) {
                        this.mainnav = params.mainnav !== 'false';
                    }

                    // get id from route and validate input data
                    const id = params.id;
                    if (params.addToOther) {
                        this.nodeService
                            .getNodeMetadata(params.addToOther)
                            .subscribe((data: EduData.NodeWrapper) => {
                                this.addToOther = [data.node];
                            });
                    }
                    await this.displayCollectionById(id);
                    this.dropUnlistedScope();
                    if (params.createCollection) {
                        this.openCreateCollectionSidebar();
                    }

                    /*if (params.nodeId) {
                        let node = params.nodeId.split('/');
                        node = node[node.length - 1];
                        this.collectionService
                            .addNodeToCollection(id, node, null)
                            .subscribe(
                                () => this.navigate(id),
                                (error: any) => {
                                    this.handleError(error);
                                    this.navigate(id);
                                    //this.displayCollectionById(id)
                                },
                            );
                    } else {*/
                    // }
                });
            },
            (error) => {
                // FAIL
                this.toast.error(error);
                this.isReady = true;
            },
        );
    }

    private setCollectionId(id: string) {
        this.collection = {} as Node;
        this.collection.ref = { id } as NodeRef;
        this.collection.aspects = [RestConstants.CCM_ASPECT_COLLECTION];
    }

    private getCollectionId() {
        const c = this.collection;
        return c != null && c.ref != null ? c.ref.id : null;
    }

    /**
     * allowed to create collections?
     */
    createAllowed = () => {
        if (this.isGuest) {
            return false;
        }
        if (this.isRootLevelCollection()) {
            let allowed = this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
            );
            if (this.tabSelected === RestConstants.COLLECTIONSCOPE_MY) {
                return allowed;
            }
            if (this.tabSelected === RestConstants.COLLECTIONSCOPE_ORGA) {
                allowed =
                    allowed &&
                    this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE);
            } else {
                // for anything else, the user must be able to invite everyone
                allowed =
                    allowed &&
                    this.connector.hasToolPermissionInstant(
                        RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                    );
                if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER) {
                    allowed = allowed && this.adminMediacenters?.length === 1;
                } else if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                    allowed =
                        allowed &&
                        this.connector.hasToolPermissionInstant(
                            RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
                        );
                }
            }
            return allowed;
        } else {
            return (
                this.connector.hasToolPermissionInstant(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                ) && this.isAllowedToEditCollection()
            );
        }
    };
}

export interface SortInfo {
    name: 'cm:name' | 'cm:modified' | 'ccm:collection_ordered_position';
    ascending: boolean;
    userModifyActive?: boolean;
}
