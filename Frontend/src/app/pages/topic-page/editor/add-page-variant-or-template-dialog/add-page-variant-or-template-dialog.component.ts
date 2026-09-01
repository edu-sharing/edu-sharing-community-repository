import {
    Component,
    EventEmitter,
    input,
    Input,
    InputSignal,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    inject,
} from '@angular/core';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { SortDirection } from '@angular/material/sort';
import {
    CONTENT_TYPE_ALL,
    HOME_REPOSITORY,
    MdsQueryCriteria,
    Node,
    Pagination,
    PROPERTY_FILTER_ALL,
    SearchRequestParams,
    SearchResults,
    SearchService,
} from 'ngx-edu-sharing-api';
import {
    FetchEvent,
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
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { MdsModule } from '../../../../features/mds/mds.module';
import { SharedModule } from '../../../../shared/shared.module';
import {
    DEFAULT_PAGE_VARIANT_GLOBAL_PROP,
    DEFAULT_PAGE_VARIANT_PARENT_RECURSIVE_PROP,
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
    selector: 'es-add-page-variant-or-template-dialog',
    imports: [SharedModule, MdsModule],
    templateUrl: 'add-page-variant-or-template-dialog.component.html',
    styleUrls: ['add-page-variant-or-template-dialog.component.scss'],
})
export class AddPageVariantOrTemplateDialogComponent implements OnDestroy, OnInit {
    public genericWidgetGlobalService = inject(GenericWidgetGlobalService);
    private searchHelperService = inject(SearchHelperService);
    private searchService = inject(SearchService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.CREATE_PAGE_VARIANT.';
    readonly templateI18nPrefix: string = 'TOPIC_PAGE.CREATE_PAGE_TEMPLATE.';

    @Input() dialogRef: CardDialogRef;
    // the collection the topic page belongs to — its subtree is searched for existing page variants
    collectionNodeId: InputSignal<string> = input<string>(null);
    @Input() pageConfigRef: string;
    @Input() pageVariantNode: Node;
    @Input() selectedNode: Node;
    templateMode: InputSignal<boolean> = input(false);
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

    // whether the search is restricted to the collection the topic page belongs to
    collectionFilterActive: boolean = true;

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
    // newest first: a freshly published template has to be visible without paging or searching
    sortActive: string = RestConstants.CM_MODIFIED_DATE;
    readonly sortColumns = [
        new ListItemSort('NODE', RestConstants.LOM_PROP_TITLE),
        new ListItemSort('NODE', RestConstants.CM_MODIFIED_DATE),
    ];
    sortDirection: SortDirection = 'desc';
    private readonly MAX_ITEMS_PER_REQUEST: number = 50;

    // whether the initial node was attempted to be selected
    private initialSelectionMade: boolean = false;
    private initialized: boolean = false;
    private destroyed: Subject<void> = new Subject<void>();

    @ViewChild('nodeEntriesWrapper') nodeEntries: NodeEntriesWrapperComponent<Node>;

    constructor() {
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
     * Whether the selected copy option can be searched for: the topic page option needs a
     * collection to scope the search to, while the global templates are always reachable.
     */
    private searchScopeAvailable(): boolean {
        return (
            this.selectedOption === CopyOption.Template ||
            !this.collectionFilterActive ||
            !!this.collectionNodeId()
        );
    }

    /**
     * Whether the removable collection filter chip applies to the current copy option: the global
     * templates are not scoped to a collection, so the chip is only meaningful for topic pages.
     */
    protected collectionFilterApplicable(): boolean {
        return this.selectedOption === CopyOption.TopicPage && !!this.collectionNodeId();
    }

    /**
     * Switches the search between the collection the topic page belongs to and all collections.
     *
     * @param event
     */
    protected onCollectionFilterChange(event: MatCheckboxChange): void {
        this.collectionFilterActive = event.checked;
        // the result list is a different one, so the referenced variant has to be looked up and
        // sorted to the top again
        this.initialSelectionMade = false;
        void this.updateList();
    }

    /**
     * Builds the criteria that restrict the search to the selected copy option: either the global
     * page variant templates or the page variants living inside the collection subtree.
     */
    private createScopeCriteria(): MdsQueryCriteria | null {
        if (this.selectedOption === CopyOption.Template) {
            return {
                property: DEFAULT_PAGE_VARIANT_GLOBAL_PROP,
                values: ['true'],
            };
        }
        // without the collection restriction the page variants of all collections are searched
        if (!this.collectionFilterActive) {
            return null;
        }
        return {
            property: DEFAULT_PAGE_VARIANT_PARENT_RECURSIVE_PROP,
            values: [this.collectionNodeId()],
        };
    }

    /**
     * Builds the search request for the page variants of the selected copy option.
     *
     * @param skipCount
     */
    private createSearchRequest(skipCount: number = 0): SearchRequestParams {
        const criteria: MdsQueryCriteria[] = [];
        const scopeCriteria: MdsQueryCriteria | null = this.createScopeCriteria();
        if (scopeCriteria) {
            criteria.push(scopeCriteria);
        }
        if (this.searchValue) {
            criteria.push({
                property: 'ngsearchword',
                values: [this.searchValue.trim()],
            });
        }
        if (this.searchFilters && Object.keys(this.searchFilters)?.length) {
            criteria.push(...this.searchHelperService.convertCritieria(this.searchFilters, []));
        }
        return {
            query: DEFAULT_PAGE_VARIANT_QUERY_ID,
            repository: HOME_REPOSITORY,
            sortProperties: [this.sortActive],
            sortAscending: [this.sortDirection === 'asc'],
            maxItems: this.MAX_ITEMS_PER_REQUEST,
            skipCount,
            propertyFilter: [PROPERTY_FILTER_ALL],
            contentType: CONTENT_TYPE_ALL,
            metadataset: this.genericWidgetGlobalService.getDefaultMds(),
            body: {
                criteria,
            },
        };
    }

    /**
     * Loads the next page of page variants, triggered by the fetchData output of the node entries
     * wrapper. Without this, the list would be capped at the first request and a newly created
     * variant or template could be missing from it entirely.
     *
     * @param event
     */
    async loadMore(event: FetchEvent): Promise<void> {
        if (
            !this.searchScopeAvailable() ||
            !this.dataSource.hasMore() ||
            this.dataSource.isLoading
        ) {
            return;
        }
        this.dataSource.isLoading = true;
        try {
            const searchResult: SearchResults = await firstValueFrom(
                this.searchService.search(this.createSearchRequest(event.offset)),
            );
            this.dataSource.appendData(searchResult.nodes ?? []);
        } finally {
            this.dataSource.isLoading = false;
        }
    }

    /**
     * Updates the list of page variants based on the selected copy option and search criteria.
     * Both copy options are resolved by the `page_variant` search query, so searching, filtering,
     * sorting and paging are handled by the backend instead of being emulated on a fixed list.
     */
    async updateList(): Promise<void> {
        if (!this.searchScopeAvailable()) {
            this.dataSource.setData([], null);
            return;
        }
        let nodes: Node[];
        let pagination: Pagination;
        this.dataSource.isLoading = true;
        try {
            const searchResult: SearchResults = await firstValueFrom(
                this.searchService.search(this.createSearchRequest()),
            );
            nodes = searchResult.nodes ?? [];
            pagination = searchResult.pagination;
        } finally {
            this.dataSource.isLoading = false;
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
        this.dataSource.setData(nodes, pagination ?? null);
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
