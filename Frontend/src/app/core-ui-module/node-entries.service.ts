import { SelectionModel } from '@angular/cdk/collections';
import { EventEmitter, Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UIService } from '../core-module/rest/services/ui.service';
import { ListItem } from '../core-module/ui/list-item';
import {
    FetchEvent,
    GridConfig,
    InteractionType,
    ListDragGropConfig,
    ListEventInterface,
    ListOptions,
    ListSortConfig,
    NodeClickEvent,
    NodeEntriesDisplayType,
} from '../features/node-entries/entries-model';
import { NodeDataSource } from '../features/node-entries/node-data-source';
import { NodeDataSourceRemote } from '../features/node-entries/node-data-source-remote';
import {
    NodeEntriesGlobalService,
    PaginationStrategy,
} from '../features/node-entries/node-entries-global.service';
import { NodeEntriesDataType } from '../features/node-entries/node-entries.component';

import { OptionItem, Scope } from './option-item';

@Injectable()
export class NodeEntriesService<T extends NodeEntriesDataType> {
    list: ListEventInterface<T>;
    readonly dataSource$ = new BehaviorSubject<NodeDataSource<T> | NodeDataSourceRemote<T> | null>(
        null,
    );
    /**
     * scope the current list is in, e.g. workspace
     * This is used for additional config injection based on the scope
     */
    scope: Scope;
    get dataSource(): NodeDataSource<T> | NodeDataSourceRemote<T> {
        return this.dataSource$.value;
    }
    set dataSource(value: NodeDataSource<T> | NodeDataSourceRemote<T>) {
        this.dataSource$.next(value);
    }
    get paginationStrategy(): PaginationStrategy {
        return this.entriesGlobal.getPaginationStrategy(this.scope);
    }
    // TODO: Use a subject of an immutable type for columns, so users don't have to monitor
    // `columnsChange` separately.
    columns: ListItem[];
    configureColumns: boolean;
    columnsChange: EventEmitter<ListItem[]>;
    displayType: NodeEntriesDisplayType;
    selection = new SelectionModel<T>(true, []);
    elementInteractionType: InteractionType;
    options$ = new BehaviorSubject<ListOptions>(null);
    get options() {
        return this.options$.value;
    }
    set options(options: ListOptions) {
        this.options$.next(options);
    }
    checkbox: boolean;
    globalOptions: OptionItem[];
    sortSubject = new BehaviorSubject<ListSortConfig>(void 0);
    get sort(): ListSortConfig {
        return this.sortSubject.value;
    }
    set sort(value: ListSortConfig) {
        this.sortSubject.next(value);
    }
    sortChange: EventEmitter<ListSortConfig>;
    dragDrop: ListDragGropConfig<T>;
    clickItem: EventEmitter<NodeClickEvent<T>>;
    dblClickItem: EventEmitter<NodeClickEvent<T>>;
    fetchData: EventEmitter<FetchEvent>;
    readonly gridConfig$ = new BehaviorSubject<GridConfig | null>(null);
    get gridConfig(): GridConfig {
        return this.gridConfig$.value;
    }
    set gridConfig(value: GridConfig) {
        this.gridConfig$.next(value);
    }
    primaryInstance: boolean;
    singleClickHint: 'dynamic' | 'static';
    disableInfiniteScroll: boolean;

    constructor(private uiService: UIService, private entriesGlobal: NodeEntriesGlobalService) {}

    onClicked({ event, ...data }: NodeClickEvent<T> & { event: MouseEvent }) {
        if (event.ctrlKey || event.metaKey) {
            this.selection.toggle(data.element);
        } else if (event.shiftKey) {
            this.expandSelectionTo(data.element);
        } else {
            this.clickItem.emit(data);
        }
    }

    onCheckboxChanged(node: T, checked: boolean) {
        if (this.uiService.shiftKeyPressed) {
            this.expandSelectionTo(node);
        }
        if (checked !== this.selection.isSelected(node)) {
            this.selection.toggle(node);
        }
    }

    toggleSelectAll() {
        if (this.isAllSelected()) {
            this.selection.clear();
        } else {
            this.selectAll();
        }
    }

    loadMore(source: 'button' | 'scroll'): boolean {
        if (this.paginationStrategy !== 'infinite-scroll') {
            return false;
        }
        if (source === 'scroll' && this.disableInfiniteScroll) {
            return false;
        }
        // TODO: focus next item when triggered via button.
        if (this.dataSource instanceof NodeDataSourceRemote) {
            return this.dataSource.loadMore();
        } else {
            if (this.dataSource.hasMore()) {
                this.fetchData.emit({
                    offset: this.dataSource.getData().length,
                    reset: false,
                });
                return true;
            } else {
                return false;
            }
        }
    }

    private selectAll() {
        this.selection.select(...this.dataSource.getData());
    }

    private isAllSelected(): boolean {
        return this.dataSource.getData().every((node) => this.selection.isSelected(node));
    }

    private expandSelectionTo(node: T) {
        const nodeIndex = this.dataSource.getData().indexOf(node);
        const selectionIndexes = this.selection.selected
            .map((node) => this.dataSource.getData().indexOf(node))
            .filter((index) => index >= 0);
        if (Math.min(...selectionIndexes) < nodeIndex) {
            for (let i = Math.min(...selectionIndexes) + 1; i <= nodeIndex; i++) {
                this.selection.select(this.dataSource.getData()[i]);
            }
        } else if (Math.max(...selectionIndexes) > nodeIndex) {
            for (let i = nodeIndex; i < Math.max(...selectionIndexes); i++) {
                this.selection.select(this.dataSource.getData()[i]);
            }
        }
    }
}
