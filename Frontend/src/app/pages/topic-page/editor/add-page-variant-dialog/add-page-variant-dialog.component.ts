import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { SortDirection } from '@angular/material/sort';
import { TranslateService } from '@ngx-translate/core';
import {
    CONTENT_TYPE_ALL,
    HOME_REPOSITORY,
    MdsQueryCriteria,
    Node,
    Pagination,
    PROPERTY_FILTER_ALL,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    InteractionType,
    ListItem,
    ListItemSort,
    ListSortConfig,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    Scope,
    SearchHelperService,
    Values,
} from 'ngx-edu-sharing-ui';
import { firstValueFrom, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { MdsModule } from '../../../../features/mds/mds.module';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import {
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_QUERY_ID,
} from '../../shared/types/custom-definitions';
import { SelectOption } from '../../shared/types/select-option';
import {
    convertNodeRefIntoNodeId,
    retrieveNodeId,
    retrievePageVariantTemplateRef,
} from '../../shared/utils/template-util';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';

export enum CopyOption {
    TopicPage = 'topicPage',
    Template = 'template',
}

@Component({
    selector: 'es-add-page-variant-dialog',
    imports: [SharedModule, MdsModule],
    templateUrl: 'add-page-variant-dialog.component.html',
    styleUrls: ['add-page-variant-dialog.component.scss'],
})
export class AddPageVariantDialogComponent implements OnDestroy, OnInit {
    readonly i18nPrefix: string = 'TOPIC_PAGE.CREATE_PAGE_VARIANT.';

    @Input() dialogRef: CardDialogRef;
    @Input() pageVariantConfigNodes: Node[];
    @Input() pageConfigRef: string;
    @Input() pageVariantNode: Node;
    @Input() selectedNode: Node;
    @Output() copyOptionChanged: EventEmitter<CopyOption> = new EventEmitter<CopyOption>();
    @Output() selectedNodeChange: EventEmitter<Node> = new EventEmitter<Node>();

    // copy options toggle
    copyOptions: SelectOption[] = [
        {
            icon: 'menu_book',
            value: CopyOption.TopicPage,
            viewValue: 'COPY_OPTIONS.TOPIC_PAGE',
        },
        {
            icon: 'account_tree',
            value: CopyOption.Template,
            viewValue: 'COPY_OPTIONS.TEMPLATE',
        },
    ];
    selectedOption: string;

    // mds-editor-wrapper
    searchFilters: Values;

    // search-related variables
    private searchSubject: Subject<string> = new Subject<string>();
    searchValue: string;

    // node-entries-wrapper
    columns = {
        Default: [
            new ListItem('NODE', RestConstants.LOM_PROP_TITLE),
            new ListItem('NODE', 'ccm:page_variant_profiling_target_group'),
            new ListItem('NODE', 'ccm:educationalcontext'),
        ],
    };
    dataSource: NodeDataSource<Node | any> = new NodeDataSource<Node | any>();
    sortActive: string;
    readonly sortColumns = [
        new ListItemSort('NODE', RestConstants.LOM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
    ];
    sortDirection: SortDirection = 'asc';

    // whether the initial node was attempted to be selected
    private initialSelectionMade: boolean = false;
    private initialized: boolean = false;
    private destroyed: Subject<void> = new Subject<void>();

    @ViewChild('nodeEntriesWrapper') nodeEntries: NodeEntriesWrapperComponent<Node>;

    constructor(
        public genericWidgetGlobalService: GenericWidgetGlobalService,
        private searchHelperService: SearchHelperService,
        private searchService: SearchService,
        private topicPageHelperService: TopicPageHelperService,
        private translate: TranslateService,
    ) {
        // setup search with debouncing
        this.searchSubject
            .pipe(debounceTime(500), distinctUntilChanged(), takeUntil(this.destroyed))
            .subscribe(() => {
                void this.updateList();
            });
    }

    async ngOnInit(): Promise<void> {
        this.selectedOption = !this.pageConfigRef
            ? this.copyOptions[this.copyOptions.length - 1].value
            : this.copyOptions[0].value;
        this.copyOptionChanged.emit(this.selectedOption as CopyOption);
        // if no page config exists, retrieve page variant templates to be chosen by the user
        await this.updateList();
        this.initialized = true;
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    /**
     * Switches to the clicked copy option and updates the list of page variants.
     *
     * @param event
     */
    onCopyOptionChange(event: any): void {
        this.selectedOption = event.value;
        this.copyOptionChanged.emit(event.value);
        // reset the initial decision to allow selecting a variant again
        this.initialSelectionMade = false;
        void this.updateList();
    }

    /**
     * Handles input changes of the search field.
     */
    onSearchInput(event: Event): void {
        const target = event.target as HTMLInputElement;
        const query: string = target.value.trim();
        this.searchSubject.next(query);
    }

    /**
     * Clears the search field and updates the list of page variants.
     */
    clearSearch() {
        this.searchValue = '';
        void this.updateList();
    }

    /**
     * Sets the search filters to a given value.
     *
     * @param filters
     */
    onSearchFiltersChange(filters: Values): void {
        this.searchFilters = filters;
        if (!this.initialized) {
            return;
        }
        void this.updateList();
    }

    /**
     * Handles the sorting of the list of page variants.
     *
     * @param event
     */
    onSort(event: ListSortConfig) {
        this.sortActive = event.active;
        this.sortDirection = event.direction;
        void this.updateList();
    }

    /**
     * Handles the click event on a page variant.
     *
     * @param event
     */
    onNodeClick(event: NodeClickEvent<any>) {
        const alreadySelected: boolean = this.nodeEntries.getSelection().isSelected(event.element);
        this.nodeEntries.getSelection().clear();
        if (!alreadySelected) {
            this.nodeEntries.getSelection().select(event.element);
        }
        void this.updateSelectedNode();
    }

    /**
     * Updates the list of page variants based on the selected copy option and search criteria.
     */
    async updateList(): Promise<void> {
        let nodes: Node[];
        let pagination: Pagination;
        // distinguish between template and topic page mode
        if (this.selectedOption === CopyOption.Template) {
            // in template mode, search for page variant templates
            const criteria: MdsQueryCriteria[] = [
                {
                    property: DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
                    values: ['true'],
                },
            ];
            if (this.searchValue) {
                criteria.push({
                    property: 'ngsearchword',
                    values: [this.searchValue.trim()],
                });
            }
            if (this.searchFilters && Object.keys(this.searchFilters)?.length) {
                criteria.push(...this.searchHelperService.convertCritieria(this.searchFilters, []));
            }
            const searchResult: SearchResults = await firstValueFrom(
                this.searchService.search({
                    query: DEFAULT_PAGE_VARIANT_QUERY_ID,
                    repository: HOME_REPOSITORY,
                    sortProperties: [this.sortActive],
                    sortAscending: [this.sortDirection === 'asc'],
                    maxItems: 100,
                    propertyFilter: [PROPERTY_FILTER_ALL],
                    contentType: CONTENT_TYPE_ALL,
                    metadataset: this.genericWidgetGlobalService.getDefaultMds(),
                    body: {
                        criteria,
                    },
                }),
            );
            // add default template if either no search term is defined or it includes the default template name
            if (!searchResult.nodes) {
                searchResult.nodes = [];
            }
            const defaultTemplateName: string = this.translate.instant(
                'TOPIC_PAGE.NO_PAGE_CONFIG.DEFAULT_TEMPLATE',
            );
            const noSearchFilterDefined: boolean =
                !this.searchFilters ||
                !Object.keys(this.searchFilters)?.length ||
                !this.searchHelperService.convertCritieria(this.searchFilters, [])?.length;
            const noSearchValueOrIncluded: boolean =
                !this.searchValue?.trim() || this.searchValue?.includes(defaultTemplateName);
            if (noSearchFilterDefined && noSearchValueOrIncluded) {
                searchResult.nodes.push({
                    aspects: [],
                    ref: {
                        archived: false,
                        id: uuidv4(),
                        repo: HOME_REPOSITORY,
                    },
                    name: defaultTemplateName,
                    title: defaultTemplateName,
                    iconURL:
                        location.origin +
                        this.topicPageHelperService.getBaseHref() +
                        '/themes/default/images/common/mime-types/svg/folder.svg',
                    mediatype: 'folder',
                    type: RestConstants.CCM_TYPE_MAP,
                    properties: {
                        [DEFAULT_PAGE_VARIANT_CONFIG_PROP]: ['{"structure":{"swimlanes":[]}}'],
                        [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: ['true'],
                        [RestConstants.LOM_PROP_TITLE]: [defaultTemplateName],
                    },
                } as Partial<Node> as Node);
            }
            nodes = searchResult.nodes;
        } else {
            // in topic page mode, display existing page variants
            nodes = this.pageVariantConfigNodes;
            // manual search
            if (this.searchValue) {
                nodes = nodes.filter((n) =>
                    n.title.toLowerCase().includes(this.searchValue.toLowerCase()),
                );
            }
            // manual properties filter
            if (this.searchFilters && Object.keys(this.searchFilters)?.length) {
                const propertyFilters = Object.fromEntries(
                    Object.entries(this.searchFilters).filter(
                        ([, value]) => value && value.length > 0,
                    ),
                );
                if (Object.keys(propertyFilters).length) {
                    nodes = nodes.filter((n) =>
                        Object.entries(propertyFilters).every(([property, allowedValues]) => {
                            const nodeValues: string[] = n.properties?.[property];
                            if (!nodeValues) {
                                return false;
                            }
                            return nodeValues.some((v) => (allowedValues as string[]).includes(v));
                        }),
                    );
                }
            }
            // manual sorting
            if (this.sortActive) {
                nodes = nodes.sort((a: Node, b: Node) => {
                    const valueA: string = a.properties[this.sortActive]?.[0] || '';
                    const valueB: string = b.properties[this.sortActive]?.[0] || '';

                    const comparison: number = valueA.localeCompare(valueB);

                    return this.sortDirection === 'asc' ? comparison : -comparison;
                });
            }
        }
        // select the page variant referred to as the template (if one exists)
        let matchingVariantNode: Node;
        if (!this.initialSelectionMade) {
            this.initialSelectionMade = true;
            // retrieve either the template ref ID (copy mode: template) or the page variant ID (copy mode: topic page)
            let nodeRefToReferTo: string;
            if (this.pageVariantNode) {
                nodeRefToReferTo =
                    this.selectedOption === CopyOption.Template
                        ? retrievePageVariantTemplateRef(this.pageVariantNode)
                        : retrieveNodeId(this.pageVariantNode);
            }
            if (nodeRefToReferTo) {
                const nodeIdToReferTo = convertNodeRefIntoNodeId(nodeRefToReferTo);
                matchingVariantNode = nodes.find((n) => retrieveNodeId(n) === nodeIdToReferTo);
                // sort node to the top of the list
                if (matchingVariantNode) {
                    const filteredNodes: Node[] = nodes.filter(
                        (n) => retrieveNodeId(n) !== nodeIdToReferTo,
                    );
                    nodes = [matchingVariantNode, ...filteredNodes];
                }
            } else {
                matchingVariantNode = nodes[0];
            }
        }
        // update the data source
        this.dataSource.setData(nodes);
        if (pagination) {
            this.dataSource.setPagination(pagination);
        }
        // give the node entries wrapper time to render the list
        setTimeout(async () => {
            this.nodeEntries.getSelection().clear();
            // select the matching variant node if existing
            if (matchingVariantNode) {
                this.nodeEntries.getSelection().select(matchingVariantNode);
            }
            await this.updateSelectedNode();
        });
    }

    /**
     * Updates the selected node and the state of the use button based on the current selection.
     */
    private async updateSelectedNode(): Promise<void> {
        this.selectedNode = this.nodeEntries.getSelection().selected[0] || null;
        this.selectedNodeChange.emit(this.selectedNode);
        const buttons = (await firstValueFrom(this.dialogRef.observeConfig())).buttons;
        const useButton = buttons.find((b) => b.label === 'USE');
        if (useButton) {
            useButton.disabled = !this.nodeEntries.getSelection().hasValue();
        }
        this.dialogRef.patchConfig({ buttons });
    }

    protected readonly CopyOption = CopyOption;
    protected readonly DisplayType = NodeEntriesDisplayType;
    protected readonly HOME_REPOSITORY = HOME_REPOSITORY;
    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
}
