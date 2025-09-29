import {
    Component,
    computed,
    Input,
    OnInit,
    Signal,
    signal,
    ViewChild,
    WritableSignal,
} from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import {
    HOME_REPOSITORY,
    MdsQueryCriteria,
    Node,
    NodeService,
    PROPERTY_FILTER_ALL,
    SearchRequestParams,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    ColumnType,
    FetchEvent,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { CollectionSubcollections } from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestCollectionService } from '../../../core-module/rest/services/rest-collection.service';
import { MainNavConfig, MainNavService } from '../../../main/navigation/main-nav.service';
import { Toast } from '../../../services/toast';
import { SharedModule } from '../../../shared/shared.module';
import { UIService } from '../../../core-module/rest/services/ui.service';

enum TabType {
    SEARCH = 'search',
    COLLECTIONS = 'collections',
    WORKSPACE = 'workspace',
    UPLOAD = 'upload',
}

enum StepType {
    SELECT = 'select',
    CONFIGURE = 'configure',
}

@Component({
    selector: 'es-nodes-selector',
    templateUrl: 'nodes-selector.component.html',
    styleUrls: ['nodes-selector.component.scss'],
    imports: [SharedModule],
    providers: [NodeEntriesService],
})
export class NodesSelectorComponent implements OnInit {
    protected readonly i18nPrefix: string = 'EDITORIAL.OPTIONS.SORT_INTO_TAB.';
    protected readonly idPrefix: string = 'nodes-selector-tab';

    @Input() parent: Node;

    selectedTab: WritableSignal<TabType> = signal(TabType.SEARCH);
    selectedNodes: WritableSignal<Partial<Node>[]> = signal([]);
    private currentStep: WritableSignal<StepType> = signal(StepType.SELECT);
    isSelectStep: Signal<boolean> = computed((): boolean => this.currentStep() === StepType.SELECT);
    onlyOneSelected: Signal<boolean> = computed(() => this.selectedNodes().length === 1);
    onlyFilesSelected: Signal<boolean> = computed((): boolean =>
        this.selectedNodes().every((node) => node.type === RestConstants.CCM_TYPE_IO),
    );
    isValidSelection: Signal<boolean> = computed(
        (): boolean => this.onlyOneSelected() || this.onlyFilesSelected(),
    );
    configOption = {
        includeMain: false,
        includeSub: false,
        includeItems: false,
    };
    // main nav config necessary for triggering the copy process
    mainNavConfig: MainNavConfig;

    // search tab
    searchColumns: ColumnType;
    dataSourceSearch: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    searchDisplayType: NodeEntriesDisplayType = NodeEntriesDisplayType.Table;
    searchText: string = '';
    searchSent: WritableSignal<boolean> = signal(false);
    @ViewChild('actionbarReferences') actionbarReferences: ActionbarComponent;

    // collections tab
    collectionsColumns: ColumnType;
    dataSourceCollections: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();

    // workspace tab
    workspaceColumns: ColumnType;
    dataSourceWorkspace: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();

    constructor(
        private collectionService: RestCollectionService,
        private mainNavService: MainNavService,
        private nodeService: NodeService,
        private uiService: UIService,
        private searchService: SearchService,
        private toast: Toast,
    ) {}

    /**
     * Initializes the component by definition the default columns for the collections data source.
     */
    async ngOnInit(): Promise<void> {
        this.searchColumns = {
            Default: [new ListItem('NODE', RestConstants.CM_PROP_TITLE)],
        };
        this.collectionsColumns = {
            Default: ListItem.getCollectionDefaults(),
        };
        this.workspaceColumns = {
            Default: ListItem.getCollectionDefaults(),
        };
        this.mainNavConfig = await firstValueFrom(this.mainNavService.observeMainNavConfig());
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
        this.selectedNodes.update(() => [...selectedNodes]);
    }

    /**
     * Callback for the tab change.
     *
     * @param event
     */
    async onTabChange(event: MatTabChangeEvent) {
        // reset step information and individual variables
        this.currentStep.set(StepType.SELECT);
        this.selectedNodes.update(() => []);
        this.searchText = '';
        // execute tab-specific actions
        switch (event.tab.id) {
            case this.idPrefix + TabType.SEARCH:
                this.selectedTab.set(TabType.SEARCH);
                await this.updateSearchDataSource();
                break;
            case this.idPrefix + TabType.COLLECTIONS:
                this.selectedTab.set(TabType.COLLECTIONS);
                await this.updateCollectionsDataSource();
                break;
            case this.idPrefix + TabType.WORKSPACE:
                this.selectedTab.set(TabType.WORKSPACE);
                await this.updateWorkspaceDataSource();
                break;
            case this.idPrefix + TabType.UPLOAD:
                this.selectedTab.set(TabType.UPLOAD);
                break;
            default:
                console.log('onTabChange', event);
        }
    }

    /**
     * Executes the search query and updates the search datasource.
     */
    async executeSearch() {
        this.dataSourceSearch.isLoading = true;
        this.searchSent.set(true);
        this.selectedNodes.update(() => []);

        // reset the search datasource, if it is already initialized
        if (!this.dataSourceSearch.isEmpty()) {
            this.dataSourceSearch.reset();
        }

        if (!this.searchText) {
            this.dataSourceSearch.setData([]);
            this.dataSourceSearch.isLoading = false;
            return;
        }

        const request = this.createSearchRequest();
        const searchResult: SearchResults = await firstValueFrom(
            this.searchService.search(request),
        );

        this.dataSourceSearch.setData(searchResult.nodes, searchResult.pagination);
        this.dataSourceSearch.isLoading = false;
    }

    /**
     * Clears the search text and executes the search again.
     */
    clearSearch(): void {
        this.searchText = '';
        void this.executeSearch();
    }

    /**
     * Reacts to fetchData output of search datasource by loading further results.
     *
     * @param event
     */
    async loadMore(event: FetchEvent): Promise<void> {
        if (!this.dataSourceSearch.hasMore() || this.dataSourceSearch.isLoading) {
            return;
        }

        this.dataSourceSearch.isLoading = true;
        const request = this.createSearchRequest(event.offset);
        const searchResult: SearchResults = await firstValueFrom(
            this.searchService.search(request),
        );

        this.dataSourceSearch.appendData(searchResult.nodes);
        this.dataSourceSearch.isLoading = false;
    }

    /**
     * Copies the selected nodes into the currently opened view.
     */
    async copyNodes(): Promise<void> {
        if (!this.parent || !this.selectedNodes().length) {
            return;
        }
        if (this.onlyFilesSelected()) {
            try {
                this.uiService.addToCollection(this.parent, this.selectedNodes() as Node[], false);
            } catch (e) {
                console.error(e);
                this.toast.error({}, 'Der gewählte Inhalt existiert bereits in der Sammlung.');
            }
        } else {
            this.currentStep.set(StepType.CONFIGURE);
        }
    }

    /**
     * Sets the step back to the node selection and resets the selected nodes as the view is rendered again.
     */
    goBack() {
        this.currentStep.set(StepType.SELECT);
        this.selectedNodes.update(() => []);
    }

    /**
     * Helper function to initialize the search datasource.
     */
    private async updateSearchDataSource(): Promise<void> {
        this.dataSourceSearch.isLoading = true;
        this.dataSourceSearch.setData([]);
        this.dataSourceSearch.isLoading = false;
    }

    /**
     * Helper function to initialize the collections datasource with (faked) nodes for "my" and "editorial" collections.
     */
    private async updateCollectionsDataSource(): Promise<void> {
        // return, if dataSource is already initialized
        if (!this.dataSourceCollections.isEmpty()) {
            return;
        }
        this.dataSourceCollections.isLoading = true;
        let initialData: Partial<Node>[] = [];
        const request = {
            sortBy: [RestConstants.CM_PROP_TITLE],
            sortAscending: true,
        };
        // my collections
        const myCollectionsNode: Partial<Node> = this.createFakeNode(
            'Meine Sammlungen',
            'person',
            RestConstants.COLLECTIONSCOPE_MY,
            true,
        );
        const subMyCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_MY,
                [],
                request,
            ),
        );
        subMyCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = myCollectionsNode.ref.id;
        });
        initialData.push(myCollectionsNode);
        initialData = initialData.concat(subMyCollections.collections);
        // editorial collections
        const editorialCollectionsNode: Partial<Node> = this.createFakeNode(
            'Redaktionelle Sammlungen',
            'star',
            RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
            true,
        );
        const subEditorialCollections: CollectionSubcollections = await firstValueFrom(
            this.collectionService.getCollectionSubcollections(
                RestConstants.ROOT,
                RestConstants.COLLECTIONSCOPE_TYPE_EDITORIAL,
                [],
                request,
            ),
        );
        subEditorialCollections.collections?.forEach((collection) => {
            // set the ID to the (fake) parent node
            collection.parent.id = editorialCollectionsNode.ref.id;
        });
        initialData.push(editorialCollectionsNode);
        initialData = initialData.concat(subEditorialCollections.collections);
        this.dataSourceCollections.setData(initialData);
        this.dataSourceCollections.isLoading = false;
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
        const myContentsNode: Partial<Node> = this.createFakeNode('Meine Inhalte', 'person');
        const subMyContents: Node[] = (
            await firstValueFrom(this.nodeService.getChildren(RestConstants.USERHOME, params))
        ).nodes;
        subMyContents?.forEach((node) => {
            // set the ID to the (fake) parent node
            node.parent.id = myContentsNode.ref.id;
        });
        initialData.push(myContentsNode);
        initialData = initialData.concat(subMyContents);
        // shared contents
        const sharedContentsNode: Partial<Node> = this.createFakeNode(
            'Gemeinsame Inhalte',
            'group',
        );
        const subSharedContents: Node[] = (
            await firstValueFrom(this.nodeService.getChildren(RestConstants.SHARED_FILES, params))
        ).nodes;
        subSharedContents?.forEach((node) => {
            // set the ID to the (fake) parent node
            node.parent.id = sharedContentsNode.ref.id;
        });
        initialData.push(sharedContentsNode);
        initialData = initialData.concat(subSharedContents);
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
            collection: {
                fromUser: false,
                level0: false,
                scope,
                title,
                type: '',
            },
            preview: {
                isIcon: true,
                height: 20,
                url: icon,
                width: 20,
            },
            ref: {
                archived: false,
                id: uuidv4(),
                repo: HOME_REPOSITORY,
            },
            title,
        };
        if (isCollection) {
            node.mediatype = 'collection';
        }
        return node;
    }

    /**
     * Helper function to retrieve the base search request parameters.
     */
    private createSearchRequest(skipCount: number = 0): SearchRequestParams {
        const criteria: MdsQueryCriteria[] = [
            {
                property: 'ngsearchword',
                values: [this.searchText],
            },
        ];

        return {
            query: 'ngsearch',
            repository: HOME_REPOSITORY,
            maxItems: 51,
            skipCount,
            propertyFilter: [PROPERTY_FILTER_ALL],
            contentType: 'ALL',
            metadataset: '-default-',
            sortProperties: ['cm:created'],
            sortAscending: [true],
            body: {
                criteria,
                resolveCollections: true,
            },
        };
    }

    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly Scope = Scope;
    protected readonly TabType = TabType;
}
