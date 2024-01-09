import {
    Component,
    ContentChild,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { AuthenticationService, MdsService, Node, UserService } from 'ngx-edu-sharing-api';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { filter, takeUntil } from 'rxjs/operators';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { NodeDataSource } from '../../../features/node-entries/node-data-source';
import {
    CollectionReference,
    ListItemSort,
    LoginResult,
    NodesRightMode,
    NodeWrapper,
    Permission,
    ProposalNode,
} from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { ListItem } from '../../../core-module/ui/list-item';
import {
    DropSource,
    FetchEvent,
    InteractionType,
    ListEventInterface,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDisplayType,
} from '../../../features/node-entries/entries-model';
import { RestConnectorService } from '../../../core-module/rest/services/rest-connector.service';
import { OptionItem, Scope } from '../../../core-ui-module/option-item';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { ActivatedRoute, Router } from '@angular/router';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { forkJoin, forkJoin as observableForkJoin, Subject } from 'rxjs';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { Toast } from '../../../core-ui-module/toast';
import { DialogButton } from '../../../core-module/ui/dialog-button';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { RequestObject } from '../../../core-module/rest/request-object';
import { Helper } from '../../../core-module/rest/helper';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { LoadingScreenService } from '../../../main/loading-screen/loading-screen.service';
import * as EduData from '../../../core-module/core.module';
import { ConfigurationHelper, ConfigurationService } from '../../../core-module/core.module';
import { MdsHelper } from '../../../core-module/rest/mds-helper';
import { TranslateService } from '@ngx-translate/core';
import { ActionbarComponent } from '../../../shared/components/actionbar/actionbar.component';
import { ListTableComponent } from '../../../core-ui-module/components/list-table/list-table.component';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import {
    ManagementEvent,
    ManagementEventType,
} from '../../management-dialogs/management-dialogs.component';
import { OptionsHelperService } from '../../../core-ui-module/options-helper.service';
import { CollectionInfoBarComponent } from '../collection-info-bar/collection-info-bar.component';
import { DialogType } from '../../../common/ui/modal-dialog-toast/modal-dialog-toast.component';
import { DragData } from '../../../services/nodes-drag-drop.service';
import { CanDrop } from '../../../shared/directives/nodes-drop-target.directive';

@Component({
    selector: 'es-collection-content',
    templateUrl: 'collection-content.component.html',
    styleUrls: ['collection-content.component.scss'],
})
export class CollectionContentComponent implements OnChanges, OnInit, OnDestroy {
    private static DEFAULT_REQUEST = {
        sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE,
        ],
        sortAscending: [false, true, false],
    };
    referencesDisplayType = NodeEntriesDisplayType.Grid;
    private readonly destroyed$ = new Subject<void>();
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;

    @Input() collection: Node;
    /**
     * you can subscribe to the clickItem event in case if you want to use emitter
     */
    @Input() interactionType: InteractionType = InteractionType.DefaultActionLink;
    @Input() scope: string;
    /**
     * reference to the infobar component
     * this is required if you want to interconnect it to have support for editing & managing the collection
     * (the options service is inited in the content component)
     */
    @Input() getInfobar: () => CollectionInfoBarComponent;
    @Input() isRootLevel: boolean;
    @Input() createAllowed: () => boolean;
    @Output() clickItem = new EventEmitter<NodeClickEvent<Node | CollectionReference>>();
    @ContentChild('empty') emptyRef: TemplateRef<unknown>;
    @ViewChild('actionbarReferences') actionbarReferences: ActionbarComponent;
    @ViewChild('listCollections') listCollections: ListTableComponent;
    @ViewChild('listReferences') listReferences: ListEventInterface<CollectionReference>;

    private mainNavUpdateTrigger = new Subject<void>();
    sortCollectionColumns: ListItemSort[] = [
        new ListItemSort('NODE', RestConstants.CM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
        new ListItemSort('NODE', RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION, 'ascending'),
    ];
    createSubCollectionOptionItem = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', () =>
        this.onCreateCollection(),
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
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    });
    addMaterialBinaryOptionItem = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () => {
        this.mainNavService.getMainNav().topBar.createMenu.openUploadSelect();
    });
    dataSourceCollections = new NodeDataSource<Node>();
    dataSourceReferences = new NodeDataSource<CollectionReference>();
    collectionsColumns: ListItem[] = [];
    referencesColumns: ListItem[] = [];
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed$ });

    private contentNode: Node;
    permissions: Permission[];
    login: LoginResult;
    constructor(
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private collectionService: RestCollectionService,
        private toast: Toast,
        private bridge: BridgeService,
        private route: ActivatedRoute,
        private loadingScreen: LoadingScreenService,
        private uiService: UIService,
        private router: Router,
        private connector: RestConnectorService,
        private translation: TranslateService,
        private userService: UserService,
        private configurationService: ConfigurationService,
        private optionsService: OptionsHelperService,
        private mdsService: MdsService,
        private mainNavService: MainNavService,
        private authenticationService: AuthenticationService,
    ) {
        this.sortCollectionColumns[this.sortCollectionColumns.length - 1].mode = 'ascending';
        // this.collectionSortEmitter.subscribe((sort: SortEvent) => this.setCollectionSort(sort));
        // this.collectionCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleCollectionsOrder() : this.changeCollectionsOrder());
        // this.referenceSortEmitter.subscribe((sort: SortEvent) => this.setReferenceSort(sort));
        // this.referenceCustomSortEmitter.subscribe((state: boolean) => state ? this.toggleReferencesOrder() : this.changeReferencesOrder());
        this.collectionsColumns.push(new ListItem('COLLECTION', 'title'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'info'));
        this.collectionsColumns.push(new ListItem('COLLECTION', 'scope'));

        this.mainNavService.getDialogs().onEvent.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                if (event.data.collection.ref.id === this.collection.ref.id) {
                    this.listReferences.addVirtualNodes(event.data.references);
                }
            }
        });
        this.authenticationService
            .observeLoginInfo()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((login) => {
                this.login = login;
                this.addMaterialBinaryOptionItem.isEnabled = login.toolPermissions.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
                );
                this.createSubCollectionOptionItem.isEnabled = login.toolPermissions.includes(
                    RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                );
            });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    async ngOnInit() {
        const mdsSets = await ConfigurationHelper.getAvailableMds(
            RestConstants.HOME_REPOSITORY,
            this.mdsService,
            this.configurationService,
        );
        const set = await this.mdsService
            .getMetadataSet({ metadataSet: mdsSets[0].id })
            .toPromise();
        this.referencesColumns = MdsHelper.getColumns(
            this.translation,
            set,
            'collectionReferences',
        );

        this.optionsService.clearComponents(this.actionbarReferences);
        this.registerMainNav();
        this.mainNavUpdateTrigger.next();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.collection) {
            this.dataSourceCollections.reset();
            this.dataSourceReferences.reset();

            this.createSubCollectionOptionItem.name =
                'OPTIONS.' + (this.isRootLevel ? 'NEW_COLLECTION' : 'NEW_SUB_COLLECTION');
            if (this.isRootLevel) {
                // display root collections with tabs

                // Use hardcoded sorting for root collection.
                this.sortCollections.active = RestConstants.CM_MODIFIED_DATE;
                this.sortCollections.direction = 'desc';
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

                this.collectionService.getCollection(this.collection.ref.id).subscribe(
                    ({ collection }) => {
                        // set the collection and load content data by refresh
                        const orderCollections =
                            collection.properties[
                                RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE
                            ];
                        this.sortCollections.active =
                            orderCollections?.[0] || RestConstants.CM_MODIFIED_DATE;

                        this.sortCollections.direction =
                            orderCollections?.[0] ===
                            RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION
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
                        this.sortReferences.direction =
                            this.sortReferences.active ===
                            RestConstants.COLLECTION_ORDER_MODE_CUSTOM
                                ? 'asc'
                                : refAscending
                                ? 'asc'
                                : 'desc';
                        this.collection = collection;
                        this.mainNavUpdateTrigger.next();
                        this.dataSourceCollections.isLoading = false;
                        this.setOptionsCollection();
                        this.refreshContent();
                        if (
                            this.collection.access.indexOf(
                                RestConstants.ACCESS_CHANGE_PERMISSIONS,
                            ) !== -1
                        ) {
                            this.nodeService
                                .getNodePermissions(this.collection.ref.id)
                                .subscribe((permissions) => {
                                    this.permissions =
                                        permissions.permissions.localPermissions.permissions.concat(
                                            permissions.permissions.inheritedPermissions,
                                        );
                                });
                        }
                    },
                    (error) => {
                        if (error.status === 404) {
                            this.toast.error(null, 'COLLECTIONS.ERROR_NOT_FOUND');
                        } else {
                            this.toast.error(error);
                        }
                        this.dataSourceCollections.isLoading = false;
                        if (!this.loadingTask.isDone) {
                            this.loadingTask.done();
                        }
                    },
                );
            }
        }
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
    onCreateCollection() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            this.router.navigate(
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
                    NodesRightMode.Original,
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
        // do not allow to drop here
        return {
            accept: false,
        };
    };

    dropOnRef = (target: Node, source: DropSource<CollectionReference>) => {
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
            this.nodeService
                .moveNode(
                    (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                    source.element[0].ref.id,
                )
                .subscribe(
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
            if (source.mode === 'copy') {
                UIHelper.addToCollection(
                    this.nodeHelper,
                    this.collectionService,
                    this.router,
                    this.bridge,
                    target as Node,
                    source.element,
                    false,
                    (nodes) => {
                        this.toast.closeModalDialog();
                        this.refreshContent();
                    },
                );
            } else if (source.mode === 'move') {
                forkJoin(
                    source.element.map((toMove) =>
                        this.nodeService.moveNode(
                            (target as Node)?.ref?.id || RestConstants.COLLECTIONHOME,
                            toMove.ref.id,
                        ),
                    ),
                ).subscribe(
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

    onContentClick(event: NodeClickEvent<CollectionReference | ProposalNode>, force = false): void {
        this.contentNode = event.element;
        let buttons: DialogButton[] = [];
        if (event.element.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
            this.clickElementEvent(event);
            return;
        }
        if (this.isAllowedToDeleteNodes([event.element])) {
            buttons.push(
                new DialogButton('OPTIONS.REMOVE_REF', { color: 'standard' }, () =>
                    this.deleteFromCollection(() => this.toast.closeModalDialog()),
                ),
            );
        }
        buttons.push(
            new DialogButton('COLLECTIONS.OPEN_MISSING', { color: 'primary' }, () => {
                this.onContentClick(event, true);
                this.toast.closeModalDialog();
            }),
        );
        if ((event.element as CollectionReference).originalId == null && !force) {
            this.toast.showConfigurableDialog({
                title: 'COLLECTIONS.ORIGINAL_MISSING',
                message: 'COLLECTIONS.ORIGINAL_MISSING_INFO',
                isCancelable: true,
                buttons,
            });
            return;
        }
        this.clickElementEvent(event);
    }

    private clickElementEvent(event: NodeClickEvent<CollectionReference | ProposalNode>) {
        if (this.interactionType === InteractionType.DefaultActionLink) {
            this.nodeService
                .getNodeMetadata(event.element.ref.id)
                .subscribe((data: NodeWrapper) => {
                    this.contentNode = data.node;
                    this.router.navigate([
                        UIConstants.ROUTER_PREFIX + 'render',
                        event.element.ref.id,
                    ]);
                });
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
        this.toast.showProgressDialog();
        this.collectionService
            .removeFromCollection(this.contentNode.ref.id, this.collection.ref.id)
            .subscribe(
                () => {
                    if (!('proposal' in this.collection)) {
                        this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                    }
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

    private registerMainNav(): void {
        this.mainNavService.setMainNavConfig({
            title: 'COLLECTIONS.TITLE',
            currentScope: 'collections',
            // TODO: document where this fails.
            //
            // onCreate: (nodes) => this.addNodesToCollection(nodes),
        });
        this.mainNavService
            .getDialogs()
            .onUploadFilesProcessed.pipe(
                takeUntil(this.destroyed$),
                filter((nodes) => !!nodes),
            )
            .subscribe((nodes) => this.addNodesToCollection(nodes));
        this.mainNavService.onConnectorCreated
            .pipe(takeUntil(this.destroyed$))
            .subscribe((node) => this.addNodesToCollection([node]));
        this.mainNavUpdateTrigger.pipe(takeUntil(this.destroyed$)).subscribe(async () => {
            this.mainNavService.patchMainNavConfig({
                create: {
                    allowed: this.createAllowed(),
                    allowBinary: !this.isRootLevel && (await this.isAllowedToEditCollection()),
                    parent: this.collection ?? null,
                },
            });
        });
    }

    private refreshContent() {
        this.dataSourceCollections.reset();
        this.dataSourceReferences.reset();
        this.listReferences?.getSelection().clear();
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
                (collection) => {
                    // transfere sub collections and content
                    this.dataSourceCollections.setData(
                        collection.collections,
                        collection.pagination,
                    );
                    this.dataSourceCollections.isLoading = false;
                    if (this.isRootLevel) {
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
                            this.finishCollectionLoading();
                        });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }

    isMobile() {
        return this.uiService.isMobile();
    }
    async setReferenceSort(sort: ListSortConfig) {
        const diff = Helper.getKeysWithDifferentValues(this.sortReferences, sort);
        this.sortReferences = sort;
        // auto activate the custom sorting when the users switch to "custom order"
        if (diff.includes('active')) {
            this.sortReferences.customSortingInProgress =
                this.sortReferences.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION;
        }
        this.toggleReferencesOrder();
        if (this.sortReferences.customSortingInProgress) {
            await this.loadMoreReferences({
                reset: true,
                amount: RestConstants.COUNT_UNLIMITED,
                offset: 0,
            });
        }
        if (diff.includes('customSortingInProgress') && sort.customSortingInProgress) {
            return;
        }

        try {
            await this.nodeService
                .editNodeProperty(
                    this.collection.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_ORDER_MODE,
                    [sort.active, (sort.direction === 'asc') + ''],
                )
                .toPromise();
            if (sort.active !== RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION) {
                this.toast.toast('COLLECTIONS.TOAST.SORT_SAVED_TYPE', {
                    type: this.translation.instant('NODE.' + sort.active),
                });
            }
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
        if (callback) {
            callback();
        }
        setTimeout(() => {
            this.setOptionsCollection();
            this.listReferences?.initOptionsGenerator({
                actionbar: this.actionbarReferences,
                parent: this.collection,
            });
            if (!this.loadingTask.isDone) {
                this.loadingTask.done();
            }
        });
    }

    deleteReference(content: EduData.CollectionReference | EduData.Node) {
        this.contentNode = content;
        this.deleteFromCollection();
    }

    async setCollectionSort(sort: ListSortConfig) {
        this.sortCollections = sort;
        try {
            await this.nodeService
                .editNodeProperty(
                    this.collection.ref.id,
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
                type: this.translation.instant('NODE.' + sort.active),
            });
        }
        this.sortCollections.customSortingInProgress =
            this.sortCollections.active === RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION;
        if (this.sortCollections.customSortingInProgress) {
            this.toggleCollectionsOrder();
        }
    }

    private setOptionsCollection() {
        this.optionsService.setData({
            scope: Scope.CollectionsCollection,
            activeObjects: [this.collection],
        });
        this.optionsService.initComponents(this.getInfobar()?.actionbar, this.listReferences);
        this.optionsService.refreshComponents();
    }
    toggleCollectionsOrder() {
        if (this.sortCollections.customSortingInProgress) {
            this.toast.showConfigurableDialog({
                dialogType: DialogType.Infobar,
                title: 'COLLECTIONS.ORDER_COLLECTIONS',
                message: 'COLLECTIONS.ORDER_COLLECTIONS_INFO',
                isCancelable: true,
                buttons: DialogButton.getSingleButton('SAVE', () => {
                    this.changeCollectionsOrder();
                }),
                onCancel: () => {
                    this.sortCollections.customSortingInProgress = false;
                    this.toggleCollectionsOrder();
                },
            });
        } else {
            this.toast.closeModalDialog();
            this.refreshContent();
        }
    }
    toggleReferencesOrder() {
        if (this.sortReferences.customSortingInProgress) {
            this.toast.showConfigurableDialog({
                dialogType: DialogType.Infobar,
                title: 'COLLECTIONS.ORDER_ELEMENTS',
                message: 'COLLECTIONS.ORDER_ELEMENTS_INFO',
                isCancelable: true,
                buttons: DialogButton.getSingleButton('SAVE', () => {
                    this.changeReferencesOrder();
                    this.sortReferences.customSortingInProgress = false;
                    this.listReferences.getSelection().clear();
                }),
                onCancel: () => {
                    this.sortReferences.customSortingInProgress = false;
                    this.toggleReferencesOrder();
                },
            });
        } else {
            this.toast.closeModalDialog();
            this.refreshContent();
        }
    }

    addNodesToCollection(nodes: Node[], allowDuplicate: boolean | 'ignore' = false) {
        this.toast.showProgressDialog();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            this.collection,
            nodes,
            false,
            () => {
                this.refreshContent();
                this.toast.closeModalDialog();
            },
            allowDuplicate,
        );
    }

    private async changeReferencesOrder() {
        this.toast.showProgressDialog();
        this.collectionService
            .setOrder(
                this.collection.ref.id,
                RestHelper.getNodeIds(await this.dataSourceReferences.getData()),
            )
            .subscribe(
                () => {
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
                this.collection.ref.id,
                RestHelper.getNodeIds(await this.dataSourceCollections.getData()),
            )
            .subscribe(
                () => {
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

    canDelete(node: EduData.CollectionReference) {
        return RestHelper.hasAccessPermission(node, 'Delete');
    }

    isDeleted(node: CollectionReference) {
        return (
            node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE) &&
            !node.aspects.includes(RestConstants.CCM_ASPECT_REMOTEREPOSITORY) &&
            !node.originalId
        );
    }
}
