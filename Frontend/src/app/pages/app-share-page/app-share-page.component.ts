import {
    ColumnType,
    DateHelper,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeTitlePipe,
    NodeEntriesDisplayType,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { ActivatedRoute, Router } from '@angular/router';
import { Toast } from '../../services/toast';
import {
    Connector,
    FrameEventsService,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestUtilitiesService,
    UIService,
} from '../../core-module/core.module';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import {
    Assignment,
    AssignmentFile,
    AssignmentV1Service,
    ConnectorService,
    DEFAULT,
    HOME_REPOSITORY,
    ME,
    Node,
    NodeService,
    PROPERTY_FILTER_ALL,
    SearchService,
    Submission,
} from 'ngx-edu-sharing-api';
import { take } from 'rxjs/operators';
import { firstValueFrom } from 'rxjs';
import { Component, inject } from '@angular/core';
import { Helper } from '../../core-module/rest/helper';
import { CordovaService, OnBackBehaviour } from '../../services/cordova.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { UploadDialogService } from '../../services/upload-dialog.service';
import { NodeHelperService } from '../../services/node-helper.service';

/** A place in the user's submission that can take the shared content. */
type SubmissionTarget =
    | { kind: 'template'; file: AssignmentFile }
    | { kind: 'additional' }
    | { kind: 'notes' };

/** An assignment the shared content can be handed in to, with the user's submission so far. */
interface ShareAssignment {
    assignment: Assignment;
    submission: Submission;
    targets: SubmissionTarget[];
}

@Component({
    selector: 'es-app-share-page',
    templateUrl: 'app-share-page.component.html',
    styleUrls: ['app-share-page.component.scss'],
    animations: [],
    standalone: false,
})
export class AppSharePageComponent {
    private toast = inject(Toast);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private sanitizer = inject(DomSanitizer);
    private node = inject(RestNodeService);
    private nodeService = inject(NodeService);
    private connectorService = inject(ConnectorService);
    private events = inject(FrameEventsService);
    private uiService = inject(UIService);
    private utilities = inject(RestUtilitiesService);
    private translate = inject(TranslateService);
    private translations = inject(TranslationsService);
    private collectionApi = inject(RestCollectionService);
    private cordova = inject(CordovaService);
    private connector = inject(RestConnectorService);
    private mainNav = inject(MainNavService);
    private searchService = inject(SearchService);
    private uploadDialog = inject(UploadDialogService);
    private nodeHelper = inject(NodeHelperService);
    private assignmentService = inject(AssignmentV1Service);
    private nodeTitle = new NodeTitlePipe();

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    globalProgress = true;
    uri: string;
    private type = 'LINK';
    title: string;
    description: string;
    previewUrl: any;
    /** folder the shared content is saved to; the inbox while none is resolved yet */
    parent: Node;
    columns: ColumnType;
    collections = new NodeDataSource<Node>();
    readonly assignmentColumns: ColumnType = { Default: UIHelper.getDefaultAssignmentColumns() };
    /** assignments the user can still hand the shared content in to */
    assignments = new NodeDataSource<Assignment>();
    private assignmentEntries: ShareAssignment[] = [];
    /** assignment whose submission targets are offered for choice */
    chosenAssignment: ShareAssignment = null;
    private cordovaType: string;
    private mimetype: string;
    private editorType: string;
    private file: File;
    private fileName: string;
    private text: string;
    constructor() {
        this.mainNav.setMainNavConfig({ title: 'SHARE_APP.TITLE', currentScope: 'app-share' });
        // when the user finished sharing and navigates back he must return to the origin app
        this.cordova.setOnBackBehaviour(OnBackBehaviour.closeApp);
        this.columns = {
            Default: [
                new ListItem('COLLECTION', 'title'),
                new ListItem('COLLECTION', 'info'),
                new ListItem('COLLECTION', 'scope'),
            ],
        };
        if (this.cordova.isRunningCordova()) {
            this.cordova.subscribeServiceReady().subscribe(() => {
                this.init();
            });
        } else {
            this.init();
        }
    }
    getType() {
        if (this.isLink()) return 'link';
        if (this.isTextSnippet()) return 'file-txt';
        if (this.mimetype === 'application/pdf') {
            return 'file-pdf';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/msword',
            ].includes(this.mimetype)
        ) {
            return 'file-word';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'application/vnd.ms-excel',
            ].includes(this.mimetype)
        ) {
            return 'file-excel';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                'application/vnd.ms-powerpoint',
            ].includes(this.mimetype)
        ) {
            return 'file-powerpoint';
        }
        if (
            [
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/msword',
            ].includes(this.mimetype)
        ) {
            return 'file-word';
        }
        if (this.mimetype) {
            let type = this.mimetype.split('/')[0];
            if (this.translate.instant('MEDIATYPE.file-' + type) === 'MEDIATYPE.file-' + type) {
                type = null;
            }
            if (type != null) {
                return 'file-' + type;
            }
        }
        return 'file';
    }
    saveInternal(callback: Function) {
        this.globalProgress = true;
        this.createSharedNode().then(
            (node) => {
                void this.editAfterSave(node, callback);
            },
            (error) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }
    /** Stores the shared content as a new node in the chosen folder. */
    private async createSharedNode(): Promise<Node> {
        const parent = this.parent?.ref?.id || RestConstants.INBOX;
        if (this.isLink()) {
            const prop: any = {};
            prop[RestConstants.CCM_PROP_IO_WWWURL] = [this.getUri()];
            const data = await firstValueFrom(
                this.node.createNode(
                    parent,
                    RestConstants.CCM_TYPE_IO,
                    [],
                    prop,
                    true,
                    RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                ),
            );
            return data.node;
        }
        const prop: any = RestHelper.createNameProperty(this.title);
        if (this.editorType) {
            prop[RestConstants.CCM_PROP_EDITOR_TYPE] = [this.editorType];
        }
        if (this.text && !this.isTextSnippet()) {
            prop[RestConstants.LOM_PROP_GENERAL_DESCRIPTION] = [this.text];
        }
        const data = await firstValueFrom(
            this.node.createNode(parent, RestConstants.CCM_TYPE_IO, [], prop, true),
        );
        await firstValueFrom(
            this.nodeService.changeContent(
                data.node.ref.repo,
                data.node.ref.id,
                this.mimetype || 'auto',
                RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                { file: this.file },
            ),
        );
        return data.node;
    }
    /** Opens the dialogs configured for new uploads; a cancel removes the node and stays here. */
    private async editAfterSave(node: Node, callback: Function) {
        // the progress overlay sits above dialogs
        this.globalProgress = false;
        const nodes = await this.uploadDialog.editCreatedNodes(
            [node],
            this.isLink() ? 'link' : 'file',
        );
        if (Array.isArray(nodes) && nodes.length > 0) {
            this.globalProgress = true;
            callback(nodes[0]);
        }
    }
    saveFile() {
        this.saveInternal((node: Node) => {
            this.goToFolder(node);
            this.events.broadcastEvent(FrameEventsService.EVENT_SHARED, node);
        });
    }
    saveToCollection(collection: Node) {
        // the server takes a reference only into a collection the user may add children to
        if (!collection.access?.includes(RestConstants.ACCESS_ADD_CHILDREN)) {
            this.toast.error(null, 'COLLECTIONS.NO_ADD_PERMISSIONS');
            return;
        }
        this.saveInternal((node: Node) => {
            this.uiService.addToCollection(collection, [node], false, (added) => {
                this.events.broadcastEvent(FrameEventsService.EVENT_SHARED, node);
                if (added.length > 0) {
                    this.uiService.goToCollection(collection, null, { replaceUrl: true });
                } else {
                    // the upload stays in its folder when the collection does not take it
                    this.goToFolder(node);
                }
            });
        });
    }
    saveToAssignment(entry: ShareAssignment) {
        if (entry.targets.length === 1) {
            void this.submitToAssignment(entry, entry.targets[0]);
        } else {
            this.chosenAssignment = entry;
        }
    }
    /**
     * Hands the shared content in to the user's submission of the assignment.
     * Files are handed in without the upload dialogs, the submission takes a copy of them.
     */
    async submitToAssignment(entry: ShareAssignment, target: SubmissionTarget) {
        this.chosenAssignment = null;
        this.globalProgress = true;
        let node: Node = null;
        try {
            if (target.kind === 'notes') {
                await this.appendToUserNotes(entry);
            } else {
                node = await this.createSharedNode();
                await firstValueFrom(
                    this.assignmentService.createSubmissionFile({
                        assignmentId: entry.assignment.ref.id,
                        submissionId: ME,
                        body: {
                            metadata: {
                                originalFile: node.ref.id,
                                assignmentFile:
                                    target.kind === 'template' ? target.file.ref.id : undefined,
                                properties: {},
                            },
                        },
                    }),
                );
            }
        } catch (error) {
            this.toast.error(error);
            if (node) {
                // the node only exists to be handed in
                this.node.deleteNode(node.ref.id).subscribe({ error: () => {} });
            }
            this.globalProgress = false;
            return;
        }
        this.events.broadcastEvent(FrameEventsService.EVENT_SHARED, node);
        void this.router.navigate([UIConstants.ROUTER_PREFIX, 'editorial', 'assignment'], {
            queryParams: {
                mainComponent: 'submitAssignment',
                assignment: entry.assignment.ref.id,
                tab: 'submission',
            },
            replaceUrl: true,
        });
    }
    /** Appends the shared text or link as a paragraph to the user's notes of the submission. */
    private async appendToUserNotes(entry: ShareAssignment) {
        const content = this.isLink() ? this.getUri() : this.getTextContent();
        const paragraph = document.createElement('p');
        paragraph.innerText = content ?? '';
        const notes = [entry.submission?.userNotes, paragraph.outerHTML].filter((n) => !!n);
        await firstValueFrom(
            this.assignmentService.editSubmissionInfo({
                assignmentId: entry.assignment.ref.id,
                submissionId: ME,
                body: { status: 'PENDING', userNotes: notes.join('') },
            }),
        );
    }
    getTargetLabel(target: SubmissionTarget) {
        switch (target.kind) {
            case 'template':
                return this.translate.instant('SHARE_APP.ASSIGNMENT_TARGET_TEMPLATE', {
                    title: this.nodeTitle.transform(target.file.referNode),
                });
            case 'additional':
                return this.translate.instant('SHARE_APP.ASSIGNMENT_TARGET_ADDITIONAL');
            case 'notes':
                return this.translate.instant('SHARE_APP.ASSIGNMENT_TARGET_NOTES');
        }
    }
    /**
     * Where the shared content can go in the user's submission, mirroring what the submission
     * view offers: templates still open, own files if allowed, and notes for text and links.
     */
    private async getSubmissionTargets(assignment: Assignment): Promise<ShareAssignment> {
        const endTime = Date.parse(assignment.endTime) || (assignment.endTime as unknown as number);
        if (
            assignment.type !== 'SUBMISSION' ||
            assignment.isCoordinator ||
            assignment.status !== 'INPROGRESS' ||
            (endTime && endTime <= Date.now())
        ) {
            return null;
        }
        const id = assignment.ref.id;
        const [submission, files, submissionFiles] = await Promise.all([
            firstValueFrom(
                this.assignmentService.getSubmission({ assignmentId: id, submissionId: ME }),
            ),
            firstValueFrom(this.assignmentService.getAssignmentFiles({ assignmentId: id })),
            firstValueFrom(
                this.assignmentService.getSubmissionFiles({ assignmentId: id, submissionId: ME }),
            ),
        ]);
        if (submission?.submissionStatus === 'FINISHED') {
            return null;
        }
        const targets: SubmissionTarget[] = (files ?? [])
            .filter(
                (file) =>
                    file.documentRole === 'SUBMITTABLE' &&
                    !submissionFiles?.some((s) => s.assignmentFile?.ref?.id === file.ref.id),
            )
            .map((file) => ({ kind: 'template', file }));
        if (assignment.allowAdditionalDocumentSubmissions) {
            targets.push({ kind: 'additional' });
        }
        if (this.isLink() || this.isTextSnippet()) {
            targets.push({ kind: 'notes' });
        }
        return targets.length ? { assignment, submission, targets } : null;
    }
    private loadAssignments() {
        this.assignments.reset();
        this.assignments.isLoading = true;
        this.assignmentService
            .searchAssignments({
                body: {
                    criteria: [{ property: 'virtual:assignmentType', values: ['assigned'] }],
                },
                sortProperties: [RestConstants.CM_PROP_C_CREATED],
                sortAscending: [false],
                maxItems: 50,
            })
            .subscribe(
                async (data) => {
                    try {
                        const entries = await Promise.all(
                            data.nodes.map((a) => this.getSubmissionTargets(a)),
                        );
                        this.assignmentEntries = entries.filter((e) => !!e);
                        this.assignments.setData(
                            this.assignmentEntries.map((e) => e.assignment),
                            data.pagination,
                        );
                    } catch (error) {
                        this.toast.error(error);
                    }
                    this.assignments.isLoading = false;
                },
                (error) => {
                    this.toast.error(error);
                    this.assignments.isLoading = false;
                },
            );
    }
    onAssignmentClick(assignment: Assignment) {
        const entry = this.assignmentEntries.find((e) => e.assignment.ref.id === assignment.ref.id);
        if (entry) {
            this.saveToAssignment(entry);
        }
    }
    private isLink() {
        if (this.cordovaType == 'public.url')
            // ios
            return true;
        if (this.hasData()) return false;
        if (!this.uri) return false;
        if (this.uri.startsWith('content://') || this.uri.startsWith('file://')) return false;
        let pos = this.uri.indexOf('://');
        return pos > 0 && pos < 10;
    }
    private isTextSnippet() {
        if (this.cordovaType == 'public.text')
            // ios
            return true;
        if (this.hasData()) return false;
        if (this.isLink()) return false;
        if (!this.uri) return false;
        return !this.uri.startsWith('content://') && !this.uri.startsWith('file://');
    }
    parentSelected(parent: Node) {
        this.parent = parent;
    }
    /** Opens the chosen folder, or the one the shared node was saved to while none is known. */
    private goToFolder(node: Node) {
        const folder = this.parent?.ref?.id ?? node?.parent?.id;
        UIHelper.goToWorkspaceFolder(this.router, null, folder, {
            replaceUrl: true,
        });
    }
    private init() {
        this.translations.waitForInit().subscribe(() => {
            this.route.queryParams.subscribe((params: any) => {
                this.uri = params['uri'];
                this.mimetype = params['mimetype'];
                if (this.mimetype == 'public.image') {
                    // ios
                    this.mimetype = 'image/jpeg';
                } else if (!this.mimetype.includes('/')) {
                    // ios sends invalid mimetypes from certain apps
                    console.info('invalid mimetype from ios', this.mimetype);
                    this.mimetype = 'application/octet-stream';
                }
                this.cordovaType = params['mimetype'];
                this.fileName = params['file'];
                this.text = params['text']; // ios only: custom description
                this.description = null;
                this.collections.reset();
                this.collections.isLoading = true;
                this.searchService
                    .requestSearch({
                        repository: HOME_REPOSITORY,
                        metadataset: DEFAULT,
                        query: 'collections',
                        contentType: 'COLLECTIONS',
                        propertyFilter: [PROPERTY_FILTER_ALL],
                        sortProperties: [RestConstants.CM_MODIFIED_DATE],
                        sortAscending: [false],
                        skipCount: 0,
                        maxItems: 50,
                        // the index holds roles, Coordinator is not resolved into AddChildren
                        body: {
                            criteria: [],
                            permissions: [
                                RestConstants.PERMISSION_COORDINATOR,
                                RestConstants.PERMISSION_COLLABORATOR,
                            ],
                        },
                    })
                    .subscribe(
                        (data) => {
                            // sharing into a collection needs the right to add to it
                            const collections = (data.nodes as Node[]).filter((collection) =>
                                collection.access?.includes(RestConstants.ACCESS_ADD_CHILDREN),
                            );
                            this.collections.setData(collections, data.pagination);
                            this.collections.isLoading = false;
                        },
                        (error) => {
                            this.toast.error(error);
                            this.collections.isLoading = false;
                        },
                    );
                this.loadAssignments();
                this.nodeHelper.getDefaultInboxFolder().subscribe(
                    (folder) => {
                        // a folder picked while the default was resolving is kept
                        if (!this.parent) {
                            this.parent = folder;
                        }
                    },
                    // saving falls back to the inbox id, which the server resolves itself
                    () => {},
                );
                this.previewUrl = this.connector.getThemeMimePreview(this.getType() + '.svg');
                if (this.isLink()) {
                    this.utilities.getWebsiteInformation(this.getUri()).subscribe(
                        (data: any) => {
                            if (data.title) {
                                this.title = data.title + ' - ' + data.page;
                            } else {
                                this.title = this.getUri();
                            }
                            this.description = data.description;
                            this.globalProgress = false;
                        },
                        (error) => {
                            this.title = this.getUri();
                            this.globalProgress = false;
                        },
                    );
                } else if (this.isTextSnippet()) {
                    this.globalProgress = false;
                    this.connectorService
                        .observeConnectorList()
                        .pipe(take(1))
                        .subscribe(
                            (list) => {
                                this.prepareTextSnippet(list?.connectors);
                            },
                            (error: any) => {
                                this.prepareTextSnippet(null);
                            },
                        );
                } else {
                    if (
                        this.cordova.isRunningCordova() &&
                        this.cordova.getLastIntent() &&
                        this.cordova.getLastIntent().stream
                    ) {
                        let base64 = this.cordova.getLastIntent().stream;
                        if (this.mimetype.startsWith('image/')) {
                            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                                'data:' + this.mimetype + ';base64,' + base64,
                            );
                        }
                        this.file = Helper.base64toBlob(base64, this.mimetype) as any;
                        this.cordova.getFileAsBlob(this.getUri(), this.mimetype).subscribe(
                            (data: any) => {
                                this.globalProgress = false;
                                this.updateTitle();
                            },
                            (error: any) => {
                                console.warn(error);
                                this.globalProgress = false;
                                this.updateTitle();
                            },
                        );
                    } else {
                        void this.router.navigate([
                            UIConstants.ROUTER_PREFIX,
                            'messages',
                            'SHARING_ERROR',
                        ]);
                    }
                    /*
                    this.cordova.getFileAsBlob(this.getUri(),this.mimetype).subscribe((data:any)=>{
                        let split=this.fileName ? this.fileName.split("/") : this.getUri().split("/");
                        this.title=split[split.length-1];
                        this.file=data;
                        if(this.mimetype.startsWith("image/"))
                            this.previewUrl=this.sanitizer.bypassSecurityTrustUrl(data.localURL);
                        let request = new XMLHttpRequest();
                        let result=request.open('GET', data.localURL, true);
                        request.responseType = 'blob';
                        request.onload = ()=> {
                            if(request.response.size<=0){
                                this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                            }
                            this.file=request.response;
                            (this.file as any).name=this.title;
                        };
                        request.onerror=(e)=>{
                            this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                        }
                        request.send();
                    },(error:any)=>{
                        this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                    });*/
                }
            });
        });
    }

    private updateTitle() {
        let split = this.fileName ? this.fileName.split('/') : this.uri.split('/');
        this.title = decodeURIComponent(split[split.length - 1]);
    }

    private hasData() {
        return (
            this.cordova.isRunningCordova() &&
            this.cordova.getLastIntent() &&
            this.cordova.getLastIntent().stream != null
        );
    }

    private prepareTextSnippet(connectors: Connector[]) {
        this.mimetype = 'text/plain';
        let filetype = 'txt';
        if (connectors && connectors.length) {
            let i = Helper.indexOfObjectArray(connectors, 'id', 'TINYMCE');
            if (i != -1) {
                let connector = connectors[i];
                this.mimetype = connector.filetypes[0].mimetype;
                filetype = connector.filetypes[0].filetype;
                this.editorType = connector.filetypes[0].editorType;
            }
        }
        this.title =
            this.translate.instant('SHARE_APP.TEXT_SNIPPET') +
            ' ' +
            DateHelper.getDateForNewFile() +
            '.' +
            filetype;
        let content = this.getTextContent();
        if (this.mimetype == 'text/html') {
            content = content.replace(/\n/g, '<br />');
        }
        this.file = new Blob([content], {
            type: this.mimetype,
        }) as any;
    }

    getTextContent(): string {
        return this.cordova.isAndroid() ? this.uri : this.text;
    }

    private getUri() {
        return this.cordova.isAndroid() ? this.uri : this.cordova.getLastIntent().base64;
    }
}
