import { Component, computed, effect, input } from '@angular/core';
import {
    ListItem,
    ListItemsModule,
    NodeEntriesService,
    TreeNodeService,
    UserAvatarComponent,
} from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import {
    Assignment,
    AssignmentV1Service,
    Node,
    Submission,
    SubmissionFile,
} from 'ngx-edu-sharing-api';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import { ManageSubmissionNodesComponent } from '../manage-submission-nodes/manage-submission-nodes.component';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { debounce, debounceTime } from 'rxjs/operators';

export type SubmissionConfig = {
    submission: Submission;
    assignment: Assignment;
    submissionFileCallback: (selected: SubmissionFile) => void;
};

@Component({
    selector: 'es-submission-sidebar',
    templateUrl: 'submission-sidebar.component.html',
    styleUrls: ['submission-sidebar.component.scss'],
    imports: [SharedModule, UserAvatarComponent, ListItemsModule, ManageSubmissionNodesComponent],
    providers: [NodeEntriesService, TreeNodeService],
})
export class SubmissionSidebarComponent {
    data = input.required<SubmissionConfig>();
    readonly feedbackForm = new FormGroup({
        validationNodes: new FormControl('', Validators.nullValidator),
    });
    readonly submission = computed(() => this.data()?.submission);
    readonly submissionStatus = new ListItem('SUBMISSION', 'submissionStatus');
    readonly validationStatus = new ListItem('SUBMISSION', 'validationStatus');
    constructor(
        public editorialSidebarService: EditorialSidebarService,
        private assignmentV1Service: AssignmentV1Service,
    ) {
        effect(() => {
            this.feedbackForm.setValue({
                validationNodes: this.submission().validationNotes || 'TEST',
            });
        });
        this.feedbackForm.valueChanges.pipe(debounceTime(5000)).subscribe((value) => {
            this.assignmentV1Service.editSubmission1({
                submissionId: this.data().submission.ref.id,
                assignmentId: this.data().assignment.ref.id,
                body: {
                    validationNotes: value.validationNodes,
                },
            });
        });
    }
}
