import { Component, Input, OnInit } from '@angular/core';
import {ListWidget} from '../list-widget';
import {ListItem} from '../../../../../core-module/ui/list-item';
import {NodeHelperService} from '../../../../node-helper.service';

@Component({
    selector: 'es-list-collection-info',
    templateUrl: './list-collection-info.component.html',
})
export class ListCollectionInfoComponent extends ListWidget {
    static supportedItems = [
        new ListItem('COLLECTION', 'info'),
        new ListItem('COLLECTION', 'scope')
    ]

    constructor(public nodeHelper: NodeHelperService) {
        super();
    }

}
