import { Component, computed, signal, ViewChild } from '@angular/core';
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
    AuthenticationService,
    Authority,
    CreateAssignmentRequest,
    Permission,
    PermissionRequest,
    Submission,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from '../../../services/node-helper.service';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { PlatformLocation } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ManageAssignmentAuthoritiesComponent } from '../manage-assignment-authorities/manage-assignment-authorities.component';
import { Toast } from 'ngx-edu-sharing-ui';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { combineLatest, EMPTY, firstValueFrom } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { NodesSelectorConfig } from '../nodes-selector/nodes-selector.component';
import { EditorialPageService } from '../editorial-page.service';

export type AssignmentBase = Pick<Assignment, 'title' | 'type' | 'summary'>;
export const AssignmentEditorConfig = {
    branding: false,
    height: 200,
    suffix: '.min',
    menubar: false,
    statusbar: false,
    resize: true,
    plugins: ['lists'],
    default_link_target: '_blank',
    link_title: false,
    newline_behavior: 'invert',
    link_assume_external_targets: true,
    toolbar: 'bold | bullist numlist | undo redo',
} as any;
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
        ...AssignmentEditorConfig,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'tinymce',
        language: this.translateService.getDefaultLang(),
    };
    now = new Date().getTime();
    dateTime = new Date().getTime() + 1000 * 3600 * 24 * 5;
    @ViewChild(MatStepper) matStepper: MatStepper;
    @ViewChild('dateChooser') dateChooserRef: ShareDialogChooseDateComponent;
    readonly EmptyAssignment = {
        type: 'SUBMISSION',
        status: 'DRAFT',
    } as Assignment;
    assignment = signal<Assignment>(this.EmptyAssignment);
    authorities = signal<Permission[]>(null);
    mainDataFormGroup: FormGroup;
    nodes = signal<NodeWithRole[]>(null);
    submissions = signal<Submission[]>(null);
    submissionsWithContent = computed(() =>
        this.submissions()?.filter((s) => s.submissionStatus !== 'NOT_STARTED'),
    );
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
        private authenticationService: AuthenticationService,
        private dialogsService: DialogsService,
        private assignmentService: AssignmentV1Service,
        private nodeHelperService: NodeHelperService,
        private platformLocation: PlatformLocation,
        private translateService: TranslateService,
        private editorialPageService: EditorialPageService,
        private editorialSidebarService: EditorialSidebarService,
        private editorialBreadcrumbService: EditorialBreadcrumbService,
    ) {
        this.mainDataFormGroup = this.formBuilder.group({
            title: ['', [Validators.required]],
            summary: ['', [Validators.required]],
            useEndTime: [false, []],
            allowAdditionalDocumentSubmissions: [true, []],
        });
        this.route.queryParams
            .pipe(
                map((p) => p.assignment),
                distinctUntilChanged(),
                switchMap((assignmentId) => {
                    if (assignmentId) {
                        return combineLatest([
                            this.assignmentService.getAssignment({
                                assignmentId,
                            }),
                            this.assignmentService.getAssignmentFiles({
                                assignmentId,
                            }),
                            this.assignmentService.getSubmissions({
                                assignmentId,
                            }),
                        ]);
                    } else {
                        this.editorialBreadcrumbService.path.set([]);
                        this.assignment.set(this.EmptyAssignment);
                        this.submissions.set(null);
                        this.authorities.set(null);
                        this.mainDataFormGroup.reset();
                        this.nodes.set(null);
                        return EMPTY;
                    }
                }),
            )
            .subscribe(([assignment, files, submissions]) => {
                this.editorialBreadcrumbService.path.set([{ title: assignment.title }]);
                this.assignment.set(assignment);
                this.submissions.set(submissions);
                this.authorities.set(assignment.permissions);
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
            this.editorialSidebarService.sidebarOpened.set(false);
        });
    }

    showFileDialog() {
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            optionConfig: {
                upload: 'fast',
                applyLabel: 'EDITORIAL.ASSIGNMENT.SELECT_FILE',
                applyCallback: (nodes) =>
                    nodes.every(
                        (n) => !this.nodeHelperService.isNodeCollection(n) && !n.isDirectory,
                    ),
            } as NodesSelectorConfig,
            trap: true,
        });
    }

    async addAuthority(authority: Authority) {
        if (
            (this.authorities() || []).some(
                (n) => n.authority.authorityName === authority.authorityName,
            )
        ) {
            return;
        }
        const login = await firstValueFrom(this.authenticationService.observeLoginInfo());
        if (login.authorityName === authority.authorityName) {
            this.toast.error(null, 'EDITORIAL.ASSIGNMENT.ERROR.OWN_AUTHORITY');
            return;
        }
        this.authorities.set(
            (this.authorities() || []).concat({
                authority,
                role: 'ASSIGNEE',
            }),
        );
    }

    async submit(status: Assignment['status']) {
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
        console.log(this.authorities(), this.nodes());
        const permissions: PermissionRequest[] = this.authorities().map((a) => {
            return {
                authorityName: a.authority.authorityName,
                role: a.role,
            };
        });
        const assignmentFiles =
            this.nodes()?.map((n) => {
                return {
                    refId: n.ref.id || n.refId,
                    isDone: n.isDone || false,
                    documentRole: n.documentRole,
                } as AssignmentFileRequest;
            }) || [];
        const assignment: CreateAssignmentRequest = {
            id: this.assignment().ref?.id,
            status: status,
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
        const newAssignment = await firstValueFrom(
            this.assignmentService.createOrUpdateAssignment({
                body: assignment,
            }),
        );
        this.editorialPageService.addVirtualNodes([newAssignment], 'assignment');
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
