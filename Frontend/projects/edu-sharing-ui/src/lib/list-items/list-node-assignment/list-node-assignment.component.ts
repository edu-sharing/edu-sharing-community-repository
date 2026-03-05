import { Component, computed, Signal } from '@angular/core';
import { ListWidget } from '../list-widget';
import { ListItem } from '../../types/list-item';
import { NodeHelperService } from '../../services/node-helper.service';
import { Assignment } from 'ngx-edu-sharing-api';
import { toSignal } from '@angular/core/rxjs-interop';
import { SubmissionWithAssignment } from '../../node-entries/data-type';

@Component({
    selector: 'es-list-node-assignment',
    templateUrl: './list-node-assignment.component.html',
    styleUrls: ['./list-node-assignment.component.scss'],
    standalone: false,
})
export class ListNodeAssignmentComponent extends ListWidget {
    static supportedItems = [new ListItem('ASSIGNMENT', '*'), new ListItem('SUBMISSION', '*')];
    private _assignment = toSignal(this.nodeSubject) as Signal<Assignment>;
    assignment = computed(() => this.submission().assignment || this._assignment());
    submission = toSignal(this.nodeSubject) as Signal<SubmissionWithAssignment>;
    isBeforeEndDate = computed(() => {
        // @TODO check endTime format vs delivered type
        return (
            !this.assignment().endTime ||
            (Date.parse(this.assignment().endTime) ||
                (this.assignment().endTime as unknown as number)) > new Date().getTime()
        );
    });
    submissionStatus = computed(() => {
        if (this.isBeforeEndDate()) {
            return this.submission()?.submissionStatus;
        }
        if (this.submission()?.submissionStatus === 'PENDING') {
            return 'WITHDRAWN';
        }
        return this.submission()?.submissionStatus;
    });

    constructor(private nodeHelper: NodeHelperService) {
        super();
    }
}
