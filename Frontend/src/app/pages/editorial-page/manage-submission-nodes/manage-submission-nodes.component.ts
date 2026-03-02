import {
    Component,
    EventEmitter,
    input,
    OnChanges,
    Output,
    signal,
    SimpleChanges,
} from '@angular/core';
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
    data = input.required<SubmissionConfig>();
    files = signal<SubmissionFile[]>(null);
    selected = signal<SubmissionFile>(null);
    @Output() nodeClick = new EventEmitter<SubmissionFile>();
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

    click(item: SubmissionFile) {
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
        this.nodeClick.emit(item);
    }
}
