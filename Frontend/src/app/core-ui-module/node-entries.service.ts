import {EventEmitter, Injectable} from '@angular/core';
import {Node} from '../core-module/rest/data-object';
import {NodeDataSource} from './components/node-entries-wrapper/node-data-source';
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
    NodeEntriesWrapperComponent
} from './components/node-entries-wrapper/node-entries-wrapper.component';
import {ListItem} from '../core-module/ui/list-item';
import {SelectionModel} from '@angular/cdk/collections';
import {OptionItem} from './option-item';
import {Sort} from '@angular/material/sort';
import {UIService} from '../core-module/rest/services/ui.service';

@Injectable()
export class NodeEntriesService<T extends Node> {
    list: ListEventInterface<T>;
    dataSource: NodeDataSource<T>;
    columns: ListItem[];
    displayType: NodeEntriesDisplayType;
    selection = new SelectionModel<T>(true, []);
    elementInteractionType: InteractionType;
    options: ListOptions;
    globalOptions: OptionItem[];
    sort: ListSortConfig;
    sortChange: EventEmitter<ListSortConfig>;
    dragDrop: ListDragGropConfig<T>;
    clickItem: EventEmitter<NodeClickEvent<T>>;
    dblClickItem: EventEmitter<NodeClickEvent<T>>;
    fetchData: EventEmitter<FetchEvent>;
    gridConfig: GridConfig;

    constructor(
        private uiService: UIService,
    ) {
    }

    handleSelectionEvent(node: T) {
        if(this.selection.isSelected(node)) {
            this.selection.toggle(node);
        } else {
            if(this.uiService.isShiftCmd()) {
                const selected = this.selection.selected.map((s) => this.dataSource.getData().indexOf(s)).sort(
                    (a,b) => a > b ? 1 : -1
                );
                for(let i = selected[0]; i <= this.dataSource.getData().indexOf(node); i++) {
                    this.selection.select(this.dataSource.getData()[i]);
                }
            } else {
                this.selection.toggle(node);
            }
        }
    }
}
