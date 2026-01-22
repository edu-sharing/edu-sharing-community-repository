import { AfterViewInit, Component, EventEmitter, Output, signal, ViewChild } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { combineLatest, distinctUntilChanged, filter } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Assignment, AssignmentV1Service, Submission } from 'ngx-edu-sharing-api';
import { map, switchMap } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';
import {
    ColumnType,
    InteractionType,
    ListItem,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    Scope,
} from 'ngx-edu-sharing-ui';
import { EditorialPageService } from '../editorial-page.service';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';

/**
 * lists all submissions (for teacher view)
 */
@Component({
    selector: 'es-assignment-submission',
    templateUrl: 'assignment-submission.component.html',
    styleUrls: ['assignment-submission.component.scss'],
    imports: [SharedModule, TranslateModule],
})
export class AssignmentSubmissionComponent implements AfterViewInit {
    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Submission>;
    dataSource = new NodeDataSource<Submission>();
    columns = {
        Default: [
            new ListItem('SUBMISSION', 'assignee'),
            new ListItem('SUBMISSION', 'submissionStatus'),
            new ListItem('SUBMISSION', 'validationStatus'),
        ],
    } as ColumnType;
    private assignment = signal<Assignment>(null);
    constructor(
        private route: ActivatedRoute,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
        public editorialPageService: EditorialPageService,
        public editorialSidebarService: EditorialSidebarService,
        private assignmentService: AssignmentV1Service,
    ) {
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
                this.editorialBreadcrumbService.path.set([assignment.title]);
                this.dataSource.setData(files);
            });
    }

    ngAfterViewInit(): void {}

    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    select(event: NodeClickEvent<Submission>) {
        this.nodeEntries.getSelection().setSelection(event.element);
        this.editorialSidebarService.showOption({
            option: 'MANAGE_SUBMISSION',
            trap: true,
            optionConfig: {
                assignment: this.assignment(),
                submission: event.element,
            },
        });
    }
}
