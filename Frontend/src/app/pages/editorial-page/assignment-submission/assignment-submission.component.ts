import {
    AfterViewInit,
    Component,
    computed,
    effect,
    EventEmitter,
    Output,
    signal,
    ViewChild,
} from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { combineLatest, distinctUntilChanged, filter, firstValueFrom } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import {
    Assignment,
    AssignmentV1Service,
    HOME_REPOSITORY,
    Submission,
    SubmissionFile,
} from 'ngx-edu-sharing-api';
import { map, switchMap } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
    AuthorityNamePipe,
    ColumnType,
    InteractionType,
    ListItem,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    RepoUrlService,
    Scope,
    Toast,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { EditorialPageService } from '../editorial-page.service';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import { SubmissionConfig } from '../submission-sidebar/submission-sidebar.component';
import { NgxExtendedPdfViewerModule, NgxExtendedPdfViewerService } from 'ngx-extended-pdf-viewer';
import { RenderWrapperComponent } from '../../render2-page/render-wrapper-component/render-wrapper.component';
import { RestConstants } from '../../../core-module/rest/rest-constants';

/**
 * lists all submissions (for teacher view)
 */
@Component({
    selector: 'es-assignment-submission',
    templateUrl: 'assignment-submission.component.html',
    styleUrls: ['assignment-submission.component.scss'],
    imports: [SharedModule, TranslateModule, NgxExtendedPdfViewerModule, RenderWrapperComponent],
})
export class AssignmentSubmissionComponent implements AfterViewInit {
    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Submission>;
    dataSource = new NodeDataSource<Submission>();
    language: string = 'de-DE';
    columns = {
        Default: [
            new ListItem('SUBMISSION', 'assignee'),
            new ListItem('SUBMISSION', 'submissionStatus'),
            new ListItem('SUBMISSION', 'validationStatus'),
        ],
    } as ColumnType;
    private assignment = signal<Assignment>(null);
    hasCorrectionChanges = signal(false);
    correctionSaving = signal(false);
    selectedSubmissionFile = signal<SubmissionFile>(null);
    selectedSubmissionFileUrl = signal<string>(undefined);
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
    ) {
        this.language = this.translationsService.getLocale();
        effect(() => {
            const file = this.selectedSubmissionFile();
            this.selectedSubmissionFileUrl.set(undefined);
            const correction = file?.correction;

            if (!correction?.downloadUrl) {
                this.selectedSubmissionFileUrl.set(null);
            } else {
                this.repoUrlService
                    .getRepoUrl(correction.downloadUrl, correction)
                    .then((url) => this.selectedSubmissionFileUrl.set(url));
            }
            this.selectedSubmissionFile()
                ? this.editorialBreadcrumbService.path.set([
                      {
                          title: this.assignment().title,
                          callback: () => this.selectedSubmissionFile.set(null),
                      },
                      {
                          title: new AuthorityNamePipe(this.translate).transform(
                              this.submission().assignee,
                          ),
                      },
                  ])
                : this.assignment
                ? this.editorialBreadcrumbService.path.set([{ title: this.assignment().title }])
                : this.editorialBreadcrumbService.path.set([]);
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
                        this.assignmentService.getSubmissions({
                            assignmentId,
                        }),
                    ]),
                ),
            )
            .subscribe(([assignment, files]) => {
                this.assignment.set(assignment);
                this.dataSource.setData(files);
            });
    }

    ngAfterViewInit(): void {}

    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    select(event: NodeClickEvent<Submission>) {
        this.nodeEntries.getSelection().setSelection(event.element);
        this.submission.set(event.element);
        this.editorialSidebarService.showOption({
            option: 'MANAGE_SUBMISSION',
            trap: true,
            optionConfig: {
                assignment: this.assignment(),
                submission: event.element,
                submissionFileCallback: (submission) => {
                    console.log(submission);
                    this.selectedSubmissionFile.set(submission);
                },
            } as SubmissionConfig,
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
                    submissionFileId: this.selectedSubmissionFile().ref.id,
                    body: {
                        metadata: {
                            validationStatus:
                                this.selectedSubmissionFile().validationStatus || 'PENDING',
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
}
