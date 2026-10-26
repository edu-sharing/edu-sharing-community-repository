import {
    Component,
    ContentChild,
    effect,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
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
    NodeEntriesWrapperComponent,
    TreeConfig,
    NodesRightMode,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    UIConstants,
    VirtualNode,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom, forkJoin, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
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
    private collectionsOrderSnapshot: string[] = [];
    private referencesOrderSnapshot: string[] = [];
    private loadingScreen = inject(LoadingScreenService);
    private mainNavService = inject(MainNavService);
    private mdsService = inject(MdsService);
    private mdsHelperService = inject(MdsHelperService);
    private nodeHelper = inject(NodeHelperService);
    private nodeService = inject(RestNodeService);
    private nodeServiceApi = inject(NodeService);
    private optionsService = inject(OptionsHelperDataService);
    private platformLocation = inject(PlatformLocation);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private toast = inject(Toast);
    private translation = inject(TranslateService);
    private uiService = inject(UIService);
    private globalCollectionsPageService = inject(GlobalCollectionsPageService);

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
        showFiles: true,
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
            (target as Node).ref.id === this.collection.ref.id ||
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
    };

    canDropOnCollection = (dropData: DragData<Node>): CanDrop => {
        return this.canDropOnCollectionBreadcrumbs(dropData);
    };

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
                                    this.refreshContent();
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
                                this.refreshContent();
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
        this.selection = selection.source.selected;
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
        this.listReferences?.getSelection().clear();
        this.selection = [];
        this.selectionOverlayOpen = false;
    }

    /**
     * Remove a single node from the selection (triggered by unchecking it in the selection overlay).
     * Deselecting on the list's selection model fires `selectionChange`, which updates `selection`.
     */
    deselectNode(node: Node) {
        this.listReferences?.getSelection().deselect(node as CollectionReference);
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
            // the tree selects a node on click already, so there the selection is kept
            const inTree = this.viewState.viewType() === 'tree';
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
                    this.searchTruncated.set(result.truncated);
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
    /** Enters or leaves the mode in which the materials are arranged by hand. */
    async setReferenceSort(sort: ListSortConfig) {
        this.sortReferences = sort;
        if (sort.customSortingInProgress) {
            // all materials have to be present to arrange them
            await this.loadMoreReferences({
                reset: true,
                amount: RestConstants.COUNT_UNLIMITED,
                offset: 0,
            });
        }
        await this.toggleReferencesOrder();
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
        if (this.viewState.viewType() === 'tree') {
            void this.buildTree();
        }
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
            actionbar: [this.actionbarToggles, this.selectionActionbar?.()],
            parent: this.collection,
        });
        // in reurl picker mode with applyCollections, wire options on the sub-collections
        // list too so their cards expose the APPLY action (mirrors the search page). The
        // one-shot call in ngOnChanges runs before the view/data are ready and is unreliable.
        const queryParams = this.route.snapshot.queryParams;
        if (queryParams.reurl && queryParams.applyCollections === 'true') {
            void this.listCollections?.initOptionsGenerator({});
        }
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
        this.dataSourceTree.setData(
            [
                this.treeRoot(),
                ...(this.dataSourceCollections.getData() ?? []).map((node) =>
                    this.isRootLevel || this.searchState.active()
                        ? { ...node, parent: { ...node.parent, id: this.collection.ref.id } }
                        : node,
                ),
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
        if (this.searchState.active() || !(await this.dataSourceCollections.hasMore())) {
            return;
        }
        const request: RequestObject = Helper.deepCopy(CollectionContentComponent.DEFAULT_REQUEST);
        request.offset = (await this.dataSourceCollections.getData()).length;
        request.count = RestConstants.COUNT_UNLIMITED;
        this.dataSourceCollections.isLoading = true;
        try {
            const remaining = await firstValueFrom(
                this.collectionService.getCollectionSubcollections(
                    this.collection.ref.id,
                    this.scope,
                    [],
                    request,
                    this.collection.ref.repo,
                ),
            );
            this.dataSourceCollections.appendData(remaining.collections);
        } finally {
            this.dataSourceCollections.isLoading = false;
        }
    }

    private handlingTreeSelection = false;

    /**
     * A material that is selected in the tree is opened the same way as in the list; a selected
     * collection shows its information.
     */
    onTreeSelectionChange(selection: SelectionChange<Node>) {
        // Opening the material re-selects it in the tree, which emits another change event.
        if (this.handlingTreeSelection) {
            return;
        }
        const collection = selection.added.find((node) => this.nodeHelper.isNodeCollection(node));
        if (collection) {
            this.editorialSidebarService.nodes.set([collection]);
            this.editorialSidebarService.showOption({ option: 'COLLECTION_INFO', trap: false });
            return;
        }
        if (
            selection.removed.some((node) => this.nodeHelper.isNodeCollection(node)) &&
            !selection.source.selected.length
        ) {
            this.editorialSidebarService.close();
            return;
        }
        const material = selection.added.find((node) => !this.nodeHelper.isNodeCollection(node));
        if (material) {
            this.handlingTreeSelection = true;
            void this.onContentClick({
                element: material as CollectionReference,
                source: ClickSource.Preview,
            }).finally(() => (this.handlingTreeSelection = false));
        }
    }

    deleteReference(content: EduData.CollectionReference | EduData.Node) {
        this.contentNode = content;
        this.deleteFromCollection();
    }

    /** Enters or leaves the mode in which the sub-collections are arranged by hand. */
    async setCollectionSort(sort: ListSortConfig) {
        this.sortCollections = sort;
        await this.toggleCollectionsOrder();
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [this.collection],
        });
        void this.optionsService.initComponents(this.headerActionbar?.(), this.listReferences);
        void this.optionsService.refreshComponents();
    }

    async toggleCollectionsOrder() {
        if (this.sortCollections.customSortingInProgress) {
            this.collectionsOrderSnapshot = await this.getOrderSnapshot(this.dataSourceCollections);
            const response = await this.infobar.open({
                title: 'COLLECTIONS.ORDER_COLLECTIONS',
                message: 'COLLECTIONS.ORDER_COLLECTIONS_INFO',
                buttons: [
                    { label: 'CANCEL', config: { color: 'standard' } },
                    { label: 'SAVE', config: { color: 'primary' } },
                ],
            });
            if (response === 'SAVE') {
                return this.changeCollectionsOrder();
            }
            if (response === null && !this.sortCollections.customSortingInProgress) {
                // The mode was left via the toggle, which takes care of the pending changes.
                return;
            }
            this.sortCollections.customSortingInProgress = false;
        } else {
            const decision = await this.confirmLeavingOrderMode(
                this.dataSourceCollections,
                this.collectionsOrderSnapshot,
            );
            if (decision === 'CANCEL') {
                this.sortCollections.customSortingInProgress = true;
                return;
            }
            this.infobar.close();
            if (decision === 'SAVE') {
                return this.changeCollectionsOrder();
            }
        }
        this.refreshContent();
    }

    async toggleReferencesOrder() {
        if (this.sortReferences.customSortingInProgress) {
            this.referencesOrderSnapshot = await this.getOrderSnapshot(this.dataSourceReferences);
            const response = await this.infobar.open({
                title: 'COLLECTIONS.ORDER_ELEMENTS',
                message: 'COLLECTIONS.ORDER_ELEMENTS_INFO',
                buttons: [
                    { label: 'CANCEL', config: { color: 'standard' } },
                    { label: 'SAVE', config: { color: 'primary' } },
                ],
            });
            if (response === 'SAVE') {
                this.sortReferences.customSortingInProgress = false;
                void this.changeReferencesOrder();
                this.listReferences?.getSelection().clear();
                return;
            }
            if (response === null && !this.sortReferences.customSortingInProgress) {
                // The mode was left via the toggle, which takes care of the pending changes.
                return;
            }
            this.sortReferences.customSortingInProgress = false;
            this.refreshContent();
        } else {
            const decision = await this.confirmLeavingOrderMode(
                this.dataSourceReferences,
                this.referencesOrderSnapshot,
            );
            if (decision === 'CANCEL') {
                this.sortReferences.customSortingInProgress = true;
                return;
            }
            this.infobar.close();
            if (decision === 'SAVE') {
                void this.changeReferencesOrder();
                this.listReferences?.getSelection().clear();
            } else {
                this.refreshContent();
            }
        }
    }

    private async getOrderSnapshot<T extends Node>(
        dataSource: NodeDataSource<T>,
    ): Promise<string[]> {
        return RestHelper.getNodeIds(await dataSource.getData());
    }

    /**
     * Resolves what to do with the arrangement made in the edit mode when the mode is left.
     *
     * Without any change in the order nothing needs to be decided and the arrangement is dropped.
     */
    private async confirmLeavingOrderMode<T extends Node>(
        dataSource: NodeDataSource<T>,
        snapshot: string[],
    ): Promise<'SAVE' | 'DISCARD' | 'CANCEL'> {
        const current = await this.getOrderSnapshot(dataSource);
        if (current.length === snapshot.length && current.every((id, i) => id === snapshot[i])) {
            return 'DISCARD';
        }
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'COLLECTIONS.ORDER_UNSAVED_TITLE',
            message: 'COLLECTIONS.ORDER_UNSAVED_MESSAGE',
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'DISCARD', config: { color: 'standard' } },
                { label: 'SAVE', config: { color: 'primary' } },
            ],
        });
        // Dismissing the dialog without a choice keeps the user in the edit mode.
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

    private async changeReferencesOrder() {
        this.toast.showProgressSpinner();
        try {
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
            this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
        } catch (error) {
            this.toast.error(error);
        } finally {
            this.toast.closeProgressSpinner();
        }
    }

    private async changeCollectionsOrder() {
        this.toast.showProgressSpinner();
        try {
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
                customSortingInProgress: false,
            };
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
