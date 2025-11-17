import { Component, signal, ViewChild } from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ShareDialogChooseDateComponent } from '../../../features/dialogs/dialog-modules/share-dialog/permission/choose-date/choose-date.component';
import { EditorialSidebarService } from '../editorial-sidebar/editorial-sidebar.service';
import {
    ManageAssignmentNodesComponent,
    NodeWithRole,
} from '../manage-assignment-nodes/manage-assignment-nodes.component';
import {
    Assignment,
    AssignmentFileRequest,
    AssignmentV1Service,
    Authority,
    CreateAssignmentRequest,
    Permission,
    PermissionRequest,
    Submission,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../services/node-helper.service';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { PlatformLocation } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ManageAssignmentAuthoritiesComponent } from '../manage-assignment-authorities/manage-assignment-authorities.component';
import { Toast } from 'ngx-edu-sharing-ui';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { combineLatest, filter, firstValueFrom } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { EditorialPageService } from '../editorial-page.service';

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
    assignment = signal<Assignment>(null);
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
                    ]),
                ),
            )
            .subscribe(([assignment]) => {
                this.assignment.set(assignment);
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
}
