import { Component, Input } from '@angular/core';
import { WorkflowDefinition } from 'ngx-edu-sharing-ui';
import { WorkflowEntry } from '../../../../../core-module/rest/data-object';
import { NodeHelperService } from '../../../../../services/node-helper.service';

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
