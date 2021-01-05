import {Input, Type} from '@angular/core';
import {Widget} from '../../../../common/ui/mds-editor/mds-editor-instance.service';
import {Group, Node, Person} from '../../../../core-module/rest/data-object';
import {ListItem} from '../../../../core-module/ui/list-item';
import {ListCollectionInfoComponent} from './list-collection-info/list-collection-info.component';

export class ListWidget{
    @Input() node: Node|Group|Person; // node (or group/user)
    @Input() item: ListItem;

    constructor() {
    }
}
export type ListWidgetClass = {
    supportedItems: ListItem[];
} & Type<ListWidget>;