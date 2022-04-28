import { SelectionModel } from '@angular/cdk/collections';
import { EventEmitter, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
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
} from './components/node-entries-wrapper/entries-model';
import { NodeDataSource } from './components/node-entries-wrapper/node-data-source';
import { OptionItem } from './option-item';
import {NodeEntriesDataType} from './components/node-entries/node-entries.component';

@Injectable()
export class NodeEntriesService<T extends NodeEntriesDataType> {
    list: ListEventInterface<T>;
    readonly dataSource$ = new BehaviorSubject<NodeDataSource<T> | null>(null);
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

    constructor(private uiService: UIService) {}

    handleSelectionEvent(node: T) {
        if (this.selection.isSelected(node)) {
            this.selection.toggle(node);
        } else {
            if (this.uiService.isShiftCmd()) {
                const selected = this.selection.selected
                    .map((s) => this.dataSource.getData().indexOf(s))
                    .sort((a, b) => (a > b ? 1 : -1));
                for (let i = selected[0]; i <= this.dataSource.getData().indexOf(node); i++) {
                    this.selection.select(this.dataSource.getData()[i]);
                }
            } else {
                this.selection.toggle(node);
            }
        }
    }
}
