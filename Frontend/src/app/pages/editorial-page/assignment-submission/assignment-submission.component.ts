import {
    AfterViewInit,
    Component,
    effect,
    EventEmitter,
    Output,
    signal,
    ViewChild,
} from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { combineLatest, distinctUntilChanged, filter } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Assignment, AssignmentV1Service, Submission, SubmissionFile } from 'ngx-edu-sharing-api';
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
    Scope,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { EditorialPageService } from '../editorial-page.service';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import { SubmissionConfig } from '../submission-sidebar/submission-sidebar.component';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { RenderWrapperComponent } from '../../render2-page/render-wrapper-component/render-wrapper.component';

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
    selectedSubmissionFile = signal<SubmissionFile>(null);
    private submission = signal<Submission>(null);
    constructor(
        private route: ActivatedRoute,
        private translate: TranslateService,
        private translationsService: TranslationsService,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
        public editorialPageService: EditorialPageService,
        public editorialSidebarService: EditorialSidebarService,
        private assignmentService: AssignmentV1Service,
    ) {
        this.language = this.translationsService.getLocale();
        effect(() => {
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

    changeAnnotation() {}
}
