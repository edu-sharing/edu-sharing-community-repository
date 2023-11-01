import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    OnDestroy,
    OnInit,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { combineLatest, forkJoin as observableForkJoin, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import {
    DropSource,
    InteractionType,
    ListEventInterface,
    ListSortConfig,
    NodeEntriesDisplayType,
} from 'src/app/features/node-entries/entries-model';
import { NodeDataSource } from 'src/app/features/node-entries/node-data-source';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import * as EduData from '../../core-module/core.module';
import {
    CollectionReference,
    ConfigurationHelper,
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    ListItem,
    ListItemSort,
    MdsMetadatasets,
    Mediacenter,
    Node,
    NodeRef,
    NodesRightMode,
    NodeWrapper,
    Permission,
    ProposalNode,
    RequestObject,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMdsService,
    RestMediacenterService,
    RestNetworkService,
    RestNodeService,
    RestOrganizationService,
    TemporaryStorageService,
    UIService,
} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { ColorHelper, PreferredColor } from '../../core-module/ui/color-helper';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { ListTableComponent } from '../../core-ui-module/components/list-table/list-table.component';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import { OptionItem, Scope } from '../../core-ui-module/option-item';
import { OptionsHelperService } from '../../core-ui-module/options-helper.service';
import { Toast } from '../../core-ui-module/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { DragData } from '../../services/nodes-drag-drop.service';
import { ActionbarComponent } from '../../shared/components/actionbar/actionbar.component';
import { SortEvent } from '../../shared/components/sort-dropdown/sort-dropdown.component';
import { CanDrop } from '../../shared/directives/nodes-drop-target.directive';
import { TranslationsService } from '../../translations/translations.service';
import {
    ManagementEvent,
    ManagementEventType,
} from '../management-dialogs/management-dialogs.component';

// component class
@Component({
    selector: 'es-collections',
    templateUrl: 'collections.component.html',
    styleUrls: ['collections.component.scss'],
    // provide a new instance so to not get conflicts with other service instances
    providers: [OptionsHelperService],
})
export class CollectionsMainComponent implements OnInit, OnDestroy {
    static INDEX_MAPPING = [
        RestConstants.COLLECTIONSCOPE_MY,
        RestConstants.COLLECTIONSCOPE_ORGA,
        RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
        RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
        RestConstants.COLLECTIONSCOPE_ALL,
    ];
    private static PROPERTY_FILTER = [RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR];
    private static DEFAULT_REQUEST = {
        sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE,
        ],
        sortAscending: [false, true, false],
    };
    readonly SCOPES = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;

    @ViewChild('actionbarCollection') actionbarCollection: ActionbarComponent;
    @ViewChild('actionbarReferences') actionbarReferences: ActionbarComponent;
    @ViewChild('listCollections') listCollections: ListTableComponent;
    @ViewChild('listReferences') listReferences: ListEventInterface<CollectionReference>;
    @ViewChild('listProposals') listProposals: ListEventInterface<ProposalNode>;
    @ContentChild('collectionContentTemplate') collectionContentTemplateRef: TemplateRef<any>;

    dataSourceCollections = new NodeDataSource<Node>();
    dataSourceReferences = new NodeDataSource<CollectionReference>();

    referencesDisplayType = NodeEntriesDisplayType.Grid;
    viewTypeNodes: 0 | 1 | 2 = ListTableComponent.VIEW_TYPE_GRID;

    dialogTitle: string;
    dialogCancelable = false;
    dialogMessage: string;
    dialogButtons: DialogButton[];
    tabSelected: string = RestConstants.COLLECTIONSCOPE_MY;
    isLoading = true;
    isReady = false;
    collectionContent: {
        node: Node;
    };
    collectionSortEmitter = new EventEmitter<SortEvent>();
    collectionCustomSortEmitter = new EventEmitter<boolean>();
    referenceSortEmitter = new EventEmitter<SortEvent>();
    referenceCustomSortEmitter = new EventEmitter<boolean>();
    mainnav = true;
    isGuest = true;
    sortCollectionColumns: ListItemSort[] = [
        new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
        new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, 'ascending'),
    ];
    addToOther: EduData.Node[];
    addPinning: string;
    infoTitle: string;
    infoMessage: string;
    infoButtons: DialogButton[];
    infoClose: Function;
    collectionsColumns: ListItem[] = [];
    referencesColumns: ListItem[] = [];
    createSubCollectionOptionItem = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', () =>
        this.onCreateCollection(),
    );
    addMaterialSearchOptionItem = new OptionItem('OPTIONS.SEARCH_OBJECT', 'redo', () => {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this.collectionContent.node.ref.id;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    });
    addMaterialBinaryOptionItem = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () => {
        this.mainNavService.getMainNav().topBar.createMenu.openUploadSelect();
    });
    dataSourceCollectionProposals = new NodeDataSource<ProposalNode>();
    proposalColumns = [
        new ListItem('NODE', RestConstants.CM_PROP_TITLE),
        new ListItem('NODE_PROPOSAL', RestConstants.CM_CREATOR, { showLabel: false }),
        new ListItem('NODE_PROPOSAL', RestConstants.CM_PROP_C_CREATED, { showLabel: false }),
    ];
    tutorialElement: ElementRef;
    permissions: Permission[];
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
    // FIXME: `collectionShare` is expected to be of type `Node[]` by `workspace-management` but is
    // of type `Node` here.
    private adminMediacenters: Mediacenter[];
    private mainNavUpdateTrigger = new Subject<void>();

    set collectionShare(collectionShare: Node[]) {
        this._collectionShare = collectionShare as any as Node;
        this.refreshAll();
    }

    get collectionShare() {
        return this._collectionShare as any as Node[];
    }

    set tabSelectedIndex(pos: number) {
        if (this.isGuest) {
            pos += 2; // skip first 2 tabs
        }
        if (!this.hasEditorial && pos > 1) {
            pos++; // skip editorial
        }
        if (!this.hasMediacenter && pos > 2) {
            pos++; // skip mediacenter
        }
        this.selectTab(CollectionsMainComponent.INDEX_MAPPING[pos]);
    }

    get tabSelectedIndex() {
        let pos = CollectionsMainComponent.INDEX_MAPPING.indexOf(this.tabSelected);
        if (this.isGuest) {
            pos -= 2;
        }
        if (!this.hasEditorial && pos > 1) {
            pos--;
        }
        if (!this.hasMediacenter && pos > 2) {
            pos--;
        }
        return pos;
    }

    toggleCollectionsOrder() {
        if (this.sortCollections.customSortingInProgress) {
            this.infoTitle = 'COLLECTIONS.ORDER_COLLECTIONS';
            this.infoMessage = 'COLLECTIONS.ORDER_COLLECTIONS_INFO';
            this.infoButtons = DialogButton.getSingleButton('SAVE', () => {
                this.changeCollectionsOrder();
            });
            this.infoClose = () => {
                this.sortCollections.customSortingInProgress = false;
                this.toggleCollectionsOrder();
            };
        } else {
            this.infoTitle = null;
            this.refreshContent();
        }
    }

    toggleReferencesOrder() {
        if (this.sortReferences.customSortingInProgress) {
            this.infoTitle = 'COLLECTIONS.ORDER_ELEMENTS';
            this.infoMessage = 'COLLECTIONS.ORDER_ELEMENTS_INFO';
            this.infoButtons = DialogButton.getSingleButton('SAVE', () => {
                this.changeReferencesOrder();
                this.sortReferences.customSortingInProgress = false;
                this.listReferences.getSelection().clear();
            });
            this.infoClose = () => {
                this.sortReferences.customSortingInProgress = false;
                this.toggleReferencesOrder();
            };
        } else {
            this.infoTitle = null;
            this.refreshContent();
        }
    }

    private collectionContentOriginal: any;
    private filteredOutCollections: Array<EduData.Collection> = new Array<EduData.Collection>();
    private filteredOutReferences: Array<EduData.CollectionReference> =
        new Array<EduData.CollectionReference>();
    private collectionIdParamSubscription: any;
    private contentDetailObject: any = null;
    // real parentCollectionId is only available, if user was browsing
    private parentCollectionId: EduData.Reference = new EduData.Reference(
        RestConstants.HOME_REPOSITORY,
        RestConstants.ROOT,
    );
    private temp: string;
    private lastScrollY: number;
    private person: EduData.User;
    path: EduData.Node[];
    hasEditorial = false;
    hasMediacenter = false;
    private showCollection = false;
    reurl: any;
    private _collectionShare: Node;
    private params: Params;
    private destroyed = new Subject<void>();
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });

    // inject services
    constructor(
        private frame: FrameEventsService,
        private http: HttpClient,
        private temporaryStorageService: TemporaryStorageService,
        private location: Location,
        private collectionService: RestCollectionService,
        private nodeHelper: NodeHelperService,
        private mediacenterService: RestMediacenterService,
        private nodeService: RestNodeService,
        private mainNavService: MainNavService,
        private networkService: RestNetworkService,
        private organizationService: RestOrganizationService,
        private iamService: RestIamService,
        private mdsService: RestMdsService,
        private connector: RestConnectorService,
        private route: ActivatedRoute,
        private uiService: UIService,
        private router: Router,
        private tempStorage: TemporaryStorageService,
        private optionsService: OptionsHelperService,
        private toast: Toast,
        private bridge: BridgeService,
        private config: ConfigurationService,
        private translationService: TranslateService,
        private translations: TranslationsService,
        private loadingScreen: LoadingScreenService,
    ) {
        this.sortCollectionColumns[this.sortCollectionColumns.length - 1].mode = 'ascending';
        // this.collectionSortEmitter.subscribe((sort: SortEvent) => this.setCollectionSort(sort));
        // this.collectionCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleCollectionsOrder() : this.changeCollectionsOrder());
        // this.referenceSortEmitter.subscribe((sort: SortEvent) => this.setReferenceSort(sort));
        // this.referenceCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleReferencesOrder() : this.changeReferencesOrder());
        this.collectionsColumns.push(new ListItem('COLLECTION', 'title'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'info'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'scope'));
        this.setCollectionId(RestConstants.ROOT);
        this.translations.waitForInit().subscribe(() => {
            combineLatest([
                this.connector.isLoggedIn(),
                this.networkService.getRepositories(),
            ]).subscribe(
                ([data]) => {
                    if (data.isValidLogin && data.currentScope == null) {
                        this.addMaterialBinaryOptionItem.isEnabled =
                            this.connector.hasToolPermissionInstant(
                                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
                            );
                        this.createSubCollectionOptionItem.isEnabled =
                            this.connector.hasToolPermissionInstant(
                                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                            );
                        this.isGuest = data.isGuest;
                        this.mainNavUpdateTrigger.next();
                        this.mediacenterService.getMediacenters().subscribe((mediacenters) => {
                            this.adminMediacenters = mediacenters.filter(
                                (m) => m.administrationAccess,
                            );
                        });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                            )
                            .subscribe((data) => {
                                this.hasEditorial = data.collections.length > 0;
                            });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
                            )
                            .subscribe((data) => {
                                this.hasMediacenter = data.collections.length > 0;
                            });
                        this.mdsService.getSets().subscribe(
                            (data: MdsMetadatasets) => {
                                const mdsSets = ConfigurationHelper.filterValidMds(
                                    RestConstants.HOME_REPOSITORY,
                                    data.metadatasets,
                                    this.config,
                                );
                                this.mdsService.getSet(mdsSets[0].id).subscribe((mds) => {
                                    this.referencesColumns = MdsHelper.getColumns(
                                        this.translationService,
                                        mds,
                                        'collectionReferences',
                                    );
                                    //const info = MdsHelper.getSortInfo(mds, 'collections');
                                    //this.sortCollections = info.default;
                                    this.initialize();
                                });
                            },
                            (e) => {
                                console.warn(e);
                                this.initialize();
                            },
                        );
                    } else {
                        RestHelper.goToLogin(this.router, this.config);
                    }
                },
                (error: any) => RestHelper.goToLogin(this.router, this.config),
            );
        });
        this.mainNavService.getDialogs().onEvent.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                if (event.data.collection.ref.id === this.collectionContent.node.ref.id) {
                    console.log('add virtual', event.data.references);
                    this.listReferences.addVirtualNodes(event.data.references);
                }
            }
        });
    }

    ngOnInit(): void {
        this.registerMainNav();
    }

    ngOnDestroy() {
        this.destroyed.next();
        this.destroyed.complete();
        this.temporaryStorageService.set(
            TemporaryStorageService.NODE_RENDER_PARAMETER_DATA_SOURCE,
            this.dataSourceReferences,
        );
    }

    private registerMainNav(): void {
        this.mainNavService.setMainNavConfig({
            title: 'COLLECTIONS.TITLE',
            currentScope: 'collections',
            searchEnabled: false,
            // TODO: document where this fails.
            //
            // onCreate: (nodes) => this.addNodesToCollection(nodes),
        });
        this.mainNavService
            .getDialogs()
            .onUploadFilesProcessed.pipe(
                takeUntil(this.destroyed),
                filter((nodes) => !!nodes),
            )
            .subscribe((nodes) => this.addNodesToCollection(nodes));
        this.mainNavUpdateTrigger.pipe(takeUntil(this.destroyed)).subscribe(() => {
            this.mainNavService.patchMainNavConfig({
                create: {
                    allowed: this.createAllowed(),
                    allowBinary: !this.isRootLevelCollection() && this.isAllowedToEditCollection(),
                    parent: this.collectionContent?.node ?? null,
                },
            });
        });
    }

    isMobileWidth() {
        return window.innerWidth < UIConstants.MOBILE_WIDTH;
    }

    navigate(id = '', addToOther = '', feedback = false) {
        const params: any = {};
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.scope = this.tabSelected;
            params.id = id;
            if (feedback) {
                params.feedback = feedback;
            }
            if (addToOther) {
                params.addToOther = addToOther;
            }
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'collections'], {
                queryParams: params,
            });
        });
    }

    closeAddToOther() {
        this.navigate(this.collectionContent.node.ref.id);
    }

    selectTab(tab: string) {
        if (this.tabSelected != tab || this.getCollectionId() != RestConstants.ROOT) {
            this.tabSelected = tab;
            this.setCollectionId(RestConstants.ROOT);
            this.parentCollectionId = new EduData.Reference(
                RestConstants.HOME_REPOSITORY,
                RestConstants.ROOT,
            );
            this.contentDetailObject = null;
            this.navigate();
        }
    }

    isRootLevelCollection(): boolean {
        return !this.showCollection;
        /*
        if (this.collectionContent==null) return false;
        return this.collectionContent.getCollectionID()=='-root-';
        */
    }

    isAllowedToEditCollection(): boolean {
        if (this.isRootLevelCollection()) {
            return !this.isGuest; //this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
        }
        if (
            RestHelper.hasAccessPermission(
                this.collectionContent.node,
                RestConstants.PERMISSION_WRITE,
            )
        ) {
            return true;
        }
        return false;
    }

    feedbackAllowed(): boolean {
        return (
            !this.isGuest &&
            RestHelper.hasAccessPermission(
                this.collectionContent.node,
                RestConstants.PERMISSION_FEEDBACK,
            )
        );
    }

    isAllowedToDeleteCollection(): boolean {
        if (this.isRootLevelCollection()) {
            return false;
        }
        if (
            RestHelper.hasAccessPermission(
                this.collectionContent.node,
                RestConstants.PERMISSION_DELETE,
            )
        ) {
            return true;
        }
        return false;
    }

    isUserAllowedToEdit(collection: Node): boolean {
        return RestHelper.isUserAllowedToEdit(collection, this.person);
    }

    pinCollection() {
        this.addPinning = this.collectionContent.node.ref.id;
    }

    getPrivacyScope(collection: EduData.Collection): string {
        return collection.scope;
        //  return RestHelper.getPrivacyScope(collection);
    }

    navigateToSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    isBrightColor() {
        return (
            ColorHelper.getPreferredColor(this.collectionContent?.node?.collection?.color) ===
            PreferredColor.White
        );
    }

    getScopeInfo() {
        return this.nodeHelper.getCollectionScopeInfo(this.collectionContent.node);
    }

    dropOnRef = (target: Node, source: DropSource<Node>) => {
        return;
    };
    dropOnCollection = (target: Node | 'HOME', source: DropSource<Node>) => {
        if (source.element[0] === target) {
            return;
        }
        this.toast.showProgressDialog();
        if (source.element[0].mediatype === 'collection') {
            if (source.mode === 'copy') {
                this.toast.error(null, 'INVALID_OPERATION');
                this.toast.closeModalDialog();
                return;
            }
            const targetId = target === 'HOME' ? RestConstants.COLLECTIONHOME : target.ref.id;
            this.nodeService.moveNode(targetId, source.element[0].ref.id).subscribe(
                () => {
                    this.toast.closeModalDialog();
                    this.refreshContent();
                },
                (error) => {
                    this.handleError(error);
                    this.toast.closeModalDialog();
                },
            );
        } else if (target !== 'HOME') {
            UIHelper.addToCollection(
                this.nodeHelper,
                this.collectionService,
                this.router,
                this.bridge,
                target,
                source.element,
                false,
                (nodes) => {
                    if (source.mode === 'copy') {
                        this.toast.closeModalDialog();
                        this.refreshContent();
                        return;
                    }
                    if (nodes.length > 0) {
                        const movedNodes = source.element.filter(
                            (sourceElement: CollectionReference) =>
                                nodes.some((node) => node.originalId === sourceElement.originalId),
                        );
                        const observables = movedNodes.map((n) =>
                            this.collectionService.removeFromCollection(
                                n.ref.id,
                                this.collectionContent.node.ref.id,
                            ),
                        );
                        observableForkJoin(observables).subscribe(
                            () => {
                                this.toast.closeModalDialog();
                                this.refreshContent();
                            },
                            (error) => {
                                this.handleError(error);
                                this.toast.closeModalDialog();
                            },
                        );
                    } else {
                        this.toast.closeModalDialog();
                    }
                },
            );
        }
    };

    addNodesToCollection(nodes: Node[], allowDuplicate: boolean | 'ignore' = false) {
        this.toast.showProgressDialog();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            this.collectionContent.node,
            nodes,
            false,
            (refNodes) => {
                this.refreshContent();
                this.toast.closeModalDialog();
            },
            allowDuplicate,
        );
    }

    canDropOnBreadcrumb = (dragData: DragData<'HOME' | Node>): CanDrop => {
        if (dragData.target === 'HOME') {
            // TODO: explicitDeny and reason
            if (dragData.action !== 'move') {
                return { accept: false };
            } else if (
                dragData.draggedNodes.some(
                    (node) => !node.aspects.includes(RestConstants.CCM_ASPECT_COLLECTION),
                )
            ) {
                return { accept: false };
            } else if (
                !this.nodeHelper.getNodesRight(dragData.draggedNodes, RestConstants.ACCESS_WRITE)
            ) {
                return { accept: false };
            } else {
                return { accept: true };
            }
        } else {
            return this.canDropOnCollection(dragData as DragData<Node>);
        }
    };

    canDropOnCollection = (dragData: DragData<Node>): CanDrop => {
        // TODO: explicitDeny and reason
        if (dragData.draggedNodes[0].ref.id === (dragData.target as Node).ref.id) {
            return { accept: false };
        }
        if ((dragData.target as Node).ref.id === this.collectionContent.node.ref.id) {
            return { accept: false };
        }
        if (dragData.draggedNodes[0].collection && dragData.action === 'copy') {
            return { accept: false };
        }
        // do not allow to move anything else than editorial collections into editorial collections (if the source is a collection)
        if (dragData.draggedNodes[0].collection?.hasOwnProperty('childCollectionsCount')) {
            if (
                (dragData.draggedNodes[0].collection.type ===
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (dragData.target as Node).collection.type !==
                        RestConstants.COLLECTIONTYPE_EDITORIAL) ||
                (dragData.draggedNodes[0].collection.type !==
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    (dragData.target as Node).collection.type ===
                        RestConstants.COLLECTIONTYPE_EDITORIAL)
            ) {
                return { accept: false };
            }
        }
        if (
            (dragData.action === 'copy' &&
                !this.nodeHelper.getNodesRight(
                    dragData.draggedNodes,
                    RestConstants.ACCESS_CC_PUBLISH,
                    NodesRightMode.Original,
                )) ||
            (dragData.action === 'move' &&
                !this.nodeHelper.getNodesRight(
                    dragData.draggedNodes,
                    RestConstants.ACCESS_WRITE,
                    NodesRightMode.Original,
                ))
        ) {
            return { accept: false };
        }

        if (
            !this.nodeHelper.getNodesRight(
                [dragData.target],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            )
        ) {
            return { accept: false };
        }
        return { accept: true };
    };

    canDropOnRef(dragData: DragData): CanDrop {
        // do not allow to drop here
        return { accept: false };
    }

    collectionDelete(): void {
        this.dialogTitle = 'COLLECTIONS.CONFIRM_DELETE';
        this.dialogMessage = 'COLLECTIONS.CONFIRM_DELETE_INFO';
        this.dialogCancelable = true;
        this.dialogButtons = DialogButton.getYesNo(
            () => this.closeDialog(),
            () => {
                this.isLoading = true;
                this.closeDialog();
                this.collectionService
                    .deleteCollection(
                        this.collectionContent.node.ref.id,
                        this.collectionContent.node.ref.repo,
                    )
                    .subscribe(
                        (result) => {
                            this.isLoading = false;
                            this.navigate(this.parentCollectionId.id);
                        },
                        (error) => {
                            this.isLoading = false;
                            this.toast.error(null, 'COLLECTIONS.ERROR_DELETE');
                        },
                    );
            },
        );
    }

    collectionEdit(): void {
        if (this.isAllowedToEditCollection()) {
            this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'edit',
                    this.collectionContent.node.ref.id,
                ],
                { queryParams: { mainnav: this.mainnav } },
            );
            return;
        }
    }

    // gets called by user if something went wrong to start fresh from beginning
    resetCollections(): void {
        let url = window.location.href;
        url = url.substring(0, url.indexOf('collections') + 11);
        window.location.href = url;
        return;
    }

    refreshContent(callback: () => void = null): void {
        if (!this.isReady) {
            return;
        }
        this.dataSourceCollectionProposals.reset();
        this.isLoading = true;
        if (!this.loadingTask.isDone) {
            this.loadingTask.done();
        }

        // set correct scope
        const request: RequestObject = Helper.deepCopy(CollectionsMainComponent.DEFAULT_REQUEST);
        if (this.sortCollections) {
            request.sortBy = [this.sortCollections.active];
            request.sortAscending = [this.sortCollections.direction === 'asc'];
        } else {
            console.warn('Sort for collections is not defined in the mds!');
        }
        // when loading child collections, we load all of them
        if (!this.isRootLevelCollection()) {
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
                this.collectionContent.node.ref.id,
                this.getScope(),
                [],
                request,
                this.collectionContent.node.ref.repo,
            )
            .subscribe(
                (collection) => {
                    // transfere sub collections and content
                    this.dataSourceCollections.setData(
                        collection.collections,
                        collection.pagination,
                    );
                    if (this.isRootLevelCollection()) {
                        this.finishCollectionLoading(callback);
                        return;
                    }
                    if (this.isAllowedToEditCollection()) {
                        this.refreshProposals();
                    }
                    const requestRefs = this.getReferencesRequest();
                    requestRefs.count = null;
                    this.collectionService
                        .getCollectionReferences(
                            this.collectionContent.node.ref.id,
                            CollectionsMainComponent.PROPERTY_FILTER,
                            requestRefs,
                            this.collectionContent.node.ref.repo,
                        )
                        .subscribe((refs) => {
                            this.dataSourceReferences.setData(refs.references, refs.pagination);
                            this.finishCollectionLoading(callback);
                        });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }

    async loadMoreReferences(loadAll = false) {
        if (!(await this.dataSourceReferences.hasMore()) || this.dataSourceReferences.isLoading) {
            return;
        }
        const request = this.getReferencesRequest();
        request.offset = (await this.dataSourceReferences.getData()).length;
        if (loadAll) {
            request.count = RestConstants.COUNT_UNLIMITED;
        }
        this.dataSourceReferences.isLoading = true;
        this.collectionService
            .getCollectionReferences(
                this.collectionContent.node.ref.id,
                CollectionsMainComponent.PROPERTY_FILTER,
                request,
                this.collectionContent.node.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceReferences.appendData(refs.references);
                this.dataSourceReferences.isLoading = false;
            });
    }

    async loadMoreCollections() {
        if (!(await this.dataSourceCollections.hasMore()) || this.dataSourceCollections.isLoading) {
            return;
        }
        const request: any = Helper.deepCopy(CollectionsMainComponent.DEFAULT_REQUEST);
        request.offset = (await this.dataSourceCollections.getData()).length;
        this.dataSourceCollections.isLoading = true;
        this.collectionService
            .getCollectionSubcollections(
                this.collectionContent.node.ref.id,
                this.getScope(),
                [],
                request,
                this.collectionContent.node.ref.repo,
            )
            .subscribe((refs) => {
                this.dataSourceCollections.appendData(refs.collections);
                this.dataSourceCollections.isLoading = false;
            });
    }

    getScope() {
        return this.tabSelected ? this.tabSelected : RestConstants.COLLECTIONSCOPE_ALL;
    }

    onCreateCollection() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            this.router.navigate(
                [
                    UIConstants.ROUTER_PREFIX + 'collections/collection',
                    'new',
                    this.collectionContent.node.ref.id,
                ],
                { queryParams: params },
            );
        });
    }

    onCollectionsClick(collection: Node): void {
        // remember actual collection as breadcrumb
        if (!this.isRootLevelCollection()) {
            this.parentCollectionId = this.collectionContent.node.ref;
        }

        // set thru router so that browser back button can work
        this.navigate(collection.ref.id);
    }

    deleteReference(content: EduData.CollectionReference | EduData.Node) {
        this.contentDetailObject = content;
        this.deleteFromCollection();
    }

    canDelete(node: EduData.CollectionReference) {
        return RestHelper.hasAccessPermission(node, 'Delete');
    }

    onContentClick(content: any, force = false): void {
        this.contentDetailObject = content;
        if (content.originalId == null && !force) {
            this.dialogTitle = 'COLLECTIONS.ORIGINAL_MISSING';
            this.dialogMessage = 'COLLECTIONS.ORIGINAL_MISSING_INFO';
            this.dialogCancelable = true;
            this.dialogButtons = [];
            if (this.isAllowedToDeleteNodes([content])) {
                this.dialogButtons.push(
                    new DialogButton('OPTIONS.REMOVE_REF', { color: 'standard' }, () =>
                        this.deleteFromCollection(() => this.closeDialog()),
                    ),
                );
            }
            this.dialogButtons.push(
                new DialogButton('COLLECTIONS.OPEN_MISSING', { color: 'primary' }, () =>
                    this.onContentClick(content, true),
                ),
            );
            return;
        }
        this.nodeService.getNodeMetadata(content.ref.id).subscribe((data: NodeWrapper) => {
            this.contentDetailObject = data.node;

            // remember the scroll Y before displaying content
            this.lastScrollY = window.scrollY;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', content.ref.id]);
        });
    }

    contentDetailBack(event: any): void {
        // scroll to last Y
        window.scrollTo(0, this.lastScrollY);

        this.navigate(this.collectionContent.node.ref.id);
        // refresh content if signaled
        if (event.refresh) {
            this.refreshContent();
        }
    }

    refreshAll() {
        this.displayCollectionById(this.collectionContent.node.ref.id);
    }

    displayCollectionById(id: string, callback: () => void = null): void {
        this.parentCollectionId = new EduData.Reference(
            RestConstants.HOME_REPOSITORY,
            RestConstants.ROOT,
        );
        if (id == null) {
            id = RestConstants.ROOT;
        }
        this.createSubCollectionOptionItem.name =
            'OPTIONS.' + (this.isRootLevelCollection() ? 'NEW_COLLECTION' : 'NEW_SUB_COLLECTION');
        if (id == RestConstants.ROOT) {
            // display root collections with tabs
            this.sortCollections.active = RestConstants.CM_MODIFIED_DATE;
            this.sortCollections.direction = 'desc';
            this.setCollectionId(RestConstants.ROOT);
            this.refreshContent(callback);
        } else {
            // load metadata of collection
            this.isLoading = true;

            this.collectionService.getCollection(id).subscribe(
                ({ collection }) => {
                    // set the collection and load content data by refresh
                    this.setCollectionId(null);
                    const orderCollections =
                        collection.properties[
                            RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE
                        ];
                    this.sortCollections.active =
                        orderCollections?.[0] || RestConstants.CM_MODIFIED_DATE;
                    this.sortCollections.direction =
                        orderCollections?.[0] === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                            ? 'asc'
                            : orderCollections?.[1] === 'true'
                            ? 'asc'
                            : 'desc';

                    const refMode = collection.collection.orderMode;
                    const refAscending = collection.collection.orderAscending;
                    // cast old order mode to new parameter
                    this.sortReferences.active = ((refMode ===
                    RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                        ? RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
                        : refMode) || RestConstants.CM_MODIFIED_DATE) as any;
                    this.sortReferences.direction = RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                        ? 'asc'
                        : refAscending
                        ? 'asc'
                        : 'desc';
                    this.collectionContent.node = collection;
                    this.mainNavUpdateTrigger.next();

                    this.renderBreadcrumbs();
                    this.setOptionsCollection();
                    this.refreshContent(callback);
                    if (this.feedbackAllowed() && this.params.feedback === 'true') {
                        this.mainNavService.getDialogs().materialWriteFeedback = collection;
                    }
                    if (
                        this.collectionContent.node.access.indexOf(
                            RestConstants.ACCESS_CHANGE_PERMISSIONS,
                        ) !== -1
                    ) {
                        this.nodeService.getNodePermissions(id).subscribe((permissions) => {
                            this.permissions =
                                permissions.permissions.localPermissions.permissions.concat(
                                    permissions.permissions.inheritedPermissions,
                                );
                        });
                    }
                },
                (error) => {
                    if (id != RestConstants.ROOT) {
                        this.navigate();
                    }
                    if (error.status == 404) {
                        this.toast.error(null, 'COLLECTIONS.ERROR_NOT_FOUND');
                    } else {
                        this.toast.error(error);
                    }
                    this.isLoading = false;
                    if (!this.loadingTask.isDone) {
                        this.loadingTask.done();
                    }
                },
            );
        }
    }

    closeDialog() {
        this.dialogTitle = null;
    }

    showTabs() {
        return this.isRootLevelCollection() && (!this.isGuest || this.hasEditorial);
    }

    getCollectionViewType() {
        // on mobile, we will always show the small collection list
        if (
            UIHelper.evaluateMediaQuery(UIConstants.MEDIA_QUERY_MAX_WIDTH, UIConstants.MOBILE_WIDTH)
        ) {
            return ListTableComponent.VIEW_TYPE_GRID_SMALL;
        }
        return this.isRootLevelCollection()
            ? ListTableComponent.VIEW_TYPE_GRID
            : ListTableComponent.VIEW_TYPE_GRID_SMALL;
    }

    hasNonIconPreview(): boolean {
        const preview = this.collectionContent?.node?.preview;
        return preview && !preview.isIcon;
    }

    private renderBreadcrumbs() {
        this.path = [];
        this.nodeService
            .getNodeParents(this.collectionContent.node.ref.id, false)
            .subscribe((data: EduData.NodeList) => {
                this.path = data.nodes.reverse();
                if (this.path.length > 1) {
                    this.parentCollectionId = new EduData.Reference(
                        this.path[this.path.length - 2].ref.repo,
                        this.path[this.path.length - 2].ref.id,
                    );
                }
            });
    }

    private initialize() {
        this.optionsService.clearComponents(this.actionbarReferences);

        // load user profile
        this.iamService.getCurrentUserAsync().then(
            (iamUser) => {
                // WIN

                this.person = iamUser.person;

                // set app to ready state
                this.isReady = true;
                // subscribe to parameters of url
                this.route.queryParams.subscribe((params) => {
                    const diffs = Helper.getDifferentKeys(this.params, params);
                    if (Object.keys(diffs).length === 1 && diffs.viewType) {
                        this.params = params;
                        this.viewTypeNodes = diffs.viewType;
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

                    this.infoTitle = null;
                    // get id from route and validate input data
                    let id = params.id || '-root-';
                    if (id == ':id') {
                        id = '-root-';
                    }
                    if (id == '') {
                        id = '-root-';
                    }
                    if (params.addToOther) {
                        this.nodeService
                            .getNodeMetadata(params.addToOther)
                            .subscribe((data: EduData.NodeWrapper) => {
                                this.addToOther = [data.node];
                            });
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
                    this.showCollection = id !== '-root-';
                    this.displayCollectionById(id, async () => {
                        if (params.content) {
                            for (const content of await this.dataSourceReferences.getData()) {
                                if (content.ref.id === params.content) {
                                    this.contentDetailObject = content;
                                    break;
                                }
                            }
                        }
                        this.frame.broadcastEvent(FrameEventsService.EVENT_INVALIDATE_HEIGHT);
                    });
                    this.mainNavUpdateTrigger.next();
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

    private deleteFromCollection(callback: Function = null) {
        this.toast.showProgressDialog();
        this.collectionService
            .removeFromCollection(
                this.contentDetailObject.ref.id,
                this.collectionContent.node.ref.id,
            )
            .subscribe(
                () => {
                    this.afterDeleteFromCollection([this.contentDetailObject]);
                    this.toast.closeModalDialog();
                    if (callback) {
                        callback();
                    }
                },
                (error: any) => {
                    this.toast.closeModalDialog();
                    this.toast.error(error);
                },
            );
    }

    afterDeleteFromCollection(nodes: Node[]) {
        if (nodes?.some((n) => !(n as ProposalNode).proposal)) {
            this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
        }
        this.refreshContent();
    }

    private deleteMultiple(nodes: Node[], position = 0, error = false) {
        if (position == nodes.length) {
            if (!error) {
                this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
            }
            this.toast.closeModalDialog();
            this.refreshContent();
            return;
        }
        this.toast.showProgressDialog();
        this.collectionService
            .removeFromCollection(nodes[position].ref.id, this.collectionContent.node.ref.id)
            .subscribe(
                () => {
                    this.deleteMultiple(nodes, position + 1, error);
                },
                (error: any) => {
                    this.toast.error(error);
                    this.deleteMultiple(nodes, position + 1, true);
                },
            );
    }

    private downloadMaterial() {
        window.open(this.contentDetailObject.downloadUrl);
    }

    private addToOtherCollection(node: EduData.Node) {
        this.navigate(this.collectionContent.node.ref.id, node.ref.id);
    }

    private handleError(error: any) {
        if (error.status == RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.toast.error(null, 'COLLECTIONS.ERROR_NODE_EXISTS');
        } else {
            this.toast.error(error);
        }
    }

    private async changeReferencesOrder() {
        this.toast.showProgressDialog();
        this.collectionService
            .setOrder(
                this.collectionContent.node.ref.id,
                RestHelper.getNodeIds(await this.dataSourceReferences.getData()),
            )
            .subscribe(
                () => {
                    this.collectionContentOriginal = Helper.deepCopy(this.collectionContent);
                    this.infoTitle = null;
                    this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
                    this.toast.closeModalDialog();
                },
                (error: any) => {
                    this.toast.closeModalDialog();
                    this.toast.error(error);
                },
            );
    }

    private async changeCollectionsOrder() {
        this.toast.showProgressDialog();
        this.collectionService
            .setOrder(
                this.collectionContent.node.ref.id,
                RestHelper.getNodeIds(await this.dataSourceCollections.getData()),
            )
            .subscribe(
                () => {
                    this.collectionContentOriginal = Helper.deepCopy(this.collectionContent);
                    this.infoTitle = null;
                    this.sortCollections.customSortingInProgress = false;
                    this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_CUSTOM');
                    this.toast.closeModalDialog();
                },
                (error: any) => {
                    this.toast.closeModalDialog();
                    this.toast.error(error);
                },
            );
    }

    private isAllowedToDeleteNodes(nodes: Node[]) {
        return (
            this.isAllowedToDeleteCollection() ||
            this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)
        );
    }

    private collectionPermissions() {
        this._collectionShare = this.collectionContent.node;
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [this.collectionContent.node],
        });
        this.optionsService.initComponents(this.actionbarCollection, this.listReferences);
        this.optionsService.nodesDeleted
            .pipe(takeUntil(this.destroyed))
            .subscribe(({ objects }) => {
                if (
                    objects.length === 1 &&
                    objects[0].ref.id === this.collectionContent.node.ref.id
                ) {
                    // use set timeout to allow router navigate to work
                    const id = this.parentCollectionId.id;
                    setTimeout(() => this.navigate(id));
                }
            });
        this.optionsService.refreshComponents();
    }

    private setCollectionId(id: string) {
        this.dataSourceCollections.reset();
        this.dataSourceReferences.reset();
        this.collectionContent = {
            node: new Node(),
        };
        this.collectionContent.node.ref = {} as NodeRef;
        this.collectionContent.node.ref.id = id;
        this.collectionContent.node.aspects = [RestConstants.CCM_ASPECT_COLLECTION];
        this.mainNavUpdateTrigger.next();
    }

    private getCollectionId() {
        const c = this.collectionContent.node;
        return c != null && c.ref != null ? c.ref.id : null;
    }

    private finishCollectionLoading(callback?: () => void) {
        this.collectionContentOriginal = Helper.deepCopy(this.collectionContent);
        this.mainNavService.getMainNav()?.refreshBanner();

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
        this.isLoading = false;
        if (callback) {
            callback();
        }
        setTimeout(() => {
            this.setOptionsCollection();
            this.listReferences?.initOptionsGenerator({
                scope: Scope.CollectionsReferences,
                actionbar: this.actionbarReferences,
                parent: this.collectionContent.node,
            });
        });
    }

    async setCollectionSort(sort: ListSortConfig) {
        this.sortCollections = sort;
        try {
            await this.nodeService
                .editNodeProperty(
                    this.collectionContent.node.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE,
                    [this.sortCollections.active, (this.sortCollections.direction === 'asc') + ''],
                )
                .toPromise();
        } catch (e) {
            this.toast.error(e);
        }
        this.refreshContent();
        if (sort.active !== RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION) {
            this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_TYPE', {
                type: this.translationService.instant('NODE.' + sort.active),
            });
        }
        this.sortCollections.customSortingInProgress =
            this.sortCollections.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION;
        if (this.sortCollections.customSortingInProgress) {
            this.toggleCollectionsOrder();
        }
    }

    async setReferenceSort(sort: ListSortConfig) {
        const diff = Helper.getKeysWithDifferentValues(this.sortReferences, sort);
        this.sortReferences = sort;
        // auto activate the custom sorting when the users switches to "custom order"
        if (diff.includes('active')) {
            this.sortReferences.customSortingInProgress =
                this.sortReferences.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION;
        }
        this.toggleReferencesOrder();
        if (this.sortReferences.customSortingInProgress) {
            await this.loadMoreReferences(true);
        }
        if (diff.includes('customSortingInProgress') && sort.customSortingInProgress) {
            return;
        }

        try {
            await this.nodeService
                .editNodeProperty(
                    this.collectionContent.node.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_ORDER_MODE,
                    [sort.active, (sort.direction === 'asc') + ''],
                )
                .toPromise();
        } catch (e) {
            this.toast.error(e);
        }
        this.refreshContent();
    }

    private getReferencesRequest(): RequestObject {
        return {
            sortBy: [this.sortReferences.active],
            sortAscending: [this.sortReferences.direction === 'asc'],
        };
    }

    createAllowed(): boolean {
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
            // for anything else, the user must be able to invite everyone
            allowed =
                allowed &&
                this.connector.hasToolPermissionInstant(
                    RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                );
            if (this.tabSelected === RestConstants.COLLECTIONSCOPE_ORGA) {
                allowed = false;
            } else if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                allowed = allowed && this.adminMediacenters?.length === 1;
            } else if (this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                allowed =
                    allowed &&
                    this.connector.hasToolPermissionInstant(
                        RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
                    );
            }
            return allowed;
        } else {
            return this.isAllowedToEditCollection();
        }
    }

    private refreshProposals() {
        this.dataSourceCollectionProposals.reset();
        this.dataSourceCollectionProposals.isLoading = true;
        if (this.isAllowedToEditCollection()) {
            this.collectionService
                .getCollectionProposals(this.collectionContent.node.ref.id)
                .subscribe((proposals) => {
                    proposals.nodes = proposals.nodes.map((p) => {
                        p.proposalCollection = this.collectionContent.node;
                        return p;
                    });
                    this.dataSourceCollectionProposals.setData(
                        proposals.nodes,
                        proposals.pagination,
                    );
                    this.dataSourceCollectionProposals.isLoading = false;
                    setTimeout(() => {
                        this.listProposals?.initOptionsGenerator({
                            scope: Scope.CollectionsProposals,
                        });
                    });
                });
        }
    }

    isDeleted(node: CollectionReference) {
        return (
            node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE) &&
            !node.aspects.includes(RestConstants.CCM_ASPECT_REMOTEREPOSITORY) &&
            !node.originalId
        );
    }
}

export interface SortInfo {
    name: 'cm:name' | 'cm:modified' | 'ccm:collection_ordered_position';
    ascending: boolean;
    userModifyActive?: boolean;
}
