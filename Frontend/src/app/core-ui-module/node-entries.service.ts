import { SelectionModel } from '@angular/cdk/collections';
import { EventEmitter, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { UIService } from '../core-module/rest/services/ui.service';
import { ListItem } from '../core-module/ui/list-item';
import {
    ListEventInterface,
    NodeEntriesDisplayType,
    InteractionType,
    ListOptions,
    ListSortConfig,
    ListDragGropConfig,
    NodeClickEvent,
    FetchEvent,
    GridConfig,
} from '../features/node-entries/entries-model';
import { NodeDataSource } from '../features/node-entries/node-data-source';
import { NodeEntriesDataType } from '../features/node-entries/node-entries.component';

import { OptionItem, Scope } from './option-item';

@Injectable()
export class NodeEntriesService<T extends NodeEntriesDataType> {
    list: ListEventInterface<T>;
    readonly dataSource$ = new BehaviorSubject<NodeDataSource<T> | null>(null);
    /**
     * scope the current list is in, e.g. workspace
     * This is used for additional config injection based on the scope
     */
    scope: Scope;
    get dataSource(): NodeDataSource<T> {
        return this.dataSource$.value;
    }
    set dataSource(value: NodeDataSource<T>) {
        this.dataSource$.next(value);
    }
    columns: ListItem[];
    configureColumns: boolean;
    columnsChange: EventEmitter<ListItem[]>;
    displayType: NodeEntriesDisplayType;
    selection = new SelectionModel<T>(true, []);
    elementInteractionType: InteractionType;
    options: ListOptions;
    checkbox: boolean;
    globalOptions: OptionItem[];
    sort: ListSortConfig;
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
    globalKeyboardShortcuts: boolean;
    singleClickHint: 'dynamic' | 'static';
    disableInfiniteScroll: boolean;

    constructor(private uiService: UIService) {}

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
