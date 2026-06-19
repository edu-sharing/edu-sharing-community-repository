import { Component, Input, inject } from '@angular/core';
import { Target } from '../../types/option-item';
import { ClickSource, InteractionType } from '../entries-model';

import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { Assignment, Node, Submission } from 'ngx-edu-sharing-api';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { UIService } from '../../services/ui.service';
import { AssignmentPipe } from '../../pipes/assignment.pipe';

@Component({
    selector: 'es-node-entries-card-small',
    templateUrl: 'node-entries-card-small.component.html',
    styleUrls: ['node-entries-card-small.component.scss'],
    standalone: false,
})
export class NodeEntriesCardSmallComponent<T extends Node> {
    entriesService = inject<NodeEntriesService<T>>(NodeEntriesService);
    nodeHelper = inject(NodeHelperService);
    templatesService = inject(NodeEntriesTemplatesService);

    readonly ClickSource = ClickSource;
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    @Input() node: T;
    @Input() dropdown: DropdownComponent;

    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback([this.node]))) {
            return always;
        }
        return options.filter((o) => o.showAsAction && o.showCallback([this.node])).slice(0, 3);
    }
    async openMenu(event: MouseEvent, node: T) {
        event.stopPropagation();
        if (UIService.isMobileWidth()) {
            this.entriesService.openDropdown(this.dropdown, node, () =>
                this.dropdown.triggerBottomSheet(),
            );
        } else {
            this.entriesService.openDropdown(this.dropdown, node);
        }
    }
    openContextmenu(event: MouseEvent | Event) {
        event.preventDefault();
        event.stopPropagation();
    }

    readonly AssignmentSubmissionStatusIcon: { [key in Submission['submissionStatus']]: string } = {
        NOT_STARTED: 'inbox',
        PENDING: 'timer',
        FINISHED: 'done',
    };
    readonly AssignmentValidationStatusIcon: { [key in Submission['validationStatus']]: string } = {
        NOT_STARTED: 'timer',
        PENDING: 'timer',
        FINISHED: 'done',
    };
    readonly AssignmentStatusIcon: { [key in Assignment['status']]: string } = {
        DRAFT: 'news',
        INPROGRESS: 'schedule_send',
        CANCELED: 'cancel',
        CORRECTED: 'done',
        FINISHED: 'done',
    };

    assignmentStatus(assignment: Assignment) {
        if (
            assignment.status === 'INPROGRESS' &&
            assignment.submissions?.some((s) => s.submissionStatus === 'FINISHED')
        ) {
            return 'HAS_SUBMISSIONS';
        }
        return assignment.status;
    }
    assignmentStatusAssignee(assignment: Assignment) {
        if (assignment.status === 'INPROGRESS') {
            const sub = assignment.submissions?.[0];
            if (sub?.validationStatus === 'FINISHED') {
                return 'CORRECTED';
            } else if (sub?.submissionStatus === 'FINISHED') {
                return 'SUBMITTED';
            } else {
                return 'TO_SUBMIT';
            }
        }
        return assignment.status;
    }

    protected readonly UIService = UIService;
}
