import { Component, computed, effect, model, signal, ViewChild } from '@angular/core';
import {
    ListItem,
    ListItemsModule,
    NodeEntriesService,
    SubmissionWithAssignment,
    TreeNodeService,
    UserAvatarComponent,
} from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../shared/shared.module';
import {
    Assignment,
    AssignmentFile,
    AssignmentV1Service,
    CommentV1Service,
    HOME_REPOSITORY,
    Node,
    Submission,
    SubmissionFile,
} from 'ngx-edu-sharing-api';
import { ManageSubmissionNodesComponent } from '../manage-submission-nodes/manage-submission-nodes.component';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';
import { firstValueFrom } from 'rxjs';
import { CommentsListComponent } from '../../../features/mds/mds-editor/widgets/mds-editor-widget-comments/comments-list/comments-list.component';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { AssignmentEditorConfig } from '../manage-assignment/manage-assignment.component';
import { TranslateService } from '@ngx-translate/core';
import { PlatformLocation } from '@angular/common';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';

export type AssignmentConfig = {
    submission: SubmissionWithAssignment;
    assignmentFiles: AssignmentFile[];
    submissionFiles: SubmissionFile[];
    selected: AssignmentFile | SubmissionFile;
    selectedFileCallback: (selected: Node) => void;
};
export type SubmissionConfig = {
    submission: SubmissionWithAssignment;
    /**
     * list of all submissions for navigation
     */
    submissionList: SubmissionWithAssignment[];
    assignment: Assignment;
    submissionFileCallback: (selected: SubmissionFile) => void;
};

@Component({
    selector: 'es-submission-sidebar',
    templateUrl: 'submission-sidebar.component.html',
    styleUrls: ['submission-sidebar.component.scss'],
    imports: [
        SharedModule,
        UserAvatarComponent,
        ListItemsModule,
        ManageSubmissionNodesComponent,
        CommentsListComponent,
        EditorComponent,
    ],
    providers: [NodeEntriesService, TreeNodeService],
})
export class SubmissionSidebarComponent {
    @ViewChild(CommentsListComponent) commentsRef: CommentsListComponent;
    readonly editorConfig = {
        ...AssignmentEditorConfig,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'tinymce',
        language: this.translateService.getDefaultLang(),
    };
    submitFormGroup: FormGroup;
    data = model.required<SubmissionConfig>();
    showValidationNotes = signal(false);
    readonly feedbackForm = new FormGroup({
        validationNotes: new FormControl('', Validators.nullValidator),
        feedback: new FormControl('', Validators.nullValidator),
    });
    readonly canGoBack = computed(
        () => this.data().submissionList?.indexOf(this.data().submission) > 0,
    );
    readonly canGoForward = computed(
        () =>
            this.data().submissionList?.indexOf(this.data().submission) <
            this.data().submissionList?.length - 1,
    );
    readonly submission = computed(() => this.data()?.submission);
    readonly isReadOnly = computed(
        () =>
            this.data()?.assignment?.status === 'CANCELED' ||
            this.data()?.assignment?.status === 'DRAFT' ||
            this.data()?.assignment?.status === 'FINISHED',
    );
    readonly submissionStatus = new ListItem('SUBMISSION', 'submissionStatus');
    readonly validationStatus = new ListItem('SUBMISSION', 'validationStatus');
    readonly submissionDate = new ListItem('SUBMISSION', 'submissionDate');
    readonly returnDate = new ListItem('SUBMISSION', 'returnDate');
    constructor(
        public editorialSidebarService: EditorialSidebarService,
        private translateService: TranslateService,
        private platformLocation: PlatformLocation,
        private assignmentV1Service: AssignmentV1Service,
        private commentV1Service: CommentV1Service,
        private formBuilder: FormBuilder,
    ) {
        this.submitFormGroup = this.formBuilder.group({
            submitComment: ['', [Validators.required]],
        });
        effect(() => {
            this.feedbackForm.setValue({
                validationNotes: this.submission().validationNotes || '',
                feedback: this.submission().feedback || '',
            });
            this.feedbackForm.markAsPristine();
            if (this.isReadOnly()) {
                this.feedbackForm.disable();
                this.submitFormGroup.disable();
            } else {
                this.feedbackForm.enable();
                this.submitFormGroup.enable();
            }
        });
        this.feedbackForm.valueChanges.pipe(debounceTime(3000)).subscribe(async (value) => {
            await this.saveNotes();
        });
    }

    private async ensureSubmissionExists() {
        if (!this.data().submission.ref?.id) {
            const created = await firstValueFrom(
                this.assignmentV1Service.createSubmission({
                    assignmentId: this.data().assignment.ref.id,
                    user: this.submission().assignee.authorityName,
                }),
            );
            this.syncData(created);
        }
    }

    private async saveNotes() {
        if (this.feedbackForm.dirty) {
            await this.ensureSubmissionExists();
            const submission = await firstValueFrom(
                this.assignmentV1Service.editSubmissionValidation({
                    submissionId: this.data().submission.ref.id,
                    assignmentId: this.data().assignment.ref.id,
                    body: {
                        validationNotes: this.feedbackForm.value.validationNotes,
                        feedback: this.feedbackForm.value.feedback,
                    },
                }),
            );
            this.syncData(submission);
        }
    }

    async addComment() {
        const control = this.submitFormGroup.get('submitComment');
        control.disable();
        await firstValueFrom(
            this.commentV1Service.addComment({
                repository: HOME_REPOSITORY,
                node: this.submission().ref.id,
                body: control.value,
            }),
        );
        control.reset();
        control.enable();
        void this.commentsRef.refresh();
    }

    async stepElement(offset: number) {
        await this.saveNotes();
        const index = this.data().submissionList.indexOf(this.data().submission);
        this.data.set({
            ...this.data(),
            submission: this.data().submissionList[index + offset],
        });
    }
    async markAsFinished() {
        await this.ensureSubmissionExists();
        await this.saveNotes();
        const submission = await firstValueFrom(
            this.assignmentV1Service.editSubmissionValidation({
                submissionId: this.submission().ref.id,
                assignmentId: this.data().assignment.ref.id,
                body: {
                    validationStatus: 'FINISHED',
                },
            }),
        );
        this.syncData(submission);
        if (this.canGoForward()) {
            await this.stepElement(+1);
        } else {
            this.editorialSidebarService.close();
        }
    }

    private syncData(submission: Submission) {
        const idx = this.data().submissionList.findIndex(
            (s) => this.data().submission.ref?.id === s.ref?.id,
        );
        const newSubmission = {
            ...submission,
            assignment: this.data().assignment,
        };
        this.data().submissionList.splice(idx, 1, newSubmission);
        this.data.set({
            ...this.data(),
            submission: newSubmission,
        });
    }
}
