import { Component, computed, model, ViewChild } from '@angular/core';
import { ListItem, ListItemsModule, NodeEntriesService, TreeNodeService } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import { CommentsListComponent } from '../../../features/mds/mds-editor/widgets/mds-editor-widget-comments/comments-list/comments-list.component';
import { AssignmentConfig } from '../submission-sidebar/submission-sidebar.component';
import { TranslateModule } from '@ngx-translate/core';
import { AssignmentFile, Node, SubmissionFile } from 'ngx-edu-sharing-api';

/**
 * sidebar shows information about an assignment
 * shown when the user opens a correction node
 */
@Component({
    selector: 'es-assignment-sidebar',
    templateUrl: 'assignment-sidebar.component.html',
    styleUrls: ['assignment-sidebar.component.scss'],
    imports: [SharedModule, ListItemsModule, TranslateModule],
    providers: [NodeEntriesService, TreeNodeService],
})
export class AssignmentSidebarComponent {
    readonly submissionStatus = new ListItem('SUBMISSION', 'submissionStatus');
    readonly validationStatus = new ListItem('SUBMISSION', 'validationStatus');
    readonly submissionDate = new ListItem('SUBMISSION', 'submissionDate');

    @ViewChild(CommentsListComponent) commentsRef: CommentsListComponent;
    data = model.required<AssignmentConfig>();
    assignment = computed(() => this.data()?.submission?.assignment);
    submission = computed(() => this.data()?.submission);
    selected = computed(() => this.data()?.selected);
    selectedRefId = computed(
        () =>
            (this.selected() as SubmissionFile)?.correction?.ref.id ||
            (this.selected() as AssignmentFile)?.referNode?.ref.id,
    );
    correctedFiles = computed(() => {
        return this.data()
            .submissionFiles?.filter(
                (s) => s.validationStatus !== 'NOT_STARTED' && s.correction?.downloadUrl,
            )
            .map((s) => {
                return {
                    ...s.correction,
                    name: s.content?.name,
                    title: s.content?.title,
                };
            });
    });

    submittableFiles = computed(() => {
        return this.data()
            .assignmentFiles?.filter((s) => s?.documentRole === 'SUBMITTABLE')
            .map((f) => f.referNode);
    });
    supplementaryFiles = computed(() => {
        return this.data()
            .assignmentFiles?.filter((s) => s?.documentRole === 'SUPPLEMENTARY')
            .map((f) => f.referNode);
    });

    setFile(item: Node) {
        const file =
            this.data().submissionFiles.find((f) => f.correction?.ref.id === item.ref.id) ||
            this.data().assignmentFiles.find((f) => f.referNode?.ref.id === item.ref.id);
        console.log(item);
        this.data().selectedFileCallback(item);
        this.data.set({ ...this.data(), selected: file });
    }
}
