import { Component, computed, model, ViewChild } from '@angular/core';
import { ListItem, ListItemsModule, NodeEntriesService, TreeNodeService } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import { CommentsListComponent } from '../../../features/mds/mds-editor/widgets/mds-editor-widget-comments/comments-list/comments-list.component';
import { AssignmentConfig } from '../submission-sidebar/submission-sidebar.component';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';

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

    setCorrectedFile(item: Node) {
        this.data().selectedFileCallback(
            this.data().submissionFiles.find((f) => f.correction?.ref.id === item.ref.id),
        );
    }
}
