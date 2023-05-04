import { Component, Input } from '@angular/core';
import { Group, User } from 'ngx-edu-sharing-api';
import { WorkflowDefinition } from 'ngx-edu-sharing-ui';
import { NodeHelperService } from '../../../../core-ui-module/node-helper.service';
import { WorkflowEntry } from '../../../../core-module/rest/data-object';

type WorkflowReceiver = User | Group;

@Component({
    selector: 'es-workflow-list',
    templateUrl: 'workflow-history.component.html',
    styleUrls: ['workflow-history.component.scss'],
})
export class WorkflowListComponent {
    @Input() history: WorkflowEntry[];
    defaultStatus: WorkflowDefinition;

    constructor(private nodeHelper: NodeHelperService) {
        ({ initial: this.defaultStatus } = this.nodeHelper.getDefaultWorkflowStatus(false));
    }

    getWorkflowForId(id: string) {
        return this.nodeHelper.getWorkflowStatusById(id);
    }
}
