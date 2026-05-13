import { NgTemplateOutlet } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    input,
    Input,
    InputSignal,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    signal,
    SimpleChanges,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
    ViewEncapsulation,
    WritableSignal,
} from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
    HOME_REPOSITORY,
    MdsQueryCriteria,
    MdsService,
    Node,
    Pagination,
    PROPERTY_FILTER_ALL,
    SearchRequestParams,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    CheckTextOverflowDirective,
    ColumnType,
    CustomOptions,
    EduSharingUiModule,
    GridConfig,
    Helper,
    InteractionType,
    MdsHelperService,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    NodePersonNamePipe,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    UIService,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { PreviewSidebarService } from '../../../../../features/preview-sidebar/preview-sidebar.service';
import { OptionsHelperService } from '../../../shared/services/options-helper.service';
import { TopicPageHelperService } from '../../../shared/services/topic-page-helper.service';
import { GenericNodeEntriesDisplayType } from '../../../shared/types/generic-node-entries-display-type';
import { GenericWidgetGlobalService } from '../../generic-widget/generic-widget-global.service';

export interface DisplayTypeComponentInterface {
    // inputs
    contextNodeId: string;
    criteria: MdsQueryCriteria[];
    selectedNode: Node;
    // outputs
    itemClicked: EventEmitter<Node>;
    totalSearchResultCountChanged: EventEmitter<number>;
    visibleNodesChanged: EventEmitter<Node[]>;
    // methods
    setDataSource(resetNecessary: boolean, skipCount?: number): Promise<void>;
}
export enum CustomCardRole {
    /**
     * card that should incentivize people to propose contents
     * Is injected somewhere in the swimlane
     */
    SuggestContent,
}

@Component({
    selector: 'es-node-entries',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [EduSharingUiModule, MatTooltipModule, NgTemplateOutlet],
    providers: [
        CheckTextOverflowDirective,
        NodePersonNamePipe,
        OptionsHelperDataService,
        OptionsHelperService,
    ],
    templateUrl: './generic-node-entries.component.html',
    styleUrls: ['./generic-node-entries.component.scss'],
})
export class GenericNodeEntriesComponent implements OnChanges, OnDestroy, OnInit {
    @ViewChild('customType', { read: ViewContainerRef, static: false })
    customType!: ViewContainerRef;
    @ViewChild('customType') customTypeElement!: ElementRef<HTMLElement>;

    private readonly CUSTOM_CARD_POSITION_INDEX: number = 6;
    private readonly DEFAULT_WIDTH: string = '314px';
    private readonly FULL_WIDTH: string = 'max(314px, 100% - 200px)';
    // taxonomy URL
    // TODO: make this configurable
    private readonly DISCIPLINES_TAXONOMY_URL: string =
        'https://vocabs.openeduhub.de/w3id.org/openeduhub/vocabs/discipline/index.json';
    private readonly UNIVERSITY_DISCIPLINES_TAXONOMY_URL: string =
        'https://vocabs.openeduhub.de/w3id.org/openeduhub/vocabs/hochschulfaechersystematik/index.json';
    private readonly LRT_TAXONOMY_URL: string =
        'https://vocabs.openeduhub.de/w3id.org/openeduhub/vocabs/new_lrt/index.json';
    private readonly OEH_LRT_TAXONOMY_URL: string =
        'https://vocabs.openeduhub.de/w3id.org/openeduhub/vocabs/oehMetadatasets/index.json';
    private readonly TOPICS_TAXONOMY_URL: string =
        'https://vocabs.openeduhub.de/w3id.org/openeduhub/vocabs/oeh-topics/5e40e372-735c-4b17-bbf7-e827a5702b57.json';

    @Input() blacklistedNodeIds: string[] = [];
    @Input() contextNodeId: string;
    @Input() criteria: MdsQueryCriteria[] = [];
    @Input() gridConfig: GridConfig = {
        layout: 'scroll',
    };
    private _hasEditRightsAndIsEditMode: boolean;
    customTypeInstance: DisplayTypeComponentInterface;
    @Input() get hasEditRightsAndIsEditMode(): boolean {
        return this._hasEditRightsAndIsEditMode;
    }
    set hasEditRightsAndIsEditMode(val: boolean) {
        this._hasEditRightsAndIsEditMode = val;
        // check whether the selectedNodeIds do still exist in the view mode
        if (!val && this.selectedNodeIds?.[0]) {
            const filteredNodes: Node[] = this.allRequestedNodes.filter(
                (node: Node) => !this.blacklistedNodeIds.includes(node.ref.id),
            );
            // selected node is not included anymore
            if (!filteredNodes.map((node: Node) => node.ref.id).includes(this.selectedNodeIds[0])) {
                // send an update
                this.itemClicked.emit(null);
            }
        }
    }
    includeCustomCard: InputSignal<boolean> = input(true);
    @Input() lastSearchUpdate: Date | null;
    private _layout: GenericNodeEntriesDisplayType;
    @Input() get layout() {
        return this._layout;
    }
    set layout(val: GenericNodeEntriesDisplayType) {
        let newDisplayType: NodeEntriesDisplayType;
        // match layout of GenericNodeEntries to NodeEntriesDisplayType from ngx-edu-sharing-ui
        switch (val) {
            case GenericNodeEntriesDisplayType.SingleView:
                newDisplayType = NodeEntriesDisplayType.Grid;
                this.gridConfig.layout = 'scroll';
                void this.nodeEntries?.ngOnChanges();
                this.elementRef.nativeElement.style.setProperty('--cardWidth', this.FULL_WIDTH);
                break;
            case GenericNodeEntriesDisplayType.SplitView:
                newDisplayType = NodeEntriesDisplayType.Grid;
                this.gridConfig.layout = 'scroll';
                void this.nodeEntries?.ngOnChanges();
                this.elementRef.nativeElement.style.setProperty('--cardWidth', this.FULL_WIDTH);
                break;
            case GenericNodeEntriesDisplayType.StandardView:
                newDisplayType = NodeEntriesDisplayType.Grid;
                this.gridConfig.layout = 'scroll';
                void this.nodeEntries?.ngOnChanges();
                this.elementRef.nativeElement.style.setProperty('--cardWidth', this.DEFAULT_WIDTH);
                break;
            case GenericNodeEntriesDisplayType.CompactView:
                newDisplayType = NodeEntriesDisplayType.Grid;
                this.gridConfig.layout = 'grid';
                void this.nodeEntries?.ngOnChanges();
                this.elementRef.nativeElement.style.setProperty('--cardWidth', this.DEFAULT_WIDTH);
                break;
            case GenericNodeEntriesDisplayType.ListView:
                newDisplayType = NodeEntriesDisplayType.Table;
                void this.nodeEntries?.ngOnChanges();
                this.elementRef.nativeElement.style.setProperty('--cardWidth', this.DEFAULT_WIDTH);
                break;
            default:
                if (this.genericWidgetGlobalService.hasCustomDisplayType(val)) {
                    void this.genericWidgetGlobalService
                        .getCustomDisplayTypeComponent(val)
                        .then((componentClass) => {
                            // inject the component into the widget container
                            this.customTypeInstance = this.uiService.injectAngularComponent(
                                this.customType,
                                componentClass,
                                this.customTypeElement.nativeElement,
                                {
                                    contextNodeId: this.contextNodeId,
                                    criteria: this.criteria,
                                    selectedNode: this.selectedNode(),
                                } as unknown as Partial<DisplayTypeComponentInterface>,
                                { replace: false },
                            ).instance;
                            // listen to outputs of custom type instance
                            this.setupCustomTypeInstanceOutputs();
                        });
                }
        }
        // emit an event that the display type has been changed, in case it is not the initial change
        // TODO: use es-node-entries-wrappers displayTypeChanges event
        if (this._layout !== undefined) {
            setTimeout((): void => {
                this.onDisplayTypeChanged();
            }, 500);
        }
        this._layout = val;
        this.nodeEntriesDisplayType.set(newDisplayType);
    }
    // load three rows of nodes at once on desktop (-1 due to propose card being added)
    @Input() maxItems: number = 11;
    @Input() mds: string | null = null;
    @Input() queryId: string = RestConstants.DEFAULT_QUERY_NAME;
    @HostBinding('style.--scroll-gradient-color') @Input() scrollGradientColor: string = '#fff';
    @Input() searchText: string;
    @Output() blacklistChanged: EventEmitter<string> = new EventEmitter<string>();
    @Output() displayTypeChanged: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() includeCardChanged: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() itemClicked: EventEmitter<Node> = new EventEmitter<Node>();
    @Output() totalSearchResultCountChanged: EventEmitter<number> = new EventEmitter<number>();
    @Output() visibleNodesChanged: EventEmitter<Node[]> = new EventEmitter<Node[]>();

    // es-node-entries-wrapper
    @ViewChild('contentWrapper') nodeEntries: NodeEntriesWrapperComponent<Node>;
    // es-custom-propose-content-card
    @ViewChild('cardSuggest') cardSuggestRef: TemplateRef<unknown>;

    private readonly i18nPrefix: string = 'TOPIC_PAGE.WIDGET.CONTENT_TEASER.';

    // criteria extension related fields
    private propertyToTaxonomiesMatching: Map<string, string[]> = new Map<string, string[]>([
        ['ccm:curriculum', [this.TOPICS_TAXONOMY_URL]],
        ['virtual:collection_id_primary', [this.TOPICS_TAXONOMY_URL]],
        ['virtual:oeh_lrt', [this.LRT_TAXONOMY_URL, this.OEH_LRT_TAXONOMY_URL]],
        [
            'virtual:taxonid',
            [this.DISCIPLINES_TAXONOMY_URL, this.UNIVERSITY_DISCIPLINES_TAXONOMY_URL],
        ],
    ]);
    private taxonomyToJsonMatching: Map<string, any> = new Map<string, any>();
    private taxonomiesRetrieved: boolean = false;

    // custom names for blacklisted and selected item(s)
    private allRequestedNodes: Node[] = [];
    private blacklistedClassName: string = 'blacklisted';
    private selectedClassName: string = 'selected';
    columns: ColumnType;
    tableColumns: ColumnType;
    private customOptions: CustomOptions = {};
    dataSource: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    private destroy$ = new Subject<void>();

    nodeEntriesDisplayType: WritableSignal<NodeEntriesDisplayType> = signal(
        NodeEntriesDisplayType.Grid,
    );
    selectedNode: WritableSignal<Node> = signal(null);
    private selectedNodeIds: string[] = [];
    sidebarOpen: WritableSignal<boolean> = signal(false);

    /**
     * Returns, whether at least one item is selected.
     */
    private get atLeastOneItemSelected(): boolean {
        return this.selectedNodeIds?.length > 0;
    }

    constructor(
        private elementRef: ElementRef,
        public genericWidgetGlobalService: GenericWidgetGlobalService,
        private mdsHelperService: MdsHelperService,
        private mdsService: MdsService,
        private previewSidebarService: PreviewSidebarService,
        private searchService: SearchService,
        private topicPageHelperService: TopicPageHelperService,
        private uiService: UIService,
    ) {
        // subscribe to changes on the selected node
        this.previewSidebarService
            .getCurrentNode()
            .pipe(takeUntil(this.destroy$))
            .subscribe((node: Node | null): void => {
                const selectedNode: Node = node;
                if (selectedNode?.ref.id) {
                    // overwrite the array instead of pushing to it as only one node can be selected
                    this.selectedNodeIds = [selectedNode.ref.id];
                    this.selectedNode.set(selectedNode);
                } else {
                    this.selectedNodeIds = [];
                    this.selectedNode.set(null);
                }
                // update the selected node if a custom type instance exists
                if (this.customTypeInstance) {
                    this.customTypeInstance.selectedNode = this.selectedNode();
                }
                let nodes: Node[] = this.allRequestedNodes;
                if (!this.hasEditRightsAndIsEditMode) {
                    nodes = this.allRequestedNodes.filter(
                        (node: Node) => !this.blacklistedNodeIds.includes(node.ref.id),
                    );
                }
                this.dynamicallyAddCssClasses(nodes, this.selectedNodeIds, this.selectedClassName);
                if (!this.nodeEntries) {
                    return;
                }
                this.sidebarOpen.set(this.atLeastOneItemSelected);
                // check whether the selection matches the currently selected node
                // sidebar was closed, but selection still exists -> clear selection
                if (!this.sidebarOpen() && this.nodeEntries.getSelection().selected?.length > 0) {
                    this.nodeEntries.getSelection().clear();
                }
                // sidebar is opened, but the selection does not match
                else if (
                    this.sidebarOpen() &&
                    this.nodeEntries.getSelection().selected?.length > 0 &&
                    this.nodeEntries.getSelection().selected[0].ref.id !== this.selectedNodeIds[0]
                ) {
                    this.nodeEntries.getSelection().setSelection(selectedNode);
                }
            });
    }

    /**
     * Initializes the translations service, columns and custom options.
     */
    async ngOnInit(): Promise<void> {
        // specify columns
        if (!this.columns || !this.tableColumns) {
            const mds = await firstValueFrom(
                this.mdsService.getMetadataSet({
                    repository: HOME_REPOSITORY,
                    metadataSet: this.genericWidgetGlobalService.getDefaultMds(),
                }),
            );
            this.columns = this.mdsHelperService.getColumns(mds, 'genericWidget');
            this.tableColumns = this.mdsHelperService.getColumns(mds, 'genericWidgetTable');
        }
        // specify addOptions
        if (!this.customOptions?.addOptions) {
            this.customOptions = this.retrieveCustomOptions();
        }
    }

    /**
     * Retrieves the custom options to be displayed in the node entries component.
     */
    retrieveCustomOptions(): CustomOptions {
        const changeOnInspectionTable: OptionItem = new OptionItem(
            this.i18nPrefix + 'CHANGE_ON_INSPECTION_TABLE',
            'assignment_turned_in',
            (node: any, nodes: any[]): void => {
                // if showAlways is used, the node is set, otherwise it has to be retrieved using the activeObjects of the optionsHelper
                // the workaround of using the activeObjects of the optionsHelper does not work, when showAlways is set
                const nodeToChange: Node =
                    node ?? this.nodeEntries.optionsHelper.getData()?.activeObjects?.[0] ?? null;
                if (nodeToChange) {
                    this.topicPageHelperService.openChangeOnInspectionTableLink(nodeToChange);
                }
            },
        );
        changeOnInspectionTable.showAlways = true;
        changeOnInspectionTable.isPrimary = true;
        changeOnInspectionTable.customShowCallback = async (): Promise<boolean> =>
            this.hasEditRightsAndIsEditMode;
        const removeFromWidget: OptionItem = new OptionItem(
            this.i18nPrefix + 'REMOVE_FROM_WIDGET',
            'visibility',
            async (node: any, nodes: any[]): Promise<void> => {
                // if showAlways is used, the node is set, otherwise it has to be retrieved using the activeObjects of the optionsHelper
                // the workaround of using the activeObjects of the optionsHelper does not work, when showAlways is set
                const nodeToRemove: Node =
                    node ?? this.nodeEntries.optionsHelper.getData()?.activeObjects?.[0] ?? null;
                if (nodeToRemove) {
                    this.blacklistChanged.emit(nodeToRemove.ref.id);
                    if (this.hasEditRightsAndIsEditMode) {
                        this.dynamicallyAddCssClasses(
                            this.allRequestedNodes,
                            this.blacklistedNodeIds,
                            this.blacklistedClassName,
                        );
                    }
                }
            },
        );
        removeFromWidget.showAlways = true;
        removeFromWidget.isPrimary = true;
        removeFromWidget.customShowCallback = async (nodes: Node[]): Promise<boolean> =>
            this.hasEditRightsAndIsEditMode &&
            !this.blacklistedNodeIds.includes(nodes?.[0]?.ref?.id);
        const addToWidget: OptionItem = new OptionItem(
            this.i18nPrefix + 'ADD_TO_WIDGET',
            'visibility_off',
            async (node: any, nodes: any[]): Promise<void> => {
                // if showAlways is used, the node is set, otherwise it has to be retrieved using the activeObjects of the optionsHelper
                // the workaround of using the activeObjects of the optionsHelper does not work, when showAlways is set
                const nodeToAdd: Node =
                    node ?? this.nodeEntries.optionsHelper.getData()?.activeObjects?.[0] ?? null;
                if (nodeToAdd) {
                    this.blacklistChanged.emit(nodeToAdd.ref.id);
                    if (this.hasEditRightsAndIsEditMode) {
                        this.dynamicallyAddCssClasses(
                            this.allRequestedNodes,
                            this.blacklistedNodeIds,
                            this.blacklistedClassName,
                        );
                    }
                }
            },
        );
        addToWidget.showAlways = true;
        addToWidget.isPrimary = true;
        addToWidget.customShowCallback = async (nodes: Node[]): Promise<boolean> =>
            this.hasEditRightsAndIsEditMode &&
            this.blacklistedNodeIds.includes(nodes?.[0]?.ref?.id);
        // note: these options do not have a visible effect
        // .showAsAction = true;
        // .showName = true;
        // .isSeparate = true;
        // .isEnabled = true;
        return {
            addOptions: [changeOnInspectionTable, removeFromWidget, addToWidget],
        };
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Reloads the view by (re)setting the datasource and node-entries-wrapper.
     *
     * @param changes
     */
    async ngOnChanges(changes: SimpleChanges): Promise<void> {
        // check if changes were really made and return, if not
        let changedMade: boolean = false;
        Object.keys(changes).forEach((key: string): void => {
            const change = changes[key];
            if (
                change &&
                (change.firstChange ||
                    JSON.stringify(change.currentValue) !== JSON.stringify(change.previousValue))
            ) {
                changedMade = true;
            }
        });
        if (!changedMade) {
            return;
        }
        this.dataSource.isLoading = true;
        const resetOnFirstLoad: boolean = Object.keys(changes).length > 1;
        const resetDueToCriteriaChange: boolean =
            changes.criteria?.previousValue !== changes.criteria?.currentValue;
        // update the criteria if a custom type instance exists
        if (this.customTypeInstance && changes.criteria) {
            this.customTypeInstance.criteria = changes.criteria.currentValue;
        }
        await this.setDataSource(resetOnFirstLoad || resetDueToCriteriaChange);
        // workaround for updating showAlways options: override customOptions variable
        this.customOptions = this.retrieveCustomOptions();
        await this.nodeEntries?.initOptionsGenerator({
            customOptions: this.hasEditRightsAndIsEditMode
                ? this.customOptions
                : { addOptions: [] },
        });
        this.dataSource.isLoading = false;
        // add blacklisted class names for edit mode
        if (this.hasEditRightsAndIsEditMode) {
            this.dynamicallyAddCssClasses(
                this.allRequestedNodes,
                this.blacklistedNodeIds,
                this.blacklistedClassName,
            );
        }
        // add selected class name
        let nodes: Node[] = this.allRequestedNodes;
        if (!this.hasEditRightsAndIsEditMode) {
            nodes = this.allRequestedNodes.filter(
                (node: Node) => !this.blacklistedNodeIds.includes(node.ref.id),
            );
        }
        this.dynamicallyAddCssClasses(nodes, this.selectedNodeIds, this.selectedClassName);
        if (!this.nodeEntries) {
            return;
        }
        this.sidebarOpen.set(this.atLeastOneItemSelected);
        // check, whether the selection matches the currently selected node
        // sidebar was closed, but selection still exists
        if (!this.sidebarOpen() && this.nodeEntries.getSelection().selected?.length > 0) {
            this.nodeEntries.getSelection().clear();
        }
        // sidebar is opened, but the selection does not match
        else if (
            this.sidebarOpen() &&
            this.nodeEntries.getSelection().selected?.[0].ref.id !== this.selectedNodeIds[0]
        ) {
            this.nodeEntries
                .getSelection()
                .setSelection(
                    this.allRequestedNodes.find((n) => n.ref.id === this.selectedNodeIds[0]),
                );
        }
    }

    /**
     * Performs a search query to set the data source.
     *
     * @param resetNecessary
     * @param skipCount
     */
    private async setDataSource(
        resetNecessary: boolean = false,
        skipCount?: number,
    ): Promise<void> {
        if (
            this.layout === GenericNodeEntriesDisplayType.MapView &&
            this.genericWidgetGlobalService.hasCustomDisplayType(this.layout)
        ) {
            if (this.customTypeInstance) {
                return this.customTypeInstance.setDataSource(true, 0);
            } else {
                return Promise.resolve();
            }
        }
        if (resetNecessary) {
            this.dataSource.reset();
            this.allRequestedNodes = [];
        }
        let query: string = this.queryId;
        // create a deep copy of criteria, as we will modify (and later might reset) it
        let criteria: MdsQueryCriteria[] = Helper.deepCopy(
            await this.extendCriteria(this.criteria),
        );
        // for the map view, overwrite the criteria to search for nodes with location
        // note: resolveCollections is set to true to be consistent with the editorial desk
        //       for the map view, set it to false
        const request: SearchRequestParams = {
            query,
            repository: HOME_REPOSITORY,
            maxItems: !skipCount ? this.maxItems : this.maxItems + 1, // +1 to load full row
            skipCount: skipCount ?? 0,
            propertyFilter: [PROPERTY_FILTER_ALL],
            contentType: 'ALL',
            metadataset: this.mds || this.genericWidgetGlobalService.getDefaultMds(),
            sortProperties: ['cm:created'],
            sortAscending: [true],
            body: {
                criteria,
                resolveCollections: false,
            },
        };
        const searchResult: SearchResults = await firstValueFrom(
            this.searchService.search(request),
        );

        // avoid pushing potential duplicates
        // TODO: This duplicate check is currently necessary, as the same items might be requested again (and again)
        const existingNodeIds: string[] = this.allRequestedNodes
            .filter((n) => n.ref?.id)
            .map((n: Node) => n.ref.id);
        searchResult.nodes?.forEach((node: Node) => {
            if (!existingNodeIds.includes(node.ref.id)) {
                this.allRequestedNodes.push(node);
                existingNodeIds.push(node.ref.id);
            }
        });

        // count the number of currently blacklisted nodes
        const numberOfBlacklistedVisibleNodes =
            this.allRequestedNodes.filter(
                (n: Node) => n?.ref?.id && this.blacklistedNodeIds.includes(n.ref.id),
            )?.length || 0;
        // emit the total pagination number with removed blacklisted nodes
        this.totalSearchResultCountChanged.emit(
            searchResult.pagination.total - numberOfBlacklistedVisibleNodes,
        );

        let customCardsCount: number;
        // in edit mode, display all requested nodes
        if (this.hasEditRightsAndIsEditMode) {
            this.dataSource.setData(this.allRequestedNodes, searchResult.pagination);
            customCardsCount = this.allRequestedNodes.length;
        }
        // in view mode, display the filtered nodes
        else {
            const filteredNodes: Node[] = this.allRequestedNodes.filter(
                (node: Node) => !this.blacklistedNodeIds.includes(node.ref.id),
            );
            // note: as the requests are cached, do not change the original result object, otherwise, it is changed permanently
            const updatedPagination: Pagination = JSON.parse(
                JSON.stringify(searchResult.pagination),
            );
            updatedPagination.total -= this.allRequestedNodes.length - filteredNodes.length;
            this.dataSource.setData(filteredNodes, updatedPagination);
            customCardsCount = filteredNodes.length;
        }

        // inject custom card,
        // if too few elements exist
        // and if the correct type is selected (TODO: list is currently not supported)
        const isGridItemType: boolean = [
            GenericNodeEntriesDisplayType.SingleView,
            GenericNodeEntriesDisplayType.SplitView,
            GenericNodeEntriesDisplayType.StandardView,
            GenericNodeEntriesDisplayType.CompactView,
        ].includes(this.layout);
        if ((this.includeCustomCard() || this.hasEditRightsAndIsEditMode) && isGridItemType) {
            this.injectCustomCards(customCardsCount);
        }

        // emit the currently loaded visible nodes
        const visibleNodes: Node[] = this.allRequestedNodes.filter(
            (n: Node) => n?.ref?.id && !this.blacklistedNodeIds.includes(n.ref.id),
        );
        this.visibleNodesChanged.emit(visibleNodes);
    }

    /**
     * Helper function to initially retrieve the taxonomies.
     */
    async retrieveTaxonomies(): Promise<void> {
        for (const [propertyName, taxonomies] of this.propertyToTaxonomiesMatching) {
            for (const taxonomy of taxonomies) {
                try {
                    const response: Response = await fetch(taxonomy);
                    if (!response.ok) {
                        throw new Error(`Response status: ${response.status}`);
                    }
                    const taxonomyJson = await response.json();
                    this.taxonomyToJsonMatching.set(taxonomy, taxonomyJson);
                } catch (error) {
                    console.error(error.message);
                }
            }
        }
    }

    /**
     * Helper function to handle the entire extension of the criteria array.
     *
     * @param criteria
     */
    private async extendCriteria(criteria: MdsQueryCriteria[]): Promise<MdsQueryCriteria[]> {
        // if necessary, retrieve the taxonomies first
        if (!this.taxonomiesRetrieved) {
            await this.retrieveTaxonomies();
            this.taxonomiesRetrieved = true;
        }

        // iterate the matchings and extend the criteria
        for (const [propertyName, taxonomies] of this.propertyToTaxonomiesMatching) {
            const mdsQueryCriteria: MdsQueryCriteria = criteria.find(
                (mdsQueryCriteria: MdsQueryCriteria): boolean =>
                    mdsQueryCriteria.property === propertyName,
            );
            const atLeastOneValue: boolean = mdsQueryCriteria?.values?.length > 0;
            // for the matching, at least one value has been defined
            if (atLeastOneValue) {
                // iterate the set values (multiple values can be selected) and extend each of those values
                for (const selectedValueId of mdsQueryCriteria.values) {
                    // if multiple matchingValues do exist, iterate each of them
                    for (const taxonomy of taxonomies) {
                        // if no matching does exist, retrieve the taxonomy once again
                        if (!this.taxonomyToJsonMatching.get(taxonomy)) {
                            await this.retrieveTaxonomies();
                        }
                        // extend the criteria value and overwrite the criteria variable
                        criteria = await this.extendCriteriaValue(
                            selectedValueId,
                            criteria,
                            this.taxonomyToJsonMatching.get(taxonomy),
                            propertyName,
                        );
                    }
                }
            }
        }
        return criteria;
    }

    /**
     * Extends a given propertyName in a given criteria array by its children IDs using the taxonomy.
     *
     * @param criteriaId
     * @param criteria
     * @param taxonomyJson
     * @param propertyName
     */
    private async extendCriteriaValue(
        criteriaId: string,
        criteria: MdsQueryCriteria[],
        taxonomyJson: any,
        propertyName: string,
    ): Promise<MdsQueryCriteria[]> {
        try {
            if (taxonomyJson?.hasTopConcept?.length > 0) {
                // check, if the criteriaId is part of the top level taxonomy JSON itself or its top concept
                const isTopLevelElement: boolean = !!this.searchTree(taxonomyJson, criteriaId);
                let criteriaElement =
                    isTopLevelElement || this.searchTree(taxonomyJson.hasTopConcept, criteriaId);
                if (!criteriaElement) {
                    return criteria;
                }
                let updatedIds: string[] = [criteriaId];
                // note: the top level element does not have narrow elements, but a top concept itself
                if (isTopLevelElement) {
                    taxonomyJson.hasTopConcept?.forEach((topLevelChild: any): void => {
                        updatedIds = this.retrieveNarrowIds(topLevelChild, updatedIds);
                    });
                } else {
                    updatedIds = this.retrieveNarrowIds(criteriaElement, updatedIds);
                }
                // insert the updated values
                const criteriaIndex: number = criteria.findIndex(
                    (c: MdsQueryCriteria): boolean => c.property === propertyName,
                );
                if (criteriaIndex !== -1) {
                    // reference: https://medium.com/@rivoltafilippo/javascript-merge-arrays-without-duplicates-3fbd8f4881be
                    criteria[criteriaIndex].values = [
                        ...new Set([...criteria[criteriaIndex].values, ...updatedIds]),
                    ];
                }
            }
            return criteria;
        } catch (error) {
            console.error(error.message);
            return criteria;
        }
    }

    /**
     * Helper function to search a sub-element with a given matchingId in a given element's tree.
     *
     * @param element
     * @param matchingId
     */
    private searchTree(element: any, matchingId: string): any {
        if (element.id && element.id === matchingId) {
            return element;
        } else if (Array.isArray(element) || element.narrower) {
            const elements = Array.isArray(element) ? element : element.narrower;
            let result = null;
            for (let i: number = 0; result === null && i < elements.length; i++) {
                result = this.searchTree(elements[i], matchingId);
            }
            return result;
        }
        return null;
    }

    /**
     * Helper function to recursively retrieve the full list of narrow IDs of a given element.
     *
     * @param element
     * @param ids
     */
    private retrieveNarrowIds(element: any, ids: string[]): string[] {
        if (element.id && !ids.includes(element.id)) {
            ids.push(element.id);
        }
        element.narrower?.forEach((narrowElement: any): void => {
            ids = this.retrieveNarrowIds(narrowElement, ids);
        });
        return ids;
    }

    /**
     * Helper function to push custom card at the end of the data source.
     *
     * @param positionToAdd
     */
    private injectCustomCards(positionToAdd: number): void {
        if (this.genericWidgetGlobalService.getCustomCards(CustomCardRole.SuggestContent)?.length) {
            // the position should either be the seventh or the last element, if less than seven elements exist
            if (positionToAdd > this.CUSTOM_CARD_POSITION_INDEX) {
                positionToAdd = this.CUSTOM_CARD_POSITION_INDEX;
            }
            // workaround to check whether the custom card was already added (it is not a node with a ref ID)
            if (this.dataSource.getData()?.[positionToAdd]?.ref?.id) {
                this.dataSource.getData().splice(positionToAdd, 0, this.cardSuggestRef);
            }
        }
    }

    /**
     * Helper function to mark blacklisted / selected nodes by adding / removing a specific CSS class to their card / row.
     *
     * @param nodes
     * @param relevantNodeIds
     * @param className
     */
    private dynamicallyAddCssClasses(
        nodes: Node[],
        relevantNodeIds: string[],
        className: string,
    ): void {
        nodes?.forEach((node: Node, index: number): void => {
            let element: HTMLElement | null = this.queryElement(index);
            if (!element || !node?.ref?.id) {
                return;
            }

            if (relevantNodeIds.includes(node.ref.id)) {
                element.classList?.add(className);
            } else if (element.classList?.contains(className)) {
                element.classList.remove(className);
            }
        });
    }

    /**
     * Helper function to query the correct HTML element for a given index.
     *
     * @param index
     */
    private queryElement(index: number): HTMLElement | null {
        if (
            this.layout === GenericNodeEntriesDisplayType.SingleView ||
            this.layout === GenericNodeEntriesDisplayType.SplitView ||
            this.layout === GenericNodeEntriesDisplayType.StandardView ||
            this.layout === GenericNodeEntriesDisplayType.CompactView
        ) {
            return this.elementRef.nativeElement?.querySelectorAll(
                'es-node-entries-wrapper es-node-entries-card .grid-card',
            )?.[index];
        } else if (this.layout === GenericNodeEntriesDisplayType.ListView) {
            return this.elementRef.nativeElement?.querySelectorAll(
                'es-node-entries-wrapper es-node-entries-table mat-table .mat-row',
            )?.[index];
        }
        return null;
    }

    /**
     * Fetches further data in response to es-node-entries-wrapper's fetchData output.
     *
     * @param currentPagination
     */
    async fetchData(currentPagination: any): Promise<void> {
        this.dataSource.isLoading = true;
        // in edit mode, all requested nodes are shown, while in view mode, some requested nodes might be omitted, which has to be taken into account
        // TODO: an alternative solution might be replacing the offset calculation by this.allRequestedNodes.length
        const offsetToTakeIntoAccount: number = this.hasEditRightsAndIsEditMode
            ? currentPagination.offset
            : currentPagination.offset +
              this.allRequestedNodes.filter((node: Node) =>
                  this.blacklistedNodeIds.includes(node.ref.id),
              ).length;
        await this.setDataSource(false, offsetToTakeIntoAccount);
        this.dataSource.isLoading = false;
        if (this.hasEditRightsAndIsEditMode) {
            this.dynamicallyAddCssClasses(
                this.allRequestedNodes,
                this.blacklistedNodeIds,
                this.blacklistedClassName,
            );
        }
    }

    /**
     * Pass through the item clicked event.
     *
     * @param node
     */
    onItemClicked(node: Node): void {
        this.itemClicked.emit(node);
        this.previewSidebarService.handleNodeClick(node);
    }

    /**
     * Emits an event, when the display type changes.
     */
    onDisplayTypeChanged(): void {
        this.displayTypeChanged.emit(true);
    }

    /**
     * Emits an event when the include custom card state changes.
     */
    onIncludeCardChanged(includeCard: boolean): void {
        this.includeCardChanged.emit(includeCard);
    }

    /**
     * Registers actions that should be executed if a specific output is called.
     */
    private setupCustomTypeInstanceOutputs(): void {
        if (!this.customTypeInstance) {
            return;
        }
        this.customTypeInstance.visibleNodesChanged
            ?.pipe(takeUntil(this.destroy$))
            .subscribe((nodes: Node[]) => {
                this.visibleNodesChanged.emit(nodes);
            });
        this.customTypeInstance.totalSearchResultCountChanged
            ?.pipe(takeUntil(this.destroy$))
            .subscribe((count: number) => {
                // no blacklisting supported here, so emit the total count
                this.totalSearchResultCountChanged.emit(count);
            });
        this.customTypeInstance.itemClicked
            ?.pipe(takeUntil(this.destroy$))
            .subscribe((node: Node) => this.onItemClicked(node));
    }

    protected readonly CustomCardRole = CustomCardRole;
    protected readonly GenericNodeEntriesDisplayType = GenericNodeEntriesDisplayType;
    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
}
