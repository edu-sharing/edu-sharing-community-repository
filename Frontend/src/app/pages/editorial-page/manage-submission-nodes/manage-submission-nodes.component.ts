import { Component, effect, input, model, signal, inject } from '@angular/core';
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
export class ManageSubmissionNodesComponent {
    private assignmentV1Service = inject(AssignmentV1Service);

    data = input.required<SubmissionConfig>();
    files = input<SubmissionFile[]>(null);
    localFiles = signal<SubmissionFile[]>(null);
    selected = model<SubmissionFile>(null);

    constructor() {
        effect(() => {
            this.localFiles.set(this.files());
        });
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
        this.localFiles.set(
            this.localFiles().map((f) => {
                if (f === item) {
                    f.validationStatus = 'PENDING';
                }
                return f;
            }),
        );
        this.selected.set(item);
    }
}
