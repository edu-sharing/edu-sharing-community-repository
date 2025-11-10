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
    PermissionRequest,
    Submission,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../services/node-helper.service';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { PlatformLocation } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import {
    AuthorityWithSubmission,
    ManageAssignmentAuthoritiesComponent,
} from '../manage-assignment-authorities/manage-assignment-authorities.component';
import { Toast } from 'ngx-edu-sharing-ui';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { combineLatest, filter, firstValueFrom } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';

export type AssignmentBase = Pick<Assignment, 'title' | 'type' | 'summary'>;
@Component({
    selector: 'es-manage-assignment',
    templateUrl: 'manage-assignment.component.html',
    styleUrls: ['manage-assignment.component.scss'],
    imports: [
        SharedModule,
        MatStepperModule,
        MatFormFieldModule,
        ShareDialogChooseDateComponent,
        ManageAssignmentNodesComponent,
        ManageAssignmentAuthoritiesComponent,
        EditorComponent,
    ],
})
export class ManageAssignmentComponent {
    readonly editorConfig = {
        branding: false,
        height: 200,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'tinymce',
        suffix: '.min',
        menubar: false,
        statusbar: false,
        resize: true,
        plugins: ['lists'],
        default_link_target: '_blank',
        link_title: false,
        link_assume_external_targets: true,
        toolbar: 'bold | bullist numlist | undo redo',
        language: this.translateService.getDefaultLang(),
    };
    now = new Date().getTime();
    dateTime = new Date().getTime() + 1000 * 3600 * 24 * 5;
    @ViewChild(MatStepper) matStepper: MatStepper;
    @ViewChild('dateChooser') dateChooserRef: ShareDialogChooseDateComponent;
    assignment = signal<Assignment>({
        type: 'SUBMISSION',
        status: 'OPEN',
    } as Assignment);
    authorities = signal<AuthorityWithSubmission[]>(null);
    mainDataFormGroup: FormGroup;
    nodes = signal<NodeWithRole[]>(null);
    submissions = signal<Submission[]>(null);
    validateMainForm() {
        this.mainDataFormGroup.markAllAsTouched();
        if (!this.mainDataFormGroup.valid) {
            for (let entry of Object.entries(this.mainDataFormGroup.controls)) {
                if (entry[1].errors) {
                    this.toast.error(
                        null,
                        'EDITORIAL.ASSIGNMENT.ERROR.FIELD_' + entry[0].toUpperCase(),
                    );
                    break;
                }
            }
        } else {
            this.matStepper.next();
        }
    }

    constructor(
        private formBuilder: FormBuilder,
        private toast: Toast,
        private router: Router,
        private route: ActivatedRoute,
        private dialogsService: DialogsService,
        private assignmentService: AssignmentV1Service,
        private nodeHelperService: NodeHelperService,
        private platformLocation: PlatformLocation,
        private translateService: TranslateService,
        private editorialSidebarService: EditorialSidebarService,
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
                        this.assignmentService.getSubmissions({
                            assignmentId,
                        }),
                    ]),
                ),
            )
            .subscribe(([assignment, files, submissions]) => {
                this.assignment.set(assignment);
                this.submissions.set(submissions);
                this.mainDataFormGroup.setValue({
                    title: assignment.title,
                    summary: assignment.summary,
                    useEndTime: assignment.endTime !== null,
                    allowAdditionalDocumentSubmissions:
                        assignment.allowAdditionalDocumentSubmissions,
                });
                this.nodes.set(
                    files.map((f) => {
                        return {
                            ...f.referNode,
                            documentRole: f.documentRole,
                            isDone: f.isDone,
                            refId: f.ref.id,
                        } as NodeWithRole;
                    }),
                );
            });

        this.mainDataFormGroup = this.formBuilder.group({
            title: ['', [Validators.required]],
            summary: ['', [Validators.required]],
            useEndTime: [false, [Validators.required]],
            allowAdditionalDocumentSubmissions: [true, [Validators.required]],
        });
        this.editorialSidebarService.applyNodeEmitted.subscribe(({ nodes }) => {
            this.nodes.set(
                (this.nodes() || []).concat(
                    nodes
                        .filter((n) => !(this.nodes() || []).some((n2) => n2.ref?.id === n.ref?.id))
                        .map((node) => {
                            return {
                                ...node,
                                documentRole: 'SUPPLEMENTARY',
                            } as NodeWithRole;
                        }),
                ),
            );
        });
    }

    showFileDialog() {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            trap: true,
            applyCallback: (nodes) =>
                nodes.every((n) => !this.nodeHelperService.isNodeCollection(n) && !n.isDirectory),
        });
    }

    addAuthority(authority: Authority) {
        if ((this.authorities() || []).some((n) => n.authorityName === authority.authorityName)) {
            return;
        }
        (authority as AuthorityWithSubmission).role = 'ASSIGNEE';
        this.authorities.set((this.authorities() || []).concat(authority));
    }

    async submit() {
        if (!this.authorities()?.length) {
            this.toast.error(null, 'EDITORIAL.ASSIGNMENT.ERROR.MISSING_AUTHORITIES');
            return;
        }
        if (
            this.assignment().type === 'SUBMISSION' &&
            !this.authorities()?.some((a) => a.role === 'ASSIGNEE')
        ) {
            this.toast.error(null, 'EDITORIAL.ASSIGNMENT.ERROR.MISSING_AUTHORITIES_ASSIGNEE');
            return;
        }
        const permissions: PermissionRequest[] = this.authorities().map((a) => {
            return {
                authorityName: a.authorityName,
                role: a.role,
            };
        });
        const assignmentFiles =
            this.nodes()?.map((n) => {
                return {
                    refId: n.refId || n.ref.id,
                    isDone: n.isDone || false,
                    documentRole: n.documentRole,
                } as AssignmentFileRequest;
            }) || [];
        const assignment: CreateAssignmentRequest = {
            id: this.assignment().ref?.id,
            status: this.assignment().status,
            type: this.assignment().type,
            title: this.mainDataFormGroup.get('title').value,
            summary: this.mainDataFormGroup.get('summary').value,
            allowAdditionalDocumentSubmissions: this.mainDataFormGroup.get(
                'allowAdditionalDocumentSubmissions',
            ).value,
            endTime: this.mainDataFormGroup.get('useEndTime').value
                ? new Date(this.dateTime).toISOString()
                : null,
            permissions,
            assignmentFiles,
        };
        await firstValueFrom(
            this.assignmentService.createOrUpdateAssignment({
                body: assignment,
            }),
        );
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: {
                mainComponent: null,
                filters: JSON.stringify({ 'virtual:assignmentType': ['created'] }),
            },
        });
    }

    async cancel() {
        let close = false;
        if (this.mainDataFormGroup.dirty) {
            if (await this.dialogsService.openGenericConfirmCancelDialog()) {
                close = true;
            }
        } else {
            close = true;
        }
        if (close) {
            void this.router.navigate([], {
                relativeTo: this.route,
                queryParamsHandling: 'merge',
                queryParams: {
                    mainComponent: null,
                },
            });
        }
    }
}
