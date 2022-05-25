import { Component, Input, OnInit } from '@angular/core';
import {ListWidget} from '../list-widget';
import {ListItem} from '../../../../../core-module/ui/list-item';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';
import {Node} from '../../../../../core-module/rest/data-object';
import {NodeHelperService} from '../../../../node-helper.service';

@Component({
    selector: 'es-list-node-workflow',
    templateUrl: './list-node-workflow.component.html',
})
export class ListNodeWorkflowComponent extends ListWidget {
    static supportedItems = [
        new ListItem('NODE', RestConstants.CCM_PROP_WF_STATUS),
    ]

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }

    getWorkflowStatus(){
        return this.nodeHelper.getWorkflowStatus(this.node as Node).current;
    }

}
