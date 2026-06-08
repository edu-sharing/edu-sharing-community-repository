import { Component, effect, OnDestroy, signal, ViewChild, inject } from '@angular/core';
import { MatTabChangeEvent, MatTabGroup } from '@angular/material/tabs';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import {
    combineLatest,
    distinctUntilChanged,
    filter,
    firstValueFrom,
    interval,
    merge,
    Subject,
} from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Assignment, AssignmentV1Service, Submission, SubmissionFile } from 'ngx-edu-sharing-api';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
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
import { UIService } from '../../../core-module/rest/services/ui.service';
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
    private route = inject(ActivatedRoute);
    private translate = inject(TranslateService);
    private translationsService = inject(TranslationsService);
    private editorialBreadcrumbService = inject(EditorialBreadcrumbService);
    editorialPageService = inject(EditorialPageService);
    private repoUrlService = inject(RepoUrlService);
    private pdfViewerService = inject(NgxExtendedPdfViewerService);
    private toast = inject(Toast);
    editorialSidebarService = inject(EditorialSidebarService);
    private assignmentService = inject(AssignmentV1Service);
    private dialogs = inject(DialogsService);
    private authorityNamePipe = inject(AuthorityNamePipe);

    @ViewChild(NgxExtendedPdfViewerComponent)
    pdfViewer: NgxExtendedPdfViewerComponent;
    @ViewChild(NodeEntriesWrapperComponent)
    nodeEntries: NodeEntriesWrapperComponent<SubmissionWithAssignment>;
    @ViewChild(MatTabGroup)
    tabGroup: MatTabGroup;
    dataSource = new NodeDataSource<SubmissionWithAssignment>();
    language: string = 'de-DE';

    columns = {
        Default: [
            new ListItem('SUBMISSION', 'assignee'),
            new ListItem('SUBMISSION', 'validationStatus'),
        ],
    } as ColumnType;
    private destroyed$ = new Subject<void>();
    private sidebarClosed$ = new Subject<void>();
    assignment = signal<Assignment>(null);
    hasCorrectionChanges = signal(false);
    tabSelected = signal(0);
    correctionSaving = signal(false);
    selectedCorrectedFile = signal<SubmissionFile>(null);
    selectedSubmissionFileUrl = signal<string>(undefined);
    corrected = signal(0);
    private submission = signal<Submission>(null);
    constructor() {
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
            if (this.selectedCorrectedFile()) {
                this.editorialPageService.close.set({
                    show: true,
                    callback: () => void this.closeDocument(),
                });
            } else {
                this.editorialPageService.close.set(null);
            }
        });
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
                        callback: async () => {
                            if (await this.confirmUnsavedChanges()) {
                                this.selectedCorrectedFile.set(null);
                            }
                        },
                    },
                    {
                        title: this.authorityNamePipe.transform(this.submission().assignee),
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
                this.setSubmissions(files);
            });
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.sidebarClosed$.next();
        this.sidebarClosed$.complete();
        this.editorialPageService.close.set(null);
    }

    private async closeDocument() {
        if (await this.confirmUnsavedChanges()) {
            this.selectedCorrectedFile.set(null);
        }
    }
    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    private setSubmissions(submissions: Submission[]) {
        this.dataSource.setData(
            submissions.map((submission) => ({
                ...submission,
                assignment: this.assignment(),
            })) as SubmissionWithAssignment[],
        );
    }

    select(event: SubmissionWithAssignment) {
        this.sidebarClosed$.next();
        this.nodeEntries.getSelection().setSelection(event);
        this.submission.set(event);
        this.editorialSidebarService.showOption({
            option: 'MANAGE_SUBMISSION',
            trap: true,
            optionConfig: {
                assignment: this.assignment(),
                submission: event,
                submissionList: this.dataSource.getData(),
                submissionFileCallback: async (submission) => {
                    if (!(await this.confirmUnsavedChanges())) {
                        return false;
                    }
                    this.selectedCorrectedFile.set(submission);
                    if (UIService.isMobileWidth()) {
                        this.editorialSidebarService.close();
                    }
                    return true;
                },
            } as SubmissionConfig,
        });
        this.editorialSidebarService.configChange$
            .pipe(takeUntil(merge(this.sidebarClosed$, this.destroyed$)))
            .subscribe((config: SubmissionConfig) => {
                this.dataSource.setData(config.submissionList);
                this.nodeEntries?.getSelection()?.setSelection(config.submission);
                this.submission.set(config.submission);
            });
    }

    async onTabChange(event: MatTabChangeEvent): Promise<void> {
        if (event.index !== 0 && this.tabSelected() === 0 && this.hasCorrectionChanges()) {
            // revert synchronously to keep the pdf viewer (and its annotations) alive while asking
            this.tabGroup.selectedIndex = 0;
            if (await this.confirmUnsavedChanges()) {
                this.tabSelected.set(event.index);
                this.tabGroup.selectedIndex = event.index;
            }
            return;
        }
        this.tabSelected.set(event.index);
    }

    private async confirmUnsavedChanges(): Promise<boolean> {
        if (!this.hasCorrectionChanges()) {
            return true;
        }
        const SAVE = 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.SAVE_EDIT';
        const buttons: GenericDialogButton<string>[] = [
            { label: 'DISCARD', config: { color: 'standard' } },
            { label: SAVE, config: { color: 'primary' } },
        ];
        const result = await firstValueFrom(
            (
                await this.dialogs.openGenericDialog({
                    title: 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.UNSAVED_CHANGES_TITLE',
                    message: 'EDITORIAL.ASSIGNMENT.SUBMISSIONS.UNSAVED_CHANGES_MESSAGE',
                    buttons,
                })
            ).afterClosed(),
        );
        if (result === SAVE) {
            // save and stay on the editor
            await this.saveCorrection();
            return false;
        }
        if (result === 'DISCARD') {
            this.hasCorrectionChanges.set(false);
            return true;
        }
        return false;
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
            // clear the pdf.js dirty flag, otherwise the interval check re-flags unsaved changes
            const pdf = (this.pdfViewerService as any)
                ?.PDFViewerApplication as IPDFViewerApplication;
            (pdf?.pdfDocument?.annotationStorage as any)?.resetModified?.();
            this.hasCorrectionChanges.set(false);
        } catch (e) {}
        this.correctionSaving.set(false);
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
        this.setSubmissions(
            await firstValueFrom(
                this.assignmentService.getSubmissions({
                    assignmentId: this.assignment().ref.id,
                }),
            ),
        );
        this.toast.toast('EDITORIAL.ASSIGNMENT.SUBMISSIONS.ALL_CORRECTED');
    }
}
