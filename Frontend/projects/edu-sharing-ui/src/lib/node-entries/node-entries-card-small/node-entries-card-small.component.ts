import { Component, Input } from '@angular/core';
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
    readonly ClickSource = ClickSource;
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    @Input() node: T;
    @Input() dropdown: DropdownComponent;

    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        public templatesService: NodeEntriesTemplatesService,
    ) {}

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
        FINISHED: 'done',
    };

    assignmentEndTimePriority(assignment: Assignment) {
        const now = new Date().getTime();
        const permissions = new AssignmentPipe().transform(assignment, { mode: 'permissions' });
        if (permissions === 'COORDINATOR') {
            if (assignment.status !== 'INPROGRESS') {
                return 'low';
            }
        } else if (permissions === 'ASSIGNEE') {
            if (
                assignment.submissions?.[0]?.submissionStatus === 'FINISHED' ||
                assignment.submissions?.[0]?.validationStatus === 'FINISHED'
            ) {
                return 'low';
            }
        }
        const delayUntil =
            (Date.parse(assignment.endTime as string) ||
                (assignment.endTime as unknown as number)) - now;
        // > 5 days == low delay
        if (delayUntil < 3600 * 1000 * 24 * 1) {
            return 'high';
        } else if (delayUntil < 3600 * 1000 * 24 * 2) {
            return 'medium';
        }
        return 'low';
    }

    assignmentStatus(assignment: Assignment) {
        if (assignment.submissions?.some((s) => s.submissionStatus === 'FINISHED')) {
            return 'HAS_SUBMISSIONS';
        }
        return assignment.status;
    }

    protected readonly UIService = UIService;
}
