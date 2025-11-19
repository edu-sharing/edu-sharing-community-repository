import { Component, computed, signal } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { Assignment, AssignmentFile, AssignmentV1Service } from 'ngx-edu-sharing-api';
import { TranslateModule } from '@ngx-translate/core';
import { combineLatest, filter } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import {
    ColumnType,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
} from 'ngx-edu-sharing-ui';

/**
 * submits an invdividual assignment (for student)
 */
@Component({
    selector: 'es-submit-assignment',
    templateUrl: 'submit-assignment.component.html',
    styleUrls: ['submit-assignment.component.scss'],
    imports: [SharedModule, TranslateModule],
})
export class SubmitAssignmentComponent {
    columns: ColumnType = {
        Default: [new ListItem('NODE', 'title')],
    };
    assignment = signal<Assignment>(null);
    submittableFiles = new NodeDataSource<AssignmentFile>();
    supplementaryFiles = new NodeDataSource<AssignmentFile>();
    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
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
                        this.assignmentService.getAssignmentFiles({
                            assignmentId,
                        }),
                    ]),
                ),
            )
            .subscribe(([assignment, files]) => {
                this.assignment.set(assignment);
                this.submittableFiles.setData(
                    files.filter((f) => f.documentRole === 'SUBMITTABLE'),
                );
                this.supplementaryFiles.setData(
                    files.filter((f) => f.documentRole === 'SUPPLEMENTARY'),
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

    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly InteractionType = InteractionType;
}
