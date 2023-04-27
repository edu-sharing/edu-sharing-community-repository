import { Component } from '@angular/core';
import { ListItem, RestConstants, Node } from '../../../core-module/core.module';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { ListWidget } from '../list-widget';

@Component({
    selector: 'es-list-node-workflow',
    templateUrl: './list-node-workflow.component.html',
})
export class ListNodeWorkflowComponent extends ListWidget {
    static supportedItems = [new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS)];

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }

    getWorkflowStatus() {
        return this.nodeHelper.getWorkflowStatus(this.node as Node).current;
    }
}
