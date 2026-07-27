import {
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    OnDestroy,
    QueryList,
    signal,
    Signal,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { SharedModule } from '../../../shared/shared.module';
import {
    Assignment,
    AssignmentFile,
    AssignmentV1Service,
    ConnectorService,
    ME,
    Node,
    NodeService,
    Permission,
    Submission,
    SubmissionFile,
} from 'ngx-edu-sharing-api';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
    combineLatest,
    firstValueFrom,
    interval,
    of,
    Subject,
    Subscription,
    throwError,
} from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    switchMap,
    takeUntil,
    takeWhile,
} from 'rxjs/operators';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import {
    ActionbarComponent,
    ColumnType,
    Constrain,
    DefaultGroups,
    InteractionType,
    ListItem,
    ListOptionsConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    NodeTitlePipe,
    OptionData,
    OptionItem,
    OptionsHelperDataService,
    RepoUrlService,
    Scope,
    TranslationsService,
    UserAvatarComponent,
} from 'ngx-edu-sharing-ui';
import { UIService } from '../../../core-module/rest/services/ui.service';
import { RestConnectorsService } from '../../../core-module/rest/services/rest-connectors.service';
import { OptionsHelperService } from '../../../services/options-helper.service';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { AssignmentEditorConfig } from '../manage-assignment/manage-assignment.component';
import { PlatformLocation } from '@angular/common';
import { FormBuilder, FormGroup } from '@angular/forms';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { NodesSelectorConfig, TabType } from '../nodes-selector/nodes-selector.component';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { Toast, ToastType } from '../../../services/toast';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { toObservable } from '@angular/core/rxjs-interop';
import { EditorialPageService } from '../editorial-page.service';
import { AssignmentConfig } from '../submission-sidebar/submission-sidebar.component';
import { RenderWrapperComponent } from '../../render2-page/render-wrapper-component/render-wrapper.component';
import { NodeHelperService } from '../../../services/node-helper.service';
import { ThemeService } from '../../../services/theme.service';

/**
 * submits an individual assignment (for student)
 */
@Component({
    selector: 'es-submit-assignment',
    templateUrl: 'submit-assignment.component.html',
    styleUrls: ['submit-assignment.component.scss'],
    imports: [
        SharedModule,
        TranslateModule,
        EditorComponent,
        RenderWrapperComponent,
        NgxExtendedPdfViewerModule,
        UserAvatarComponent,
    ],
    providers: [OptionsHelperDataService],
})
export class SubmitAssignmentComponent implements OnDestroy {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private editorialBreadcrumbService = inject(EditorialBreadcrumbService);
    private editorialSidebarService = inject(EditorialSidebarService);
    private editorialPageService = inject(EditorialPageService);
    private nodeHelperService = inject(NodeHelperService);
    private translateService = inject(TranslateService);
    private platformLocation = inject(PlatformLocation);
    private nodeService = inject(NodeService);
    private assignmentService = inject(AssignmentV1Service);
    private dialogs = inject(DialogsService);
    private translationsService = inject(TranslationsService);
    private toast = inject(Toast);
    private repoUrlService = inject(RepoUrlService);
    private optionsHelperService = inject(OptionsHelperService);
    private formBuilder = inject(FormBuilder);
    private uiService = inject(UIService);
    private restConnectorsService = inject(RestConnectorsService);
    private assignmentFileOptionsHelper = inject(OptionsHelperDataService);
    private nodeTitlePipe = inject(NodeTitlePipe);
    private theme = inject(ThemeService);

    @ViewChild('feedback') feedbackRef: ElementRef;
    @ViewChildren(NodeEntriesWrapperComponent) nodeEntriesRef: QueryList<
        NodeEntriesWrapperComponent<Node>
    >;
    readonly editorConfig = computed(() => ({
        ...AssignmentEditorConfig,
        base_url: this.platformLocation.getBaseHrefFromDOM() + 'assets/tinymce',
        language: this.translateService.getDefaultLang(),
        skin: this.theme.isDarkMode() ? 'oxide-dark' : 'oxide',
        content_css: this.theme.isDarkMode() ? 'dark' : 'default',
    }));
    readonly destroyed$ = new Subject<void>();
    columns: ColumnType = {
        Default: [new ListItem('NODE', 'title')],
    };
    uploadOption = new OptionItem(
        'EDITORIAL.SUBMIT_ASSIGNMENT.ADD_ASSIGNMENT_MATERIAL',
        'add',
        () => this.showFileDialog(),
    );
    submitFormGroup: FormGroup;
    submittableConfig: ListOptionsConfig;
    submittableConfigRO: ListOptionsConfig;
    supplementaryConfig: ListOptionsConfig;
    correctionConfig: ListOptionsConfig;
    files = signal<AssignmentFile[]>(null);
    loading = signal(false);
    assignment = signal<Assignment>(null);
    submission = signal<Submission>(null);
    coordinators = computed<Permission[]>(
        () => this.assignment()?.permissions?.filter((p) => p.role === 'COORDINATOR') ?? [],
    );
    isOpenForSubmission = computed(() =>
        ['DRAFT', 'INPROGRESS'].includes(this.assignment().status),
    );
    isBeforeEndDate = computed(() => {
        // @TODO check endTime format vs delivered type
        return (
            !this.assignment().endTime ||
            (Date.parse(this.assignment().endTime) ||
                (this.assignment().endTime as unknown as number)) > new Date().getTime()
        );
    });
    /**
     * files that the student wants to submit
     */
    submissionFiles = signal<SubmissionFile[]>(null);
    submissionAssignmentRefFile = signal<AssignmentFile>(null);
    submissionReplaceFile = signal<SubmissionFile | AssignmentFile>(null);
    canSubmitMaterials = computed(
        () => this.isOpenForSubmission() && this.isBeforeEndDate() && !this.submissionSent(),
    );
    submissionSent = computed(
        () => this.submission() && this.submission()?.submissionStatus === 'FINISHED',
    );
    canEditSubmissionNotes = computed(
        () =>
            this.isOpenForSubmission() &&
            !this.submissionSent() &&
            this.isBeforeEndDate() &&
            !this.loading(),
    );
    canSendSubmission = computed(
        () =>
            this.canEditSubmissionNotes() &&
            this.pollingNodeIds().size === 0 &&
            this.files().every(
                (f) => f.documentRole === 'SUPPLEMENTARY' || this.hasSubmissionFor(f.referNode),
            ),
    );
    selectedTabIndex = signal(0);
    selectedAssignmentFile = signal<Node>(null);
    selectedFileMode = signal<'assignment' | 'submission' | 'supplementary'>('assignment');
    private _actionbarRef: ActionbarComponent | null = null;
    @ViewChild('assignmentFileActionbar')
    set actionbarRef(val: ActionbarComponent) {
        this._actionbarRef = val;
        if (val) {
            void this.syncActionbar();
        }
    }
    selectedCorrectedFile = signal<Node>(null);
    selectedCorrectedFileUrl = signal<string>(undefined);
    submittableFiles = new NodeDataSource<Node>();
    /**
     * all files including user attached
     */
    submittableFilesAll = new NodeDataSource<Node>();
    correctedFiles = new NodeDataSource<Node>();
    supplementaryFiles = new NodeDataSource<Node>();
    language: string = 'de-DE';
    private readonly actionbarOptionData: OptionData = {
        scope: Scope.EditorialPage,
        activeObjects: [] as Node[],
    };
    private connectorPolling = signal<
        Map<string, { subscription: Subscription; win: Window | null; connectorId: string }>
    >(new Map());
    readonly pollingNodeIds: Signal<Set<string>> = computed(
        () => new Set(this.connectorPolling().keys()),
    );

    constructor() {
        this.initOptions();
        effect(() => {
            const node = this.selectedAssignmentFile();
            const mode = this.selectedFileMode();
            if (!node || !this._actionbarRef) {
                return;
            }
            this.actionbarOptionData.activeObjects = [node];
            this.assignmentFileOptionsHelper.setData({
                ...this.actionbarOptionData,
                customOptions: this.configForMode(mode)?.customOptions,
            });
            void this.assignmentFileOptionsHelper.refreshComponents();
        });
        this.language = this.translationsService.getLocale();
        effect(() => {
            const file = this.selectedCorrectedFile();
            this.selectedCorrectedFileUrl.set(undefined);
            if (this.canEditSubmissionNotes()) {
                this.submitFormGroup?.get('userNotes')?.enable();
            } else {
                this.submitFormGroup?.get('userNotes')?.disable();
            }
            if (!file?.downloadUrl) {
                this.selectedCorrectedFileUrl.set(null);
            } else {
                void this.repoUrlService
                    .getRepoUrl(file.downloadUrl, file)
                    .then((url) => this.selectedCorrectedFileUrl.set(url));
            }
        });
        combineLatest([
            toObservable(this.selectedAssignmentFile),
            toObservable(this.selectedCorrectedFileUrl),
        ])
            .pipe(takeUntil(this.destroyed$), distinctUntilChanged())
            .subscribe(() => this.updateBreadcrumbs());
        this.submitFormGroup = this.formBuilder.group({
            userNotes: [''],
        });
        this.submitFormGroup
            .get('userNotes')
            .valueChanges.pipe(
                distinctUntilChanged(),
                debounceTime(2000),
                // only save when the content actually differs from the stored state
                filter(
                    (value) =>
                        this.canEditSubmissionNotes() &&
                        value !== (this.submission()?.userNotes ?? ''),
                ),
                takeUntil(this.destroyed$),
            )
            .subscribe(() => void this.saveUserNotes());

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
                        this.assignmentService
                            .getSubmission({
                                assignmentId,
                                submissionId: ME,
                            })
                            .pipe(
                                catchError((err) => {
                                    if (err.status === RestConstants.HTTP_NOT_FOUND) {
                                        err.preventDefault();
                                        return of(null);
                                    }
                                    return throwError(() => err);
                                }),
                            ),
                        this.assignmentService
                            .getSubmissionFiles({
                                assignmentId,
                                submissionId: ME,
                            })
                            .pipe(
                                catchError((err) => {
                                    if (err.status === RestConstants.HTTP_NOT_FOUND) {
                                        err.preventDefault();
                                        return of(null);
                                    }
                                    return throwError(() => err);
                                }),
                            ),
                    ]),
                ),
            )
            .subscribe(([assignment, files, submission, submissionFiles]) => {
                this.assignment.set(assignment);
                this.files.set(files);
                this.submission.set(submission);
                this.submitFormGroup
                    .get('userNotes')
                    .setValue(submission?.userNotes ?? '', { emitEvent: false });
                this.submissionFiles.set(submissionFiles);
                this.syncSubmissionDataSource();
                this.supplementaryFiles.setData(
                    files.filter((f) => f.documentRole === 'SUPPLEMENTARY').map((n) => n.referNode),
                );
                this.updateBreadcrumbs();
            });
    }

    private updateBreadcrumbs() {
        if (this.selectedCorrectedFileUrl() || this.selectedAssignmentFile()) {
            this.editorialPageService.close.set({
                show: true,
                callback: () => {
                    this.closePreview();
                },
            });
            this.editorialBreadcrumbService.path.set([
                {
                    title: this.assignment()?.title,
                    callback: () => {
                        this.closePreview();
                    },
                },
                {
                    title: this.nodeTitlePipe.transform(
                        this.selectedCorrectedFile() || this.selectedAssignmentFile(),
                    ),
                },
            ]);
        } else {
            this.editorialBreadcrumbService.path.set([{ title: this.assignment()?.title }]);
            this.editorialPageService.close.set({ show: false });
        }
    }

    private closePreview() {
        this.editorialSidebarService.close();
        this.selectedAssignmentFile.set(null);
        this.selectedCorrectedFile.set(null);
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.connectorPolling().forEach(({ subscription }) => subscription.unsubscribe());
    }

    private async createVariantAndEdit(node: Node): Promise<void> {
        // if a submission file already exists for this node (whether looked up from the RO list
        // by assignment file or from the submission list by variant content), edit it rather than
        // forking a new variant
        this.selectedTabIndex.set(1);
        this.selectedAssignmentFile.set(null);
        this.editorialSidebarService.close();
        const existing = this.hasSubmissionFor(node);
        if (existing) {
            this.toast.showProgressSpinner();
            try {
                // fetch original id cause this contains the original element for a submitted file
                const originalId = this.nodeHelperService.getOriginalId(existing.content);
                const variantNode = await firstValueFrom(this.nodeService.getNode(originalId));
                const connectorId =
                    this.restConnectorsService.connectorSupportsEdit(variantNode)?.id;
                const win = await this.uiService.editConnector(variantNode, { preferEdit: true });
                this.startConnectorPolling(existing, variantNode, win, connectorId);
            } catch (e) {
                this.toast.error(e, null);
            } finally {
                this.toast.closeProgressSpinner();
            }
            return;
        }
        this.toast.showProgressSpinner();
        try {
            const variantName = this.translateService.instant('NODE_VARIANT.DEFAULT_NAME', {
                name: node.name,
            });
            const created = await firstValueFrom(
                this.nodeService.forkNode(RestConstants.INBOX, node.ref.id, variantName),
            );
            const variantNode = created.node;

            const assignmentFile =
                this.files()?.find((f) => f.referNode.ref.id === node.ref.id) ?? null;
            const newFiles = await this.saveSubmissionFiles([
                {
                    assignmentFile,
                    content: variantNode,
                    ref: variantNode.ref,
                    validationStatus: 'NOT_STARTED',
                } as SubmissionFile,
            ]);
            this.submissionFiles.set((this.submissionFiles() || []).concat(newFiles));

            // the connector edits the variant (fork); the submission file's content is what the
            // list displays and what polling/overlay track
            const connectorId = this.restConnectorsService.connectorSupportsEdit(variantNode)?.id;
            const win = await this.uiService.editConnector(variantNode, { preferEdit: true });
            // register polling BEFORE rendering the list so the overlay's
            // pollingNodeIds() check is already populated on first render
            this.startConnectorPolling(newFiles[0], variantNode, win, connectorId);
            this.syncSubmissionDataSource();
        } catch (e) {
            this.toast.error(e, null);
        } finally {
            this.toast.closeProgressSpinner();
        }
    }

    /**
     * poll the variant node (the one the connector edits) until its content version
     * changes (write-back), then re-submit the variant so the submission file's content
     * is synced with it, and stop polling
     * @private
     */
    private startConnectorPolling(
        submissionFile: SubmissionFile,
        variantNode: Node,
        win: Window | null,
        connectorId: string,
    ): void {
        const contentId = submissionFile.content.ref.id;
        this.connectorPolling().get(contentId)?.subscription.unsubscribe();
        const initialVersion = variantNode.content?.version || '1.0';
        const sub = interval(5000)
            .pipe(
                takeUntil(this.destroyed$),
                switchMap(() =>
                    this.nodeService.getNode(variantNode.ref.id).pipe(catchError(() => of(null))),
                ),
                filter((updated): updated is Node => !!updated),
                // emit while unchanged; emit the first changed variant too (inclusive), then complete
                takeWhile((updated) => updated.content?.version === initialVersion, true),
            )
            .subscribe({
                next: (updatedVariant) => {
                    if (updatedVariant.content?.version === initialVersion) {
                        return;
                    }
                    // the variant got a new version (connector wrote back) → re-submit it so
                    // the submission file's content is synced with the new variant version
                    void this.resubmitVariant(submissionFile, updatedVariant);
                },
                complete: () => {
                    this.connectorPolling.update((m) => {
                        const next = new Map(m);
                        next.delete(contentId);
                        return next;
                    });
                },
            });
        this.connectorPolling.update(
            (m) => new Map([...m, [contentId, { subscription: sub, win, connectorId }]]),
        );
    }

    /**
     * replace an existing submission file with a fresh one created from the (updated) variant,
     * so the submitted content reflects the latest variant version
     * @private
     */
    private async resubmitVariant(oldFile: SubmissionFile, variantNode: Node): Promise<void> {
        await this.deleteSubmissionFiles(oldFile);
        const [newFile] = await this.saveSubmissionFiles([
            {
                assignmentFile: oldFile.assignmentFile,
                content: variantNode,
                ref: variantNode.ref,
                validationStatus: 'NOT_STARTED',
            } as SubmissionFile,
        ]);
        this.submissionFiles.set(
            (this.submissionFiles() || [])
                .filter((f) => f.ref?.id !== oldFile.ref?.id)
                .concat(newFile),
        );
        this.syncSubmissionDataSource();
    }

    closeConnectorWindow(element: Node) {
        this.connectorPolling().get(element.ref.id)?.win?.close();
    }

    cancelConnectorPolling(element: Node) {
        const entry = this.connectorPolling().get(element.ref.id);
        if (!entry) {
            return;
        }
        entry.subscription.unsubscribe();
        this.connectorPolling.update((m) => {
            const next = new Map(m);
            next.delete(element.ref.id);
            return next;
        });
    }

    isConnectorWindowOpen(element: Node): boolean {
        const win = this.connectorPolling().get(element.ref.id)?.win;
        return !!win && !win.closed;
    }

    pollingConnectorName(element: Node): string {
        return this.connectorPolling().get(element.ref.id)?.connectorId ?? '';
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

    showFileDialog(replaceFile?: SubmissionFile, assignmentFile?: Node) {
        this.submissionReplaceFile.set(replaceFile);
        this.submissionAssignmentRefFile.set(
            assignmentFile
                ? this.files().find((f) => f.referNode.ref.id === assignmentFile.ref.id)
                : null,
        );
        this.editorialSidebarService.showOption({
            option: 'SORT_INTO',
            optionConfig: {
                state: TabType.UPLOAD,
                upload: 'fast',
                autoClose: true,
                allowedConnectorIds: [ConnectorService.ID_ONLY_OFFICE, ConnectorService.ID_TINYMCE],
                applyCallback: (nodes) =>
                    nodes.every(
                        (n) => !this.nodeHelperService.isNodeCollection(n) && !n.isDirectory,
                    ),
                onNodesChoosen: async ({ nodes, connectorId, window: connectorWindow }) => {
                    if (this.submissionReplaceFile()) {
                        return;
                    }
                    let newFiles = nodes.map(
                        (node) =>
                            ({
                                assignmentFile: this.submissionAssignmentRefFile(),
                                content: node,
                                ref: node.ref,
                                validationStatus: 'NOT_STARTED',
                            } as SubmissionFile),
                    );
                    newFiles = await this.saveSubmissionFiles(newFiles);
                    this.submissionFiles.set((this.submissionFiles() || []).concat(newFiles));
                    if (connectorId) {
                        newFiles.forEach((file, idx) => {
                            this.startConnectorPolling(
                                file,
                                nodes[idx],
                                connectorWindow ?? null,
                                connectorId,
                            );
                        });
                    }
                    this.syncSubmissionDataSource();
                },
            } as NodesSelectorConfig,
            trap: true,
        });
    }

    protected readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    protected readonly InteractionType = InteractionType;

    private initOptions() {
        const editConnectorNode = new OptionItem(
            'OPTIONS.EDIT_CONNECTOR',
            'edit',
            (node, nodes) => {
                void this.createVariantAndEdit(node ?? nodes?.[0]);
            },
        );
        editConnectorNode.customShowCallback = async (nodes) => {
            return await this.uiService.hasAvailableConnector(nodes ? nodes[0] : null);
        };
        editConnectorNode.customEnabledCallback = async () => this.canSubmitMaterials();
        editConnectorNode.group = DefaultGroups.View;
        editConnectorNode.priority = 5;
        editConnectorNode.showAlways = true;
        editConnectorNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
        ];
        const download = this.optionsHelperService.getDownloadOption(this.actionbarOptionData);
        download.group = DefaultGroups.View;
        download.priority = 10;
        download.constrains = [];
        download.showAlways = true;

        const remove = new OptionItem(
            'EDITORIAL.OPTIONS.SUBMISSION_REMOVE',
            'close',
            async (node, nodes) => {
                const n = node ?? nodes?.[0];
                const submission = this.hasSubmissionFor(n);
                if (!submission) {
                    return;
                }
                const title = this.nodeTitlePipe.transform(n);
                const result = await firstValueFrom(
                    (
                        await this.dialogs.openGenericDialog({
                            title: 'EDITORIAL.SUBMIT_ASSIGNMENT.REMOVE_FILE_CONFIRM_TITLE',
                            message: 'EDITORIAL.SUBMIT_ASSIGNMENT.REMOVE_FILE_CONFIRM_INFO',
                            messageParameters: { title },
                            buttons: [
                                { label: 'CANCEL', config: { color: 'standard' } },
                                {
                                    label: 'EDITORIAL.OPTIONS.SUBMISSION_REMOVE',
                                    config: { color: 'primary' },
                                },
                            ],
                        })
                    ).afterClosed(),
                );
                if (result !== 'EDITORIAL.OPTIONS.SUBMISSION_REMOVE') {
                    return;
                }
                this.selectedAssignmentFile.set(null);
                await this.deleteSubmissionFiles(submission);
                this.submissionFiles.set(
                    (this.submissionFiles() || []).filter((f) => f.ref?.id !== submission?.ref?.id),
                );
                this.syncSubmissionDataSource();
            },
        );
        remove.group = DefaultGroups.Delete;
        remove.priority = 10;
        remove.showAlways = true;
        remove.customShowCallback = async (nodes) =>
            this.canSubmitMaterials() && !!this.hasSubmissionFor(nodes?.[0]);

        this.submittableConfigRO = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [editConnectorNode, download],
            },
        };

        this.submittableConfig = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [editConnectorNode, download, remove],
            },
        };

        this.supplementaryConfig = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [download],
            },
        };
        this.correctionConfig = {
            customOptions: {
                useDefaultOptions: false,
                addOptions: [download],
            },
        };
    }

    hasSubmissionFor(element: Node) {
        if (!element) {
            return undefined;
        }
        return this.submissionFiles()?.find(
            (n) =>
                n.assignmentFile?.referNode.ref.id === element.ref.id ||
                n.content?.ref.id === element.ref.id ||
                (n.content && this.nodeHelperService.getOriginalId(n.content) === element.ref.id),
        );
    }

    private syncSubmissionDataSource() {
        const originalFiles = this.files()
            .filter((f) => f.documentRole === 'SUBMITTABLE')
            .map((n) => n.referNode);
        this.submittableFiles.setData(originalFiles);
        const nodes = this.files()
            .filter((f) => f.documentRole === 'SUBMITTABLE')
            .map(
                (o) =>
                    // for submission view, prefer showing the "own" file names and content of the submitted files (if present)
                    this.submissionFiles().find((s) => s.assignmentFile?.ref?.id === o.ref.id)
                        ?.content || o.referNode,
            )
            .concat(
                (this.submissionFiles() || [])
                    .filter((f) => !f.assignmentFile)
                    .map((f) => f.content),
            );
        this.submittableFilesAll.setData(nodes);
        this.correctedFiles.setData(
            this.submissionFiles()
                .filter((s) => s.validationStatus !== 'NOT_STARTED' && s.correction?.downloadUrl)
                .map((s) => {
                    return {
                        ...s.correction,
                        name: s.content?.name,
                        title: s.content?.title,
                    };
                }),
        );
        this.initOptions();
    }

    async deleteSubmissionFiles(file: SubmissionFile) {
        this.loading.set(true);
        await firstValueFrom(
            this.assignmentService.deleteSubmissionFile({
                assignmentId: this.assignment().ref.id,
                submissionId: this.submission()?.ref.id || ME,
                submissionFileId: file.ref.id,
            }),
        );
        this.loading.set(false);
    }

    private async saveSubmissionFiles(newFiles: SubmissionFile[]) {
        this.loading.set(true);
        await this.prepareSubmission();
        const files = [];
        for (let file of newFiles) {
            files.push(
                await firstValueFrom(
                    this.assignmentService.createSubmissionFile({
                        assignmentId: this.assignment().ref.id,
                        submissionId: this.submission()?.ref.id || ME,
                        body: {
                            metadata: {
                                originalFile: file.ref.id,
                                assignmentFile: file.assignmentFile?.ref.id,
                                properties: {},
                            },
                        },
                    }),
                ),
            );
        }
        this.loading.set(false);
        return files;
    }

    async submit() {
        const result = await firstValueFrom(
            (
                await this.dialogs.openGenericDialog({
                    title: 'EDITORIAL.SUBMIT_ASSIGNMENT.SUBMIT_CONFIRM_TITLE',
                    message: 'EDITORIAL.SUBMIT_ASSIGNMENT.SUBMIT_CONFIRM_INFO',
                    buttons: [
                        { label: 'CANCEL', config: { color: 'standard' } },
                        {
                            label: 'EDITORIAL.SUBMIT_ASSIGNMENT.SUBMIT',
                            config: { color: 'primary' },
                        },
                    ],
                })
            ).afterClosed(),
        );
        if (result === 'EDITORIAL.SUBMIT_ASSIGNMENT.SUBMIT') {
            this.loading.set(true);
            await this.prepareSubmission();
            await firstValueFrom(
                this.assignmentService.editSubmissionInfo({
                    assignmentId: this.assignment().ref.id,
                    submissionId: this.submission().ref.id,
                    body: {
                        status: 'FINISHED',
                        userNotes: this.submitFormGroup.get('userNotes').value,
                    },
                }),
            );
            this.submission.set({ ...this.submission(), submissionStatus: 'FINISHED' });
            this.nodeEntriesRef?.forEach((n) => {
                // enforce refresh so that customShowCallback can get re-evaluated
                void n.optionsHelper.refreshComponents();
            });
            this.toast.show({
                type: 'info',
                subtype: ToastType.InfoSimple,
                message: 'EDITORIAL.SUBMIT_ASSIGNMENT.SUBMITTED',
            });
            this.loading.set(false);
        }
    }

    async saveUserNotes() {
        this.loading.set(true);
        await this.prepareSubmission();
        const updated = await firstValueFrom(
            this.assignmentService.editSubmissionInfo({
                assignmentId: this.assignment().ref.id,
                submissionId: this.submission().ref.id,
                body: {
                    status: this.submission().submissionStatus || 'PENDING',
                    userNotes: this.submitFormGroup.get('userNotes').value,
                },
            }),
        );
        this.submission.set(updated);
        this.toast.show({
            type: 'info',
            subtype: ToastType.InfoSimple,
            message: 'EDITORIAL.SUBMIT_ASSIGNMENT.NOTES_SAVED',
        });
        this.loading.set(false);
    }

    private async prepareSubmission() {
        if (!this.submission().ref?.id) {
            this.submission.set(
                await firstValueFrom(
                    this.assignmentService.editSubmissionInfo({
                        assignmentId: this.assignment().ref.id,
                        submissionId: ME,
                        body: {
                            status: 'PENDING',
                            userNotes: this.submitFormGroup.get('userNotes').value,
                        },
                    }),
                ),
            );
        }
    }
    scrollToFeedback() {
        this.feedbackRef.nativeElement.scrollIntoView({ behavior: 'smooth' });
    }

    /**
     * Mode for the selected node (node entries component context)
     */
    private configForMode(mode: 'assignment' | 'submission' | 'supplementary') {
        if (mode === 'submission') {
            return this.submittableConfig;
        }
        if (mode === 'supplementary') {
            return this.supplementaryConfig;
        }
        return this.submittableConfigRO;
    }

    private async syncActionbar() {
        const node = this.selectedAssignmentFile();
        const mode = this.selectedFileMode();
        if (!node || !this._actionbarRef) {
            return;
        }
        this.actionbarOptionData.activeObjects = [node];
        this.assignmentFileOptionsHelper.setData({
            ...this.actionbarOptionData,
            customOptions: this.configForMode(mode)?.customOptions,
        });
        await this.assignmentFileOptionsHelper.initComponents(this._actionbarRef);
        void this.assignmentFileOptionsHelper.refreshComponents();
    }

    selectSubmissionFile(
        element: Node,
        mode: 'assignment' | 'submission' | 'supplementary' = 'assignment',
    ) {
        this.selectedAssignmentFile.set(element);
        this.selectedFileMode.set(mode);
        this.showSidebar(mode === 'submission' ? 'submission' : 'assignment');
    }
    selectCorrectionFile(element: Node) {
        this.selectedCorrectedFile.set(element);
        this.showSidebar();
    }

    private showSidebar(mode: 'assignment' | 'submission' = 'assignment') {
        if (!UIService.isMobileWidth()) {
            const selected =
                mode === 'submission'
                    ? this.selectedAssignmentFile()
                    : this.submissionFiles().find(
                          (file) =>
                              file.correction?.ref.id === this.selectedCorrectedFile()?.ref?.id,
                      ) ||
                      this.files().find(
                          (file) =>
                              file.referNode?.ref.id === this.selectedAssignmentFile()?.ref?.id,
                      );
            this.editorialSidebarService.showOption({
                option: 'VIEW_ASSIGNMENT',
                trap: true,
                title: this.assignment().title,
                optionConfig: {
                    submission: {
                        ...this.submission(),
                        assignment: this.assignment(),
                    },
                    selected,
                    assignmentFiles: this.files(),
                    submissionFiles: this.submissionFiles(),
                    submissionFilesAll:
                        mode === 'submission' ? this.submittableFilesAll.getData() : undefined,
                    selectedFileCallback: (selected: Node) => {
                        if (mode === 'submission') {
                            this.selectedAssignmentFile.set(selected);
                        } else {
                            this.selectedAssignmentFile.set(
                                this.files().find((f) => f.referNode?.ref?.id === selected.ref.id)
                                    ?.referNode,
                            );
                            this.selectedCorrectedFile.set(
                                this.submissionFiles().find(
                                    (f) => f.correction?.ref?.id === selected.ref.id,
                                )?.correction,
                            );
                        }
                    },
                    mode,
                } as AssignmentConfig,
            });
        }
    }
}
