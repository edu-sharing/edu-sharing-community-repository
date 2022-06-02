import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/ui/dialog-button';
import {
    Group,
    UserSimple,
    WorkflowDefinition,
    WorkflowEntry,
} from '../../../../core-module/rest/data-object';
import { NodeHelperService } from '../../../../core-ui-module/node-helper.service';

type WorkflowReceiver = UserSimple | Group;

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
