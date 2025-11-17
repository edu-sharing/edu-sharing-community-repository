import { Component } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { combineLatest, distinctUntilChanged, filter } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { AssignmentV1Service, Submission } from 'ngx-edu-sharing-api';
import { map, switchMap } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';
import {
    ColumnType,
    InteractionType,
    ListItem,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    Scope,
} from 'ngx-edu-sharing-ui';
import { EditorialPageService } from '../editorial-page.service';

/**
 * lists all submissions (for teacher view)
 */
@Component({
    selector: 'es-assignment-submission',
    templateUrl: 'assignment-submission.component.html',
    styleUrls: ['assignment-submission.component.scss'],
    imports: [SharedModule, TranslateModule],
})
export class AssignmentSubmissionComponent {
    dataSource = new NodeDataSource<Submission>();
    columns = {
        Default: [
            new ListItem('SUBMISSION', 'assignee'),
            new ListItem('SUBMISSION', 'submissionStatus'),
            new ListItem('SUBMISSION', 'validationStatus'),
        ],
    } as ColumnType;
    constructor(
        private route: ActivatedRoute,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
        public editorialPageService: EditorialPageService,
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
                this.editorialBreadcrumbService.path.set([assignment.title]);
                this.dataSource.setData(files);
            });
    }

    protected readonly InteractionType = InteractionType;
    protected readonly Scope = Scope;
    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    select(event: NodeClickEvent<Submission>) {}
}
