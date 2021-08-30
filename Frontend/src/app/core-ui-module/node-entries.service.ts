import {Injectable} from '@angular/core';
import {Node} from '../core-module/rest/data-object';
import {NodeDataSource} from './components/node-entries-wrapper/node-data-source';
import {NodeEntriesDisplayType} from './components/node-entries-wrapper/node-entries-wrapper.component';
import {ListItem} from '../core-module/ui/list-item';

@Injectable()
export class NodeEntriesService<T extends Node> {
    dataSource: NodeDataSource<T>;
    columns: ListItem[];
    displayType: NodeEntriesDisplayType;
}
