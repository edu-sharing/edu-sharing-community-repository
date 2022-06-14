import {
    AfterViewInit,
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChange,
    TemplateRef,
    Type,
    ViewContainerRef,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
    CollectionReference,
    ListItem,
    Node,
    TemporaryStorageService,
    User,
} from '../../core-module/core.module';
import { NodeEntriesService } from '../../core-ui-module/node-entries.service';
import { NodeHelperService } from '../../core-ui-module/node-helper.service';
import { OptionItem } from '../../core-ui-module/option-item';
import {
    OptionsHelperService,
    OPTIONS_HELPER_CONFIG,
} from '../../core-ui-module/options-helper.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { NodeEntriesTemplatesService } from '../node-entries/node-entries-templates.service';
import { NodeEntriesComponent, NodeEntriesDataType } from '../node-entries/node-entries.component';
import {
    FetchEvent,
    GridConfig,
    InteractionType,
    ListDragGropConfig,
    ListEventInterface,
    ListOptions,
    ListOptionsConfig,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDisplayType,
} from './entries-model';
import { NodeDataSource } from './node-data-source';

@Component({
    selector: 'es-node-entries-wrapper',
    template: ` <es-node-entries *ngIf="!customNodeListComponent"> </es-node-entries>`,
    providers: [
        NodeEntriesService,
        OptionsHelperService,
        NodeEntriesTemplatesService,
        {
            provide: OPTIONS_HELPER_CONFIG,
            useValue: {
                subscribeEvents: false,
            },
        },
    ],
})
export class NodeEntriesWrapperComponent<T extends NodeEntriesDataType>
    implements AfterViewInit, OnChanges, OnDestroy, ListEventInterface<T>
{
    @ContentChild('title') titleRef: TemplateRef<any>;
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    @ContentChild('actionArea') actionAreaRef: TemplateRef<any>;
    @Input() dataSource: NodeDataSource<T>;
    @Input() columns: ListItem[];
    @Input() configureColumns: boolean;
    @Input() checkbox = true;
    @Output() columnsChange = new EventEmitter<ListItem[]>();
    @Input() globalOptions: OptionItem[];
    @Input() displayType = NodeEntriesDisplayType.Grid;
    @Output() displayTypeChange = new EventEmitter<NodeEntriesDisplayType>();
    @Input() elementInteractionType = InteractionType.DefaultActionLink;
    @Input() sort: ListSortConfig;
    @Input() dragDrop: ListDragGropConfig<T>;
    @Input() gridConfig: GridConfig;
    @Output() fetchData = new EventEmitter<FetchEvent>();
    @Output() clickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() dblClickItem = new EventEmitter<NodeClickEvent<T>>();
    @Output() sortChange = new EventEmitter<ListSortConfig>();
    @Output() virtualNodesAdded = this.optionsHelper.virtualNodesAdded;
    @Output() nodesChanged = this.optionsHelper.nodesChanged;
    @Output() nodesDeleted = this.optionsHelper.nodesDeleted;
    @Output() displayTypeChanged = this.optionsHelper.displayTypeChanged;

    customNodeListComponent: Type<NodeEntriesComponent<T>>;
    private componentRef: ComponentRef<any>;
    private options: ListOptions;
    private destroyed = new Subject<void>();

    constructor(
        private temporaryStorageService: TemporaryStorageService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private ngZone: NgZone,
        private entriesService: NodeEntriesService<T>,
        private optionsHelper: OptionsHelperService,
        private nodeHelperService: NodeHelperService,
        private mainNav: MainNavService,
        private templatesService: NodeEntriesTemplatesService,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        /*
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.emptyRef = this.emptyRef)
        );
        */
        this.entriesService.selection.changed.subscribe(() => {
            this.optionsHelper.getData().selectedObjects = this.entriesService.selection.selected;
            this.optionsHelper.getData().activeObjects = this.entriesService.selection.selected;
            this.optionsHelper.refreshComponents();
        });
    }

    ngOnChanges(changes: { [key: string]: SimpleChange } = {}) {
        if (!this.componentRef) {
            this.init();
        }
        this.entriesService.list = this;
        this.entriesService.dataSource = this.dataSource;
        this.entriesService.columns = this.columns;
        this.entriesService.configureColumns = this.configureColumns;
        this.entriesService.checkbox = this.checkbox;
        this.entriesService.columnsChange = this.columnsChange;
        this.entriesService.displayType = this.displayType;
        this.entriesService.elementInteractionType = this.elementInteractionType;
        this.entriesService.gridConfig = this.gridConfig;
        this.entriesService.options = this.options;
        this.entriesService.globalOptions = this.globalOptions;
        this.entriesService.sort = this.sort;
        this.entriesService.sortChange = this.sortChange;
        this.entriesService.dragDrop = this.dragDrop;
        this.entriesService.clickItem = this.clickItem;
        this.entriesService.dblClickItem = this.dblClickItem;
        this.entriesService.fetchData = this.fetchData;

        if (this.componentRef) {
            this.componentRef.instance.changeDetectorRef?.detectChanges();
        }
        // This might need wrapping with `setTimeout`.
        this.updateTemplates();
    }

    ngOnDestroy(): void {
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
        this.componentRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
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
            let hits = (nodes as T[]).filter((n) => (n as Node).ref.id === (d as Node).ref.id);
            if (hits.length === 0) {
                // handle if the original has changed (for collection refs)
                hits = (nodes as T[]).filter(
                    (n) => (n as Node).ref.id === (d as unknown as CollectionReference).originalId,
                );
            }
            if (hits.length === 1) {
                this.nodeHelperService.copyDataToNode(d as Node, hits[0] as Node);
            }
        });
        nodes?.forEach((node) => {
            if (
                !this.dataSource
                    .getData()
                    .filter((n) => (n as Node).ref.id === (node as Node).ref.id).length
            ) {
                (node as Node).virtual = true;
                this.dataSource.appendData([node], 'before');
            }
        });
    }

    showReorderColumnsDialog(): void {}

    addVirtualNodes(virtual: T[]): void {
        virtual = virtual.map((o) => {
            (o as Node).virtual = true;
            return o;
        });
        virtual.forEach((v) => {
            const contains = this.dataSource
                .getData()
                .some((d) =>
                    (d as Node).ref
                        ? (d as Node).ref?.id === (v as Node).ref?.id
                        : (d as User).authorityName === (v as User).authorityName,
                );
            if (contains) {
                this.updateNodes([v]);
            } else {
                this.dataSource.appendData([v], 'before');
            }
        });
        this.entriesService.selection.clear();
        this.entriesService.selection.select(...virtual);
    }

    setOptions(options: ListOptions): void {
        this.options = options;
        this.ngOnChanges();
    }

    getSelection() {
        return this.entriesService.selection;
    }

    async initOptionsGenerator(config: ListOptionsConfig) {
        await this.optionsHelper.initComponents(config.actionbar, this);
        this.optionsHelper.setData({
            scope: config.scope,
            activeObjects: this.entriesService.selection.selected,
            selectedObjects: this.entriesService.selection.selected,
            allObjects: this.dataSource.getData(),
            parent: config.parent,
            customOptions: config.customOptions,
        });
        this.optionsHelper.refreshComponents();
    }

    ngAfterViewInit(): void {
        // Prevent changed-after-checked error
        Promise.resolve().then(() => this.updateTemplates());
    }

    private updateTemplates(): void {
        this.templatesService.title = this.titleRef;
        this.templatesService.empty = this.emptyRef;
        this.templatesService.actionArea = this.actionAreaRef;
    }
}
