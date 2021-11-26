import { Component } from '@angular/core';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { ListItem } from '../../../../../core-module/ui/list-item';
import { ListWidget } from '../list-widget';

@Component({
    selector: 'es-list-node-replication-source',
    templateUrl: './list-node-replication-source.component.html',
})
export class ListNodeReplicationSourceComponent extends ListWidget {
    static supportedItems = [new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE)];
}
