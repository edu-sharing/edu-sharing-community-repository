import { SelectionModel } from '@angular/cdk/collections';
import {
    ChangeDetectorRef,
    Component,
    computed,
    effect,
    input,
    model,
    OnInit,
    Signal,
    signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatButtonToggleChange } from '@angular/material/button-toggle';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import {
    AboutService,
    AuthenticationService,
    CollectionService as ApiCollectionService,
    Connector,
    Copy,
    CreateSuggestionRequestDto,
    DEFAULT,
    HOME_REPOSITORY,
    MdsQueryCriteria,
    NetworkService,
    Node,
    NodeService,
    PROPERTY_FILTER_ALL,
    ROOT,
    SearchRequestParams,
    SearchResults,
    SearchService,
    SuggestionsV1Service,
} from 'ngx-edu-sharing-api';
import {
    CanDrop,
    ColumnType,
    DragData,
    DropSource,
    FetchEvent,
    InteractionType,
    ListItem,
    LocalEventsService,
    MdsExtendedValueData,
    MdsExtendedValues,
    MdsHelperService,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    NodeEntriesService,
    NodeEntriesWrapperComponent,
    NodesRightMode,
    OptionItem,
    Scope,
    TreeConfig,
    TreeNodeService,
} from 'ngx-edu-sharing-ui';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, firstValueFrom, map, of, switchMap } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import {
    CollectionReference,
    CollectionSubcollections,
} from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { RestConnectorService } from '../../../core-module/rest/services/rest-connector.service';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { ConnectorOptionsService } from '../../../services/connector-options.service';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import {
    AddWithConnectorDialogData,
    AddWithConnectorDialogResult,
} from '../../../features/dialogs/dialog-modules/add-with-connector-dialog/add-with-connector-dialog-data';
import { AddMaterialDialogResult } from '../../../features/dialogs/dialog-modules/add-material-dialog/add-material-dialog-data';
import { AddMaterialDialogModule } from '../../../features/dialogs/dialog-modules/add-material-dialog/add-material-dialog.module';
import {
    OptionState,
    SidebarContext,
} from '../../../features/editorial-sidebar/editorial-sidebar.component';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import { MdsModule } from '../../../features/mds/mds.module';
import { MetadataTemplateManagementComponent } from '../../../features/metadata-template-management/metadata-template-management.component';
import { BridgeService } from '../../../services/bridge.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast, ToastType } from '../../../services/toast';
import { UploadDialogService } from '../../../services/upload-dialog.service';
import { SharedModule } from '../../../shared/shared.module';
import { MessageType } from '../../../util/message-type';

export enum TabType {
    SEARCH = 'search',
    METHODOLOGY = 'methodology',
    COLLECTIONS = 'collections',
    WORKSPACE = 'workspace',
    UPLOAD = 'upload',
}

enum StepType {
    SELECT = 'select',
    CONFIGURE = 'configure',
}

enum InvalidSelectionReason {
    INVALID_COMBINATION = 'invalidCombination',
    INVALID_SELECTION = 'invalidSelection',
    MISSING_PRIVILEGES = 'missingPrivileges',
    AT_LEAST_ROOT_OR_CHILDREN_SELECTED = 'atLeastRootOrChildrenSelected',
}

export type NodesSelectorConfig = {
    state?: TabType;
    /**
     * fast skips metadata & question for duplicate behaviour
     */
    upload?: 'fast' | 'default';
    /**
     * selected nodes that should be sorted into
     * If null or an empty selection, we assume that this component is used to select the nodes that SHALL be sorted into
     */
    selection?: SelectionModel<NodeEntriesDataType>;
    /**
     * the callback to check if the given selection is valid as a target
     */
    applyCallback?: (selected: Node[]) => boolean;
    /**
     * custom label for the APPLY button
     */
    applyLabel?: string;
    /**
     * whether to show the connector "Create" button in the upload tab (default: true)
     */
    allowCreate?: boolean;
    /**
     * automatically close the sidebar after nodes are emitted
     */
    autoClose?: boolean;
    /**
     * called whenever nodes are produced (upload, copy, or connector create).
     * Use this instead of subscribing to EditorialSidebarService.applyNodeEmitted globally to only listen to your current trigger session
     */
    onNodesChoosen?: (result: { nodes: Node[]; connectorId?: string; window?: Window }) => void;
};

@Component({
    selector: 'es-nodes-selector',
    templateUrl: 'nodes-selector.component.html',
    styleUrls: ['nodes-selector.component.scss'],
    imports: [
        SharedModule,
        AddMaterialDialogModule,
        MdsModule,
        MetadataTemplateManagementComponent,
    ],
    providers: [NodeEntriesService, TreeNodeService],
})
export class NodesSelectorComponent implements OnInit {
    protected readonly i18nPrefix: string = 'EDITORIAL.OPTIONS.NODES_SELECTOR.';
    protected readonly idPrefix: string = 'nodes-selector-tab';

    option = input<OptionState<NodesSelectorConfig>>();
    parent = input<Node>();
    primaryMode = input<SidebarContext>();
    chooseParent = computed(
        () => !this.parent() || this.nodeHelperService.isNodeCollection(this.parent()),
    );

    selectedTab: WritableSignal<TabType> = signal(null);
    selectedTabId = computed(() => this.supportedTabs().indexOf(this.selectedTab()));
    selectedNodeChildren: WritableSignal<Partial<Node>[]> = signal([]);
    selectedNodes: WritableSignal<Partial<Node>[]> = signal([]);
    currentExtendedValues: WritableSignal<MdsExtendedValues> = signal(null);
    private atLeastOneEnabledExtendedValue: Signal<boolean> = computed((): boolean => {
        const values: MdsExtendedValues = this.currentExtendedValues();
        if (!values) {
            return false;
        }
        return Object.values(values).some((value) => {
            if (value === null || Array.isArray(value)) {
                return false;
            }
            return Object.values(value).some((data: MdsExtendedValueData) => data.enabled);
        });
    });
    supportedTabs: Signal<TabType[]> = computed(() => {
        if (this.selectionMode() === 'source') {
            if (!this.parent() || this.nodeHelperService.isNodeCollection(this.parent())) {
                return [TabType.SEARCH, TabType.COLLECTIONS, TabType.WORKSPACE, TabType.UPLOAD];
            } else {
                return [TabType.WORKSPACE, TabType.UPLOAD];
            }
        }
        if (this.allSelectedNodesFromHomeRepo()) {
            return [TabType.METHODOLOGY, TabType.COLLECTIONS, TabType.WORKSPACE];
        }
        return [TabType.COLLECTIONS];
    });
    highestSelectedNode: Signal<Partial<Node> | null> = computed((): Partial<Node> | null => {
        const selectedNodes: Partial<Node>[] = this.selectedNodes();
        // early return for empty or single selection
        if (selectedNodes.length === 0) {
            return null;
        }
        if (selectedNodes.length === 1) {
            return selectedNodes[0];
        }
        const selectedNodeIds = selectedNodes.map((n) => n.ref.id);
        return (
            selectedNodes.find((n) => !selectedNodeIds.includes(n.parent.id)) ?? selectedNodes[0]
        );
    });
    isCollectionsTab: Signal<boolean> = computed(() => this.selectedTab() === TabType.COLLECTIONS);
    isMethodologyTab: Signal<boolean> = computed(() => this.selectedTab() === TabType.METHODOLOGY);
    isSearchTab: Signal<boolean> = computed(() => this.selectedTab() === TabType.SEARCH);
    methodologyTabWithValue: Signal<boolean> = computed(
        () => this.isMethodologyTab() && this.atLeastOneEnabledExtendedValue(),
    );
    private currentStep: WritableSignal<StepType> = signal(StepType.SELECT);
    isSelectStep: Signal<boolean> = computed((): boolean => this.currentStep() === StepType.SELECT);
    onlyOneSelected: Signal<boolean> = computed(() => this.selectedNodes().length === 1);
    onlyFilesSelected: Signal<boolean> = computed((): boolean =>
        this.selectedNodes().every((node) => node.type === RestConstants.CCM_TYPE_IO),
    );
    invalidSelectionReason: Signal<InvalidSelectionReason | null> = computed(
        (): InvalidSelectionReason | null => {
            if (this.selectionMode() === 'source') {
                if (
                    this.option().optionConfig?.applyCallback &&
                    !this.option().optionConfig?.applyCallback(this.selectedNodes() as Node[])
                ) {
                    return InvalidSelectionReason.INVALID_SELECTION;
                }
                // fallback for configuration tab
                if (!this.isSelectStep() && !this.atLeastRootOrChildrenSelected()) {
                    return InvalidSelectionReason.AT_LEAST_ROOT_OR_CHILDREN_SELECTED;
                }
                // copy collection dialog
                if (this.onlyOneSelected() && this.selectedNodes()[0].mediatype === 'collection') {
                    return null;
                }
                if (!this.onlyFilesSelected()) {
                    return InvalidSelectionReason.INVALID_COMBINATION;
                }
                // only allow insert into collection when all have CCPublish
                if (
                    this.parent() &&
                    this.nodeHelperService.isNodeCollection(this.parent()) &&
                    !this.nodeHelperService.getNodesRight(
                        this.selectedNodes() as Node[],
                        RestConstants.ACCESS_CC_PUBLISH,
                        NodesRightMode.Effective,
                    )
                ) {
                    return InvalidSelectionReason.MISSING_PRIVILEGES;
                }
                return null;
            } else {
                if (
                    this.onlyOneSelected() &&
                    (this.selectedNodes()[0].mediatype === 'folder' ||
                        this.selectedNodes()[0].mediatype === 'collection')
                ) {
                    return null;
                }
                return InvalidSelectionReason.INVALID_SELECTION;
            }
        },
    );
    // The nodes that are currently selected as source
    selectedSourceNodes: Signal<Node[]> = computed(() => {
        // upload tab has no source nodes — always acts as source, never as target
        if (this.option()?.optionConfig?.state === TabType.UPLOAD) {
            return [];
        }
        const selected = this.option()?.optionConfig?.selection?.selected as Node[];
        if (selected?.length) {
            return selected;
        }
        // note: use primaryMode() over scope, as scope resets on reload
        const primaryModesWithAutomaticSelection: SidebarContext[] = [
            'collections',
            'workspace',
            'search',
        ];
        if (this.primaryMode() && primaryModesWithAutomaticSelection.includes(this.primaryMode())) {
            return (this.editorialSidebarService.nodes() ?? []) as Node[];
        }
        return [];
    });
    allSelectedNodesFromHomeRepo: Signal<boolean> = toSignal(
        toObservable(this.selectedSourceNodes).pipe(
            switchMap((nodes) =>
                nodes.length === 0
                    ? of(false)
                    : combineLatest(
                          nodes.map((n) => this.networkService.isFromHomeRepository(n as Node)),
                      ).pipe(map((results) => results.every(Boolean))),
            ),
        ),
        { initialValue: false },
    );
    workspaceAction = model<'move' | 'copy'>('move');
    canMoveWorkspaceNodes = computed(
        () =>
            this.option().optionConfig &&
            this.selectedSourceNodes()?.length &&
            this.selectedSourceNodes().every(
                (n: Node) => !n.aspects?.includes(RestConstants.CCM_ASPECT_IO_REFERENCE),
            ) &&
            this.nodeHelperService.getNodesRight(
                this.selectedSourceNodes(),
                RestConstants.ACCESS_CHANGE_PERMISSIONS,
                NodesRightMode.Effective,
            ),
    );

    // initialize collection copy variables with true
    copyRoot = model(true);
    copyChildCollections = model(true);
    copyRefs = model(true);
    // either copying root or child collections must be selected
    atLeastRootOrChildrenSelected = computed(() => {
        return this.copyRoot() || this.copyChildCollections();
    });
    numberOfRefs = computed(() => {
        // read model signal at the beginning to evaluate it
        const shouldCopyChildCollections = this.copyChildCollections();
        const collectionToCopy = this.highestSelectedNode();
        if (!collectionToCopy) {
            return 0;
        }
        const initialNumberOfRefs = collectionToCopy.collection.childReferencesCount;
        const collectionChildren = this.selectedNodeChildren();
        if (!collectionChildren.length) {
            return initialNumberOfRefs;
        }
        // sum up the number of references of all children
        const sumOfChildReferences = collectionChildren
            .filter((child) => child.collection?.childReferencesCount)
            .reduce((sum, child) => sum + child.collection.childReferencesCount, 0);
        // initialize with the number of root references, as those are always copied
        let sumOfReferences =
            initialNumberOfRefs - sumOfChildReferences > 0
                ? initialNumberOfRefs - sumOfChildReferences
                : 0;
        if (shouldCopyChildCollections) {
            sumOfReferences += sumOfChildReferences;
        }
        return sumOfReferences;
    });

    // search tab
    dataSourceSearch: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    searchColumns: ColumnType;
    searchDisplayType: NodeEntriesDisplayType = NodeEntriesDisplayType.Table;
    searchSent: WritableSignal<boolean> = signal(false);
    @ViewChild('searchWrapperRef') searchWrapper!: NodeEntriesWrapperComponent<Node>;

    // collections tab
    collectionsGridColumns: ColumnType;
    collectionsTableColumns: ColumnType;
    collectionsDisplayType: WritableSignal<NodeEntriesDisplayType> = signal(
        NodeEntriesDisplayType.Tree,
    );
    collectionsDisplayTypeToggleDisabled = computed(
        () =>
            this.selectedTab() === TabType.COLLECTIONS &&
            this.searchSent() &&
            this.searchText() !== '',
    );
    dataSourceCollectionsTree: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    dataSourceCollectionsFlat: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    @ViewChild('collectionsWrapperRef') collectionsWrapper!: NodeEntriesWrapperComponent<Node>;

    // workspace tab
    dataSourceWorkspace: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    workspaceTreeConfig: TreeConfig = {
        showFileName: true,
    };
    @ViewChild('workspaceWrapperRef') workspaceWrapper!: NodeEntriesWrapperComponent<Node>;

    // upload tab
    inboxNode = toSignal(this.nodeHelperService.getDefaultInboxFolder(), { initialValue: null });
    connectorOptions: WritableSignal<OptionItem[]> = signal([]);

    // shared among tabs
    searchCompleted: WritableSignal<boolean> = signal(false);
    searchText = model('');
    // Is this component acting as the target our source?
    selectionMode = computed(() => (this.selectedSourceNodes().length > 0 ? 'target' : 'source'));

    constructor(
        private apiCollectionService: ApiCollectionService,
        private bridge: BridgeService,
        private networkService: NetworkService,
        private changeDetectorRef: ChangeDetectorRef,
        private collectionService: RestCollectionService,
        private localEventsService: LocalEventsService,
        private mdsHelperService: MdsHelperService,
        public nodeHelperService: NodeHelperService,
        public editorialSidebarService: EditorialSidebarService,
        private nodeService: NodeService,
        private restNodeService: RestNodeService,
        private suggestionsV1Service: SuggestionsV1Service,
        private uiService: UIService,
        private uploadDialogService: UploadDialogService,
        private authenticationService: AuthenticationService,
        private aboutService: AboutService,
        private connectorOptionsService: ConnectorOptionsService,
        private dialogs: DialogsService,
        private searchService: SearchService,
        private toast: Toast,
        private translate: TranslateService,
        private treeNodeService: TreeNodeService,
    ) {
        this.connectorOptionsService
            .buildOptions((connector) => void this.showCreateConnector({ connector }))
            .subscribe((options) => this.connectorOptions.set(options));
        effect(() => {
            const option = this.option();
            if (option?.optionConfig?.state) {
                this.selectedTab.set(option.optionConfig.state);
                void this.refreshData(option.optionConfig.state);
            }
            this.treeNodeService.setSelectionMode(
                option?.optionConfig?.selection?.selected?.length > 0 ? 'target' : 'source',
            );
            if (!this.canMoveWorkspaceNodes()) {
                this.workspaceAction.set('copy');
            }
        });
        // only allow copying collections if the target is a collection again
        this.treeNodeService.setCustomIsValidSourceCallback((node: Node) => {
            return (
                (this.parent()?.mediatype === 'collection' &&
                    node?.mediatype === 'collection' &&
                    this.parent()?.ref.id !== node?.ref.id &&
                    this.parent()?.parent?.id !== node?.ref.id) ||
                node?.type === RestConstants.CCM_TYPE_IO
            );
        });
    }

    /**
     * Initializes the component by definition the default columns for the collections data source.
     */
    async ngOnInit(): Promise<void> {
        if (this.selectedTab() === null) {
            this.selectedTab.set(this.supportedTabs()[0]);
            await this.refreshData(this.selectedTab());
        }
        this.searchColumns = await this.mdsHelperService.getColumnsByMdsId('search', {
            repository: HOME_REPOSITORY,
        });
        this.collectionsGridColumns = {
            Default: ListItem.getCollectionDefaults(),
        };
        this.collectionsTableColumns = await this.mdsHelperService.getColumnsByMdsId('search', {
            repository: HOME_REPOSITORY,
        });
    }

    /**
     * Listens to the selection change event to update the selected nodes.
     *
     * @param event
     */
    onNodeSelectionChange(event: any) {
        const selectedNodes = this.selectedNodes();
        event.added?.forEach((node: Node) => {
            selectedNodes.push(node);
        });
        event.removed?.forEach((node: Node) => {
            const selectedIndex = this.selectedNodes().indexOf(node);
            if (selectedIndex !== -1) {
                selectedNodes.splice(selectedIndex, 1);
            }
        });
        this.selectedNodes.set([...selectedNodes]);
    }

    /**
     * Listens to the change event of the extended values to update the according value.
     *
     * @param event
     */
    onExtendedValuesChange(event: MdsExtendedValues) {
        this.currentExtendedValues.set(event);
    }

    /**
     * Callback for the tab change.
     *
     * @param event
     */
    async onTabChange(event: MatTabChangeEvent) {
        // reset step information and individual variables
        this.currentStep.set(StepType.SELECT);
        this.collectionsDisplayType.set(NodeEntriesDisplayType.Tree);
        this.selectedNodes.set([]);
        this.searchText.set('');
        this.searchCompleted.set(false);
        this.searchSent.set(false);
        this.treeNodeService.resetData();
        // execute tab-specific actions
        switch (event.tab.id) {
            case this.idPrefix + TabType.SEARCH:
                this.selectedTab.set(TabType.SEARCH);
                break;
            case this.idPrefix + TabType.METHODOLOGY:
                this.selectedTab.set(TabType.METHODOLOGY);
                break;
            case this.idPrefix + TabType.COLLECTIONS:
                this.selectedTab.set(TabType.COLLECTIONS);
                break;
            case this.idPrefix + TabType.WORKSPACE:
                this.selectedTab.set(TabType.WORKSPACE);
                break;
            case this.idPrefix + TabType.UPLOAD:
                this.selectedTab.set(TabType.UPLOAD);
                break;
        }
        await this.refreshData(this.selectedTab());
    }

    /**
     * Executes the search query and updates the search datasource.
     */
    async executeSearch() {
        this.searchCompleted.set(false);
        this.searchSent.set(true);
        this.resetNodeEntriesSelections();
        if (this.selectedTab() === TabType.SEARCH) {
            this.dataSourceSearch.isLoading = true;
            // reset the search datasource if it is already initialized
            if (!this.dataSourceSearch.isEmpty()) {
                this.dataSourceSearch.reset();
            }
            const request = this.createSearchRequest();
            const searchResult: SearchResults = await firstValueFrom(
                this.searchService.search(request),
            );
            this.dataSourceSearch.setData(searchResult.nodes, searchResult.pagination);
            this.searchCompleted.set(true);
            this.dataSourceSearch.isLoading = false;
        } else if (this.selectedTab() === TabType.COLLECTIONS) {
            this.dataSourceCollectionsFlat.isLoading = true;
            // reset the flat datasource if it is already initialized
            if (!this.dataSourceCollectionsFlat.isEmpty()) {
                this.dataSourceCollectionsFlat.reset();
            }
            if (!this.searchText()) {
                this.dataSourceCollectionsFlat.setData([]);
            } else {
                const request = this.createSearchRequest(0, true);
                const searchResult: SearchResults = await firstValueFrom(
                    this.searchService.search(request),
                );
                this.dataSourceCollectionsFlat.setData(searchResult.nodes, searchResult.pagination);
            }
            this.collectionsDisplayType.set(NodeEntriesDisplayType.Table);
            this.searchCompleted.set(true);
            this.dataSourceCollectionsFlat.isLoading = false;
        }
    }

    /**
     * Clears the search text and executes the search again.
     */
    clearSearch(): void {
        this.searchText.set('');
        if (this.selectedTab() === TabType.SEARCH) {
            void this.executeSearch();
        } else if (this.selectedTab() === TabType.COLLECTIONS) {
            // reset type to tree view and reset variables
            this.collectionsDisplayType.set(NodeEntriesDisplayType.Tree);
            this.searchCompleted.set(false);
            this.searchSent.set(false);
        }
    }

    /**
     * Manually trigger the collections display type change.
     *
     * @param event
     */
    async onCollectionsDisplayTypeChange(event: MatButtonToggleChange): Promise<void> {
        const nextDisplayType = event.value;
        const existingDisplayType = this.collectionsDisplayType();
        this.resetNodeEntriesSelections();
        // switching from tree view into a flat view -> find the deepest level of the tree to be displayed
        if (
            existingDisplayType === NodeEntriesDisplayType.Tree &&
            [NodeEntriesDisplayType.SmallGrid, NodeEntriesDisplayType.Table].includes(
                nextDisplayType,
            )
        ) {
            // reset the flat datasource
            this.dataSourceCollectionsFlat = new NodeDataSource<Node | any>();
            this.dataSourceCollectionsFlat.isLoading = true;
            const deepestNode = this.findDeepestNodeFromDataMap(
                this.treeNodeService.getDataMap(),
            )?.node;
            if (!deepestNode) {
                this.dataSourceCollectionsFlat.isLoading = false;
                return;
            }
            // retrieve the children of the deepestNode to retrieve the level to be displayed
            const nodes = this.treeNodeService.getDataMap().get(deepestNode.parent.id);
            if (!nodes?.length) {
                this.dataSourceCollectionsFlat.isLoading = false;
                return;
            }
            this.dataSourceCollectionsFlat.setData(nodes);
            this.dataSourceCollectionsFlat.isLoading = false;
        }
        this.collectionsDisplayType.set(nextDisplayType);
    }

    /**
     * Reacts to es-add-material-dialog (dialogResult) output and forwards it to the upload dialog service.
     *
     * @param result
     */
    async uploadMaterialDialogResult(result: AddMaterialDialogResult): Promise<void> {
        if (!result) {
            return;
        }
        let createdNodes: Node[] | null;
        switch (result.kind) {
            case 'file':
                createdNodes = await this.uploadDialogService.uploadFilesAndCreateNodes(
                    {
                        ...result,
                        duplicateBehaviour:
                            this.option().optionConfig?.upload === 'fast' ? 'unique' : 'ask-user',
                    },
                    this.option().optionConfig?.upload !== 'fast',
                );
                break;
            case 'link':
                createdNodes = await this.uploadDialogService.createLinkNode(
                    result,
                    this.option().optionConfig?.upload !== 'fast',
                );
                break;
            default:
                break;
        }
        // note: the nodes are added to the inbox node if the upload was successful,
        //       thus, adding them to the collection is necessary
        if (createdNodes?.length) {
            this.emitNodes({ nodes: createdNodes, parent: this.parent() });
            if (this.parent() && this.nodeHelperService.isNodeCollection(this.parent())) {
                try {
                    this.toast.showProgressSpinner();
                    this.uiService.addToCollection(this.parent(), createdNodes, false, () => {
                        this.toast.closeProgressSpinner();
                    });
                } catch (e) {
                    console.error(e);
                    this.toast.closeProgressSpinner();
                }
            }
        }
    }

    // DRAG-AND-DROP RELATED FUNCTIONS
    /**
     * Do not allow dropping on the search references.
     */
    canDropOnSearchRef = (): CanDrop => {
        return {
            accept: false,
        };
    };

    /**
     * Return if something is dropped.
     */
    dropped = async () => {
        return;
    };

    /**
     * Allow dropping on collections.
     *
     * @param dragData
     */
    canDropOnCollection = (dragData: DragData<CollectionReference>): CanDrop => {
        // allow dropping if:
        // * access information is set (i.e., no fake node) and includes AddChildren permission,
        // * only files are dragged,
        // * the target is a collection,
        // * and the view context changed.
        return {
            accept:
                this.nodeHelperService.getNodesRight(
                    [dragData.target] as Node[],
                    RestConstants.ACCESS_ADD_CHILDREN,
                    NodesRightMode.Effective,
                ) &&
                dragData.draggedNodes.every((n) => n.type === 'ccm:io') &&
                this.nodeHelperService.isNodeCollection(dragData.target) &&
                !dragData.isFromOwnContainer,
        };
    };

    /**
     * Adds the dropped nodes to the collection.
     *
     * @param target
     * @param source
     */
    dropOnCollection = (target: Node, source: DropSource<CollectionReference>) => {
        try {
            this.toast.showProgressSpinner();
            this.uiService.addToCollection(target, source.element as Node[], false, () => {
                this.toast.closeProgressSpinner();
            });
        } catch (e) {
            console.error(e);
            this.toast.closeProgressSpinner();
        }
    };

    /**
     * Reacts to fetchData output of search datasource by loading further results.
     *
     * @param event
     * @param searchForCollections
     */
    async loadMore(event: FetchEvent, searchForCollections: boolean = false): Promise<void> {
        const dataSource = searchForCollections
            ? this.dataSourceCollectionsFlat
            : this.dataSourceSearch;
        if (!dataSource.hasMore() || dataSource.isLoading) {
            return;
        }

        dataSource.isLoading = true;
        const request = this.createSearchRequest(event.offset, searchForCollections);
        const searchResult: SearchResults = await firstValueFrom(
            this.searchService.search(request),
        );

        dataSource.appendData(searchResult.nodes);
        dataSource.isLoading = false;
    }

    /**
     * Insert into the selected target.
     */
    async insertSelectedNodes(): Promise<void> {
        const target = this.selectedNodes()[0] as Node;
        const source = this.selectedSourceNodes();
        if (target.mediatype === 'collection') {
            this.editorialSidebarService.sidebarLoading.set(true);
            this.uiService.addToCollection(target, source, false, () => {
                this.editorialSidebarService.sidebarLoading.set(false);
                this.editorialSidebarService.sidebarOpened.set(false);
            });
        } else if (target.mediatype === 'folder') {
            this.editorialSidebarService.sidebarLoading.set(true);
            try {
                await this.uiService.copyOrMoveNodes(source, target, this.workspaceAction());
                this.editorialSidebarService.sidebarOpened.set(false);
            } catch (e) {}
            this.editorialSidebarService.sidebarLoading.set(false);
        }
    }

    /**
     * Saves the enabled metadata to the currently selected nodes.
     */
    async saveMetadata(): Promise<void> {
        this.editorialSidebarService.sidebarLoading.set(true);
        const source = this.selectedSourceNodes();
        // convert the extended values to a flat object with the metadata keys as keys and the enabled values as values
        const values: MdsExtendedValues = this.currentExtendedValues() ?? {};
        const enabledMetadata: { [key: string]: string[] } = {};
        Object.entries(values)?.forEach(([key, value]) => {
            if (value === null || Array.isArray(value)) {
                return;
            }
            const enabledValues: string[] = Object.entries(value)
                .filter(([_, data]) => data.enabled)
                .map(([itemKey, _]) => itemKey);

            if (enabledValues.length > 0) {
                enabledMetadata[key] = enabledValues;
            }
        });
        let writeCount = 0,
            skippedCount = 0,
            suggestionCount = 0;
        if (Object.entries(enabledMetadata).length > 0) {
            for (let node of source) {
                // update meta state
                node = await firstValueFrom(
                    this.nodeService.getNode(node.ref.id, { repository: node.ref.repo }),
                );
                const props = node.properties;
                if (
                    this.nodeHelperService.getNodesRight(
                        [node],
                        RestConstants.PERMISSION_WRITE,
                        NodesRightMode.Effective,
                    )
                ) {
                    Object.entries(enabledMetadata).forEach(([key, value]) => {
                        if (props[key]) {
                            props[key].push(...value.filter((v) => !props[key].includes(v)));
                        } else {
                            props[key] = value;
                        }
                    });
                    await firstValueFrom(
                        this.nodeService.editNodeMetadata(
                            this.nodeHelperService.getOriginalId(node),
                            props,
                        ),
                    );
                    writeCount++;
                } else if (
                    (await this.authenticationService.hasToolpermission(
                        RestConstants.TOOLPERMISSION_SUGGESTION_WRITE,
                    )) &&
                    (await this.aboutService.hasPlugin(RestConstants.PLUGIN_MONGO))
                ) {
                    await firstValueFrom(
                        this.suggestionsV1Service.createSuggestions({
                            node: this.nodeHelperService.getOriginalId(node),
                            type: 'USER_PROPOSAL',
                            version: '',
                            repository: HOME_REPOSITORY,
                            body: Object.entries(enabledMetadata)
                                .map(([key, value]) =>
                                    value.map((v) => {
                                        return {
                                            propertyId: key,
                                            value: v,
                                            description:
                                                RestConstants.SUGGESTION_DESCRIPTION_METHODOLOGY,
                                            confidence: 1,
                                        } as CreateSuggestionRequestDto;
                                    }),
                                )
                                .flat(),
                        }),
                    );
                    suggestionCount++;
                } else {
                    skippedCount++;
                }
            }
        }
        this.editorialSidebarService.sidebarLoading.set(false);
        let msg: string[] = [];
        if (writeCount) {
            msg.push(
                await firstValueFrom(
                    this.translate.get(this.i18nPrefix + 'METHODOLOGY.METADATA_SAVED_WRITE', {
                        writeCount,
                    }),
                ),
            );
        }
        if (suggestionCount) {
            msg.push(
                await firstValueFrom(
                    this.translate.get(this.i18nPrefix + 'METHODOLOGY.METADATA_SUGGESTIONS', {
                        suggestionCount,
                    }),
                ),
            );
        }
        if (skippedCount) {
            msg.push(
                await firstValueFrom(
                    this.translate.get(this.i18nPrefix + 'METHODOLOGY.METADATA_SKIPPED', {
                        skippedCount,
                    }),
                ),
            );
        }
        this.toast.show({
            type: 'info',
            subtype: ToastType.InfoSimple,
            message: msg.join('\n'),
        });
    }

    /**
     * Copies the selected nodes into the currently opened view.
     */
    async copyNodes(): Promise<void> {
        if (!this.selectedNodes().length) {
            return;
        }
        const nodesToEmit = this.selectedNodes() as Node[];
        if (!this.parent()) {
            this.emitNodes({ nodes: nodesToEmit, parent: this.parent() });
            this.resetNodeEntriesSelections();
            return;
        }
        // only files are selected -> directly copy them, depending on the parent type
        if (this.onlyFilesSelected()) {
            try {
                this.toast.showProgressSpinner();
                if (this.nodeHelperService.isNodeCollection(this.parent())) {
                    this.uiService.addToCollection(this.parent(), nodesToEmit, false, () => {
                        this.resetNodeEntriesSelections();
                        this.toast.closeProgressSpinner();
                    });
                } else {
                    await this.uiService.copyOrMoveNodes(
                        nodesToEmit,
                        this.parent(),
                        this.workspaceAction(),
                    );
                    this.resetNodeEntriesSelections();
                    this.toast.closeProgressSpinner();
                }
            } catch (e) {
                console.error(e);
                this.toast.closeProgressSpinner();
                setTimeout(() => {
                    this.toast.error({}, this.i18nPrefix + 'COPY.ERROR');
                });
            }
            this.emitNodes({ nodes: nodesToEmit, parent: this.parent() });
        }
        // when there are not the only files selected, switch to the configuration mode
        else if (this.currentStep() === StepType.SELECT) {
            // fix that selected nodes (collections) might have reset their attributes to avoid toggling them
            const selectedNode: Node = await firstValueFrom(
                this.nodeService.getNode(this.highestSelectedNode().ref.id),
            );
            this.selectedNodes.set(
                this.selectedNodes().map((node) =>
                    node.ref.id === selectedNode.ref.id ? selectedNode : node,
                ),
            );
            // reset the default configuration and sync it with the view
            this.copyRoot.set(!!selectedNode.collection);
            this.copyChildCollections.set(selectedNode.collection?.childCollectionsCount > 0);
            this.copyRefs.set(selectedNode.collection?.childReferencesCount > 0);
            // switch into the configuration step
            this.currentStep.set(StepType.CONFIGURE);
            // load the children of the selected node to be able to update the number of references
            const selectedNodeChildren = await this.treeNodeService.getChildren(selectedNode);
            this.selectedNodeChildren.set(selectedNodeChildren);
        }
        // configuration step for collections
        else if (
            this.currentStep() === StepType.CONFIGURE &&
            this.atLeastRootOrChildrenSelected()
        ) {
            try {
                this.toast.showProgressSpinner();
                const selectedNode: Partial<Node> = this.highestSelectedNode();
                if (!selectedNode) {
                    return;
                }
                const copyParams = {
                    repository: HOME_REPOSITORY,
                    sourceCollection: selectedNode.ref.id,
                    targetCollection: this.parent().ref.id,
                    copyRoot: this.copyRoot(),
                    copyChildCollections: this.copyChildCollections(),
                    copyRefs: this.copyRefs(),
                    copyPermissions: true,
                };
                const copyResponse: Copy = await firstValueFrom(
                    this.apiCollectionService.copyCollection(copyParams),
                );
                const refsWithoutPublishPermission: number =
                    copyResponse?.entries?.filter(
                        (entry) => entry?.errorCode === 'NO_PUBLISH_PERMISSION',
                    )?.length ?? 0;
                const successCount =
                    (copyResponse?.entries?.length ?? 0) - refsWithoutPublishPermission;
                if (refsWithoutPublishPermission > 0) {
                    this.bridge.showTemporaryMessage(
                        MessageType.info,
                        'COLLECTIONS.TOAST.COPIED_NO_PUBLISH_PERMISSION',
                        { count: refsWithoutPublishPermission },
                    );
                } else {
                    this.bridge.showTemporaryMessage(MessageType.info, 'COLLECTIONS.TOAST.COPIED');
                }
                this.localEventsService.nodesChanged.emit([this.parent()]);
                if (copyResponse?.root) {
                    this.localEventsService.nodesCreated.emit([copyResponse.root]);
                }
                this.toast.closeProgressSpinner();
                this.goBack();
                this.emitNodes({ nodes: this.selectedNodes() as Node[], parent: this.parent() });
            } catch (e) {
                this.toast.closeProgressSpinner();
                setTimeout(() => {
                    this.bridge.showTemporaryMessage(
                        MessageType.error,
                        this.i18nPrefix + 'COPY.ERROR',
                    );
                });
            }
        }
    }

    /**
     * Sets the step back to the node selection and resets the selected nodes as the view is rendered again.
     */
    goBack() {
        this.currentStep.set(StepType.SELECT);
        this.resetNodeEntriesSelections();
        this.selectedNodes.set([]);
        this.selectedNodeChildren.set([]);
    }

    /**
     * Callback for the node click event that selects or deselects the clicked node.
     * Only one element can be selected at a time.
     * Multi-selection using strg/ctrl key is handled separately.
     *
     * @param source
     * @param event
     */
    selectOnClick(source: NodeEntriesWrapperComponent<Node>, event: NodeClickEvent<Node>) {
        // only one node can be selected at a time, so deselect all other nodes before selecting the new one
        const nodeAlreadySelected = source.getSelection().isSelected(event.element);
        // if multiple nodes are selected, the node should be selected again
        const multipleNodesSelected = source.getSelection().selected.length > 1;
        source.getSelection().clear();
        if (!nodeAlreadySelected || multipleNodesSelected) {
            source.getSelection().select(event.element);
        }
    }

    /**
     * View helper function to cast a partial node to a node.
     *
     * @param node
     */
    partialAsNode(node: Partial<Node>): Node {
        return node as Node;
    }

    /**
     * Helper function to initialize the search datasource.
     */
    private async updateSearchDataSource(): Promise<void> {
        this.dataSourceSearch.isLoading = true;
        this.dataSourceSearch.setData([]);
        await this.executeSearch();
        this.dataSourceSearch.isLoading = false;
    }

    /**
     * Helper function to initialize the collections datasource with (faked) nodes for "my" and "editorial" collections.
     */
    private async updateCollectionsDataSource(): Promise<void> {
        // return, if dataSource is already initialized
        if (!this.dataSourceCollectionsTree.isEmpty()) {
            return;
        }
        this.dataSourceCollectionsTree.isLoading = true;
        let initialData: Partial<Node>[] = [];
        const request = {
            sortBy: [this.nodeHelperService.getSortByForCollection(ROOT).active],
            sortAscending: this.nodeHelperService.getSortByForCollection(ROOT).direction === 'asc',
        };
        // recent collections
        const recentCollectionsNode: Partial<Node> = this.createFakeNode(
            this.translate.instant(this.i18nPrefix + 'COLLECTIONS.RECENT_COLLECTIONS'),
            'history_2',
            RestConstants.COLLECTIONSCOPE_RECENT,
            true,
        );
        const subRecentCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_RECENT,
                [PROPERTY_FILTER_ALL],
                request,
            ),
        );
        subRecentCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = recentCollectionsNode.ref.id;
            // reset childCollectionsCount and childReferencesCount to provide them as a flat list
            if (collection.collection) {
                collection.collection.childCollectionsCount = 0;
                collection.collection.childReferencesCount = 0;
            }
        });
        if (subRecentCollections.collections?.length) {
            initialData.push(recentCollectionsNode);
            initialData = initialData.concat(subRecentCollections.collections);
        }
        // my collections
        const myCollectionsNode: Partial<Node> = this.createFakeNode(
            this.translate.instant(this.i18nPrefix + 'COLLECTIONS.MY_COLLECTIONS'),
            'person',
            RestConstants.COLLECTIONSCOPE_MY,
            true,
        );
        const subMyCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_MY,
                [PROPERTY_FILTER_ALL],
                request,
            ),
        );
        subMyCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = myCollectionsNode.ref.id;
        });
        if (subMyCollections.collections?.length) {
            initialData.push(myCollectionsNode);
            initialData = initialData.concat(subMyCollections.collections);
        }
        // editorial collections
        const editorialCollectionsNode: Partial<Node> = this.createFakeNode(
            this.translate.instant(this.i18nPrefix + 'COLLECTIONS.EDITORIAL_COLLECTIONS'),
            'star',
            RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
            true,
        );
        const subEditorialCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                [PROPERTY_FILTER_ALL],
                request,
            ),
        );
        subEditorialCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = editorialCollectionsNode.ref.id;
        });
        if (subEditorialCollections.collections?.length) {
            initialData.push(editorialCollectionsNode);
            initialData = initialData.concat(subEditorialCollections.collections);
        }
        this.dataSourceCollectionsTree.setData(initialData);
        this.dataSourceCollectionsTree.isLoading = false;
    }

    /**
     * Helper function to initialize the workspace datasource with (faked) nodes for "my" and "shared" files.
     */
    private async updateWorkspaceDataSource(): Promise<void> {
        // return, if dataSource is already initialized
        if (!this.dataSourceWorkspace.isEmpty()) {
            return;
        }
        this.dataSourceWorkspace.isLoading = true;
        let initialData: Partial<Node>[] = [];
        const params = {
            filter: ['folders'],
            skipCount: 0,
            maxItems: 100,
            sortProperties: [RestConstants.CM_NAME],
            sortAscending: [true],
            propertiesFilter: RestConstants.ALL,
        };
        // my contents
        const myContentsNode: Partial<Node> = this.createFakeNode(
            this.translate.instant(this.i18nPrefix + 'WORKSPACE.MY_CONTENTS'),
            'person',
        );
        const subMyContents: Node[] = (
            await firstValueFrom(this.nodeService.getChildren(RestConstants.USERHOME, params))
        ).nodes;
        subMyContents?.forEach((node) => {
            // set the ID to the (fake) parent node
            node.parent.id = myContentsNode.ref.id;
        });
        if (subMyContents?.length) {
            initialData.push(myContentsNode);
            initialData = initialData.concat(subMyContents);
        }
        // shared contents
        const sharedContentsNode: Partial<Node> = this.createFakeNode(
            this.translate.instant(this.i18nPrefix + 'WORKSPACE.SHARED_CONTENTS'),
            'group',
        );
        const subSharedContents: Node[] = (
            await firstValueFrom(this.nodeService.getChildren(RestConstants.SHARED_FILES, params))
        ).nodes;
        subSharedContents?.forEach((node) => {
            // set the ID to the (fake) parent node
            node.parent.id = sharedContentsNode.ref.id;
        });
        if (subSharedContents?.length) {
            initialData.push(sharedContentsNode);
            initialData = initialData.concat(subSharedContents);
        }
        this.dataSourceWorkspace.setData(initialData);
        this.dataSourceWorkspace.isLoading = false;
    }

    /**
     * Helper function to create a fake node for the datasource, e.g., for the parent elements
     * of the collections tree.
     *
     * @param title
     * @param icon
     * @param scope
     * @param isCollection
     */
    private createFakeNode(
        title: string,
        icon: string,
        scope: string = '',
        isCollection: boolean = false,
    ): Partial<Node> {
        const node: Partial<Node> = {
            icon: {
                fontGlyphId: icon,
            },
            ref: {
                archived: false,
                id: uuidv4(),
                repo: HOME_REPOSITORY,
            },
            title,
            type: 'ccm:map',
        };
        if (isCollection) {
            node.collection = {
                fromUser: false,
                level0: false,
                scope,
                title,
                type: '',
            };
            node.mediatype = 'collection';
        } else {
            node.mediatype = 'folder';
        }
        return node;
    }

    /**
     * Helper function to retrieve the base search request parameters.
     *
     * @param skipCount
     * @param searchForCollections
     */
    private createSearchRequest(
        skipCount: number = 0,
        searchForCollections: boolean = false,
    ): SearchRequestParams {
        const criteria: MdsQueryCriteria[] = [
            {
                property: 'ngsearchword',
                values: [this.searchText()],
            },
        ];

        return {
            query: searchForCollections
                ? RestConstants.QUERY_NAME_COLLECTIONS
                : RestConstants.DEFAULT_QUERY_NAME,
            repository: HOME_REPOSITORY,
            maxItems: RestConnectorService.DEFAULT_NUMBER_PER_REQUEST,
            skipCount,
            propertyFilter: [PROPERTY_FILTER_ALL],
            contentType: searchForCollections ? 'COLLECTIONS' : 'FILES',
            metadataset: DEFAULT,
            sortProperties: [RestConstants.CM_MODIFIED_DATE],
            sortAscending: [false],
            body: {
                criteria,
                resolveCollections: true,
            },
        };
    }

    /**
     * Helper function to find the deepest node in a map containing refId to node children.
     *
     * @param dataMap
     */
    private findDeepestNodeFromDataMap(
        dataMap: Map<string, Partial<Node>[]>,
    ): { node: Partial<Node>; level: number } | null {
        const rootNodes = dataMap.get('__root__') || [];
        if (rootNodes.length === 0) return null;

        let deepestNode: Partial<Node> | null = null;
        let maxLevel = -1;

        // BFS queue: [node, level]
        const queue: [Partial<Node>, number][] = rootNodes.map((node) => [node, 0]);

        while (queue.length > 0) {
            const [currentNode, level] = queue.shift()!;

            // update deepest if current is deeper
            if (level > maxLevel) {
                maxLevel = level;
                deepestNode = currentNode;
            }

            // add children to queue
            const children = dataMap.get(currentNode.ref.id) || [];
            for (const child of children) {
                queue.push([child, level + 1]);
            }
        }

        return deepestNode ? { node: deepestNode, level: maxLevel } : null;
    }

    /**
     * Helper function to reset the selections of the node-entries-wrapper components.
     */
    private resetNodeEntriesSelections(): void {
        this.collectionsWrapper?.getSelection().clear();
        this.searchWrapper?.getSelection().clear();
        this.workspaceWrapper?.getSelection().clear();
    }

    /**
     * Opens the AddWithConnectorDialog for the picked connector. On confirm, a node is created in
     * the inbox, the connector edit window (pre-opened by the dialog as a user gesture) is
     * navigated to the connector URL, and the new node is emitted via applyNodeEmitted with
     * connectorId + window so consumers (e.g. submit-assignment) can poll for write-back.
     */
    private emitNodes(payload: {
        nodes: Node[];
        parent?: Node;
        connectorId?: string;
        window?: Window;
    }): void {
        this.editorialSidebarService.applyNodeEmitted.emit(payload);
        this.localEventsService.nodesCreated.emit(payload.nodes);
        this.option()?.optionConfig?.onNodesChoosen?.({
            nodes: payload.nodes,
            connectorId: payload.connectorId,
            window: payload.window,
        });
        if (this.option()?.optionConfig?.autoClose) {
            this.editorialSidebarService.close();
        }
    }

    async showCreateConnector(details: AddWithConnectorDialogData): Promise<void> {
        const dialogRef = await this.dialogs.openAddWithConnectorDialog(details);
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                void this.createConnector(details.connector, result);
            }
        });
    }

    private async createConnector(
        connector: Connector,
        event: AddWithConnectorDialogResult,
    ): Promise<void> {
        const props = this.nodeHelperService.propertiesFromConnector(event);
        this.restNodeService
            .createNode(this.inboxNode().ref.id, RestConstants.CCM_TYPE_IO, [], props, false)
            .subscribe(
                (data) => {
                    void this.uiService.editConnector(data.node, {
                        type: event.type as any,
                        win: event.window,
                        connectorType: connector,
                    });
                    this.emitNodes({
                        nodes: [data.node],
                        parent: this.parent(),
                        connectorId: connector.id,
                        window: event.window,
                    });
                },
                (error: any) => {
                    event.window?.close();
                    this.nodeHelperService.handleNodeError(event.name, error);
                },
            );
    }

    protected readonly DEFAULT = DEFAULT;
    protected readonly InteractionType = InteractionType;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly Scope = Scope;
    protected readonly TabType = TabType;

    private async refreshData(tabType: TabType) {
        switch (tabType) {
            case TabType.SEARCH:
                await this.updateSearchDataSource();
                break;
            case TabType.METHODOLOGY:
                break;
            case TabType.COLLECTIONS:
                await this.updateCollectionsDataSource();
                break;
            case TabType.WORKSPACE:
                await this.updateWorkspaceDataSource();
                break;
            case TabType.UPLOAD:
                break;
        }
    }
}
