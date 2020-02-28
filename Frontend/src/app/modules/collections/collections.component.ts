import { Component, ElementRef, ViewChild } from '@angular/core';
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
    CollectionFeedback, NodesRightMode,
} from '../../core-module/core.module';
import { Toast } from '../../core-ui-module/toast';
import {DefaultGroups, OptionItem, Scope} from '../../core-ui-module/option-item';
import { NodeRenderComponent } from '../../common/ui/node-render/node-render.component';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { Title } from '@angular/platform-browser';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { ListTableComponent } from '../../core-ui-module/components/list-table/list-table.component';
import { NodeHelper } from '../../core-ui-module/node-helper';
import { TranslateService } from '@ngx-translate/core';
import { Location } from '@angular/common';
import { Helper } from '../../core-module/rest/helper';
import { MainNavComponent } from '../../common/ui/main-nav/main-nav.component';
import { ColorHelper } from '../../core-module/ui/color-helper';
import { ActionbarHelperService } from '../../common/services/actionbar-helper';
import { MdsHelper } from '../../core-module/rest/mds-helper';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import { AddElement } from '../../core-ui-module/add-element';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { HttpClient } from '@angular/common/http';
import { GlobalContainerComponent } from '../../common/ui/global-container/global-container.component';
import { Observable } from 'rxjs';
import {OptionsHelperService} from '../../common/options-helper';
import {ActionbarComponent} from '../../common/ui/actionbar/actionbar.component';

// component class
@Component({
    selector: 'app-collections',
    templateUrl: 'collections.component.html',
    styleUrls: ['collections.component.scss'],
    // provide a new instance so to not get conflicts with other service instances
    providers: [ OptionsHelperService ]
})
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

    @ViewChild('mainNav', { static: false }) mainNavRef: MainNavComponent;
    @ViewChild('actionbarCollection', { static: false }) actionbarCollection: ActionbarComponent;
    @ViewChild('actionbarReferences', { static: false }) actionbarReferences: ActionbarComponent;
    @ViewChild('listCollections', { static: false })
    listCollections: ListTableComponent;

    dialogTitle: string;
    globalProgress = false;
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
    createCollectionElement = new AddElement('COLLECTIONS.CREATE_COLLECTION');
    createCollectionReference = new AddElement(
        'COLLECTIONS.ADD_MATERIAL',
        'redo',
    );
    optionsMaterials: OptionItem[];
    tutorialElement: ElementRef;
    customNodeList = false;
    set collectionShare(collectionShare: Node) {
        this._collectionShare = collectionShare;
        this.refreshAll();
    }
    get collectionShare() {
        return this._collectionShare;
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
    private path: EduData.Node[];
    private hasEditorial = false;
    private hasMediacenter = false;
    private showCollection = false;
    private _orderActive: boolean;
    private reurl: any;
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
        private nodeService: RestNodeService,
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
        private title: Title,
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
            UIHelper.setTitle(
                'COLLECTIONS.TITLE',
                title,
                translationService,
                config,
            );
            this.mdsService.getSet().subscribe((data: MdsMetadataset) => {
                this.referencesColumns = MdsHelper.getColumns(
                    data,
                    'collectionReferences',
                );
            });

            this.connector.isLoggedIn().subscribe(
                (data: LoginResult) => {
                    if (data.isValidLogin && data.currentScope == null) {
                        this.isGuest = data.isGuest;
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
                        this.initialize();
                    } else {
                        RestHelper.goToLogin(this.router, this.config);
                    }
                },
                (error: any) => RestHelper.goToLogin(this.router, this.config),
            );
        });
        this.initCustomNodeList();
    }

    isMobile() {
        return this.uiService.isMobile();
    }

    isMobileWidth() {
        return window.innerWidth < UIConstants.MOBILE_WIDTH;
    }

    setCustomOrder(event: MatSlideToggle) {
        const checked = event.checked;
        this.collectionContent.node.collection.orderMode = checked
            ? RestConstants.COLLECTION_ORDER_MODE_CUSTOM
            : null;
        if (checked) {
            this.orderActive = true;
        } else {
            this.globalProgress = true;
            this.collectionService
                .setOrder(this.collectionContent.node.ref.id)
                .subscribe(() => {
                    this.globalProgress = false;
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

    selectTabMyCollections(): void {
        this.selectTab(RestConstants.COLLECTIONSCOPE_MY);
    }

    selectTabMyOrganizations(): void {
        this.selectTab(RestConstants.COLLECTIONSCOPE_ORGA);
    }

    selectTabAllCollections(): void {
        this.selectTab(RestConstants.COLLECTIONSCOPE_ALL);
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
            return this.tabSelected === RestConstants.COLLECTIONSCOPE_MY
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

    switchToSearch(): void {
        UIHelper.getCommonParameters(this.route).subscribe(params => {
            params.addToCollection = this.collectionContent.node.ref.id;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    isBrightColor() {
        return (
            ColorHelper.getColorBrightness(
                this.collectionContent.node.collection.color,
            ) > ColorHelper.BRIGHTNESS_THRESHOLD_COLLECTIONS
        );
    }

    getScopeInfo() {
        return NodeHelper.getCollectionScopeInfo(
            this.collectionContent.node,
        );
    }
    dropOnCollection(event: any) {
        const target = event.target;
        const source = event.source[0];
        this.toast.showProgressDialog();
        if (source.hasOwnProperty('childCollectionsCount')) {
            if (event.type === 'copy') {
                this.toast.error(null, 'INVALID_OPERATION');
                this.globalProgress = false;
                return;
            }
            this.nodeService.moveNode(target.ref.id, source.ref.id).subscribe(
                () => {
                    this.globalProgress = false;
                    this.refreshContent();
                },
                error => {
                    this.handleError(error);
                    this.globalProgress = false;
                },
            );
        } else {
            UIHelper.addToCollection(
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

    canDropOnCollection = (event: any) => {
        if (event.source[0].ref.id === event.target.ref.id) {
            return false;
        }
        if (event.target.ref.id === this.collectionContent.node.ref.id) {
            return false;
        }
        // in case it's via breadcrums, unmarshall the collection item
        if (event.target.collection) {
            event.target = event.target.collection;
        }
        console.log(event.source, event.target);
        // do not allow to move anything else than editorial collections into editorial collections (if the source is a collection)
        if (event.source[0].hasOwnProperty('childCollectionsCount')) {
            if (
                (event.source[0].type ===
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    event.target.type !==
                        RestConstants.COLLECTIONTYPE_EDITORIAL) ||
                (event.source[0].type !==
                    RestConstants.COLLECTIONTYPE_EDITORIAL &&
                    event.target.type === RestConstants.COLLECTIONTYPE_EDITORIAL)
            ) {
                return false;
            }
        }

        if (
            !NodeHelper.getNodesRight(
                event.source[0],
                RestConstants.ACCESS_CC_PUBLISH,
                NodesRightMode.Original,
            )
        ) {
            return false;
        }

        if (
            !NodeHelper.getNodesRight(
                event.target,
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Local,
            )
        ) {
            return false;
        }

        return true;
    };

    canDropOnRef() {
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
        const request: any = Helper.deepCopy(
            CollectionsMainComponent.DEFAULT_REQUEST,
        );
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
                    console.log(collection);
                    // transfere sub collections and content
                    this.collectionContent.collections = collection.collections;
                    this.collectionContent.collectionsPagination =
                        collection.pagination;
                    if (this.isRootLevelCollection()) {
                        this.finishCollectionLoading(callback);
                        return;
                    }
                    request.count = null;
                    this.collectionService
                        .getCollectionReferences(
                            this.collectionContent.node.ref.id,
                            CollectionsMainComponent.PROPERTY_FILTER,
                            request,
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
        console.log(request);
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
                this.temporaryStorageService.set(
                    TemporaryStorageService.NODE_RENDER_PARAMETER_LIST,
                    this.collectionContent.references,
                );
                this.temporaryStorageService.set(
                    TemporaryStorageService.NODE_RENDER_PARAMETER_ORIGIN,
                    'collections',
                );
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
                    console.log(collection.collection);
                    this.collectionContent.node = collection.collection;

                    this.renderBreadcrumbs();

                    this.refreshContent(callback);
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

    private renderBreadcrumbs() {
        this.path = [];
        this.nodeService
            .getNodeParents(this.collectionContent.node.ref.id, false)
            .subscribe((data: EduData.NodeList) => {
                this.path = data.nodes.reverse();
            });
    }

    private openBreadcrumb(position: number) {
        if (position == 0) {
            this.selectTab(this.tabSelected);
            return;
        }
        this.navigate(this.path[position - 1].ref.id);
    }

    private initialize() {
        this.optionsService.clearComponents(this.mainNavRef, this.actionbarReferences);

        // load user profile
        this.iamService.getUser().subscribe(
            iamUser => {
                // WIN

                this.person = iamUser.person;

                // set app to ready state
                this.isReady = true;
                // subscribe to parameters of url
                this.route.queryParams.subscribe(params => {
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
                    // @TODO handle the feedback param, maybe in management-dialogs
                    // this.feedback = params.feedback === 'true';

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
                    if (params.nodeId) {
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
                    } else {
                        this.showCollection = id != '-root-';
                        this.displayCollectionById(id, () => {
                            if (params.content) {
                                for (const content of this.collectionContent
                                    .references) {
                                    if (content.ref.id == params.content) {
                                        console.log('match');
                                        this.contentDetailObject = content;
                                        break;
                                    }
                                }
                            }
                            this.frame.broadcastEvent(
                                FrameEventsService.EVENT_INVALIDATE_HEIGHT,
                            );
                        });
                    }
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
        this.globalProgress = true;
        this.collectionService
            .removeFromCollection(
                this.contentDetailObject.ref.id,
                this.collectionContent.node.ref.id,
            )
            .subscribe(
                () => {
                    this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                    this.globalProgress = false;
                    this.refreshContent();
                    if (callback) {
                        callback();
                    }
                },
                (error: any) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            );
    }

    private deleteMultiple(nodes: Node[], position = 0, error = false) {
        if (position == nodes.length) {
            if (!error) {
                this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
            }
            this.globalProgress = false;
            this.refreshContent();
            return;
        }
        this.globalProgress = true;
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
        console.log('add to other');
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
        this.globalProgress = true;
        this.collectionService
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
                    this.globalProgress = false;
                },
                (error: any) => {
                    this.globalProgress = false;
                    this.toast.error(error);
                },
            );
    }

    private isAllowedToDeleteNodes(nodes: Node[]) {
        return (
            this.isAllowedToDeleteCollection() ||
            NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE)
        );
    }

    private collectionPermissions() {
        this._collectionShare = this.collectionContent.node;
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObject: this.collectionContent.node
        });
        this.optionsService.initComponents(this.mainNavRef, this.actionbarCollection);
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
        this.collectionContent.node.aspects = [RestConstants.CCM_ASPECT_COLLECTION];
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
        if (
            this.getCollectionId() == RestConstants.ROOT &&
            this.isAllowedToEditCollection()
        ) {
            setTimeout(() => {
                this.tutorialElement = this.listCollections.addElementRef;
            });
        }
        this.isLoading = false;
        if (callback) {
            callback();
        }
        setTimeout(() => this.setOptionsCollection());
    }

    private addToStore(nodes: Node[]) {
        this.globalProgress = true;
        RestHelper.addToStore(nodes, this.bridge, this.iamService, () => {
            this.globalProgress = false;
            this.mainNavRef.refreshNodeStore();
        });
    }

    private initCustomNodeList(): void {
        const customNodeListComponent = this.tempStorage.get(
            TemporaryStorageService.CUSTOM_NODE_LIST_COMPONENT,
        );
        this.customNodeList = !!customNodeListComponent;
    }
}
