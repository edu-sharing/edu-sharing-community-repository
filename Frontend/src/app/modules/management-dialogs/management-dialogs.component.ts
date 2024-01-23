import { trigger } from '@angular/animations';
import {
    Component,
    ContentChild,
    EventEmitter,
    HostListener,
    Input,
    Output,
    TemplateRef,
} from '@angular/core';
import { Router } from '@angular/router';
import { Observable, forkJoin as observableForkJoin } from 'rxjs';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import {
    CollectionProposalStatus,
    CollectionReference,
    ConfigurationService,
    DialogButton,
    LocalPermissions,
    Node,
    NodeVersions,
    NodeWrapper,
    ProposalNode,
    RestCollectionService,
    RestConstants,
    RestHelper,
    RestNodeService,
    TemporaryStorageService,
    Version,
} from '../../core-module/core.module';
import { UIAnimation } from '../../core-module/ui/ui-animation';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { ErrorProcessingService } from '../../core-ui-module/error.processing';
import { LinkData, NodeHelperService } from '../../core-ui-module/node-helper.service';
import { Toast } from '../../core-ui-module/toast';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { BulkBehavior } from '../../features/mds/types/types';
import { LocalEventsService } from '../../services/local-events.service';
import { SimpleEditCloseEvent } from './simple-edit-dialog/simple-edit-dialog.component';

export enum DialogType {
    SimpleEdit = 'SimpleEdit',
    Mds = 'Mds',
}
export enum ManagementEventType {
    AddCollectionNodes,
}
export interface ManagementEvent {
    event: ManagementEventType;
    data?: any;
}
@Component({
    selector: 'es-workspace-management',
    templateUrl: 'management-dialogs.component.html',
    styleUrls: ['management-dialogs.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight', UIAnimation.fromRight()),
    ],
})
export class WorkspaceManagementDialogsComponent {
    @ContentChild('collectionChooserBeforeRecent')
    collectionChooserBeforeRecentRef: TemplateRef<any>;
    @Input() uploadShowPicker = false;
    @Input() uploadMultiple = true;
    @Input() fileIsOver = false;
    @Input() addToCollection: Node[];
    @Output() addToCollectionChange = new EventEmitter();
    @Input() filesToUpload: FileList;
    @Output() filesToUploadChange = new EventEmitter();
    @Input() parent: Node;
    @Input() addPinnedCollection: Node;
    @Output() addPinnedCollectionChange = new EventEmitter();
    @Output() onEvent = new EventEmitter<ManagementEvent>();
    @Input() set nodeImportUnblock(nodeImportUnblock: Node[]) {
        this.toast.showConfigurableDialog({
            title: 'WORKSPACE.UNBLOCK_TITLE',
            message: 'WORKSPACE.UNBLOCK_MESSAGE',
            buttons: DialogButton.getOkCancel(
                () => this.toast.closeModalDialog(),
                () => this.unblockImportedNodes(nodeImportUnblock),
            ),
            isCancelable: true,
        });
    }
    @Input() nodeWorkflow: Node[];
    @Output() nodeWorkflowChange = new EventEmitter();
    @Input() signupGroup: boolean;
    @Output() signupGroupChange = new EventEmitter<boolean>();
    @Input() addNodesStream: Node[];
    @Output() addNodesStreamChange = new EventEmitter();
    @Input() nodeVariant: Node;
    @Output() nodeVariantChange = new EventEmitter();
    @Input() set nodeSimpleEdit(nodeSimpleEdit: Node[]) {
        this._nodeSimpleEdit = nodeSimpleEdit;
        this._nodeFromUpload = false;
    }
    @Input() nodeSimpleEditChange = new EventEmitter<Node[]>();
    @Input() materialViewFeedback: Node;
    @Output() materialViewFeedbackChange = new EventEmitter<Node>();
    @Input() nodeSidebar: Node;
    @Output() nodeSidebarChange = new EventEmitter<Node>();
    @Input() showUploadSelect = false;
    @Output() showUploadSelectChange = new EventEmitter();
    @Output() onUploadSelectCanceled = new EventEmitter();
    @Output() onClose = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    @Output() onRefresh = new EventEmitter<Node[] | void>();
    /**
     * Emits once when the user created new nodes (either by upload or links) after the user closed
     * the final editing dialog.
     *
     * - Does not emit when jumping between dialogs (the final dialog is also the first dialog that
     *   appears after the user creates new nodes).
     * - Emits the new nodes when the user confirms the final dialog.
     * - Emits `null` when the user cancels the final dialog.
     */
    @Output() onUploadFilesProcessed = new EventEmitter<Node[]>();
    @Output() onCloseMetadata = new EventEmitter();
    @Output() onUploadFileSelected = new EventEmitter<FileList>();
    @Output() onCloseAddToCollection = new EventEmitter();
    @Output() onStoredAddToCollection = new EventEmitter<{
        collection: Node;
        references: CollectionReference[];
    }>();
    _nodeSimpleEdit: Node[];
    _nodeFromUpload = false;
    public editorPending = false;
    reopenSimpleEdit = false;
    private nodeLicenseOnUpload = false;
    /**
     * QR Code object data to print
     * @node: Reference to the node (for header title)
     * @data: The string to display inside the qr code (e.g. an url)
     */
    //   @Input() qr: {node: Node, data: string};

    /**
     * @Deprecated, the components should use toast service directly
     */
    set globalProgress(globalProgress: boolean) {
        if (globalProgress) {
            this.toast.showProgressDialog();
        } else {
            this.toast.closeModalDialog();
        }
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            if (this.nodeSidebar != null) {
                this.closeSidebar();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
            if (this.addToCollection != null) {
                this.cancelAddToCollection();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
        }
    }
    public constructor(
        private nodeService: RestNodeService,
        private temporaryStorage: TemporaryStorageService,
        private collectionService: RestCollectionService,
        private config: ConfigurationService,
        private toast: Toast,
        private errorProcessing: ErrorProcessingService,
        private nodeHelper: NodeHelperService,
        private bridge: BridgeService,
        private router: Router,
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
    ) {}
    async openShareDialog(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openShareDialog({
            nodes,
            sendMessages: true,
        });
        dialogRef.afterClosed().subscribe((result) => {
            this.closeShare(nodes);
        });
    }
    closeShare(originalNodes: Node[]) {
        // reload node metadata
        this.toast.showProgressDialog();
        observableForkJoin(
            originalNodes.map((n) =>
                this.nodeService.getNodeMetadata(n.ref.id, [RestConstants.ALL]),
            ),
        ).subscribe(
            (nodes: NodeWrapper[]) => {
                this.localEvents.nodesChanged.next(nodes.map((n) => n.node));
                const previousNodes = originalNodes;
                if (this.reopenSimpleEdit) {
                    this.reopenSimpleEdit = false;
                    this._nodeSimpleEdit = previousNodes;
                }
                this.toast.closeModalDialog();
            },
            (error) => {
                this.toast.closeModalDialog();
            },
        );
    }
    public closeWorkflow(nodes: Node[] = null) {
        this.nodeWorkflow = null;
        this.nodeWorkflowChange.emit(null);
        if (nodes) {
            this.localEvents.nodesChanged.emit(nodes);
        }
    }

    public uploadDone(event: Node[]) {
        if (event == null) {
            // error occured
            this.onUploadFilesProcessed.emit(null);
        } else if (this.config.instant('licenseDialogOnUpload', false)) {
            void this.openLicenseDialog(event);
            this.nodeLicenseOnUpload = true;
        } else {
            this.showMetadataAfterUpload(event);
        }
        this.filesToUpload = null;
        this.filesToUploadChange.emit(null);
    }

    public uploadFile(event: FileList) {
        this.onUploadFileSelected.emit(event);
    }
    createUrlLink(link: LinkData) {
        const urlData = this.nodeHelper.createUrlLink(link);
        this.closeUploadSelect();
        this.toast.showProgressDialog();
        this.nodeService
            .createNode(
                link.parent?.ref.id,
                RestConstants.CCM_TYPE_IO,
                urlData.aspects,
                urlData.properties,
                true,
                RestConstants.COMMENT_MAIN_FILE_UPLOAD,
            )
            .subscribe((data) => {
                this.showMetadataAfterUpload([data.node]);
                this.toast.closeModalDialog();
            });
    }
    public closeUploadSelect() {
        this.showUploadSelect = false;
        this.showUploadSelectChange.emit(false);
    }
    public cancelUploadSelect() {
        this.closeUploadSelect();
        this.onUploadSelectCanceled.emit(false);
    }
    async openLicenseDialog(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openLicenseDialog({ kind: 'nodes', nodes });
        dialogRef.afterClosed().subscribe((updatedNodes) => {
            if (this.nodeLicenseOnUpload) {
                this.showMetadataAfterUpload(nodes);
            } else if (this.reopenSimpleEdit) {
                this.reopenSimpleEdit = false;
                this._nodeSimpleEdit = nodes;
            } else if (this._nodeFromUpload) {
                this.onUploadFilesProcessed.emit(nodes);
            }
            this.nodeLicenseOnUpload = false;
            if (updatedNodes) {
                this.localEvents.nodesChanged.emit(updatedNodes);
            }
        });
    }
    deleteNodes(nodes: Node[]) {
        this.toast.showProgressDialog();
        observableForkJoin(
            nodes.map((n) => this.nodeService.deleteNode(n.ref.id, false)),
        ).subscribe(
            () => {
                this.toast.closeModalDialog();
            },
            (error) => {
                this.toast.error(error);
                this.toast.closeModalDialog();
            },
        );
    }

    async openMdsEditor(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes,
            bulkBehavior: this._nodeFromUpload ? BulkBehavior.Replace : BulkBehavior.Default,
        });
        dialogRef
            .afterClosed()
            .subscribe((updatedNodes) => this.closeMdsEditor(nodes, updatedNodes));
    }

    private closeMdsEditor(originalNodes: Node[], updatedNodes: Node[] = null) {
        let refresh = !!updatedNodes;
        if (this._nodeFromUpload && !this.reopenSimpleEdit && updatedNodes == null) {
            this.deleteNodes(originalNodes);
            this.localEvents.nodesDeleted.emit(originalNodes);
            refresh = true;
        }
        this.onCloseMetadata.emit(updatedNodes);
        if (this.reopenSimpleEdit) {
            this.reopenSimpleEdit = false;
            this._nodeSimpleEdit = originalNodes;
        } else if (this._nodeFromUpload) {
            this.onUploadFilesProcessed.emit(updatedNodes);
        } else if (
            this.nodeSidebar &&
            this.nodeSidebar.ref.id === originalNodes[0]?.ref.id &&
            updatedNodes
        ) {
            this.nodeSidebar = updatedNodes[0];
        }
        if (refresh) {
            this.localEvents.nodesChanged.emit(updatedNodes);
        }
    }

    public closeStream() {
        this.addNodesStream = null;
        this.addNodesStreamChange.emit(null);
    }
    public closeVariant() {
        this.nodeVariant = null;
        this.nodeVariantChange.emit(null);
    }
    cancelAddToCollection() {
        this.addToCollection = null;
        this.addToCollectionChange.emit(null);
        this.onCloseAddToCollection.emit();
    }
    public addToCollectionCreate(parent: Node = null) {
        this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES, {
            nodes: this.addToCollection,
            callback: this.onStoredAddToCollection,
        });
        this.router.navigate([
            UIConstants.ROUTER_PREFIX,
            'collections',
            'collection',
            'new',
            parent ? parent.ref.id : RestConstants.ROOT,
        ]);
        this.addToCollection = null;
        this.addToCollectionChange.emit(null);
    }
    public addToCollectionList(
        collection: Node,
        list: Node[] = this.addToCollection,
        close = true,
        callback: () => void = null,
        asProposal = false,
        force = false,
    ) {
        if (!force) {
            if (collection.access.indexOf(RestConstants.ACCESS_WRITE) === -1) {
                this.toast.showConfigurableDialog({
                    title: 'DIALOG.COLLECTION_PROPSE',
                    message: 'DIALOG.COLLECTION_PROPSE_INFO',
                    messageParameters: { collection: RestHelper.getTitle(collection) },
                    buttons: DialogButton.getNextCancel(
                        () => this.toast.closeModalDialog(),
                        () => {
                            this.toast.closeModalDialog();
                            this.addToCollectionList(collection, list, close, callback, true, true);
                        },
                    ),
                });
                return;
            } else if (collection.collection.scope !== RestConstants.COLLECTIONSCOPE_MY) {
                this.toast.showConfigurableDialog({
                    title: 'DIALOG.COLLECTION_SHARE_PUBLIC',
                    message: 'DIALOG.COLLECTION_SHARE_PUBLIC_INFO',
                    messageParameters: { collection: RestHelper.getTitle(collection) },
                    buttons: DialogButton.getNextCancel(
                        () => this.toast.closeModalDialog(),
                        () => {
                            this.toast.closeModalDialog();
                            this.addToCollectionList(
                                collection,
                                list,
                                close,
                                callback,
                                asProposal,
                                true,
                            );
                        },
                    ),
                });
                return;
            }
        }
        if (close) this.cancelAddToCollection();
        else {
            this.toast.closeModalDialog();
        }
        this.toast.showProgressDialog();
        UIHelper.addToCollection(
            this.nodeHelper,
            this.collectionService,
            this.router,
            this.bridge,
            collection,
            list,
            asProposal,
            (nodes) => {
                this.toast.closeModalDialog();
                this.onStoredAddToCollection.emit({ collection, references: nodes });
                if (callback) {
                    callback();
                }
            },
        );
    }

    private showMetadataAfterUpload(event: Node[]) {
        this._nodeFromUpload = true;
        const dialog = this.config.instant('upload.postDialog', DialogType.SimpleEdit);
        if (dialog === DialogType.SimpleEdit) {
            this._nodeSimpleEdit = event;
            this.nodeSimpleEditChange.emit(event);
        } else if (dialog === DialogType.Mds) {
            void this.openMdsEditor(event);
        } else {
            console.error('Invalid configuration for upload.postDialog: ' + dialog);
        }
    }

    closePinnedCollection() {
        this.addPinnedCollection = null;
        this.addPinnedCollectionChange.emit(null);
    }

    restoreVersion(restore: { version: Version; node: Node }) {
        this.toast.showConfigurableDialog({
            title: 'WORKSPACE.METADATA.RESTORE_TITLE',
            message: 'WORKSPACE.METADATA.RESTORE_MESSAGE',
            buttons: DialogButton.getYesNo(
                () => this.toast.closeModalDialog(),
                () => this.doRestoreVersion(restore.version),
            ),
            node: restore.node,
            isCancelable: true,
            onCancel: () => this.toast.closeModalDialog(),
        });
    }
    private doRestoreVersion(version: Version): void {
        this.toast.showProgressDialog();
        this.nodeService
            .revertNodeToVersion(
                version.version.node.id,
                version.version.major,
                version.version.minor,
            )
            .subscribe(
                (data: NodeVersions) => {
                    this.toast.closeModalDialog();
                    this.closeSidebar();
                    // @TODO type is not compatible
                    this.nodeService
                        .getNodeMetadata(version.version.node.id, [RestConstants.ALL])
                        .subscribe(
                            (node) => {
                                this.localEvents.nodesChanged.emit([node.node]);
                                this.nodeSidebar = node.node;
                                this.nodeSidebarChange.emit(node.node);
                                this.toast.toast('WORKSPACE.REVERTED_VERSION');
                            },
                            (error: any) => this.toast.error(error),
                        );
                },
                (error: any) => this.toast.error(error),
            );
    }

    closeMaterialViewFeedback() {
        this.materialViewFeedback = null;
        this.materialViewFeedbackChange.emit(null);
    }

    closeSidebar() {
        this.nodeSidebar = null;
        this.nodeSidebarChange.emit(null);
    }

    displayNode(node: Node) {
        if (node.version) {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id, node.version]);
        } else {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
        }
    }

    closeSimpleEdit(event: SimpleEditCloseEvent) {
        if (this._nodeFromUpload) {
            if (event.reason === 'done') {
                this.onUploadFilesProcessed.emit(event.nodes);
            } else if (event.reason === 'abort') {
                this.deleteNodes(this._nodeSimpleEdit);
                this.onUploadFilesProcessed.emit(null);
            }
        }
        if (event.nodes) {
            this.localEvents.nodesChanged.emit(event.nodes);
        }
        this._nodeSimpleEdit = null;
        this.nodeSimpleEditChange.emit(null);
    }

    private unblockImportedNodes(nodes: Node[]) {
        this.toast.showProgressDialog();
        observableForkJoin(
            nodes.map((n) => {
                const properties: any = {};
                properties[RestConstants.CCM_PROP_IMPORT_BLOCKED] = [null];
                return new Observable((observer) => {
                    this.nodeService
                        .editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_BLOCKED_IMPORT,
                            properties,
                        )
                        .subscribe(({ node }) => {
                            const permissions = new LocalPermissions();
                            permissions.inherited = true;
                            permissions.permissions = [];
                            this.nodeService
                                .setNodePermissions(node.ref.id, permissions)
                                .subscribe(() => {
                                    observer.next(node);
                                    observer.complete();
                                });
                        });
                });
            }),
        ).subscribe((results: Node[]) => {
            this.toast.closeModalDialog();
            this.localEvents.nodesChanged.emit(results);
        });
    }

    declineProposals(nodes: ProposalNode[]) {
        this.errorProcessing
            .handleRestRequest(
                observableForkJoin(
                    nodes.map((n) =>
                        this.nodeService.editNodeProperty(
                            n.proposal?.ref.id ||
                                (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL
                                    ? n.ref.id
                                    : null),
                            RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                            ['DECLINED' as CollectionProposalStatus],
                        ),
                    ),
                ),
            )
            .then(() => {
                this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.DECLINED');
                this.localEvents.nodesDeleted.emit(nodes);
            });
    }

    addProposalsToCollection(nodes: ProposalNode[]) {
        this.errorProcessing
            .handleRestRequest(
                observableForkJoin(
                    nodes.map((n) =>
                        this.nodeService.editNodeProperty(
                            n.proposal?.ref.id ||
                                (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL
                                    ? n.ref.id
                                    : null),
                            RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                            ['ACCEPTED' as CollectionProposalStatus],
                        ),
                    ),
                ),
            )
            .then(() => {
                this.errorProcessing
                    .handleRestRequest(
                        observableForkJoin(
                            nodes.map((n) =>
                                this.collectionService.addNodeToCollection(
                                    n.proposalCollection.ref.id,
                                    n.ref.id,
                                    n.ref.repo,
                                ),
                            ),
                        ),
                    )
                    .then((results) => {
                        this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.ACCEPTED');
                        this.localEvents.nodesDeleted.emit(nodes);
                        this.onEvent.emit({
                            event: ManagementEventType.AddCollectionNodes,
                            data: {
                                collection: nodes[0].proposalCollection,
                                references: results.map((r) => r.node),
                            },
                        });
                    });
            });
    }
}
