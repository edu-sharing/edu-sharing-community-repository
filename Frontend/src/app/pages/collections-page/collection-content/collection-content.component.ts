import {
    Component,
    ContentChild,
    effect,
    EventEmitter,
    inject,
    Injector,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    runInInjectionContext,
    signal,
    Signal,
    SimpleChanges,
    TemplateRef,
    untracked,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PlatformLocation } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import {
    AuthenticationService,
    CollectionReference,
    ConfigService,
    ConfigValues,
    MdsService,
    Node,
    NodeService,
    ProposalNode,
    ROOT,
    SessionStorageService,
    Store,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CanDrop,
    ColumnType,
    DragData,
    DropSource,
    FetchEvent,
    InteractionType,
    ListDragGropConfig,
    ListEventInterface,
    ListItem,
    ListItemSort,
    ListSortConfig,
    LocalEventsService,
    MdsHelperService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    ClickSource,
    DefaultGroups,
    NodeEntriesWrapperComponent,
    TreeConfig,
    NodesRightMode,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    UIConstants,
    VirtualNode,
} from 'ngx-edu-sharing-ui';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { firstValueFrom, forkJoin, from, Observable, of, Subject } from 'rxjs';
import { catchError, concatMap, filter, map, takeUntil, toArray } from 'rxjs/operators';
import * as EduData from '../../../core-module/core.module';
import { ConfigurationHelper, LoginResult } from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { RequestObject } from '../../../core-module/rest/request-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DELETE_OR_CANCEL } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import {
    ManagementEvent,
    ManagementEventType,
} from '../../../features/management-dialogs/management-dialogs.component';
import { LoadingScreenService } from '../../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { BridgeService } from '../../../services/bridge.service';
import { CollectionViewStateService, CollectionViewType } from '../collection-view-state.service';
import { CollectionSearchService } from '../collection-search.service';
import { CollectionSearchStateService } from '../collection-search-state.service';
import { InfobarService } from '../infobar/infobar.service';
import { SelectionChange } from '@angular/cdk/collections';
import { ConnectedPosition } from '@angular/cdk/overlay';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import { GlobalCollectionsPageService } from '../global-collections-page.service';
import {
    NodesSelectorConfig,
    TabType,
} from '../../editorial-page/nodes-selector/nodes-selector.component';

/** Client-side property holding the title of the collection a search hit is placed in. */
const SEARCH_PARENT_TITLE_PROPERTY = 'virtual:es_search_parent_title';

@Component({
    selector: 'es-collection-content',
    templateUrl: 'collection-content.component.html',
    styleUrls: ['collection-content.component.scss'],
    standalone: false,
})
export class CollectionContentComponent implements OnChanges, OnInit, OnDestroy {
    private authenticationService = inject(AuthenticationService);
    private localEventsService = inject(LocalEventsService);
    private editorialSidebarService = inject(EditorialSidebarService);
    private bridge = inject(BridgeService);
    private collectionService = inject(RestCollectionService);
    configurationService = inject(ConfigService);
    private sessionStorageService = inject(SessionStorageService);
    private dialogs = inject(DialogsService);
    private infobar = inject(InfobarService);
    /** Order of the lists when the arrangement by hand started; null for a list not arranged. */
    private collectionsOrderSnapshot: string[] | null = null;
    private referencesOrderSnapshot: string[] | null = null;
    private loadingScreen = inject(LoadingScreenService);
    private mainNavService = inject(MainNavService);
    private mdsService = inject(MdsService);
    private mdsHelperService = inject(MdsHelperService);
    private nodeHelper = inject(NodeHelperService);
    private nodeService = inject(RestNodeService);
    private nodeServiceApi = inject(NodeService);
    private optionsService = inject(OptionsHelperDataService);
    private injector = inject(Injector);
    /**
     * Options of the selection bar. The header and each list have an options service of their
     * own, none of which knows the selection of the sub-collections and materials together.
     */
    private readonly selectionOptions = runInInjectionContext(
        this.injector,
        () => new OptionsHelperDataService(),
    );
    private platformLocation = inject(PlatformLocation);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private toast = inject(Toast);
    private translation = inject(TranslateService);
    private uiService = inject(UIService);
    private globalCollectionsPageService = inject(GlobalCollectionsPageService);

    /**
     * The number of collections requested per page when the tree needs all of them. A single
     * request for everything exceeds the backend's search result window (10000 entries counted
     * from the first one) and is rejected.
     */
    private static TREE_PAGE_SIZE = 1000;

    private static DEFAULT_REQUEST = {
        sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE,
        ],
        sortAscending: [false, true, false],
    };
    readonly viewState = inject(CollectionViewStateService);
    readonly searchState = inject(CollectionSearchStateService);
    private readonly collectionSearch = inject(CollectionSearchService);
    private readonly destroyed$ = new Subject<void>();
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;

    @Input() collection: Node;
    /**
     * you can subscribe to the clickItem event in case if you want to use emitter
     * used by extensions
     */
    @Input() interactionType: InteractionType = InteractionType.DefaultActionLink;
    @Input() scope: string;
    /**
     * the actionbar that shows the actions of the collection; the options service is initialized
     * for it by this component
     */
    @Input() headerActionbar: Signal<ActionbarComponent | undefined>;
    /** The page-level selection actionbar (rendered full-width in the bottom selection bar), provided
     * as the page's `viewChild` signal so it resolves lazily/reactively. */
    @Input() selectionActionbar: Signal<ActionbarComponent | undefined>;
    @Input() isRootLevel: boolean;
    @Input() createAllowed: () => boolean;
    @Output() clickItem = new EventEmitter<NodeClickEvent<Node | CollectionReference>>();
    @ContentChild('empty') emptyRef: TemplateRef<unknown>;
    /** Toggles-only bar above the list (display type / sort). */
    @ViewChild('actionbarToggles') actionbarToggles: ActionbarComponent;
    /** Toggles-only bar of the sub-collections list (select all). */
    @ViewChild('actionbarCollectionToggles') actionbarCollectionToggles: ActionbarComponent;
    @ViewChild('listReferences') listReferences: NodeEntriesWrapperComponent<CollectionReference>;
    @ViewChild('listTree') listTree: NodeEntriesWrapperComponent<Node>;
    @ViewChild('listCollections') listCollections: ListEventInterface<Node>;

    /** Currently selected references, mirrored from the references list for the selection bar/overlay. */
    selection: Node[] = [];
    /** Original node ids to auto-select once the references (re)load (a freshly *created* node). */
    private selectAfterLoad: string[] = [];
    /** True while a sidebar-add reload is in flight, so its selection-clear does not close the sidebar. */
    private addInProgress = false;
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

    private mainNavUpdateTrigger = new Subject<void>();
    sortCollectionColumns: ListItemSort[] = [
        new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
        new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, 'ascending'),
    ];
    createSubCollectionOptionItem = new OptionItem(
        'EDITORIAL.OPTIONS.ADD_COLLECTION',
        'layers',
        () => this.onAddCollection(),
    );
    sortReferences: ListSortConfig = {
        active: null,
        direction: 'asc',
        columns: [
            // new ListItemSort('NODE', RestConstants.LOM_PROP_TITLE),
            new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
            new ListItemSort('NODE', RestConstants.CM_PROP_C_CREATED),
            new ListItemSort(
                'NODE',
                RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                'ascending',
            ),
        ],
    };
    sortCollections: ListSortConfig = {
        active: null,
        direction: 'asc',
        columns: [
            new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
            new ListItemSort('NODE', RestConstants.CM_PROP_C_CREATED),
            new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
            new ListItemSort(
                'NODE',
                RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                'ascending',
            ),
        ],
    };
    addMaterialSearchOptionItem = new OptionItem('OPTIONS.SEARCH_OBJECT', 'redo', () => {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this.collection.ref.id;
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    });
    addMaterialBinaryOptionItem = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () => {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            trap: false,
            optionConfig: {
                state: TabType.UPLOAD,
                upload: 'default',
                allowCreate: true,
                autoClose: true,
            } as NodesSelectorConfig,
        });
    });
    dataSourceCollections = new NodeDataSource<Node>();
    dataSourceReferences = new NodeDataSource<CollectionReference>();
    /** the collection as the root of the tree, followed by its sub-collections and materials */
    readonly dataSourceTree = new NodeDataSource<Node>();
    readonly treeConfig: TreeConfig = {
        label: 'COLLECTIONS.TITLE',
        // the hits of a search are shown with the collections that lead to them
        expandAll: false,
        isMuted: (node) => this.searchAncestorIds.has(node.ref.id),
        showFiles: true,
        multipleSelection: true,
        // a selection opens the sidebar or the material, which a drag must not do
        selectOnDrag: false,
        // the stand-in for the root level is no node the options or the sidebar could work on
        hasNoOptions: (node) => node.ref.id === ROOT,
    };
    /**
     * Whether the tree view may be rendered. The tree reads its data once when it is created, so
     * it is created only after the data has been put into `dataSourceTree`.
     */
    readonly treeReady = signal(false);
    private shownViewType = this.viewState.viewType();
    private shownQuery = this.searchState.query();
    private shownIncludeSubLevels = this.searchState.includeSubLevels();
    /** The collections that lead to the hits of the search, which are no hits themselves. */
    private searchAncestors: Node[] = [];
    private searchAncestorIds = new Set<string>();
    /** The collection each hit or ancestor of the search is placed in, by id. */
    private searchParentIds = new Map<string, string>();
    /** Whether more collections match the search than are listed. */
    readonly searchTruncated = signal(false);
    collectionsColumns: ColumnType;
    /** columns of the search hits in the list view, which tell where each hit is placed */
    collectionsSearchTableColumns: ColumnType;
    /** columns of the sub-collections in the list view, which cannot render the card columns */
    readonly collectionsTableColumns: ColumnType = {
        Default: [new ListItem('NODE', 'cm:title'), new ListItem('NODE', 'cm:modified')],
    };
    referencesColumns: ColumnType;
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed$ });

    private contentNode: Node;
    login: LoginResult;
    config: ConfigValues;

    constructor() {
        effect(() => {
            const viewType = this.viewState.viewType();
            untracked(() => this.onViewTypeChange(viewType));
        });
        effect(() => {
            const query = this.searchState.query();
            const includeSubLevels = this.searchState.includeSubLevels();
            untracked(() => this.onQueryChange(query, includeSubLevels));
        });
        // the header bar is rendered independently of the content, so its actionbar can appear
        // after the options were set up
        effect(() => {
            if (this.headerActionbar?.()) {
                untracked(() => {
                    if (this.collection) {
                        this.setOptionsCollection();
                    }
                });
            }
        });
        this.sortCollectionColumns[this.sortCollectionColumns.length - 1].mode = 'ascending';
        // this.collectionSortEmitter.subscribe((sort: SortEvent) => this.setCollectionSort(sort));
        // this.collectionCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleCollectionsOrder() : this.changeCollectionsOrder());
        // this.referenceSortEmitter.subscribe((sort: SortEvent) => this.setReferenceSort(sort));
        // this.referenceCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleReferencesOrder() : this.changeReferencesOrder());
        this.collectionsColumns = {
            Default: ListItem.getCollectionDefaults(),
        };
        const parentColumn = new ListItem('NODE', SEARCH_PARENT_TITLE_PROPERTY);
        this.translation
            .get('COLLECTIONS.SEARCH_PARENT_COLUMN')
            .subscribe((label) => (parentColumn.label = label));
        this.collectionsSearchTableColumns = {
            Default: [
                new ListItem('NODE', 'cm:title'),
                parentColumn,
                new ListItem('NODE', 'cm:modified'),
            ],
        };
        this.mainNavService.getDialogs().eventTriggered.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                if (event.data.collection.ref.id === this.collection.ref.id) {
                    this.listReferences?.addVirtualNodes(event.data.references);
                }
            }
        });
        this.localEventsService.nodesChanged
            .pipe(
                takeUntil(this.destroyed$),
                filter((n) => n.some((n1) => n1?.ref?.id === this.collection?.ref?.id)),
            )
            .subscribe(() => this.refreshContent());
        this.editorialSidebarService.applyNodeEmitted
            .pipe(takeUntil(this.destroyed$))
            .subscribe((payload) => {
                if (
                    payload?.nodes?.length &&
                    payload.parent?.ref?.id === this.collection?.ref?.id
                ) {
                    // Node added via the sidebar: keep the sidebar open across the reload, and for
                    // a newly created node also queue it to be selected afterwards (mirrors the
                    // workspace: create → select+show-options, include → add+keep-open).
                    this.addInProgress = true;
                    this.selectAfterLoad = payload.created
                        ? payload.nodes.map((n) => n.ref.id)
                        : [];
                }
            });
        this.authenticationService
            .observeLoginInfo()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((login) => {
                this.login = login;
                this.addMaterialBinaryOptionItem.isEnabled = login.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
                );
                this.createSubCollectionOptionItem.isEnabled = login.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                );
            });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.selectionOptions.ngOnDestroy();
    }

    async ngOnInit() {
        this.registerMainNav();
        this.mainNavUpdateTrigger.next();
        const mdsSets = await ConfigurationHelper.getAvailableMds(
            RestConstants.HOME_REPOSITORY,
            this.mdsService,
            this.configurationService,
        );
        const set = await firstValueFrom(
            this.mdsService.getMetadataSet({ metadataSet: mdsSets[0].id }),
        );
        this.referencesColumns = this.mdsHelperService.getColumns(set, 'collectionReferences');

        // check: this sometimes caused missing actionbar data, why is it here?
        //this.optionsService.clearComponents(this.actionbarReferences);
        this.configurationService
            .observeConfig()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((config) => (this.config = config));
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.collection?.currentValue) {
            this.dataSourceCollections.reset();
            this.dataSourceReferences.reset();
            // Per-card options for the sub-collections list are wired (gated on the picker
            // params) in finishCollectionLoading; wiring them unconditionally here would
            // paint an empty options strip on the small cards in normal browsing.
            if (this.isRootLevel) {
                // display root collections with tabs

                this.sortCollections = {
                    ...this.sortCollections,
                    ...this.nodeHelper.getSortByForCollection(ROOT),
                };
                // To respect sort configuration of the mds, we would need to wait for it here.
                //
                // const sort = metadataSet.sorts.find(sort => sort.id === 'collections');
                // this.sortCollections.active = sort?.default?.sortBy ?? RestConstants.CM_MODIFIED_DATE;
                // this.sortCollections.direction = sort?.default?.sortAscending ? 'asc' : 'desc';
                this.refreshContent();
            } else {
                // load metadata of collection
                this.dataSourceCollections.isLoading = true;
                this.dataSourceReferences.isLoading = true;
                // set the collection and load content data by refresh
                this.sortCollections = {
                    ...this.sortCollections,
                    ...this.nodeHelper.getSortByForCollection(this.collection),
                };
                // cast old order mode to new parameter
                this.sortReferences.active = this.nodeHelper.getSortByForCollectionReferences(
                    this.collection,
                ).active;
                this.sortReferences.direction =
                    this.sortReferences.active === RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                        ? 'asc'
                        : this.nodeHelper.getSortByForCollectionReferences(this.collection)
                              .direction;
                this.mainNavUpdateTrigger.next();
                this.dataSourceCollections.isLoading = false;
                this.setOptionsCollection();
                this.refreshContent();
            }
        }
        // update the main nav buttons & availability
        this.mainNavUpdateTrigger.next();
    }

    isUserAllowedToEdit(collection: Node) {
        return RestHelper.isUserAllowedToEdit(collection);
    }

    isAllowedToEditCollection() {
        if (this.isRootLevel) {
            return !this.login?.isGuest; //this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_WRITE);
    }

    isAllowedToAddContent(): boolean {
        if (!this.isAllowedToEditCollection()) return false;
        // In public collections, adding content requires the ADD_TO_PUBLIC_COLLECTION tool
        // permission or the broader INVITE_ALLAUTHORITIES tool permission.
        // Sub-collection creation is handled separately via createAllowed() and is not affected.
        if (this.collection.isPublic) {
            return (
                this.login?.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_ADD_TO_PUBLIC_COLLECTION,
                ) ||
                this.login?.toolPermissions?.includes(
                    RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                ) ||
                false
            );
        }
        return true;
    }
    /**
     * Entry point of the "add collection" flow: the user first decides in the sidebar whether a new
     * collection is created or an existing one copied to the current location. Works on the root
     * level as well — the copy then targets the level0 collections.
     */
    onAddCollection() {
        this.editorialSidebarService.showOption({
            option: 'ADD_COLLECTION',
            trap: false,
        });
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
    canDropOnCollectionBreadcrumbs = (dropData: DragData<Node | 'HOME'>): CanDrop => {
        const target = dropData.target;
        if (target !== 'HOME' && target.ref.id === this.collection.ref.id) {
            return { accept: false };
        }
        return this.canDropInto(dropData);
    };

    /** The drop rules that hold for every target, independent of the collection being shown. */
    private canDropInto(dropData: DragData<Node | 'HOME'>): CanDrop {
        const nodes = dropData.draggedNodes;
        const target = dropData.target;
        if (target === 'HOME') {
            const accept =
                dropData.action === 'move' &&
                nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1 &&
                this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
            return {
                accept,
                denyExplicit: !accept,
            };
        }
        if (
            nodes[0].ref.id === (target as Node).ref.id ||
            (nodes[0].collection && dropData.action === 'copy')
        ) {
            return {
                accept: false,
            };
        }
        // do not allow to move anything else than editorial collections into editorial collections (if the source is a collection)
        if (nodes[0].collection?.hasOwnProperty('childCollectionsCount')) {
            if (
                (nodes[0].collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (target as Node).collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL) ||
                (nodes[0].collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (target as Node).collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL)
            ) {
                return {
                    accept: false,
                };
            }
        }
        if (
            (dropData.action === 'copy' &&
                !this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_CC_PUBLISH,
                    NodesRightMode.Effective,
                )) ||
            (dropData.action === 'move' &&
                !this.nodeHelper.getNodesRight(
                    nodes,
                    RestConstants.ACCESS_WRITE,
                    NodesRightMode.Local,
                ))
        ) {
            return {
                accept: false,
            };
        }

        return {
            accept: this.nodeHelper.getNodesRight(
                [target],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            ),
        };
    }

    canDropOnCollection = (dropData: DragData<Node>): CanDrop => {
        return this.canDropOnCollectionBreadcrumbs(dropData);
    };

    /**
     * The tree stands the root level in with a placeholder node, which stands for the top of the
     * selected scope.
     */
    private treeDropTarget(target: Node): Node | 'HOME' {
        return target.ref.id === ROOT ? 'HOME' : target;
    }

    canDropInTree = (dragData: DragData<Node>): CanDrop => {
        const target = this.treeDropTarget(dragData.target);
        const nodes = dragData.draggedNodes;
        const targetId = target === 'HOME' ? ROOT : target.ref.id;
        if (target !== 'HOME' && !this.nodeHelper.isNodeCollection(target)) {
            return { accept: false };
        }
        // already there, so dropping would change nothing
        if (nodes.every((node) => (node.parent?.id ?? ROOT) === targetId)) {
            return { accept: false };
        }
        // collections are moved, materials are put in; a mixed drag has no single meaning
        if (new Set(nodes.map((node) => this.nodeHelper.isNodeCollection(node))).size > 1) {
            return { accept: false };
        }
        return this.canDropInto({ ...dragData, target });
    };

    dropInTree = (target: Node, source: DropSource<Node>) =>
        this.dropOnCollection(this.treeDropTarget(target), source);

    /**
     * The drag configuration of the tree. `dragAllowed` is read whenever a drag could start, so it
     * follows the collection that is currently shown.
     */
    readonly treeDragDrop: ListDragGropConfig<Node> = (() => {
        const config: ListDragGropConfig<Node> = {
            dragAllowed: false,
            dropAllowed: this.canDropInTree,
            dropped: this.dropInTree,
        };
        Object.defineProperty(config, 'dragAllowed', {
            enumerable: true,
            get: () => this.isAllowedToEditCollection(),
        });
        return config;
    })();

    canDropOnRef = (dragData: DragData<CollectionReference>): CanDrop => {
        // allow dropping when only files are dragged and the view context was changed
        return {
            accept:
                dragData.draggedNodes.every((n) => n.type === 'ccm:io') &&
                !dragData.isFromOwnContainer,
        };
    };

    dropOnRef = (target: Node, source: DropSource<CollectionReference>) => {
        try {
            this.toast.showProgressSpinner();
            this.uiService.addToCollection(this.collection, source.element as Node[], false, () => {
                this.toast.closeProgressSpinner();
            });
        } catch (e) {
            console.error(e);
            this.toast.closeProgressSpinner();
        }
    };
    dropOnCollection = async (target: Node | 'HOME', source: DropSource<Node>) => {
        if (source.element[0] === target) {
            return;
        }
        if (source.element[0].mediatype === 'collection') {
            if (source.mode === 'copy') {
                this.toast.error(null, 'INVALID_OPERATION');
                return;
            }
            if (source.mode === 'move') {
                let parent: Node | 'COLLECTION_HOME';
                try {
                    parent = (
                        await this.nodeService
                            .getNodeMetadata(source.element?.[0]?.parent.id, [RestConstants.ALL])
                            .toPromise()
                    ).node;
                    if (!parent.aspects.includes(RestConstants.CCM_ASPECT_COLLECTION)) {
                        // root collection
                        parent = 'COLLECTION_HOME';
                    }
                } catch (e) {
                    console.info(e);
                }
                const dialogRef = await this.dialogs.openCopyMoveDialog(
                    parent,
                    source,
                    target === 'HOME' ? 'COLLECTION_HOME' : target,
                    ['move'],
                );
                dialogRef.afterClosed().subscribe((result) => {
                    if (result === 'WORKSPACE.COPY_MOVE.MOVE') {
                        this.toast.showProgressSpinner();
                        this.nodeService
                            .moveNode(
                                (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                                source.element[0].ref.id,
                            )
                            .subscribe(
                                () => {
                                    void this.globalCollectionsPageService.removeTemporaryCollections(
                                        source.element,
                                    );
                                    this.toast.closeProgressSpinner();
                                    this.refreshAfterMove(source.element, target);
                                },
                                (error) => {
                                    this.handleError(error);
                                    this.toast.closeProgressSpinner();
                                },
                            );
                    }
                });
            }
        } else {
            let parent: Node | 'COLLECTION_HOME';
            try {
                parent = (
                    await this.nodeService
                        .getNodeMetadata(source.element?.[0]?.parent.id, [RestConstants.ALL])
                        .toPromise()
                ).node;
            } catch (e) {
                console.info(e);
            }
            if (source.mode === 'copy' || source.mode === 'move') {
                const dialogRef = await this.dialogs.openCopyMoveDialog(
                    parent,
                    source,
                    target === 'HOME' ? 'COLLECTION_HOME' : target,
                );
                dialogRef.afterClosed().subscribe((result) => {
                    if (result === 'WORKSPACE.COPY_MOVE.COPY') {
                        this.uiService.addToCollection(
                            target as Node,
                            source.element,
                            false,
                            (nodes) => {
                                this.toast.closeProgressSpinner();
                                this.refreshContent();
                            },
                        );
                    } else if (result === 'WORKSPACE.COPY_MOVE.MOVE') {
                        forkJoin(
                            source.element.map((toMove) =>
                                this.nodeService.moveNode(
                                    (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                                    toMove.ref.id,
                                ),
                            ),
                        ).subscribe(
                            () => {
                                this.toast.closeProgressSpinner();
                                this.refreshAfterMove(source.element, target);
                            },
                            (error) => {
                                this.handleError(error);
                                this.toast.closeProgressSpinner();
                            },
                        );
                    }
                });
            } else {
                this.toast.error(null, 'INVALID_OPERATION');
            }
        }
    };
    /**
     * Reloads the lists after nodes were moved. The tree updates the branches that lost and gained
     * the nodes itself, so it is kept (and with it what the user has expanded) unless the nodes
     * went to the top of the scope, which the tree cannot reload.
     */
    private refreshAfterMove(nodes: Node[], target: Node | 'HOME') {
        if (this.viewState.viewType() === 'tree' && target !== 'HOME') {
            this.keepTreeOnRefresh = true;
            this.localEventsService.nodesMoved.emit({
                nodes,
                source: { ref: nodes[0]?.parent } as Node,
                target,
            });
        }
        this.refreshContent();
    }

    private handleError(error: any) {
        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.toast.error(null, 'COLLECTIONS.ERROR_NODE_EXISTS');
        } else {
            this.toast.error(error);
        }
    }

    async onContentClick(event: NodeClickEvent<CollectionReference | ProposalNode>): Promise<void> {
        this.contentNode = event.element;
        if (event.element.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
            this.clickElementEvent(event);
        } else if ((event.element as CollectionReference).accessEffective === null) {
            // no metadata available
            return;
        } else if ((event.element as CollectionReference).originalId == null) {
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'COLLECTIONS.ORIGINAL_MISSING',
                message: 'COLLECTIONS.ORIGINAL_MISSING_INFO',
                buttons: [
                    ...(this.isAllowedToDeleteNodes([event.element])
                        ? [{ label: 'OPTIONS.REMOVE_REF', config: { color: 'standard' as const } }]
                        : []),
                    { label: 'COLLECTIONS.OPEN_MISSING', config: { color: 'primary' } },
                ],
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'OPTIONS.REMOVE_REF') {
                    this.deleteFromCollection();
                } else if (response === 'COLLECTIONS.OPEN_MISSING') {
                    this.clickElementEvent(event);
                }
            });
        } else {
            this.clickElementEvent(event);
        }
    }

    handleSelection(selection: SelectionChange<Node>) {
        this.updateSelection();
        // While a sidebar-add reload is in flight, swallow the selection-clear so it does not
        // close the sidebar (selectPendingNodes decides the final state afterwards).
        if (this.interactionType === InteractionType.DefaultActionLink && !this.addInProgress) {
            this.editorialSidebarService.handleSelection(selection);
        }
    }

    /**
     * After a sidebar-add reload:
     * - included → keep open, reset selection
     * - created → select the new reference and show its options.
     */
    private selectPendingNodes(references: CollectionReference[]) {
        if (!this.addInProgress) {
            return;
        }
        this.addInProgress = false;
        const ids = this.selectAfterLoad;
        this.selectAfterLoad = [];
        if (!ids.length) {
            return;
        }
        const toSelect = references.filter((r) => ids.includes(this.nodeHelper.getOriginalId(r)));
        if (!toSelect.length) {
            return;
        }
        // selecting drives handleSelection (now unguarded) → sidebar nodes/options; re-open it
        this.listReferences?.getSelection().setSelection(...toSelect);
        this.editorialSidebarService.sidebarOpened.set(true);
    }

    clearSelection() {
        this.clearAllSelections();
        this.selection = [];
        this.selectionOverlayOpen = false;
    }

    /** The nodes selected in the lists that are currently rendered. */
    private collectSelection(): Node[] {
        if (this.viewState.viewType() === 'tree') {
            return this.listTree?.getSelection()?.selected ?? [];
        }
        return [
            ...(this.listCollections?.getSelection()?.selected ?? []),
            ...(this.listReferences?.getSelection()?.selected ?? []),
        ];
    }

    /** Mirrors the selection of the rendered lists into the selection bar and its options. */
    updateSelection() {
        this.selection = this.collectSelection();
        this.selectionOptions.setData({
            scope: Scope.CollectionsReferences,
            activeObjects: this.selection,
            selectedObjects: this.selection,
            parent: this.collection,
            postPrepareOptions: (options, nodes) =>
                this.removeOptionsUnsupportedForSelection(options, nodes),
        });
        void this.selectionOptions.initComponents(
            this.selectionActionbar?.(),
            this.viewState.viewType() === 'tree'
                ? (this.listTree as ListEventInterface<Node>)
                : (this.listReferences as ListEventInterface<Node>),
        );
        void this.selectionOptions.refreshComponents();
    }

    /**
     * Drops the options whose action only takes the first node into account or cannot be
     * carried out for a selection that mixes sub-collections and materials.
     */
    private removeOptionsUnsupportedForSelection(options: OptionItem[], nodes: Node[]) {
        const collections = nodes.filter((node) => this.nodeHelper.isNodeCollection(node)).length;
        const mixed = collections > 0 && collections < nodes.length;
        const unsupported = (option: OptionItem) =>
            (nodes.length > 1 && option.name === 'OPTIONS.ADD_SHORTCUT') ||
            (mixed && option.name === 'OPTIONS.COPY') ||
            ((collections > 1 || mixed) && option.name === 'OPTIONS.INVITE');
        for (let i = options.length - 1; i >= 0; i--) {
            if (unsupported(options[i])) {
                options.splice(i, 1);
            }
        }
        // neither the deletion of collections nor the removal of references covers both kinds
        if (mixed && this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)) {
            const deleteMixed = new OptionItem('OPTIONS.DELETE', 'delete', () =>
                this.deleteSelection(nodes),
            );
            deleteMixed.group = DefaultGroups.Delete;
            deleteMixed.color = 'warn';
            deleteMixed.priority = 10;
            options.push(deleteMixed);
        }
    }

    /**
     * Deletes the selected sub-collections and removes the selected materials from the collection
     * they are listed in, after a single confirmation.
     */
    private async deleteSelection(nodes: Node[]) {
        const collections = nodes.filter((node) => this.nodeHelper.isNodeCollection(node));
        const references = nodes.filter((node) => !this.nodeHelper.isNodeCollection(node));
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'OPTIONS.DELETE',
            message: 'COLLECTIONS.DELETE_SELECTION_DIALOG_TEXT',
            messageParameters: {
                collections: collections.length.toString(),
                references: references.length.toString(),
            },
            buttons: DELETE_OR_CANCEL,
        });
        if ((await firstValueFrom(dialogRef.afterClosed())) !== 'YES_DELETE') {
            return;
        }
        // what lies inside a collection that is deleted goes away together with it
        const deletedIds = new Set(collections.map((node) => node.ref.id));
        const isInsideDeleted = (node: Node) => deletedIds.has(node.parent?.id as string);
        const toDelete = collections.filter((node) => !isInsideDeleted(node));
        const toRemove = references.filter((node) => !isInsideDeleted(node));
        void this.globalCollectionsPageService.removeTemporaryCollections(toDelete);
        const requests: Observable<Node | { error: unknown }>[] = [
            ...toDelete.map((node) =>
                this.nodeService.deleteNode(node.ref.id).pipe(map(() => node)),
            ),
            ...toRemove.map((node) =>
                this.collectionService
                    .removeFromCollection(
                        node.ref.id,
                        (node.parent?.id ?? this.collection.ref.id) as string,
                    )
                    .pipe(map(() => node)),
            ),
        ].map((request) => request.pipe(catchError((error) => of({ error }))));
        this.toast.showProgressSpinner();
        const results = await firstValueFrom(
            from(requests).pipe(
                concatMap((request) => request),
                toArray(),
            ),
        );
        this.toast.closeProgressSpinner();
        const failed = results.find((result) => 'error' in result) as
            | { error: unknown }
            | undefined;
        if (failed) {
            this.toast.error(failed.error);
        } else {
            this.toast.toast('WORKSPACE.TOAST.DELETE_FINISHED');
        }
        const processed = results.filter((result) => !('error' in result)) as Node[];
        if (processed.length > 0) {
            this.localEventsService.nodesDeleted.emit(processed);
        }
    }

    onCollectionsSelectionChange() {
        this.updateSelection();
    }

    /**
     * Remove a single node from the selection (triggered by unchecking it in the selection overlay).
     * Deselecting on the list's selection model fires `selectionChange`, which updates `selection`.
     */
    deselectNode(node: Node) {
        this.listReferences?.getSelection()?.deselect(node as CollectionReference);
        this.listCollections?.getSelection()?.deselect(node);
        this.listTree?.getSelection()?.deselect(node);
    }
    private clickElementEvent(event: NodeClickEvent<CollectionReference | ProposalNode>) {
        if (this.interactionType === InteractionType.DefaultActionLink) {
            if (event.ctrlKey) {
                window.open(
                    this.platformLocation.getBaseHrefFromDOM() +
                        this.router.serializeUrl(
                            this.router.createUrlTree([
                                UIConstants.ROUTER_PREFIX + 'render',
                                event.element.ref.id,
                            ]),
                        ),
                    '_blank',
                );
                return;
            }
            // a click replaces the selection of the tree by the clicked material
            const inTree = this.viewState.viewType() === 'tree';
            if (!inTree) {
                this.listCollections?.getSelection()?.clear();
            }
            this.editorialSidebarService.handleSelect(
                inTree ? this.listTree : this.listReferences,
                event,
                Scope.CollectionsReferences,
                undefined,
                !inTree,
            );
        } else {
            this.clickItem.emit(event);
        }
    }

    private isAllowedToDeleteNodes(nodes: Node[]) {
        return (
            this.isAllowedToDeleteCollection() ||
            this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)
        );
    }
    isAllowedToDeleteCollection(): boolean {
        if (this.isRootLevel) {
            return false;
        }
        return RestHelper.hasAccessPermission(this.collection, RestConstants.PERMISSION_DELETE);
    }

    private deleteFromCollection(callback: Function = null) {
        this.toast.showProgressSpinner();
        this.collectionService
            .removeFromCollection(this.contentNode.ref.id, this.collection.ref.id)
            .subscribe(
                () => {
                    if (!('proposal' in this.collection)) {
                        this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                    }
                    this.toast.closeProgressSpinner();
                    this.refreshContent();
                    if (callback) {
                        callback();
                    }
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }

    private registerMainNav(): void {
        this.mainNavService.setMainNavConfig({
            title: 'COLLECTIONS.TITLE',
            currentScope: 'collections',
            onCreate: (nodes) => this.addNodesToCollection(nodes),
        });
        this.mainNavService.onConnectorCreated
            .pipe(takeUntil(this.destroyed$))
            .subscribe((node) => this.addNodesToCollection([node]));
        this.mainNavUpdateTrigger.pipe(takeUntil(this.destroyed$)).subscribe(async () => {
            this.mainNavService.patchMainNavConfig({
                create: {
                    allowed: this.createAllowed() || this.isAllowedToAddContent(),
                    allowBinary: !this.isRootLevel && this.isAllowedToAddContent(),
                    parent: this.collection ?? null,
                },
            });
        });
    }

    /** Reloads the content when the search term changed, e.g. when it was typed or cleared. */
    private onQueryChange(query: string | null, includeSubLevels: boolean) {
        if (query === this.shownQuery && includeSubLevels === this.shownIncludeSubLevels) {
            return;
        }
        this.shownQuery = query;
        this.shownIncludeSubLevels = includeSubLevels;
        if (this.collection) {
            this.refreshContent();
        }
    }

    /** Lists the collections matching the search instead of the content of the collection. */
    private searchContent() {
        this.dataSourceCollections.isLoading = true;
        this.dataSourceReferences.isLoading = false;
        const query = this.searchState.query().trim();
        const includeSubLevels = this.searchState.includeSubLevels();
        this.collectionSearch
            .search(query, {
                includeSubLevels,
                scope: this.isRootLevel ? this.scope : undefined,
                parentId: this.isRootLevel ? undefined : this.collection.ref.id,
            })
            .subscribe({
                next: (result) => {
                    // a stale answer must not replace the one of a newer search
                    if (
                        query !== this.searchState.query()?.trim() ||
                        includeSubLevels !== this.searchState.includeSubLevels()
                    ) {
                        return;
                    }
                    this.searchAncestors = result.ancestors;
                    this.searchAncestorIds = new Set(result.ancestors.map((n) => n.ref.id));
                    this.searchParentIds = result.parentIds;
                    this.searchTruncated.set(result.truncated);
                    this.searchState.hitCount.set(result.hits.length);
                    this.dataSourceCollections.setData(
                        result.hits.map((hit) => this.withParentTitle(hit, result.parents)),
                    );
                    this.dataSourceCollections.isLoading = false;
                    this.finishCollectionLoading();
                },
                error: (error: unknown) => {
                    this.dataSourceCollections.isLoading = false;
                    this.toast.error(error);
                },
            });
    }

    /** Adds the title of the collection a hit is placed in, which the list shows as a column. */
    private withParentTitle(hit: Node, parents: Map<string, Node>): Node {
        const parent = parents.get(hit.ref.id);
        return {
            ...hit,
            properties: {
                ...hit.properties,
                [SEARCH_PARENT_TITLE_PROPERTY]: [parent ? parent.title || parent.name : ''],
            },
        };
    }

    private refreshContent() {
        this.dataSourceCollections.reset();
        this.dataSourceReferences.reset();
        this.listReferences?.getSelection().clear();
        this.selection = [];
        this.selectionOverlayOpen = false;
        this.searchTruncated.set(false);
        this.searchAncestors = [];
        this.searchAncestorIds = new Set();
        this.searchParentIds = new Map();
        this.searchState.hitCount.set(null);
        if (this.searchState.active()) {
            this.searchContent();
            return;
        }
        this.dataSourceCollections.isLoading = true;
        this.dataSourceReferences.isLoading = true;

        // set correct scope
        const request: RequestObject = Helper.deepCopy(CollectionContentComponent.DEFAULT_REQUEST);
        if (this.sortCollections?.active) {
            request.sortBy = [this.sortCollections.active];

            request.sortAscending = [this.sortCollections.direction === 'asc'];
        } else {
            console.warn('Sort for collections is not defined in the mds!');
        }
        // when loading child collections, we load all of them
        if (!this.isRootLevel) {
            request.count = RestConstants.COUNT_UNLIMITED;
        } else {
            // on root level, obey pinned order if collections are pinned
            request.sortBy = [
                RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                ...request.sortBy,
            ];
            request.sortAscending = [false, true, ...request.sortAscending];
        }
        this.collectionService
            .getCollectionSubcollections(
                this.collection.ref.id,
                this.scope,
                [],
                request,
                this.collection.ref.repo,
            )
            .subscribe(
                async (collection) => {
                    // transfere sub collections and content
                    this.dataSourceCollections.setData(
                        collection.collections,
                        collection.pagination,
                    );
                    this.dataSourceCollections.isLoading = false;
                    if (this.isRootLevel) {
                        if (this.scope === RestConstants.COLLECTIONSCOPE_MY) {
                            this.listCollections?.addVirtualNodes(
                                (
                                    await this.sessionStorageService.get<VirtualNode[]>(
                                        SessionStorageService.KEY_ROOT_COLLECTIONS,
                                        [],
                                        Store.BrowserSessionStorage,
                                    )
                                ).map((n) => {
                                    n.override = false;
                                    return n;
                                }),
                                { select: false },
                            );
                        }
                        this.finishCollectionLoading();
                        return;
                    }
                    const requestRefs = this.getReferencesRequest();
                    requestRefs.count = null;
                    this.collectionService
                        .getCollectionReferences(
                            this.collection.ref.id,
                            [RestConstants.ALL],
                            requestRefs,
                            this.collection.ref.repo,
                        )
                        .subscribe((refs) => {
                            this.dataSourceReferences.setData(refs.references, refs.pagination);
                            this.dataSourceReferences.isLoading = false;
                            this.selectPendingNodes(refs.references);
                            this.finishCollectionLoading();
                        });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }

    getCollectionsColumns(): ColumnType {
        if (this.viewState.collectionsDisplayType() !== NodeEntriesDisplayType.Table) {
            return this.collectionsColumns;
        }
        return this.searchState.active()
            ? this.collectionsSearchTableColumns
            : this.collectionsTableColumns;
    }

    isMobile() {
        return this.uiService.isMobile();
    }

    private getReferencesRequest(): RequestObject {
        return {
            sortBy: [this.sortReferences.active],
            sortAscending: [this.sortReferences.direction === 'asc'],
        };
    }

    async loadMoreReferences(event: FetchEvent) {
        if (!(await this.dataSourceReferences.hasMore()) || this.dataSourceReferences.isLoading) {
            return;
        }
        const request = this.getReferencesRequest();
        request.offset = event.offset ?? (await this.dataSourceReferences.getData()).length;
        if (event.amount != null) {
            request.count = event.amount;
        }
        if (event.reset) {
            this.dataSourceReferences.reset();
        }
        this.dataSourceReferences.isLoading = true;
        this.collectionService
            .getCollectionReferences(
                this.collection.ref.id,
                [RestConstants.ALL],
                request,
                this.collection.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceReferences.appendData(refs.references);
                this.dataSourceReferences.isLoading = false;
            });
    }

    async loadMoreCollections() {
        // all hits of a search are loaded at once
        if (this.searchState.active()) {
            return;
        }
        if (!(await this.dataSourceCollections.hasMore()) || this.dataSourceCollections.isLoading) {
            return;
        }
        const request: any = Helper.deepCopy(CollectionContentComponent.DEFAULT_REQUEST);
        request.offset = (await this.dataSourceCollections.getData()).length;
        this.dataSourceCollections.isLoading = true;
        this.collectionService
            .getCollectionSubcollections(
                this.collection.ref.id,
                this.scope,
                [],
                request,
                this.collection.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceCollections.appendData(refs.collections);
                this.dataSourceCollections.isLoading = false;
            });
    }

    private finishCollectionLoading(callback?: () => void) {
        // safety net: clear the add-in-flight guard on any load path that does not run
        // selectPendingNodes (e.g. root level), so later selections are not swallowed
        this.addInProgress = false;
        void this.mainNavService.getMainNav()?.refreshBanner();

        // Cannot trivially reference the add button for the tutorial with
        // current implementation of generic options.
        //
        // TODO: Decide whether to keep the tutorial as it was and implement a
        // way to reference the option button if necessary.

        // if (
        //     this.getCollectionId() == RestConstants.ROOT &&
        //     this.isAllowedToEditCollection()
        // ) {
        //     setTimeout(() => {
        //         this.tutorialElement = this.listCollections.addElementRef;
        //     });
        // }
        if (callback) {
            callback();
        }
        if (this.viewState.viewType() === 'tree' && !this.keepTreeOnRefresh) {
            void this.buildTree();
        }
        this.keepTreeOnRefresh = false;
        setTimeout(() => {
            this.wireListOptions();
            if (!this.loadingTask.isDone) {
                this.loadingTask.done();
            }
        });
    }

    /** Connects the lists that are currently rendered to the options of the collection. */
    private wireListOptions() {
        this.setOptionsCollection();
        void this.listReferences?.initOptionsGenerator({
            actionbar: [this.actionbarToggles],
            parent: this.collection,
        });
        // the sub-collections offer the same actions as in the tree; in reurl picker mode with
        // applyCollections this also exposes the APPLY action on their cards
        void this.listCollections?.initOptionsGenerator({
            actionbar: [this.actionbarCollectionToggles],
            parent: this.collection,
        });
    }

    /**
     * The lists are destroyed and re-created when the tree is entered or left, so the selection
     * is dropped and the options are wired to whatever is rendered afterwards.
     */
    private onViewTypeChange(viewType: CollectionViewType) {
        if (viewType === this.shownViewType) {
            return;
        }
        this.shownViewType = viewType;
        this.clearSelection();
        if (viewType === 'tree') {
            void this.buildTree();
        } else {
            this.treeReady.set(false);
            setTimeout(() => this.wireListOptions());
        }
    }

    /** (Re-)creates the tree from the sub-collections and materials that are currently loaded. */
    private async buildTree() {
        this.treeReady.set(false);
        if (this.isRootLevel && !this.searchState.active()) {
            // the tree cannot page, so it needs every collection of the scope
            await this.loadRemainingCollections();
        }
        // the tree adjusts the counts of the nodes it is given, so it works on copies
        const copy = (node: Node): Node => ({
            ...node,
            collection: node.collection ? { ...node.collection } : node.collection,
        });
        this.treeConfig.expandAll = this.searchState.active();
        const collections = this.dataSourceCollections.getData() ?? [];
        // a search lists its hits below the collections leading to them; whatever has no such
        // collection among the listed ones hangs from the root
        const listedIds = new Set([...this.searchAncestors, ...collections].map((n) => n.ref.id));
        const placeInSearch = (node: Node): Node => {
            const parentId = this.searchParentIds.get(node.ref.id);
            return {
                ...node,
                parent: {
                    ...node.parent,
                    id: parentId && listedIds.has(parentId) ? parentId : this.collection.ref.id,
                },
            };
        };
        this.dataSourceTree.setData(
            [
                this.treeRoot(),
                ...(this.searchState.active()
                    ? [...this.searchAncestors, ...collections].map(placeInSearch)
                    : collections.map((node) =>
                          this.isRootLevel
                              ? { ...node, parent: { ...node.parent, id: this.collection.ref.id } }
                              : node,
                      )),
                ...(this.dataSourceReferences.getData() ?? []),
            ].map(copy),
        );
        setTimeout(() => {
            this.treeReady.set(true);
            setTimeout(() => void this.listTree?.initOptionsGenerator({ parent: this.collection }));
        });
    }

    /**
     * The node the tree hangs from. The root level has no collection of its own, so a stand-in
     * named after the selected scope is used.
     */
    private treeRoot(): Node {
        if (!this.isRootLevel) {
            return this.collection;
        }
        const title = this.translation.instant('COLLECTIONS.TAB.' + this.scope);
        return { ...this.collection, title, name: title, icon: { fontGlyphId: 'layers' } } as Node;
    }

    /** Loads the pages of the root collections that have not been requested yet. */
    private async loadRemainingCollections() {
        if (this.searchState.active()) {
            return;
        }
        this.dataSourceCollections.isLoading = true;
        try {
            while (this.dataSourceCollections.hasMore()) {
                const request: RequestObject = Helper.deepCopy(
                    CollectionContentComponent.DEFAULT_REQUEST,
                );
                request.offset = this.dataSourceCollections.getData().length;
                request.count = CollectionContentComponent.TREE_PAGE_SIZE;
                const page = await firstValueFrom(
                    this.collectionService.getCollectionSubcollections(
                        this.collection.ref.id,
                        this.scope,
                        [],
                        request,
                        this.collection.ref.repo,
                    ),
                );
                if (!page.collections.length) {
                    // the total is stale, so there is nothing more to page through
                    break;
                }
                this.dataSourceCollections.appendData(page.collections);
            }
        } finally {
            this.dataSourceCollections.isLoading = false;
        }
    }

    /** Whether the next load of the lists leaves the tree as it is instead of building it anew. */
    private keepTreeOnRefresh = false;

    onTreeSelectionChange() {
        this.updateSelection();
    }

    /**
     * A material that is clicked in the tree is opened the same way as in the list; a clicked
     * collection shows its information. With the control key the node is added to or removed from
     * the selection instead.
     */
    onTreeClick(event: NodeClickEvent<Node>) {
        if (event.ctrlKey) {
            this.listTree?.getSelection()?.toggle(event.element);
            return;
        }
        if (this.nodeHelper.isNodeCollection(event.element)) {
            this.onCollectionClick(event);
            return;
        }
        void this.onContentClick({
            element: event.element as CollectionReference,
            source: event.source,
        });
    }

    deleteReference(content: EduData.CollectionReference | EduData.Node) {
        this.contentNode = content;
        this.deleteFromCollection();
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [this.collection],
            // the topic page is reached through the view toggle, so it stays in the menu and the
            // buttons of the header are the information and the editing
            postPrepareOptions: (options) => {
                for (const option of options) {
                    if (option.name === 'OPTIONS.SHOW_TOPIC_PAGE') {
                        option.showAsAction = false;
                    }
                }
            },
        });
        void this.optionsService.initComponents(this.headerActionbar?.(), this.listReferences);
        void this.optionsService.refreshComponents();
    }

    /** Whether the sub-collections or the materials are currently arranged by hand. */
    isOrderModeActive(): boolean {
        return (
            !!this.sortCollections.customSortingInProgress ||
            !!this.sortReferences.customSortingInProgress
        );
    }

    /** Whether there is a list the user may arrange by hand, which the mode toggle depends on. */
    canOrder(): boolean {
        return (
            this.viewState.viewType() !== 'tree' &&
            (this.canOrderCollections() || this.canOrderReferences())
        );
    }

    canOrderCollections(): boolean {
        return (
            !this.isMobile() &&
            !this.searchState.active() &&
            this.viewState.collectionsDisplayType() !== NodeEntriesDisplayType.Table &&
            this.dataSourceCollections.getData()?.length > 1 &&
            this.isUserAllowedToEdit(this.collection)
        );
    }

    canOrderReferences(): boolean {
        return (
            !this.isMobile() &&
            this.viewState.referencesDisplayType() !== NodeEntriesDisplayType.Table &&
            this.dataSourceReferences.getData()?.length > 1 &&
            this.isUserAllowedToEdit(this.collection)
        );
    }

    /**
     * Handles the toggle that switches the arrangement by hand on or off for the sub-collections
     * and the materials together.
     */
    async onOrderModeToggle(event: MatSlideToggleChange) {
        if (event.checked) {
            await this.enterOrderMode();
        } else {
            await this.leaveOrderMode();
        }
        // The toggle keeps its own state, which differs when leaving the mode was refused.
        event.source.checked = this.isOrderModeActive();
    }

    private async enterOrderMode() {
        const collections = this.canOrderCollections();
        const references = this.canOrderReferences();
        if (references) {
            // all materials have to be present to arrange them
            await this.loadMoreReferences({
                reset: true,
                amount: RestConstants.COUNT_UNLIMITED,
                offset: 0,
            });
        }
        this.collectionsOrderSnapshot = collections
            ? await this.getOrderSnapshot(this.dataSourceCollections)
            : null;
        this.referencesOrderSnapshot = references
            ? await this.getOrderSnapshot(this.dataSourceReferences)
            : null;
        this.sortCollections = { ...this.sortCollections, customSortingInProgress: collections };
        this.sortReferences = { ...this.sortReferences, customSortingInProgress: references };
        void this.awaitOrderDecision();
    }

    /** Shows the info bar of the mode and applies the choice made there. */
    private async awaitOrderDecision() {
        const response = await this.infobar.open({
            title: 'COLLECTIONS.ORDER_ALL',
            message: 'COLLECTIONS.ORDER_ALL_INFO',
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'SAVE', config: { color: 'primary' } },
            ],
        });
        if (!this.isOrderModeActive()) {
            // The mode was left via the toggle, which takes care of the pending changes.
            return;
        }
        const changed = await this.getChangedOrderLists();
        this.endOrderMode();
        if (response === 'SAVE') {
            if (changed.collections || changed.references) {
                await this.saveOrder(changed);
            }
        } else {
            this.refreshContent();
        }
    }

    private async leaveOrderMode() {
        const changed = await this.getChangedOrderLists();
        let decision: 'SAVE' | 'DISCARD' | 'CANCEL' = 'DISCARD';
        if (changed.collections || changed.references) {
            decision = await this.confirmLeavingOrderMode();
        }
        if (decision === 'CANCEL') {
            return;
        }
        this.endOrderMode();
        if (decision === 'SAVE') {
            await this.saveOrder(changed);
        } else {
            this.refreshContent();
        }
    }

    /** Switches both lists back to their regular view and closes the info bar of the mode. */
    private endOrderMode() {
        this.sortCollections = { ...this.sortCollections, customSortingInProgress: false };
        this.sortReferences = { ...this.sortReferences, customSortingInProgress: false };
        this.infobar.close();
    }

    private async getOrderSnapshot<T extends Node>(
        dataSource: NodeDataSource<T>,
    ): Promise<string[]> {
        return RestHelper.getNodeIds(await dataSource.getData());
    }

    /** Tells which of the lists differ from the arrangement they had when the mode was entered. */
    private async getChangedOrderLists(): Promise<{ collections: boolean; references: boolean }> {
        const differs = async <T extends Node>(
            dataSource: NodeDataSource<T>,
            snapshot: string[] | null,
        ) => {
            if (snapshot === null) {
                return false;
            }
            const current = await this.getOrderSnapshot(dataSource);
            return (
                current.length !== snapshot.length || current.some((id, i) => id !== snapshot[i])
            );
        };
        return {
            collections: await differs(this.dataSourceCollections, this.collectionsOrderSnapshot),
            references: await differs(this.dataSourceReferences, this.referencesOrderSnapshot),
        };
    }

    /** Resolves what to do with the arrangement made in the mode when the mode is left. */
    private async confirmLeavingOrderMode(): Promise<'SAVE' | 'DISCARD' | 'CANCEL'> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'COLLECTIONS.ORDER_UNSAVED_TITLE',
            message: 'COLLECTIONS.ORDER_UNSAVED_MESSAGE',
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'DISCARD', config: { color: 'standard' } },
                { label: 'SAVE', config: { color: 'primary' } },
            ],
        });
        // Dismissing the dialog without a choice keeps the user in the mode.
        return (await firstValueFrom(dialogRef.afterClosed())) ?? 'CANCEL';
    }

    addNodesToCollection(nodes: Node[], allowDuplicate: boolean | 'ignore' = false) {
        this.toast.showProgressSpinner();
        this.uiService.addToCollection(
            this.collection,
            nodes,
            false,
            () => {
                this.refreshContent();
                this.toast.closeProgressSpinner();
            },
            allowDuplicate,
        );
    }

    /**
     * Stores the arrangement of the given lists as the order in which the children of the
     * collection show.
     */
    private async saveOrder({
        collections,
        references,
    }: {
        collections: boolean;
        references: boolean;
    }) {
        this.toast.showProgressSpinner();
        try {
            if (references) {
                await firstValueFrom(
                    this.collectionService.setOrder(
                        this.collection.ref.id,
                        RestHelper.getNodeIds(await this.dataSourceReferences.getData()),
                    ),
                );
                await this.storeCustomOrderMode(RestConstants.CCM_PROP_COLLECTION_ORDER_MODE);
                this.sortReferences = {
                    ...this.sortReferences,
                    active: RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                    direction: 'asc',
                };
            }
            if (collections) {
                await firstValueFrom(
                    this.collectionService.setOrder(
                        this.collection.ref.id,
                        RestHelper.getNodeIds(await this.dataSourceCollections.getData()),
                    ),
                );
                await this.storeCustomOrderMode(
                    RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
                );
                this.sortCollections = {
                    ...this.sortCollections,
                    active: RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
                    direction: 'asc',
                };
            }
            this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
        } catch (error) {
            this.toast.error(error);
        } finally {
            this.toast.closeProgressSpinner();
        }
    }

    /** Makes the arrangement set by hand the order in which the children of the collection show. */
    private async storeCustomOrderMode(property: string): Promise<void> {
        const value = [RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, 'true'];
        await this.nodeServiceApi
            .setProperty(this.collection.ref.repo, this.collection.ref.id, property, value)
            .toPromise();
        this.collection.properties = { ...this.collection.properties, [property]: value };
    }

    /**
     * The sub-collections are not links when the page handles them itself: a click shows the
     * information of the collection and only a double click enters it.
     */
    collectionsInteractionType(): InteractionType {
        return this.interactionType === InteractionType.DefaultActionLink
            ? InteractionType.Emitter
            : this.interactionType;
    }

    onCollectionClick(event: NodeClickEvent<Node | CollectionReference>) {
        if (this.interactionType !== InteractionType.DefaultActionLink) {
            this.clickItem.emit(event);
            return;
        }
        const list = this.viewState.viewType() === 'tree' ? this.listTree : this.listCollections;
        const selection = list?.getSelection();
        const isSoleSelection =
            selection?.selected.length === 1 && selection.selected[0] === event.element;
        this.clearAllSelections();
        if (isSoleSelection) {
            this.editorialSidebarService.close();
            return;
        }
        selection?.select(event.element as Node);
        this.editorialSidebarService.nodes.set([event.element]);
        this.editorialSidebarService.showOption({ option: 'COLLECTION_INFO', trap: false });
    }

    /** Empties the selection of every list that is currently rendered. */
    private clearAllSelections() {
        this.listReferences?.getSelection()?.clear();
        this.listCollections?.getSelection()?.clear();
        this.listTree?.getSelection()?.clear();
    }

    openCollection(event: NodeClickEvent<Node | CollectionReference>) {
        if (this.interactionType === InteractionType.DefaultActionLink) {
            void this.nodeHelper.navigateToNode(event);
        }
    }

    openItem(event: NodeClickEvent<CollectionReference>) {
        void this.nodeHelper.navigateToNode(event);
    }

    canDelete(node: EduData.CollectionReference) {
        return RestHelper.hasAccessPermission(this.collection, 'Delete');
    }

    isDeleted(node: CollectionReference) {
        return (
            node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE) &&
            !node.aspects.includes(RestConstants.CCM_ASPECT_REMOTEREPOSITORY) &&
            !node.originalId
        );
    }
}
