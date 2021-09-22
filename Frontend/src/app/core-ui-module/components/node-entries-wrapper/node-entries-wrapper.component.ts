import {OptionsHelperService} from '../../options-helper.service';
import {
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnChanges, Output,
    SimpleChange,
    TemplateRef,
    Type,
    ViewContainerRef
} from '@angular/core';
import {NodeEntriesService} from '../../node-entries.service';
import {TemporaryStorageService} from '../../../core-module/rest/services/temporary-storage.service';
import {UIHelper} from '../../ui-helper';
import {NodeEntriesComponent} from '../node-entries/node-entries.component';
import {NodeDataSource} from './node-data-source';
import {List, ListItemSort, Node, SortColumn} from '../../../core-module/rest/data-object';
import {ListItem} from '../../../core-module/ui/list-item';
import {CustomOptions, OptionItem, Scope, Target} from '../../option-item';
import {ActionbarComponent} from '../../../common/ui/actionbar/actionbar.component';
import {MainNavService} from '../../../common/services/main-nav.service';
import {SelectionModel} from '@angular/cdk/collections';
import {Sort} from '@angular/material/sort';

export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid
}
export enum InteractionType {
    DefaultActionLink,
    Emitter,
    None
}
export type ListOptions = { [key in Target]?: OptionItem[]};
export type ListOptionsConfig = {
    scope: Scope,
    actionbar: ActionbarComponent,
    parent?: Node,
    customOptions?: CustomOptions,
};
export interface ListSortConfig extends Sort {
    columns: ListItemSort[];
    allowed?: boolean;
    customSortingInProgress?: boolean;
}
export interface DropSource<T extends Node> {
    element: T;
    sourceList: ListEventInterface<T>;
}
export interface ListDragGropConfig<T extends Node> {
    dragAllowed: boolean;
    dropAllowed?: (target: T, source: DropSource<T>) => boolean;
    dropped?: () => void;
}
export enum ClickSource {
    Preview,
    Icon,
    Metadata
}
export type NodeClickEvent<T extends Node> = {
    element: T,
    source: ClickSource,
    attribute?: ListItem // only when source === Metadata
}
export type FetchEvent = {
    offset: number,
    amount?: number;
}
export type GridConfig = {
    maxCols?: number
}
export interface ListEventInterface<T extends Node> {
    updateNodes(nodes: void | T[]): void;

    getDisplayType(): NodeEntriesDisplayType;

    setDisplayType(displayType: NodeEntriesDisplayType): void;

    showReorderColumnsDialog(): void;

    addVirtualNodes(virtual: T[]): void;

    setOptions(options: ListOptions): void;

    /**
     * activate option (dropdown) generation
     */
    initOptionsGenerator(actionbar: ListOptionsConfig): void|Promise<void>;

    getSelection(): SelectionModel<T>;
}
@Component({
    selector: 'app-node-entries-wrapper',
    template: `
        <app-node-entries
            *ngIf="!customNodeListComponent"
        >
            <ng-template #title><ng-container *ngTemplateOutlet="titleRef"></ng-container></ng-template>
            <ng-template #empty><ng-container *ngTemplateOutlet="emptyRef"></ng-container></ng-template>
        </app-node-entries>`,
    providers: [
        NodeEntriesService,
    ]
})
export class NodeEntriesWrapperComponent<T extends Node> implements OnChanges, ListEventInterface<T> {
    @ContentChild('title') titleRef: TemplateRef<any>;
    @ContentChild('empty') emptyRef: TemplateRef<any>;
    @Input() dataSource: NodeDataSource<T>;
    @Input() columns: ListItem[];
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
    private componentRef: ComponentRef<any>;
    public customNodeListComponent: Type<NodeEntriesComponent<T>>;
    private options: ListOptions;

    constructor(
        private temporaryStorageService: TemporaryStorageService,
        private componentFactoryResolver: ComponentFactoryResolver,
        private viewContainerRef: ViewContainerRef,
        private ngZone: NgZone,
        private entriesService: NodeEntriesService<T>,
        private optionsHelper: OptionsHelperService,
        private mainNav: MainNavService,
        private elementRef: ElementRef,
    ) {
        // regulary re-bind template since it might have updated without ngChanges trigger
        /*
        ngZone.runOutsideAngular(() =>
            setInterval(() => this.componentRef.instance.emptyRef = this.emptyRef)
        );
        */
        this.entriesService.selection.changed.subscribe(
            () => {
                this.optionsHelper.getData().selectedObjects = this.entriesService.selection.selected;
                this.optionsHelper.getData().activeObjects = this.entriesService.selection.selected;
                this.optionsHelper.refreshComponents();
            }
        );

    }
    ngOnChanges(changes: { [key: string]: SimpleChange } = {}) {
        if (!this.componentRef) {
            this.init();
        }
        this.entriesService.list = this;
        this.entriesService.dataSource = this.dataSource;
        this.entriesService.columns = this.columns;
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
    }
    /**
     * Replaces this wrapper with the configured custom-node-list component.
     */
    private init(): void {
        this.customNodeListComponent = this.temporaryStorageService.get(
            TemporaryStorageService.CUSTOM_NODE_ENTRIES_COMPONENT,
            null
        );
        if(this.customNodeListComponent == null) {
            return;
        }
        this.componentRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            this.customNodeListComponent,
            this.elementRef.nativeElement,
            // Input bindings are initialized in `ngOnChanges`.
            this.getOutputBindings()
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

    updateNodes(nodes: void | T[]): void {
        // @TODO
    }

    showReorderColumnsDialog(): void {
    }

    addVirtualNodes(virtual: T[]): void {
        virtual = virtual.map((o) => {
            o.virtual = true;
            return o;
        });
        this.dataSource.appendData(virtual, 'before');
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
        await this.optionsHelper.initComponents(this.mainNav.getMainNav(), config.actionbar, this);
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
}

