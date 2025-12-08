import { Component, computed, signal } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import {
    Assignment,
    Node,
    AssignmentV1Service,
    ME,
    Submission,
    AssignmentFile,
} from 'ngx-edu-sharing-api';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { combineLatest, filter, firstValueFrom, of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import {
    ColumnType,
    Constrain,
    DefaultGroups,
    ElementType,
    InteractionType,
    ListItem,
    ListOptionsConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeHelperService,
    OptionData,
    OptionItem,
} from 'ngx-edu-sharing-ui';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { OptionsHelperService } from '../../../services/options-helper.service';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { AssignmentEditorConfig } from '../manage-assignment/manage-assignment.component';
import { PlatformLocation } from '@angular/common';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import { NodeWithRole } from '../manage-assignment-nodes/manage-assignment-nodes.component';
import { SubmissionFile } from '../../../../../dist/edu-sharing-api/lib/api/models/submission-file';
import { TabType } from '../nodes-selector/nodes-selector.component';

/**
 * submits an individual assignment (for student)
 */
@Component({
    selector: 'es-submit-assignment',
    templateUrl: 'submit-assignment.component.html',
    styleUrls: ['submit-assignment.component.scss'],
    imports: [SharedModule, TranslateModule, EditorComponent],
})
export class SubmitAssignmentComponent {
    readonly editorConfig = {
        ...AssignmentEditorConfig,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'tinymce',
        language: this.translateService.getDefaultLang(),
    };
    columns: ColumnType = {
        Default: [new ListItem('NODE', 'title')],
    };
    uploadOption = new OptionItem(
        'EDITORIAL.SUBMIT_ASSIGNMENT.ADD_ASSIGNMENT_MATERIAL',
        'add',
        () => this.showFileDialog(),
    );
    submitFormGroup: FormGroup;
    submittableConfig: ListOptionsConfig;
    submittableConfigRO: ListOptionsConfig;
    supplementaryConfig: ListOptionsConfig;
    files = signal<AssignmentFile[]>(null);
    loading = signal(false);
    assignment = signal<Assignment>(null);
    submission = signal<Submission>(null);
    isOpenForSubmission = computed(() => ['OPEN', 'PROGRESS'].includes(this.assignment().status));
    isBeforeEndDate = computed(() => {
        // @TODO check endTime format vs delivered type
        return (
            !this.assignment().endTime ||
            (Date.parse(this.assignment().endTime) ||
                (this.assignment().endTime as unknown as number)) > new Date().getTime()
        );
    });
    /**
     * files that the student wants to submit
     */
    submissionFiles = signal<SubmissionFile[]>(null);
    submissionAssignmentRefFile = signal<AssignmentFile>(null);
    submissionReplaceFile = signal<SubmissionFile | AssignmentFile>(null);
    canSubmitMaterials = computed(
        () => this.isOpenForSubmission() && this.isBeforeEndDate() && !this.submissionSent(),
    );
    submissionSent = computed(
        () => this.submission() && this.submission()?.submissionStatus !== 'NOT_STARTET',
    );
    canSendSubmission = computed(
        () =>
            this.isOpenForSubmission() &&
            this.isBeforeEndDate() &&
            !this.loading() &&
            this.files().every(
                (f) => f.documentRole === 'SUPPLEMENTARY' || this.hasSubmissionFor(f.referNode),
            ),
    );
    submittableFiles = new NodeDataSource<Node>();
    supplementaryFiles = new NodeDataSource<Node>();

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
        private editorialSidebarService: EditorialSidebarService,
        private nodeHelperService: NodeHelperService,
        private translateService: TranslateService,
        private platformLocation: PlatformLocation,
        private assignmentService: AssignmentV1Service,
        private optionsHelperService: OptionsHelperService,
        private formBuilder: FormBuilder,
        private uiService: UIService,
    ) {
        this.initOptions();
        this.editorialSidebarService.applyNodeEmitted.subscribe(async ({ nodes }) => {
            if (this.submissionReplaceFile()) {
            } else {
                const newFiles = nodes.map((node) => {
                    return {
                        assignmentFile: this.submissionAssignmentRefFile(),
                        content: node,
                        ref: node.ref,
                        validationStatus: 'NOT_STARTET',
                    } as SubmissionFile;
                });
                await this.saveSubmissionFiles(newFiles);
                this.submissionFiles.set((this.submissionFiles() || []).concat(newFiles));
                this.syncSubmissionDataSource();
            }
        });
        this.submitFormGroup = this.formBuilder.group({
            submitComment: ['', [Validators.required]],
        });
        this.route.queryParams
            .pipe(
                map((p) => p.assignment),
                filter((p) => !!p),
                distinctUntilChanged(),
                switchMap((assignmentId) =>
                    combineLatest([
                        this.assignmentService.getAssignment({
                            assignmentId,
                        }),
                        this.assignmentService.getAssignmentFiles({
                            assignmentId,
                        }),
                        this.assignmentService
                            .getSubmission({
                                assignmentId,
                                submissionId: ME,
                            })
                            .pipe(
                                catchError((err) => {
                                    if (err.status === RestConstants.HTTP_NOT_FOUND || true) {
                                        err.preventDefault();
                                        return of(null);
                                    }
                                    return throwError(() => err);
                                }),
                            ),
                        this.assignmentService
                            .getSubmissionFiles({
                                assignmentId,
                                submissionId: ME,
                            })
                            .pipe(
                                catchError((err) => {
                                    if (err.status === RestConstants.HTTP_NOT_FOUND || true) {
                                        err.preventDefault();
                                        return of(null);
                                    }
                                    return throwError(() => err);
                                }),
                            ),
                    ]),
                ),
            )
            .subscribe(([assignment, files, submission, submissionFiles]) => {
                this.assignment.set(assignment);
                this.files.set(files);
                this.submission.set(submission);
                this.submissionFiles.set(submissionFiles);
                this.syncSubmissionDataSource();
                this.supplementaryFiles.setData(
                    files.filter((f) => f.documentRole === 'SUPPLEMENTARY').map((n) => n.referNode),
                );
                this.editorialBreadcrumbService.path.set([assignment.title]);
            });
    }

    close() {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: {
                mainComponent: null,
            },
        });
    }

    showFileDialog(replaceFile?: SubmissionFile, assignmentFile?: Node) {
        this.submissionReplaceFile.set(replaceFile);
        this.submissionAssignmentRefFile.set(
            assignmentFile
                ? this.files().find((f) => f.referNode.ref.id === assignmentFile.ref.id)
                : null,
        );
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            optionState: TabType.UPLOAD,
            trap: true,
            applyCallback: (nodes) =>
                nodes.every((n) => !this.nodeHelperService.isNodeCollection(n) && !n.isDirectory),
        });
    }

    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly InteractionType = InteractionType;

    private initOptions() {
        const editConnectorNode = new OptionItem('OPTIONS.OPEN', 'launch', (node) => {
            void this.uiService.editConnector(node);
        });
        editConnectorNode.customShowCallback = async (nodes) => {
            return await this.uiService.hasAvailableConnector(nodes ? nodes[0] : null);
        };
        editConnectorNode.group = DefaultGroups.View;
        editConnectorNode.priority = 30;
        editConnectorNode.showAlways = true;
        editConnectorNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
        ];
        const uploadManually = new OptionItem(
            'OPTIONS.ASSIGNMENT_SUBMIT_MANUALLY',
            'cloud_upload',
            (node) => {
                void this.uiService.editConnector(node);
            },
        );
        uploadManually.customShowCallback = async (nodes) => {
            return !(await this.uiService.hasAvailableConnector(nodes ? nodes[0] : null));
        };
        uploadManually.group = DefaultGroups.View;
        uploadManually.priority = 30;
        uploadManually.showAlways = true;
        uploadManually.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];
        const download = this.optionsHelperService.getDownloadOption({} as OptionData);
        download.group = DefaultGroups.View;
        download.priority = 10;
        download.constrains = [];
        download.showAlways = true;

        const remove = new OptionItem(
            'EDITORIAL.OPTIONS.SUBMISSION_REMOVE',
            'close',
            async (node) => {
                await this.deleteSubmissionFiles(this.hasSubmissionFor(node));
                this.submissionFiles().splice(
                    this.submissionFiles().indexOf(this.hasSubmissionFor(node)),
                    1,
                );
                this.syncSubmissionDataSource();
            },
        );
        remove.group = DefaultGroups.Delete;
        remove.priority = 10;
        remove.showAlways = true;
        remove.customShowCallback = async (nodes) => !!this.hasSubmissionFor(nodes?.[0]);

        this.submittableConfigRO = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [download],
            },
        };

        this.submittableConfig = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [download, remove],
            },
        };

        this.supplementaryConfig = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [download],
            },
        };
    }

    hasSubmissionFor(element: Node) {
        return this.submissionFiles()?.find(
            (n) =>
                n.assignmentFile?.referNode.ref.id === element.ref.id ||
                n.content?.ref.id === element.ref.id,
        );
    }

    private syncSubmissionDataSource() {
        const nodes = this.files()
            .filter((f) => f.documentRole === 'SUBMITTABLE')
            .map((n) => n.referNode)
            .concat(
                (this.submissionFiles() || [])
                    .filter((f) => !f.assignmentFile)
                    .map((f) => f.content),
            );
        this.submittableFiles.setData(nodes);
        this.initOptions();
    }

    async deleteSubmissionFiles(file: SubmissionFile) {
        this.loading.set(true);
        await firstValueFrom(
            this.assignmentService.deleteSubmissionFile({
                assignmentId: this.assignment().ref.id,
                submissionId: this.submission()?.ref.id || ME,
                submissionFileId: file.ref.id,
            }),
        );
        this.loading.set(false);
    }

    private async saveSubmissionFiles(newFiles: SubmissionFile[]) {
        this.loading.set(true);
        for (let file of newFiles) {
            await firstValueFrom(
                this.assignmentService.createSubmissionFile({
                    assignmentId: this.assignment().ref.id,
                    submissionId: this.submission()?.ref.id || ME,
                    body: {
                        metadata: {
                            originalFile: file.ref.id,
                            assignmentFile: file.assignmentFile?.ref.id,
                            properties: {},
                        },
                    },
                }),
            );
        }
        this.loading.set(false);
    }

    submit() {}
}
