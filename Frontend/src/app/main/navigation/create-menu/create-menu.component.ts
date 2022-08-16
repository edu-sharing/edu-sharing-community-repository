import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ConnectorService } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable, Subject } from 'rxjs';
import { delay, take, takeUntil } from 'rxjs/operators';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import {
    ConfigurationService,
    Connector,
    DialogButton,
    Filetype,
    FrameEventsService,
    Node,
    NodeWrapper,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    SessionStorageService,
    TemporaryStorageService,
    UIConstants,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import { CardService } from '../../../core-ui-module/card.service';
import { DateHelper } from '../../../core-ui-module/DateHelper';
import { LinkData, NodeHelperService } from '../../../core-ui-module/node-helper.service';
import {
    Constrain,
    DefaultGroups,
    ElementType,
    OptionItem,
    Scope,
    Target,
} from '../../../core-ui-module/option-item';
import { OptionsHelperService } from '../../../core-ui-module/options-helper.service';
import { Toast } from '../../../core-ui-module/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import {
    DialogRef,
    ManagementDialogsService,
} from '../../../modules/management-dialogs/management-dialogs.service';
import { PasteService } from '../../../services/paste.service';
import { CardComponent } from '../../../shared/components/card/card.component';
import { DropdownComponent } from '../../../shared/components/dropdown/dropdown.component';

@Component({
    selector: 'es-create-menu',
    templateUrl: 'create-menu.component.html',
    styleUrls: ['create-menu.component.scss'],
    animations: [trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST))],
    providers: [OptionsHelperService],
})
export class CreateMenuComponent implements OnInit, OnDestroy {
    @ViewChild('dropdown', { static: true }) dropdown: DropdownComponent;

    /**
     * Currently allowed to drop files?
     */
    @Input() allowed = true;
    /**
     * Allow upload of binary files
     */
    @Input() allowBinary = true;
    @Input() scope: string;
    private fallbackFolder: Node;
    /**
     * Parent location. If null, the folder picker will be shown
     */
    @Input() set parent(parent: Node) {
        this._parent = parent;
        this.showPicker = parent == null || this.nodeHelper.isNodeCollection(parent);
        this.updateOptions();
    }
    /**
     * can a folder be created
     */
    @Input() folder = true;

    /**
     * Fired when elements are created or uploaded
     */
    @Output() onCreate = new EventEmitter<Node[]>();

    _parent: Node = null;
    addFolderName: string = null;

    uploadSelectDialogRef: DialogRef<FileList>;
    uploadDialogRef: DialogRef<Node[]>;
    connectorList: Connector[];
    fileIsOver = false;
    showPicker: boolean;
    createConnectorName: string;
    createConnectorType: Connector;
    cardHasOpenModals$: Observable<boolean>;
    options: OptionItem[];

    private params: Params;
    private destroyed = new Subject<void>();

    constructor(
        public bridge: BridgeService,
        private connector: RestConnectorService,
        private connectors: RestConnectorsService,
        private connectorApi: ConnectorService,
        private iamService: RestIamService,
        private nodeService: RestNodeService,
        private managementService: ManagementDialogsService,
        private toast: Toast,
        private router: Router,
        private translate: TranslateService,
        private configService: ConfigurationService,
        private sessionStorage: SessionStorageService,
        private temporaryStorage: TemporaryStorageService,
        private route: ActivatedRoute,
        private optionsService: OptionsHelperService,
        private iam: RestIamService,
        private nodeHelper: NodeHelperService,
        private event: FrameEventsService,
        private cardService: CardService,
        private dialogs: ManagementDialogsService,
        private paste: PasteService,
    ) {
        this.route.queryParams.subscribe((params) => {
            this.params = params;
            this.updateOptions();
        });
        this.connectorApi
            .observeConnectorList()
            .pipe(takeUntil(this.destroyed))
            .subscribe((list) => {
                this.connectorList = this.connectors.filterConnectors(list?.connectors);
                this.updateOptions();
            });
        this.connector.isLoggedIn(false).subscribe((login) => {
            if (login.statusCode === RestConstants.STATUS_CODE_OK) {
                this.nodeHelper.getDefaultInboxFolder().subscribe((n) => (this.fallbackFolder = n));
            }
        });
        this.cardHasOpenModals$ = cardService.hasOpenModals.pipe(delay(0));
    }

    ngOnInit(): void {
        this.optionsService.virtualNodesAdded
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => this.onCreate.emit(nodes));
        this.paste
            .observeUrlPasteOnPage()
            .pipe(takeUntil(this.destroyed))
            .subscribe((url) => this.onUrlPasteOnPage(url));
        this.paste
            .observeNonTextPageOnPage()
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => this.toast.error(null, 'CLIPBOARD_DATA_UNSUPPORTED'));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private onUrlPasteOnPage(url: string) {
        if (!this.allowed || !this.allowBinary) {
            return;
        }
        if (CardComponent.getNumberOfOpenCards() > 0) {
            return;
        }
        // @TODO: Later we should find a way to prevent the event from propagating
        // this currently fails because getAsString is called async!
        void this.managementService.getDialogsComponent().createUrlLink({
            ...new LinkData(url),
            parent: this.getParent(),
        });
    }

    updateOptions() {
        this.options = [];
        if (this.allowBinary && this.folder) {
            const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
                this.optionsService.pasteNode(),
            );
            pasteNodes.elementType = [ElementType.Unknown];
            pasteNodes.constrains = [
                Constrain.NoSelection,
                Constrain.ClipboardContent,
                Constrain.AddObjects,
                Constrain.User,
            ];
            pasteNodes.toolpermissions = [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            ];
            pasteNodes.keyboardShortcut = {
                keyCode: 'KeyV',
                modifiers: ['Ctrl/Cmd'],
            };
            pasteNodes.group = DefaultGroups.Primary;
            this.options.push(pasteNodes);
        }
        if (this._parent && this.nodeHelper.isNodeCollection(this._parent)) {
            const newCollection = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', (node) =>
                UIHelper.goToCollection(this.router, this._parent, 'new'),
            );
            newCollection.elementType = [ElementType.Unknown];
            newCollection.constrains = [Constrain.NoSelection, Constrain.User];
            newCollection.toolpermissions = [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
            ];
            newCollection.group = DefaultGroups.Create;
            newCollection.priority = 5;
            this.options.push(newCollection);
        }
        if (this.allowBinary) {
            if (this._parent && this.nodeHelper.isNodeCollection(this._parent)) {
                const search = new OptionItem('OPTIONS.SEARCH_OBJECT', 'redo', () =>
                    this.pickMaterialFromSearch(),
                );
                search.elementType = [ElementType.Unknown];
                search.group = DefaultGroups.Create;
                search.priority = 7.5;
                this.options.push(search);
            }
            const upload = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () =>
                this.openUploadSelect(),
            );
            upload.elementType = [ElementType.Unknown];
            upload.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
            upload.group = DefaultGroups.Create;
            upload.priority = 10;
            this.options.push(upload);
            // handle connectors
            if (this.connectorList) {
                this.options = this.options.concat(
                    this.connectorList.map((connector, i) => {
                        const option = new OptionItem(
                            'CONNECTOR.' + connector.id + '.NAME',
                            connector.icon,
                            () => this.showCreateConnector(connector),
                        );
                        option.elementType = [ElementType.Unknown];
                        option.group = DefaultGroups.CreateConnector;
                        option.priority = i;
                        return option;
                    }),
                );
            }
            // handle app
            if (this.bridge.isRunningCordova()) {
                const camera = new OptionItem('WORKSPACE.ADD_CAMERA', 'camera_alt', () =>
                    this.openCamera(),
                );
                camera.elementType = [ElementType.Unknown];
                camera.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
                camera.group = DefaultGroups.Create;
                camera.priority = 20;
                this.options.push(camera);
            }
        }
        if (this.folder) {
            const addFolder = new OptionItem(
                'WORKSPACE.ADD_FOLDER',
                'create_new_folder',
                () => (this.addFolderName = ''),
            );
            addFolder.elementType = [ElementType.Unknown];
            addFolder.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS];
            addFolder.group = DefaultGroups.Create;
            addFolder.priority = 30;
            this.options.push(addFolder);
        }
        this.optionsService.setData({
            scope: Scope.CreateMenu,
            parent: this._parent,
        });
        this.options = this.optionsService.filterOptions(this.options, Target.CreateMenu);

        // If the menu was open, we just removed all its items, leaving focus on <body>.
        setTimeout(() => {
            this.dropdown?.menu.focusFirstItem();
        });
    }

    openUploadSelect(): void {
        this.uploadSelectDialogRef = this.dialogs.openUploadSelect({
            parent: this.getParent(),
            showPicker: this.showPicker,
        });
        this.uploadSelectDialogRef.afterClosed().subscribe((files) => {
            if (files) {
                this.uploadFiles(files);
            } else {
                // When `files` is not set, that can either mean that the dialog was canceled or
                // that a link was entered, which causes an edit dialog to be opened by the
                // management dialogs component. When that edit dialog is confirmed or canceled, the
                // `onUploadFilesProcessed` event is fired. When either of this happens, the link
                // creation is completed.
                //
                // FIXME: This kind of logic should be cleanly separated. Either
                // - the management-dialogs service should process file uploads and edit dialogs and
                //   should, for both links and uploads, notify us only after the edit dialog was
                //   closed by the user, or
                // - we handle the edit dialog here as a response to the user either uploading files
                //   or entering a link.
                rxjs.merge(
                    this.dialogs.getDialogsComponent().onUploadFilesProcessed,
                    this.dialogs.getDialogsComponent().onUploadSelectCanceled,
                )
                    .pipe(take(1))
                    .subscribe((nodes) => {
                        if (nodes) {
                            this.onCreate.emit(nodes);
                        }
                    });
            }
            this.uploadSelectDialogRef = null;
        });
    }

    public hasUsableOptions() {
        return this.options.some((o) => o.isEnabled);
    }

    getParent() {
        return this._parent && !this.nodeHelper.isNodeCollection(this._parent)
            ? this._parent
            : this.fallbackFolder;
    }

    addFolder(folder: any) {
        this.addFolderName = null;
        this.toast.showProgressDialog();
        const properties = RestHelper.createNameProperty(folder.name);
        if (folder.metadataset) {
            properties[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] = [folder.metadataset];
            properties[RestConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET] = ['true'];
        }
        this.nodeService
            .createNode(this.getParent().ref.id, RestConstants.CM_TYPE_FOLDER, [], properties)
            .subscribe(
                (data: NodeWrapper) => {
                    this.toast.closeModalDialog();
                    this.onCreate.emit([data.node]);
                    this.toast.toast('WORKSPACE.TOAST.FOLDER_ADDED');
                },
                (error: any) => {
                    this.toast.closeModalDialog();
                    if (
                        this.nodeHelper.handleNodeError(folder.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        this.addFolderName = folder.name;
                    }
                },
            );
    }

    uploadFiles(files: FileList) {
        this.onFileDrop(files);
    }

    onFileDrop(files: FileList) {
        if (!this.allowed) {
            /*if (this.searchQuery) {
                this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_IN_SEARCH');
            } else {*/
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_GENERAL');
            // }
            return;
        }
        if (this.uploadDialogRef) {
            this.toast.error(null, 'WORKSPACE.TOAST.ONGOING_UPLOAD');
            return;
        }
        if (
            !this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            )
        ) {
            this.toast.toolpermissionError(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES);
            return;
        }
        this.uploadSelectDialogRef?.close();
        this.openUpload(files);
    }

    private openUpload(files: FileList): void {
        this.uploadDialogRef = this.dialogs.openUpload({
            parent: this.getParent(),
            files,
        });
        this.uploadDialogRef.afterClosed().subscribe((nodes) => {
            this.afterUpload(nodes);
            this.uploadDialogRef = null;
        });
    }

    afterUpload(nodes: Node[]) {
        if (nodes == null) {
            return;
        }
        if (this.params.reurl) {
            this.nodeHelper.addNodeToLms(nodes[0], this.params.reurl);
        }
        this.onCreate.emit(nodes);
    }

    async showCreateConnector(connector: Connector) {
        this.createConnectorName = '';
        this.createConnectorType = connector;
        const user = await this.iamService.getUser().toPromise();
        if (
            user.person.quota.enabled &&
            user.person.quota.sizeCurrent >= user.person.quota.sizeQuota
        ) {
            this.toast.showModalDialog(
                'CONNECTOR_QUOTA_REACHED_TITLE',
                'CONNECTOR_QUOTA_REACHED_MESSAGE',
                DialogButton.getOk(() => {
                    this.toast.closeModalDialog();
                }),
                true,
            );
            this.createConnectorName = null;
        }
    }

    private openCamera() {
        this.bridge.getCordova().getPhotoFromCamera(
            (data: any) => {
                const name =
                    this.translate.instant('SHARE_APP.IMAGE') +
                    ' ' +
                    DateHelper.formatDate(this.translate, new Date().getTime(), {
                        showAlwaysTime: true,
                        useRelativeLabels: false,
                    }) +
                    '.jpg';
                const blob: any = Helper.base64toBlob(data, 'image/jpeg');
                blob.name = name;
                const list: any = {};
                list.item = (i: number) => {
                    return blob;
                };
                list.length = 1;
                this.openUpload(list);
            },
            (error: any) => {
                console.warn(error);
                // this.toast.error(error);
            },
        );
    }

    private editConnector(
        node: Node = null,
        type: Filetype = null,
        win: any = null,
        connectorType: Connector = null,
    ) {
        UIHelper.openConnector(
            this.connectors,
            this.iam,
            this.event,
            this.toast,
            node,
            type,
            win,
            connectorType,
        );
    }
    pickMaterialFromSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this._parent.ref.id;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }
    createConnector(event: any) {
        const name = event.name + '.' + event.type.filetype;
        this.createConnectorName = null;
        const prop = this.nodeHelper.propertiesFromConnector(event);
        let win: any;
        if (!this.bridge.isRunningCordova()) {
            win = window.open('');
        }
        this.nodeService
            .createNode(this.getParent().ref.id, RestConstants.CCM_TYPE_IO, [], prop, false)
            .subscribe(
                (data: NodeWrapper) => {
                    this.editConnector(data.node, event.type, win, this.createConnectorType);
                    this.onCreate.emit([data.node]);
                },
                (error: any) => {
                    win.close();
                    if (
                        this.nodeHelper.handleNodeError(event.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        this.createConnectorName = event.name;
                    }
                },
            );
    }

    isAllowed() {
        return (
            this.allowed &&
            !this.uploadDialogRef &&
            this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            )
        );
    }
}
