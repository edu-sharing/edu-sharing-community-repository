import { Component, input, OnChanges, signal, SimpleChanges } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { Assignment, AssignmentV1Service, Submission, SubmissionFile } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { SubmissionConfig } from '../submission-sidebar/submission-sidebar.component';

@Component({
    selector: 'es-manage-assignment-submissions',
    templateUrl: 'manage-submission-nodes.component.html',
    styleUrls: ['manage-submission-nodes.component.scss'],
    imports: [SharedModule],
})
export class ManageSubmissionNodesComponent implements OnChanges {
    data = input.required<SubmissionConfig>();
    files = signal<SubmissionFile[]>(null);
    constructor(private assignmentV1Service: AssignmentV1Service) {}

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.data) {
            this.files.set(
                await firstValueFrom(
                    this.assignmentV1Service.getSubmissionFiles({
                        submissionId: this.data().submission.ref.id,
                        assignmentId: this.data().assignment.ref.id,
                    }),
                ),
            );
        }
    }
}
