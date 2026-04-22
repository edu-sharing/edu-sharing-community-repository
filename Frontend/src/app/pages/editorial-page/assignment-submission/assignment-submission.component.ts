import { Component, effect, OnDestroy, signal, ViewChild } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import {
    combineLatest,
    distinctUntilChanged,
    filter,
    firstValueFrom,
    interval,
    Subject,
} from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Assignment, AssignmentV1Service, Submission, SubmissionFile } from 'ngx-edu-sharing-api';
import { map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
    AuthorityNamePipe,
    ColumnType,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    RepoUrlService,
    Scope,
    SubmissionWithAssignment,
    Toast,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { EditorialPageService } from '../editorial-page.service';
import { SubmissionConfig } from '../submission-sidebar/submission-sidebar.component';
import {
    IPDFViewerApplication,
    NgxExtendedPdfViewerComponent,
    NgxExtendedPdfViewerModule,
    NgxExtendedPdfViewerService,
} from 'ngx-extended-pdf-viewer';
import { RenderWrapperComponent } from '../../render2-page/render-wrapper-component/render-wrapper.component';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { GenericDialogButton } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';

/**
 * lists all submissions (for teacher view)
 */
@Component({
    selector: 'es-assignment-submission',
    templateUrl: 'assignment-submission.component.html',
    styleUrls: ['assignment-submission.component.scss'],
    imports: [SharedModule, TranslateModule, NgxExtendedPdfViewerModule, RenderWrapperComponent],
})
export class AssignmentSubmissionComponent implements OnDestroy {
    @ViewChild(NgxExtendedPdfViewerComponent)
    pdfViewer: NgxExtendedPdfViewerComponent;
    @ViewChild(NodeEntriesWrapperComponent)
    nodeEntries: NodeEntriesWrapperComponent<SubmissionWithAssignment>;
    dataSource = new NodeDataSource<SubmissionWithAssignment>();
    language: string = 'de-DE';

    columns = {
        Default: [
            new ListItem('SUBMISSION', 'assignee'),
            new ListItem('SUBMISSION', 'validationStatus'),
        ],
    } as ColumnType;
    private destroyed$ = new Subject<void>();
    assignment = signal<Assignment>(null);
    hasCorrectionChanges = signal(false);
    tabSelected = signal(0);
    correctionSaving = signal(false);
    selectedCorrectedFile = signal<SubmissionFile>(null);
    selectedSubmissionFileUrl = signal<string>(undefined);
    corrected = signal(0);
    private submission = signal<Submission>(null);
    constructor(
        private route: ActivatedRoute,
        private translate: TranslateService,
        private translationsService: TranslationsService,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
        public editorialPageService: EditorialPageService,
        private repoUrlService: RepoUrlService,
        private pdfViewerService: NgxExtendedPdfViewerService,
        private toast: Toast,
        public editorialSidebarService: EditorialSidebarService,
        private assignmentService: AssignmentV1Service,
        private dialogs: DialogsService,
    ) {
        interval(500)
            .pipe(
                takeUntil(this.destroyed$),
                map(() => {
                    return (this.pdfViewerService as any)
                        ?.PDFViewerApplication as IPDFViewerApplication;
                }),
            )
            .subscribe((pdf) => {
                this.hasCorrectionChanges.set(
                    this.hasCorrectionChanges() ||
                        (pdf?.pdfDocument?.annotationStorage?.size > 0 &&
                            (pdf as any)?._annotationStorageModified),
                );
            });
        this.dataSource
            .connect()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((s) => {
                this.corrected.set(s.filter((f) => f.validationStatus === 'FINISHED')?.length);
            });
        this.language = this.translationsService.getLocale();
        effect(() => {
            const file = this.selectedCorrectedFile();
            this.selectedSubmissionFileUrl.set(undefined);
            const correction = file?.correction;

            if (!correction?.downloadUrl) {
                this.selectedSubmissionFileUrl.set(null);
            } else {
                void this.repoUrlService
                    .getRepoUrl(correction.downloadUrl, correction)
                    .then((url) => this.selectedSubmissionFileUrl.set(url));
                this.hasCorrectionChanges.set(false);
            }
            if (this.assignment() && this.selectedCorrectedFile()) {
                this.editorialBreadcrumbService.path.set([
                    {
                        title: this.assignment().title,
                        callback: () => this.selectedCorrectedFile.set(null),
                    },
                    {
                        title: new AuthorityNamePipe(this.translate).transform(
                            this.submission().assignee,
                        ),
                    },
                ]);
            } else if (this.assignment()) {
                this.editorialBreadcrumbService.path.set([{ title: this.assignment().title }]);
            } else {
                this.editorialBreadcrumbService.path.set([]);
            }
        });
        this.route.queryParams
            .pipe(
                map((p) => p.assignment),
                filter((p) => !!p),
                distinctUntilChanged(),
                tap(() => (this.dataSource.isLoading = true)),
                switchMap((assignmentId) =>
                    combineLatest([
                        this.assignmentService.getAssignment({
                            assignmentId,
                        }),
                        this.assignmentService.getSubmissions({
                            assignmentId,
                        }),
                    ]),
                ),
            )
            .subscribe(([assignment, files]) => {
                this.assignment.set(assignment);
                this.dataSource.isLoading = false;
                this.dataSource.setData(
                    files.map((submission) => {
                        return {
                            ...submission,
                            assignment,
                        };
                    }) as SubmissionWithAssignment[],
                );
            });
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    select(event: SubmissionWithAssignment) {
        this.nodeEntries.getSelection().setSelection(event);
        this.submission.set(event);
        this.editorialSidebarService.showOption({
            option: 'MANAGE_SUBMISSION',
            trap: true,
            optionConfig: {
                assignment: this.assignment(),
                submission: event,
                submissionList: this.dataSource.getData(),
                submissionFileCallback: (submission) => {
                    this.selectedCorrectedFile.set(submission);
                },
            } as SubmissionConfig,
        });
        this.editorialSidebarService.configChange$
            .pipe(take(1))
            .subscribe((config: SubmissionConfig) => {
                this.dataSource.setData(config.submissionList);
                this.select(config.submission);
            });
    }

    changeAnnotation() {
        this.hasCorrectionChanges.set(true);
    }

    async saveCorrection() {
        this.correctionSaving.set(true);
        try {
            const binary = await this.pdfViewerService.getCurrentDocumentAsBlob();
            await firstValueFrom(
                this.assignmentService.updateSubmissionFileValidation({
                    assignmentId: this.assignment().ref.id,
                    submissionId: this.submission().ref.id,
                    submissionFileId: this.selectedCorrectedFile().ref.id,
                    body: {
                        metadata: {
                            validationStatus:
                                this.selectedCorrectedFile().validationStatus || 'PENDING',
                        },
                        binary,
                    },
                }),
            );
            this.toast.toast('EDITORIAL.ASSIGNMENT.SUBMISSIONS.CHANGES_SAVED');
        } catch (e) {}
        this.correctionSaving.set(false);
        this.hasCorrectionChanges.set(false);
    }

    async finishAll() {
        const missing = this.dataSource.getTotal() - this.corrected();
        const confirmLabel =
            missing > 0
                ? 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.FINISH_ALL_MISSING_CONFIRM_BUTTON'
                : 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.FINISH_ALL_CONFIRM_BUTTON';
        const buttons: GenericDialogButton<string>[] = [
            { label: 'CANCEL', config: { color: 'standard' } },
            { label: confirmLabel, config: { color: 'primary' } },
        ];
        const result = await firstValueFrom(
            (
                await this.dialogs.openGenericDialog({
                    title: 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.FINISH_ALL_CONFIRM_TITLE',
                    message:
                        missing > 0
                            ? 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.FINISH_ALL_MISSING_MESSAGE'
                            : 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.FINISH_ALL_CONFIRM_MESSAGE',
                    messageParameters: missing > 0 ? { count: String(missing) } : null,
                    buttons,
                })
            ).afterClosed(),
        );
        if (result !== confirmLabel) {
            return;
        }
        await firstValueFrom(
            this.assignmentService.createOrUpdateAssignment1({
                assignmentId: this.assignment().ref.id,
                status: 'CORRECTED',
            }),
        );
        this.assignment.set({
            ...this.assignment(),
            status: 'CORRECTED',
        });
        this.toast.toast('EDITORIAL.ASSIGNMENT.SUBMISSIONS.ALL_CORRECTED');
    }
}
