import { Component, input, OnChanges, signal, SimpleChanges, inject } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { AssignmentV1Service, SubmissionFile } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { SubmissionConfig } from '../submission-sidebar/submission-sidebar.component';

@Component({
    selector: 'es-manage-assignment-submissions',
    templateUrl: 'manage-submission-nodes.component.html',
    styleUrls: ['manage-submission-nodes.component.scss'],
    imports: [SharedModule],
})
export class ManageSubmissionNodesComponent implements OnChanges {
    private assignmentV1Service = inject(AssignmentV1Service);

    data = input.required<SubmissionConfig>();
    files = signal<SubmissionFile[]>(null);
    selected = signal<SubmissionFile>(null);

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

    async click(item: SubmissionFile) {
        // let the parent veto the switch (e.g. unsaved correction changes)
        if ((await this.data().submissionFileCallback?.(item)) === false) {
            return;
        }
        void firstValueFrom(
            this.assignmentV1Service.updateSubmissionFileValidation({
                assignmentId: this.data().assignment.ref.id,
                submissionId: this.data().submission.ref.id,
                submissionFileId: item.ref.id,
                body: {
                    metadata: { validationStatus: 'PENDING' },
                },
            }),
        );
        this.files.set(
            this.files().map((f) => {
                if (f === item) {
                    f.validationStatus = 'PENDING';
                }
                return f;
            }),
        );
        this.selected.set(item);
    }
}
