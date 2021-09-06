import {EventEmitter, Injectable} from '@angular/core';
import {Node} from '../core-module/rest/data-object';
import {NodeDataSource} from './components/node-entries-wrapper/node-data-source';
import {
    InteractionType, ListEventInterface, ListOptions, ListSortConfig, NodeClickEvent,
    NodeEntriesDisplayType, NodeEntriesWrapperComponent
} from './components/node-entries-wrapper/node-entries-wrapper.component';
import {ListItem} from '../core-module/ui/list-item';
import {SelectionModel} from '@angular/cdk/collections';
import {OptionItem} from './option-item';
import {Sort} from '@angular/material/sort';

@Injectable()
export class NodeEntriesService<T extends Node> {
    dataSource: NodeDataSource<T>;
    columns: ListItem[];
    displayType: NodeEntriesDisplayType;
    selection = new SelectionModel<T>(true, []);
    elementInteractionType: InteractionType;
    options: ListOptions;
    globalOptions: OptionItem[];
    sort: ListSortConfig;
    sortChange: EventEmitter<ListSortConfig>;
    clickItem: EventEmitter<NodeClickEvent<T>>;
}
