import { Component, computed, input } from '@angular/core';
import {
    ListItem,
    ListItemsModule,
    NodeEntriesService,
    TreeNodeService,
    UserAvatarComponent,
} from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import { Assignment, Node, Submission, SubmissionFile } from 'ngx-edu-sharing-api';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import { ManageSubmissionNodesComponent } from '../manage-submission-nodes/manage-submission-nodes.component';

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
    readonly submission = computed(() => this.data()?.submission);
    readonly submissionStatus = new ListItem('SUBMISSION', 'submissionStatus');
    readonly validationStatus = new ListItem('SUBMISSION', 'validationStatus');
    constructor(public editorialSidebarService: EditorialSidebarService) {}
}
