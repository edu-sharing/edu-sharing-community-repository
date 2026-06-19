import { Component, inject } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import { NodeHelperService } from '../../services/node-helper.service';
import { Node, RestConstants } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-list-node-workflow',
    templateUrl: './list-node-workflow.component.html',
    standalone: false,
})
export class ListNodeWorkflowComponent extends ListWidget {
    private nodeHelper = inject(NodeHelperService);

    static supportedItems = [new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS)];

    constructor() {
        super();
    }

    getWorkflowStatus() {
        return this.nodeHelper.getWorkflowStatus(this.node as Node).current;
    }
}
