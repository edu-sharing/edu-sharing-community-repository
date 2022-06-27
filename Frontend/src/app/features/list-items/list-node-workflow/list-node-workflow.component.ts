import { Component } from '@angular/core';
import { ListItem, RestConstants, Node } from 'src/app/core-module/core.module';
import { NodeHelperService } from 'src/app/core-ui-module/node-helper.service';
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
