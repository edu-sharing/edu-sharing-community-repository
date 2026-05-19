import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    ContentChild,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    NgZone,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange,
    TemplateRef,
    Type,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { interval, Subject } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    skip,
    switchMap,
    take,
    takeUntil,
} from 'rxjs/operators';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';
import { NodeEntriesComponent } from './node-entries.component';
import {
    CtrlClickBehavior,
    FetchEvent,
    GridConfig,
    InteractionType,
    ListDragGropConfig,
    ListEventInterface,
    ListOptions,
    ListOptionsConfig,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDataType,
    NodeEntriesDisplayType,
    TableConfig,
} from './entries-model';
import { NodeDataSource } from './node-data-source';
import { Helper } from '../util/helper';
import { CustomSelectionModel, NodeEntriesService } from '../services/node-entries.service';
import { OptionItem, Scope } from '../types/option-item';
import { NodeHelperService } from '../services/node-helper.service';
import { ListItem } from '../types/list-item';
import { TemporaryStorageService } from '../services/temporary-storage.service';
import {
    CollectionReference,
    GenericAuthority,
    Node,
    NodeService,
    User,
} from 'ngx-edu-sharing-api';
import { VirtualNode } from '../types/api-models';
import { OptionsHelperDataService } from '../services/options-helper-data.service';
import { UIService } from '../services/ui.service';
import { ColumnType } from '../mds/mds-helper.service';
import { PaginationStrategy } from './node-entries-global.service';

@Component({
    selector: 'es-node-entries-wrapper',
    template: `<es-node-entries
        #nodeEntriesComponent
        *ngIf="!customNodeListComponent"
    ></es-node-entries>`,
    providers: [NodeEntriesService, OptionsHelperDataService, NodeEntriesTemplatesService],
    standalone: false,
})
export class NodeEntriesWrapperComponent<T extends NodeEntriesDataType>
    implements AfterViewInit, OnInit, OnChanges, OnDestroy, ListEventInterface<T>
{
    /**
     * title (above) the table/grid
     */
    @ContentChild('title') titleRef: TemplateRef<any>;
    /**
     * data shown when data source is empty
     */
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    /**
     * custom area for actions only for NodeEntriesDisplayType.SmallGrid (per card at the bottom)
     */
    @ContentChild('actionArea') actionAreaRef: TemplateRef<any>;
    /**
     * custom area for an overlay "above" each card (i.e. to show disabled infos), only for NodeEntriesDisplayType.SmallGrid & odeEntriesDisplayType.Grid
     */
    @ContentChild('overlay') overlayRef: TemplateRef<any>;
    /**
     * custom card template for each card only for NodeEntriesDisplayType.Grid
     */
    @ContentChild('customCard') customCard: TemplateRef<any>;
    @ViewChild('nodeEntriesComponent') nodeEntriesComponentRef: NodeEntriesComponent<T>;
    @Input() dataSource: NodeDataSource<T>;
    @Input() scope: Scope;
    @Input() columns: ColumnType;
    @Input() configureColumns: boolean;
    @Input() checkbox = true;
    /**
     * emits when the user re-configures the columns
     * should be used in order to save the new configuration
     */
    @Output() columnsChange = new EventEmitter<ListItem[]>();
    @Input() globalOptions: OptionItem[];
    @Input() displayType = NodeEntriesDisplayType.Grid;
    /**
     * custom pagination strategy
     * when unset, the global defined strategy for the current scope will be used
     */
    @Input() paginationStrategy: PaginationStrategy | null = null;
    @Output() displayTypeChange = new EventEmitter<NodeEntriesDisplayType>();
    @Input() elementInteractionType = InteractionType.DefaultActionLink;
    @Input() sort: ListSortConfig;
    @Input() dragDrop: ListDragGropConfig<T>;
    @Input() gridConfig: GridConfig;
    @Input() tableConfig: TableConfig;

    /**
     * This color defines the base color of gradients visually limiting a grid in scroll direction.
     * Defaults to white.
     */
    @Input() scrollGradientColor: string = '#fff';

    /**
     * this can be set instead of calling initOptionsGenerator()
     */
    @Input() initConfig: ListOptionsConfig;
    /**
     * Whether this node-entries instance represents the page's main content.
     *
     * Only set to true for one instance per page.
     *
     * If true, this instance will
     * - handle page-wide keyboard shortcuts
     * - take control of the `page` and `pageSize` query parameters for pagination
     */
    @Input() primaryInstance: boolean;
    /**
     * UI hints for whether a single click will cause a dynamic action.
     *
     * This does not configure the actual behavior but only UI hints to the user. Hints include
     * hover effects and a changed cursor.
     *
     * - When choosing 'static', the `clickItem` event should trigger some stationary action like
     *   selecting the element or displaying information in a complementary page area. The
     *   `dblClickItem` event can be used for a more disruptive action.
     * - When choosing 'dynamic', the `clickItem` event should trigger a major action like
     *   navigating to a new page or closing a dialog.
     */
    // TODO: Consider controlling the ui hints and the actual behavior with a single option.
    @Input() singleClickHint: 'dynamic' | 'static' = 'dynamic';
    /**
     * behaviour when ctrl is pressed wy clicking
     */
    @Input() ctrlClickBehavior: CtrlClickBehavior = 'multiselect';
    /**
     * Do not load more data on scroll.
     */
    @Input() disableInfiniteScroll = false;
    /**
     *  show the icon column (table view only)
     */
    @Input() showIconColumn = true;

    @Output() fetchData = new EventEmitter<FetchEvent>();
    @Output() clickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() dblClickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() sortChange = new EventEmitter<ListSortConfig>();
    @Output() virtualNodesAdded;
    @Output() displayTypeChanged;

    customNodeListComponent: Type<NodeEntriesComponent<T>>;
    private componentRef: ComponentRef<NodeEntriesComponent<T>>;
    @HostBinding('attr.data-last-loading-completed') lastLoadingCompleted: number = -1;
    private options: ListOptions;
    private dataSourceDestroy$ = new Subject<void>();
    private destroyed = new Subject<void>();

    constructor(
        private viewContainerRef: ViewContainerRef,
        private temporaryStorageService: TemporaryStorageService,
        private ngZone: NgZone,
        private entriesService: NodeEntriesService<T>,
        private nodeService: NodeService,
        public optionsHelper: OptionsHelperDataService,
        private nodeHelperService: NodeHelperService,
        private uiService: UIService,
        // @TODO
        // private mainNav: MainNavService,
        private templatesService: NodeEntriesTemplatesService,
        private changeDetectorRef: ChangeDetectorRef,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        /*
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.emptyRef = this.emptyRef)
        );
        */
        this.virtualNodesAdded = this.optionsHelper.virtualNodesAdded;
        this.displayTypeChanged = this.optionsHelper.displayTypeChanged;
        this.entriesService.selection.changed.subscribe(() => {
            if (this.optionsHelper.getData()) {
                this.optionsHelper.getData().selectedObjects =
                    this.entriesService.selection.selected;
                this.optionsHelper.getData().activeObjects = this.entriesService.selection.selected;
            } else {
                console.warn('optionsHelper is not initalized correctly; data is empty');
            }
            void this.optionsHelper.refreshComponents();
        });
    }

    ngOnInit(): void {
        if (this.primaryInstance) {
            this.optionsHelper.registerGlobalKeyboardShortcuts();
        }
        this.entriesService.columnsSubject
            .pipe(
                takeUntil(this.destroyed),
                filter((c) => c?.fromUser),
            )
            .subscribe((c) => this.columnsChange.emit(c.columns));
    }

    ngOnChanges(changes: { [key: string]: SimpleChange } = {}) {
        if (!this.componentRef) {
            this.init();
        }
        this.entriesService.list = this;
        this.entriesService.dataSource = this.dataSource;
        if (
            changes.dataSource &&
            changes.dataSource.currentValue !== changes.dataSource.previousValue
        ) {
            this.dataSourceDestroy$.next();
            this.entriesService.dataSource
                .connect()
                .pipe(distinctUntilChanged(), takeUntil(this.dataSourceDestroy$))
                .subscribe((o) => {
                    if (this.optionsHelper.getData()) {
                        this.optionsHelper.getData().allObjects = o;
                        void this.optionsHelper.refreshComponents();
                    }
                });
            this.entriesService.dataSource.isLoadingSubject
                .pipe(
                    distinctUntilChanged(),
                    skip(1),
                    filter((l) => l === false),
                    debounceTime(10),
                    takeUntil(this.dataSourceDestroy$),
                )
                .subscribe(() => (this.lastLoadingCompleted = Date.now()));
        }
        this.entriesService.scope = this.scope;
        if (changes.columns || changes.displayType) {
            this.entriesService.columnsSubject.next({
                columns:
                    this.columns?.[
                        this.displayType === NodeEntriesDisplayType.Table ? 'Table' : 'Default'
                    ] || this.columns?.Default,
                fromUser: false,
            });
        }
        this.entriesService.configureColumns = this.configureColumns;
        this.entriesService.checkbox = this.checkbox;
        this.entriesService.displayType = this.displayType;
        if (changes.paginationStrategy) {
            this.entriesService.paginationStrategy = this.paginationStrategy;
            setTimeout(() => {
                this.nodeEntriesComponentRef?.refreshPaginator();
            });
        }
        this.entriesService.elementInteractionType = this.elementInteractionType;
        this.entriesService.gridConfig = this.gridConfig;
        this.entriesService.tableConfig = this.tableConfig;
        this.entriesService.options = this.options;
        this.entriesService.globalOptions = this.globalOptions;
        this.entriesService.sort = this.sort;
        this.entriesService.sortChange = this.sortChange;
        this.entriesService.dragDrop = this.dragDrop;
        this.entriesService.clickItem = this.clickItem;
        this.entriesService.dblClickItem = this.dblClickItem;
        this.entriesService.fetchData = this.fetchData;
        this.entriesService.primaryInstance = this.primaryInstance;
        this.entriesService.singleClickHint = this.singleClickHint;
        this.entriesService.ctrlClickBehavior = this.ctrlClickBehavior;
        this.entriesService.disableInfiniteScroll = this.disableInfiniteScroll;
        if (changes.showIconColumn) {
            this.entriesService.showIconColumn.next(this.showIconColumn);
        }
        this.entriesService.scrollGradientColor.set(this.scrollGradientColor);

        if (changes['initConfig']) {
            void this.initOptionsGenerator(this.initConfig);
        }
        if (this.componentRef) {
            this.componentRef.instance.changeDetectorRef?.detectChanges();
        }
        // This might need wrapping with `setTimeout`.
        this.updateTemplates();
    }

    ngOnDestroy(): void {
        this.dataSourceDestroy$.next();
        this.dataSourceDestroy$.complete();
        this.destroyed.next();
        this.destroyed.complete();
    }

    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        this.customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_ENTRIES_COMPONENT,
            null,
        );
        if (this.customNodeListComponent == null) {
            return;
        }
        this.componentRef = this.uiService.injectAngularComponent(
            this.viewContainerRef,
            this.customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings(),
        );
    }
    /**
     * Creates a simple map of the output bindings defined in this component.
     */
    private getOutputBindings(): { [key: string]: EventEmitter<any> } {
        const outputBindings: { [key: string]: any } = {};
        for (const key of Object.keys(this)) {
            const value = (this as any)[key];
            if (value instanceof EventEmitter) {
                outputBindings[key] = value;
            }
        }
        return outputBindings;
    }

    getDisplayType(): NodeEntriesDisplayType {
        return this.displayType;
    }

    onDisplayTypeChange() {
        return this.displayTypeChange.asObservable();
    }

    setDisplayType(displayType: NodeEntriesDisplayType): void {
        this.displayType = displayType;
        this.entriesService.displayType = displayType;
        this.ngOnChanges();
        this.displayTypeChange.emit(displayType);
    }

    updateNodes(nodes: void | T[]) {
        if (!nodes) {
            return;
        }
        this.dataSource.getData().forEach((d) => {
            let hits = (nodes as T[]).filter((n) =>
                (n as Node)?.ref
                    ? (n as Node)?.ref.id === (d as Node)?.ref.id
                    : (n as GenericAuthority)?.authorityName ===
                      (d as GenericAuthority)?.authorityName,
            );
            if (hits.length === 0) {
                // handle if the original has changed (for collection refs)
                hits = (nodes as T[]).filter(
                    (n) =>
                        (n as Node)?.ref &&
                        (n as Node)?.ref?.id === (d as unknown as CollectionReference)?.originalId,
                );
            }
            if (hits.length === 1) {
                this.nodeHelperService.copyDataToNode(d as Node, hits[0] as Node);
            }
        });
        // trigger rebuild
        if (this.dataSource instanceof NodeDataSource) {
            (this.dataSource as NodeDataSource<T>).refresh();
        }
        const oldSelection = this.entriesService.selection.selected;
        this.entriesService.selection.clear();
        this.entriesService.selection.select(
            ...oldSelection.map(
                (o) => this.dataSource.getData().filter((d) => Helper.objectEquals(o, d))?.[0],
            ),
        );
        this.changeDetectorRef.detectChanges();
    }

    showReorderColumnsDialog(): void {}

    addVirtualNodes(virtual: T[], options?: { select: boolean }): void {
        virtual = virtual.map((o) => {
            (o as VirtualNode).virtual = true;
            return o;
        });
        virtual.forEach((v: T) => {
            const contains = this.dataSource
                .getData()
                .some((d) =>
                    (d as Node).ref
                        ? (d as Node).ref?.id === (v as Node).ref?.id
                        : (d as User).authorityName === (v as User).authorityName,
                );
            if (contains) {
                if ((v as VirtualNode).override !== false) {
                    this.updateNodes([v]);
                }
            } else {
                this.dataSource.appendData([v], 'before');
            }
            if ((v as VirtualNode).observe) {
                interval(2000)
                    .pipe(
                        take(10),
                        takeUntil(this.destroyed),
                        switchMap(() =>
                            this.nodeService.getNode((v as VirtualNode).ref.id, {
                                repository: (v as VirtualNode).ref.repo,
                            }),
                        ),
                    )
                    .subscribe((node) => {
                        (node as VirtualNode).virtual = true;
                        this.updateNodes([node as T]);
                    });
            }
        });
        if (options?.select !== false) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(...virtual);
        }
        this.virtualNodesAdded.emit(virtual as Node[]);
        this.changeDetectorRef.detectChanges();
    }

    setOptions(options: ListOptions): void {
        this.options = options;
        this.ngOnChanges();
    }

    selectAll() {
        this.entriesService.selection.select(...this.entriesService.dataSource.getData());
    }
    getSelection(): CustomSelectionModel<T> {
        return this.entriesService.selection;
    }

    async initOptionsGenerator(config: ListOptionsConfig) {
        await this.optionsHelper.initComponents(config.actionbar, this);
        this.optionsHelper.setData({
            scope: this.entriesService.scope,
            activeObjects: this.entriesService.selection.selected,
            selectedObjects: this.entriesService.selection.selected,
            allObjects: this.dataSource.getData(),
            parent: config.parent,
            customOptions: config.customOptions,
        });
        void this.optionsHelper.refreshComponents();
    }

    ngAfterViewInit(): void {
        // Prevent changed-after-checked error
        void Promise.resolve().then(() => this.updateTemplates());
    }

    private updateTemplates(): void {
        this.templatesService.title = this.titleRef;
        this.templatesService.empty = this.emptyRef;
        this.templatesService.actionArea = this.actionAreaRef;
        this.templatesService.overlay = this.overlayRef;
        this.templatesService.customCard = this.customCard;
    }

    /**
     * reset the pagination to the first page
     * hint: this will do nothing in case the paginationStrategy !== Pagination
     */
    resetPagination() {
        this.nodeEntriesComponentRef?.paginator?.firstPage();
    }

    deleteNodes(objects: T[]): void {
        this.dataSource.removeData(objects);
        this.getSelection().clear();
    }
}
