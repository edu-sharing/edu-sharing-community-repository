import {Component, ContentChild, ElementRef, TemplateRef, ViewChild} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Translation } from '../../core-ui-module/translation';
import * as EduData from '../../core-module/core.module';
import {
    Collection,
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    ListItem,
    LoginResult,
    MdsMetadataset,
    Node,
    NodeRef,
    NodeWrapper,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMdsService,
    RestNodeService,
    RestOrganizationService,
    SessionStorageService,
    TemporaryStorageService,
    UIService,
    CollectionReference,
    CollectionFeedback,
    NodesRightMode,
    Permission,
    MdsMetadatasets,
    ConfigurationHelper,
    RestNetworkService,
    SortDefault,
    RequestObject,
    RestMediacenterService, Mediacenter,
} from '../../core-module/core.module';
import { Toast } from '../../core-ui-module/toast';
import {DefaultGroups, OptionItem, Scope} from '../../core-ui-module/option-item';
import { NodeRenderComponent } from '../../common/ui/node-render/node-render.component';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { ListTableComponent } from '../../core-ui-module/components/list-table/list-table.component';
import {NodeHelperService} from '../../core-ui-module/node-helper.service';
import { TranslateService } from '@ngx-translate/core';
import { Location } from '@angular/common';
import { Helper } from '../../core-module/rest/helper';
import { MainNavComponent } from '../../common/ui/main-nav/main-nav.component';
import {ColorHelper, PreferredColor} from '../../core-module/ui/color-helper';
import { ActionbarHelperService } from '../../common/services/actionbar-helper';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { HttpClient } from '@angular/common/http';
import { GlobalContainerComponent } from '../../common/ui/global-container/global-container.component';
import { Observable } from 'rxjs';
import {OPTIONS_HELPER_CONFIG, OptionsHelperService} from '../../core-ui-module/options-helper.service';
import {ActionbarComponent} from '../../common/ui/actionbar/actionbar.component';
import {DropAction, DropData} from '../../core-ui-module/directives/drag-nodes/drag-nodes';
import { SkipTarget } from '../../common/ui/skip-nav/skip-nav.service';

// component class
@Component({
    selector: 'app-collections',
    templateUrl: 'collections.component.html',
    styleUrls: ['collections.component.scss'],
    // provide a new instance so to not get conflicts with other service instances
    providers: [OptionsHelperService, {provide: OPTIONS_HELPER_CONFIG, useValue: {
        subscribeEvents: false
    }}]})
export class CollectionsMainComponent {
    static INDEX_MAPPING = [
        RestConstants.COLLECTIONSCOPE_MY,
        RestConstants.COLLECTIONSCOPE_ORGA,
        RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
        RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
        RestConstants.COLLECTIONSCOPE_ALL,
    ];
    private static PROPERTY_FILTER = [
        RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
    ];
    private static DEFAULT_REQUEST = {
        sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE,
        ],
        sortAscending: [false, true, false],
    };
    readonly SCOPES = Scope;
    readonly SkipTarget = SkipTarget;

    @ViewChild('mainNav') mainNavRef: MainNavComponent;
    @ViewChild('actionbarCollection') actionbarCollection: ActionbarComponent;
    @ViewChild('actionbarReferences') actionbarReferences: ActionbarComponent;
    @ViewChild('listCollections')
    listCollections: ListTableComponent;
    @ContentChild('collectionContentTemplate') collectionContentTemplateRef: TemplateRef<any>;


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
        collections: Node[];
        references: EduData.CollectionReference[];
        collectionsPagination?: EduData.Pagination;
        referencesPagination?: EduData.Pagination;
        referencesLoading?: boolean;
        collectionsLoading?: boolean;
    };
    mainnav = true;
    isGuest = true;
    addToOther: EduData.Node[];
    addPinning: string;
    infoTitle: string;
    infoMessage: string;
    infoButtons: DialogButton[];
    infoClose: Function;
    nodeReport: Node;
    collectionsColumns: ListItem[] = [];
    referencesColumns: ListItem[] = [];
    createSubCollectionOptionItem = new OptionItem(
        'OPTIONS.NEW_COLLECTION',
        'layers',
        () => this.onCreateCollection(),
    );
    addMaterialSearchOptionItem = new OptionItem(
        'OPTIONS.SEARCH_OBJECT',
        'redo',
        () => {
            UIHelper.getCommonParameters(this.route).subscribe(params => {
                params.addToCollection = this.collectionContent.node.ref.id;
                this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                    queryParams: params,
                });
            });
        }
    );
    addMaterialBinaryOptionItem = new OptionItem(
        'OPTIONS.ADD_OBJECT',
        'cloud_upload',
        () => {
            this.mainNavRef.createMenu.showUploadSelect = true
        },
    );
    optionsMaterials: OptionItem[];
    tutorialElement: ElementRef;
    permissions: Permission[];
    private sortCollections: SortDefault;
    // FIXME: `collectionShare` is expected to be of type `Node[]` by `workspace-management` but is
    // of type `Node` here.
    private adminMediacenters: Mediacenter[];
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
        let pos = CollectionsMainComponent.INDEX_MAPPING.indexOf(
            this.tabSelected,
        );
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
    set orderActive(orderActive: boolean) {
        this._orderActive = orderActive;
        this.collectionContent.node.collection.orderMode = orderActive
            ? RestConstants.COLLECTION_ORDER_MODE_CUSTOM
            : null;

        if (this._orderActive) {
            this.infoTitle = 'COLLECTIONS.ORDER_ELEMENTS';
            this.infoMessage = 'COLLECTIONS.ORDER_ELEMENTS_INFO';
            this.infoButtons = DialogButton.getSingleButton('SAVE', () => {
                this.changeOrder();
            });
            this.infoClose = () => {
                this.orderActive = false;
            };
            this.loadMoreReferences(true);
        } else {
            this.infoTitle = null;
            // this.collectionContent.references=Helper.deepCopy(this.collectionContentOriginal.references);
            this.refreshAll();
        }
    }
    get orderActive() {
        return this._orderActive;
    }

    private collectionContentOriginal: any;
    private filteredOutCollections: Array<EduData.Collection> = new Array<
        EduData.Collection
    >();
    private filteredOutReferences: Array<
        EduData.CollectionReference
    > = new Array<EduData.CollectionReference>();
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
    private _orderActive: boolean;
    reurl: any;
    private _collectionShare: Node;
    private feedbacks: CollectionFeedback[];
    private params: Params;

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
        private networkService: RestNetworkService,
        private organizationService: RestOrganizationService,
        private iamService: RestIamService,
        private mdsService: RestMdsService,
        private actionbar: ActionbarHelperService,
        private storage: SessionStorageService,
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
    ) {
        this.collectionsColumns.push(new ListItem('COLLECTION', 'title'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'info'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'scope'));
        this.setCollectionId(RestConstants.ROOT);
        Translation.initialize(
            this.translationService,
            this.config,
            this.storage,
            this.route,
        ).subscribe(() => {
            this.connector.isLoggedIn().subscribe(
                (data: LoginResult) => {
                    if (data.isValidLogin && data.currentScope == null) {
                        this.addMaterialBinaryOptionItem.isEnabled = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES);
                        this.createSubCollectionOptionItem.isEnabled = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS);
                        this.isGuest = data.isGuest;
                        this.mediacenterService.getMediacenters().subscribe((mediacenters) => {
                            this.adminMediacenters = mediacenters.filter((m)=>m.administrationAccess);
                        });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                            )
                            .subscribe(data => {
                                this.hasEditorial = data.collections.length > 0;
                            });
                        this.collectionService
                            .getCollectionSubcollections(
                                RestConstants.ROOT,
                                RestConstants.COLLECTIONSCOPE_TYPE_MEDIA_CENTER,
                            )
                            .subscribe(data => {
                                this.hasMediacenter =
                                    data.collections.length > 0;
                            });
                        this.mdsService.getSets().subscribe(
                            (data: MdsMetadatasets) => {
                                const mdsSets = ConfigurationHelper.filterValidMds(RestConstants.HOME_REPOSITORY,
                                    data.metadatasets,
                                    this.config,
                                );
                                this.mdsService.getSet(mdsSets[0].id).subscribe((mds) => {
                                    this.referencesColumns = MdsHelper.getColumns(
                                        this.translationService,
                                        mds,
                                        'collectionReferences',
                                    );
                                    const info = MdsHelper.getSortInfo(mds, 'collections');
                                    this.sortCollections = info.default;
                                    this.initialize();
                                });
                            },(e) => {
                                console.warn(e);
                                this.initialize();
                            });
                    } else {
                        RestHelper.goToLogin(this.router, this.config);
                    }
                },
                (error: any) => RestHelper.goToLogin(this.router, this.config),
            );
        });
    }

    isMobile() {
        return this.uiService.isMobile();
    }

    isMobileWidth() {
        return window.innerWidth < UIConstants.MOBILE_WIDTH;
    }

    setCustomOrder(event: MatSlideToggleChange) {
        const checked = event.checked;
        this.collectionContent.node.collection.orderMode = checked
            ? RestConstants.COLLECTION_ORDER_MODE_CUSTOM
            : null;
        if (checked) {
            this.orderActive = true;
        } else {
            this.toast.showProgressDialog();
            this.collectionService
                .setOrder(this.collectionContent.node.ref.id)
                .subscribe(() => {
                    this.toast.closeModalDialog();
                    this.orderActive = false;
                });
        }
    }

    navigate(id = '', addToOther = '', feedback = false) {
        const params: any = {};
        UIHelper.getCommonParameters(this.route).subscribe(params => {
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
        if (
            this.tabSelected != tab ||
            this.getCollectionId() != RestConstants.ROOT
        ) {
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
        UIHelper.getCommonParameters(this.route).subscribe(params => {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    isBrightColor() {
        return (
            ColorHelper.getPreferredColor(this.collectionContent?.node?.collection?.color) === PreferredColor.White
        );
    }

    getScopeInfo() {
        return this.nodeHelper.getCollectionScopeInfo(this.collectionContent.node);
    }
    dropOnCollection(event: any) {
        const target = event.target;
        const source = event.source[0];
        this.toast.showProgressDialog();
        if (source.mediatype === 'collection') {
            if (event.type === 'copy') {
                this.toast.error(null, 'INVALID_OPERATION');
                this.toast.closeModalDialog();
                return;
            }
            this.nodeService.moveNode(target.ref.id, source.ref.id).subscribe(
                () => {
                    this.toast.closeModalDialog();
                    this.refreshContent();
                },
                error => {
                    this.handleError(error);
                    this.toast.closeModalDialog();
                },
            );
        } else {
            UIHelper.addToCollection(
                this.nodeHelper,
                this.collectionService,
                this.router,
                this.bridge,
                event.target,
                event.source,
                nodes => {
                    if (event.type === 'copy') {
                        this.toast.closeModalDialog();
                        this.refreshContent();
                        return;
                    }
                    if (event.source.length === nodes.length) {
                        const observables = event.source.map((n: any) =>
                            this.collectionService.removeFromCollection(
                                n.ref.id,
                                this.collectionContent.node.ref.id,
                            ),
                        );
                        Observable.forkJoin(observables).subscribe(
                            () => {
                                this.toast.closeModalDialog();
                                this.refreshContent();
                            },
                            error => {
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
    }
    addNodesToCollection(nodes: Node[]) {
        this.toast.showProgressDialog();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            this.collectionContent.node,
            nodes,
            refNodes => {
                this.refreshContent();
                this.toast.closeModalDialog();
            },
        );
    }

    canDropOnCollection = (event: DropData) => {
        if (event.nodes[0].ref.id === event.target.ref.id) {
            return false;
        }
        if (event.target.ref.id === this.collectionContent.node.ref.id) {
            return false;
        }
        console.log(event.nodes[0], event.dropAction);
        if(event.nodes[0].collection && event.dropAction === 'copy') {
            return false;
        }
        // do not allow to move anything else than editorial collections into editorial collections (if the source is a collection)
        if (event.nodes[0].collection?.hasOwnProperty('childCollectionsCount')) {
            if (
                (event.nodes[0].collection.type ===
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    event.target.collection.type !==
                        RestConstants.COLLECTIONTYPE_EDITORIAL) ||
                (event.nodes[0].collection.type !==
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    event.target.collection.type ===
                        RestConstants.COLLECTIONTYPE_EDITORIAL)
            ) {
                return false;
            }
        }
        if (
            event.dropAction === 'copy' &&
            !this.nodeHelper.getNodesRight(
                event.nodes,
                RestConstants.ACCESS_CC_PUBLISH,
                NodesRightMode.Original,
            )
            || event.dropAction === 'move' &&
            !this.nodeHelper.getNodesRight(
                event.nodes,
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Original,
            )
        ) {
            return false;
        }

        if (
            !this.nodeHelper.getNodesRight(
                [event.target],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            )
        ) {
            return false;
        }

        return true;
    };

    canDropOnRef(event: DropData) {
        // do not allow to drop here
        return false;
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
                        result => {
                            this.isLoading = false;
                            this.navigate(this.parentCollectionId.id);
                        },
                        error => {
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
        this.isLoading = true;
        GlobalContainerComponent.finishPreloading();

        // set correct scope
        const request: RequestObject = Helper.deepCopy(
            CollectionsMainComponent.DEFAULT_REQUEST,
        );
        if(this.sortCollections) {
            request.sortBy = [this.sortCollections.sortBy];
            request.sortAscending = [this.sortCollections.sortAscending];
        } else {
            console.warn('Sort for collections is not defined in the mds!');
        }
        // when loading child collections, we load all of them
        if (!this.isRootLevelCollection()) {
            request.count = RestConstants.COUNT_UNLIMITED;
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
                collection => {
                    // transfere sub collections and content
                    this.collectionContent.collections = collection.collections;
                    this.collectionContent.collectionsPagination =
                        collection.pagination;
                    if (this.isRootLevelCollection()) {
                        this.finishCollectionLoading(callback);
                        return;
                    }
                    const requestRefs = Helper.deepCopy(CollectionsMainComponent.DEFAULT_REQUEST);
                    requestRefs.count = null;
                    this.collectionService
                        .getCollectionReferences(
                            this.collectionContent.node.ref.id,
                            CollectionsMainComponent.PROPERTY_FILTER,
                            requestRefs,
                            this.collectionContent.node.ref.repo,
                        )
                        .subscribe(refs => {
                            this.collectionContent.references = refs.references;
                            this.collectionContent.referencesPagination =
                                refs.pagination;
                            this.finishCollectionLoading(callback);
                        });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }

    loadMoreReferences(loadAll = false) {
        if (
            this.collectionContent.references.length ==
            this.collectionContent.referencesPagination.total
        ) {
            return;
        }
        if (this.collectionContent.referencesLoading) {
            return;
        }
        const request: any = Helper.deepCopy(
            CollectionsMainComponent.DEFAULT_REQUEST,
        );
        request.offset = this.collectionContent.references.length;
        if (loadAll) {
            request.count = RestConstants.COUNT_UNLIMITED;
        }
        this.collectionContent.referencesLoading = true;
        this.collectionService
            .getCollectionReferences(
                this.collectionContent.node.ref.id,
                CollectionsMainComponent.PROPERTY_FILTER,
                request,
                this.collectionContent.node.ref.repo,
            )
            .subscribe(refs => {
                this.collectionContent.references = this.collectionContent.references.concat(
                    refs.references,
                );
                this.collectionContent.referencesLoading = false;
            });
    }

    loadMoreCollections() {
        if (
            this.collectionContent.collections.length ==
            this.collectionContent.collectionsPagination.total
        ) {
            return;
        }
        if (this.collectionContent.collectionsLoading) {
            return;
        }
        const request: any = Helper.deepCopy(
            CollectionsMainComponent.DEFAULT_REQUEST,
        );
        request.offset = this.collectionContent.collections.length;
        this.collectionContent.collectionsLoading = true;
        this.collectionService
            .getCollectionSubcollections(
                this.collectionContent.node.ref.id,
                this.getScope(),
                [],
                request,
                this.collectionContent.node.ref.repo,
            )
            .subscribe(refs => {
                this.collectionContent.collections = this.collectionContent.collections.concat(
                    refs.collections,
                );
                this.collectionContent.collectionsLoading = false;
            });
    }

    getScope() {
        return this.tabSelected
            ? this.tabSelected
            : RestConstants.COLLECTIONSCOPE_ALL;
    }

    onCreateCollection() {
        UIHelper.getCommonParameters(this.route).subscribe(params => {
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
                    new DialogButton(
                        'OPTIONS.REMOVE_REF',
                        DialogButton.TYPE_CANCEL,
                        () =>
                            this.deleteFromCollection(() => this.closeDialog()),
                    ),
                );
            }
            this.dialogButtons.push(
                new DialogButton(
                    'COLLECTIONS.OPEN_MISSING',
                    DialogButton.TYPE_PRIMARY,
                    () => this.onContentClick(content, true),
                ),
            );
            return;
        }
        this.nodeService
            .getNodeMetadata(content.ref.id)
            .subscribe((data: NodeWrapper) => {
                this.contentDetailObject = data.node;

                // remember the scroll Y before displaying content
                this.lastScrollY = window.scrollY;
                this.router.navigate([
                    UIConstants.ROUTER_PREFIX + 'render',
                    content.ref.id,
                ]);
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
        if (id == null) {
            id = RestConstants.ROOT;
        }
        if (id == '-root-') {
            // display root collections with tabs
            this.setCollectionId(RestConstants.ROOT);
            this.refreshContent(callback);
        } else {
            // load metadata of collection
            this.isLoading = true;

            this.collectionService.getCollection(id).subscribe(
                collection => {
                    // set the collection and load content data by refresh
                    this.setCollectionId(null);
                    this.collectionContent.node = collection.collection;

                    this.renderBreadcrumbs();

                    this.refreshContent(callback);
                    if(this.feedbackAllowed() && this.params.feedback === 'true') {
                        this.mainNavRef.management.collectionWriteFeedback = collection.collection;
                        this.mainNavRef.management.collectionWriteFeedbackChange.first().subscribe(() => {
                            if(this.params.feedbackClose === 'true') {
                                window.close();
                            }
                        })
                    }
                    if(this.collectionContent.node.access.indexOf(RestConstants.ACCESS_CHANGE_PERMISSIONS) !== -1) {
                        this.nodeService.getNodePermissions(id).subscribe((permissions) => {
                            this.permissions = permissions.permissions.localPermissions.permissions.
                                                            concat(permissions.permissions.inheritedPermissions);
                        });
                    }
                },
                error => {
                    if (id != '-root-') {
                        this.navigate();
                    }
                    if (error.status == 404) {
                        this.toast.error(null, 'COLLECTIONS.ERROR_NOT_FOUND');
                    } else {
                        this.toast.error(error);
                    }
                    this.isLoading = false;
                    GlobalContainerComponent.finishPreloading();
                },
            );
        }
        this.createSubCollectionOptionItem.name = 'OPTIONS.' +
            (this.isRootLevelCollection() ? 'NEW_COLLECTION' : 'NEW_SUB_COLLECTION');
    }

    closeDialog() {
        this.dialogTitle = null;
    }

    showTabs() {
        return (
            this.isRootLevelCollection() && (!this.isGuest || this.hasEditorial)
        );
    }

    getCollectionViewType() {
        // on mobile, we will always show the small collection list
        if (
            UIHelper.evaluateMediaQuery(
                UIConstants.MEDIA_QUERY_MAX_WIDTH,
                UIConstants.MOBILE_WIDTH,
            )
        ) {
            return ListTableComponent.VIEW_TYPE_GRID_SMALL;
        }
        return this.isRootLevelCollection()
            ? ListTableComponent.VIEW_TYPE_GRID
            : ListTableComponent.VIEW_TYPE_GRID_SMALL;
    }

    hasNonIconPreview(): boolean {
        return !this.collectionContent?.node?.preview?.isIcon;
    }

    private renderBreadcrumbs() {
        this.path = [];
        this.nodeService
            .getNodeParents(this.collectionContent.node.ref.id, false)
            .subscribe((data: EduData.NodeList) => {
                this.path = data.nodes.reverse();
            });
    }

    private initialize() {
        this.optionsService.clearComponents(
            this.mainNavRef,
            this.actionbarReferences,
        );

        // load user profile
        this.iamService.getCurrentUserAsync().then(
            iamUser => {
                // WIN

                this.person = iamUser.person;

                // set app to ready state
                this.isReady = true;
                // subscribe to parameters of url
                this.route.queryParams.subscribe(params => {
                    const diffs = Helper.getDifferentKeys(this.params, params);
                    if(Object.keys(diffs).length === 1 && diffs.viewType) {
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

                    this._orderActive = false;
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
                        this.showCollection = id != '-root-';
                        this.displayCollectionById(id, () => {
                            if (params.content) {
                                for (const content of this.collectionContent
                                    .references) {
                                    if (content.ref.id == params.content) {
                                        this.contentDetailObject = content;
                                        break;
                                    }
                                }
                            }
                            this.frame.broadcastEvent(
                                FrameEventsService.EVENT_INVALIDATE_HEIGHT,
                            );
                        });
                    // }
                });
            },
            error => {
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
                    this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                    this.toast.closeModalDialog();
                    this.refreshContent();
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
            .removeFromCollection(
                nodes[position].ref.id,
                this.collectionContent.node.ref.id,
            )
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

    private changeOrder() {
        this.toast.showProgressDialog();        this.collectionService
            .setOrder(
                this.collectionContent.node.ref.id,
                RestHelper.getNodeIds(this.collectionContent.references),
            )
            .subscribe(
                () => {
                    this.collectionContentOriginal = Helper.deepCopy(
                        this.collectionContent,
                    );
                    this._orderActive = false;
                    this.infoTitle = null;
                    this.toast.toast('COLLECTIONS.ORDER_SAVED');
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
        this.optionsService.initComponents(
            this.mainNavRef,
            this.actionbarCollection,
        );
        this.optionsService.refreshComponents();
    }

    private setCollectionId(id: string) {
        this.collectionContent = {
            collections: [],
            references: [],
            node: new Node(),
        };
        this.collectionContent.node.ref = new NodeRef();
        this.collectionContent.node.ref.id = id;
        this.collectionContent.node.aspects = [
            RestConstants.CCM_ASPECT_COLLECTION,
        ];
    }

    private getCollectionId() {
        const c = this.collectionContent.node;
        return c != null && c.ref != null ? c.ref.id : null;
    }

    private finishCollectionLoading(callback?: () => void) {
        this.collectionContentOriginal = Helper.deepCopy(
            this.collectionContent,
        );
        if (this.mainNavRef) {
            this.mainNavRef.refreshBanner();
        }

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
        setTimeout(() => this.setOptionsCollection());
    }

    private addToStore(nodes: Node[]) {
        this.toast.showProgressDialog();
        RestHelper.addToStore(nodes, this.bridge, this.iamService, () => {
            this.toast.closeModalDialog();
            this.mainNavRef.refreshNodeStore();
        });
    }

    createAllowed() {
        if(this.isRootLevelCollection()) {
            let allowed = this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS);
            if(this.tabSelected === RestConstants.COLLECTIONSCOPE_MY) {
                return allowed;
            }
            // for anything else, the user must be able to invite everyone
            allowed = allowed && this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES);
            if(this.tabSelected === RestConstants.COLLECTIONSCOPE_ORGA) {
                allowed = false;
            } else if(this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                allowed = allowed && this.adminMediacenters?.length === 1;
            } else if(this.tabSelected === RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL) {
                allowed = allowed && this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL);
            }
            return allowed;
        } else {
            return !this.isGuest && this.isAllowedToEditCollection();
        }
    }
}
