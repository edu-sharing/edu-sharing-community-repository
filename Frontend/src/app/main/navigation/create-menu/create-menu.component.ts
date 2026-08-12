import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ConnectorService, Node, Tool } from 'ngx-edu-sharing-api';
import {
    Constrain,
    DateHelper,
    DefaultGroups,
    DropdownComponent,
    ElementType,
    LocalEventsService,
    NodeEntriesGlobalService,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    Target,
    UIAnimation,
    VirtualNode,
} from 'ngx-edu-sharing-ui';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { delay, filter, map, startWith, takeUntil } from 'rxjs/operators';
import {
    Connector,
    Filetype,
    FrameEventsService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    UIConstants,
    UIService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { CardService } from '../../../services/card.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { AddFolderDialogResult } from '../../../features/dialogs/dialog-modules/add-folder-dialog/add-folder-dialog-data';
import {
    AddWithConnectorDialogData,
    AddWithConnectorDialogResult,
} from '../../../features/dialogs/dialog-modules/add-with-connector-dialog/add-with-connector-dialog-data';
import { OK } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { PasteService } from '../../../services/paste.service';
import { UploadDialogService } from '../../../services/upload-dialog.service';
import { CardComponent } from '../../../shared/components/card/card.component';
import { MainNavConfig, MainNavService } from '../main-nav.service';
import { CardDialogService } from '../../../features/dialogs/card-dialog/card-dialog.service';
import { BridgeService } from '../../../services/bridge.service';
import { ConnectorOptionsService } from '../../../services/connector-options.service';
import { LtiToolOptionsService } from '../../../services/lti-tool-options.service';
import { OptionsHelperService } from '../../../services/options-helper.service';
import { EditorialSidebarService } from '../../../features/editorial-sidebar/editorial-sidebar.service';
import {
    NodesSelectorConfig,
    TabType,
} from '../../../pages/editorial-page/nodes-selector/nodes-selector.component';

@Component({
    selector: 'es-create-menu',
    templateUrl: 'create-menu.component.html',
    styleUrls: ['create-menu.component.scss'],
    animations: [trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST))],
    providers: [OptionsHelperDataService, OptionsHelperService],
    standalone: false,
})
export class CreateMenuComponent implements OnInit, OnDestroy {
    public bridge = inject(BridgeService);
    private cardService = inject(CardService);
    private cardDialogService = inject(CardDialogService);
    private connector = inject(RestConnectorService);
    private connectorApi = inject(ConnectorService);
    private mainNavService = inject(MainNavService);
    private dialogs = inject(DialogsService);
    private event = inject(FrameEventsService);
    private uiService = inject(UIService);
    private iam = inject(RestIamService);
    private iamService = inject(RestIamService);
    private nodeHelper = inject(NodeHelperService);
    private localEventsService = inject(LocalEventsService);
    private nodeService = inject(RestNodeService);
    private optionsService = inject(OptionsHelperDataService);
    private optionsHelperService = inject(OptionsHelperService);
    private paste = inject(PasteService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private toast = inject(Toast);
    private translate = inject(TranslateService);
    private uploadDialog = inject(UploadDialogService);
    private editorialSidebarService = inject(EditorialSidebarService);
    private connectorOptionsService = inject(ConnectorOptionsService);
    private ltiToolOptionsService = inject(LtiToolOptionsService);
    private nodeEntriesGlobalService = inject(NodeEntriesGlobalService);

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
    private mainNavConfig: MainNavConfig;

    /**
     * Parent location. If null, the folder picker will be shown
     */
    @Input() set parent(parent: Node) {
        this._parent = parent;
        this.showPicker = parent == null || this.nodeHelper.isNodeCollection(parent);
        void this.updateOptions();
    }

    /**
     * can a folder be created
     */
    @Input() folder = true;

    /**
     * Fired when elements are created or uploaded
     */
    @Output() createElement = new EventEmitter<Node[]>();

    _parent: Node = null;

    connectorList: Connector[];
    connectorOptions: OptionItem[] = [];
    ltiToolOptions: OptionItem[] = [];
    fileIsOver = false;
    cardHasOpenModals$: Observable<boolean>;
    options: OptionItem[];

    showPicker: boolean; // keep public - used by extensions
    private params: Params;
    private destroyed = new BehaviorSubject(false);
    private destroyed$ = this.destroyed.pipe(filter((d) => d === true));

    constructor() {
        this.route.queryParams.subscribe((params) => {
            this.params = params;
            void this.updateOptions();
        });
        this.connectorOptionsService
            .observeConnectors()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((connectors) => {
                this.connectorList = connectors;
                void this.updateOptions();
            });
        this.connectorOptionsService
            .buildOptions((connector) => void this.showCreateConnector({ connector }))
            .pipe(takeUntil(this.destroyed$))
            .subscribe((options) => {
                this.connectorOptions = options;
                void this.updateOptions();
            });
        this.connector.isLoggedIn(false).subscribe((login) => {
            if (login.statusCode === RestConstants.STATUS_CODE_OK) {
            }
        });
        this.cardHasOpenModals$ = combineLatest([
            this.cardService.hasOpenModals,
            this.cardDialogService.openDialogs$.pipe(startWith([])),
        ]).pipe(
            delay(0),
            map(([a, b]) => a || b?.length > 0),
        );
        this.mainNavService
            .observeMainNavConfig()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((config) => {
                this.mainNavConfig = config;
                void this.updateOptions();
            });
        this.ltiToolOptionsService
            .buildOptions((tool) => void this.showCreateLtiTool(tool))
            .pipe(takeUntil(this.destroyed$))
            .subscribe((options) => {
                this.ltiToolOptions = options;
                void this.updateOptions();
            });
    }

    ngOnInit(): void {
        this.optionsService.virtualNodesAdded
            .pipe(takeUntil(this.destroyed$))
            .subscribe((nodes) => this.createElement.emit(nodes));
        this.paste
            .observeUrlPasteOnPage()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((url) => this.onUrlPasteOnPage(url));
        this.paste
            .observeNonTextPageOnPage()
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.toast.error(null, 'CLIPBOARD_DATA_UNSUPPORTED'));
    }

    ngOnDestroy(): void {
        this.destroyed.next(true);
        this.destroyed.complete();
    }

    private async onUrlPasteOnPage(url: string) {
        if (!this.allowed || !this.allowBinary) {
            return;
        }
        if (CardComponent.getNumberOfOpenCards() > 0) {
            return;
        }
        const nodes = await this.uploadDialog.createLinkNode({
            link: url,
            parent: await this.getParent(),
        });
        if (nodes) {
            this.createElement.emit(nodes);
        }
    }

    async updateOptions() {
        this.options = [];
        if (this.allowBinary && this.folder) {
            const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
                this.optionsService.pasteNode(),
            );
            pasteNodes.elementType = [ElementType.NoneOrUnknown];
            pasteNodes.constrains = [
                Constrain.NoSelection,
                Constrain.ClipboardContent,
                Constrain.AddObjects,
                Constrain.User,
            ];
            // collections can only be pasted into another collection
            pasteNodes.customShowCallback = async () =>
                !this.optionsHelperService.clipboardContainsCollections();
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
        if (
            (this._parent && this.nodeHelper.isNodeCollection(this._parent)) ||
            ['collections', 'landing'].includes(this.scope)
        ) {
            const newCollection = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', (node) =>
                // new collection always creates a new collection (selection to create or copy is skipped here)
                this.uiService.goToCollection(this._parent, 'new'),
            );
            newCollection.elementType = [ElementType.NoneOrUnknown];
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
                search.elementType = [ElementType.NoneOrUnknown];
                search.group = DefaultGroups.Create;
                search.priority = 7.5;
                this.options.push(search);
            }
            const upload = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () =>
                this.openUploadSelect(),
            );
            upload.elementType = [ElementType.NoneOrUnknown];
            upload.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
            upload.group = DefaultGroups.Create;
            upload.priority = 10;
            this.options.push(upload);
            // handle connectors
            if (this.connectorOptions?.length) {
                this.options = this.options.concat(this.connectorOptions);
            }
            // handle app
            if (this.bridge.isRunningCordova()) {
                const camera = new OptionItem('WORKSPACE.ADD_CAMERA', 'camera_alt', () =>
                    this.openCamera(),
                );
                camera.elementType = [ElementType.NoneOrUnknown];
                camera.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
                camera.group = DefaultGroups.Create;
                camera.priority = 20;
                this.options.push(camera);
            }

            if (this.ltiToolOptions?.length) {
                this.options = this.options.concat(this.ltiToolOptions);
            }
        }
        if (this.mainNavConfig?.customCreateOptions) {
            this.options = this.optionsHelperService.applyExternalOptions(
                this.options,
                this.mainNavConfig.customCreateOptions,
            );
        }
        if (this.folder) {
            const addFolder = new OptionItem('WORKSPACE.ADD_FOLDER', 'create_new_folder', () =>
                this.openAddFolderDialog(),
            );
            addFolder.elementType = [ElementType.NoneOrUnknown];
            addFolder.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS];
            addFolder.group = DefaultGroups.Create;
            addFolder.priority = 30;
            this.options.push(addFolder);
        }
        this.optionsService.setData({
            scope: Scope.CreateMenu,
            parent: this._parent,
        });
        this.options = await this.optionsService.filterOptions(this.options, Target.CreateMenu);

        // If the menu was open, we just removed all its items, leaving focus on <body>.
        setTimeout(() => {
            if (!this.destroyed.value) {
                this.dropdown?.menu.focusFirstItem();
            }
        });
    }

    private async openAddFolderDialog(name?: string) {
        const dialogRef = await this.dialogs.openAddFolderDialog({
            name,
            parent: await this.getParent(),
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                void this.addFolder(result);
            }
        });
    }

    async openUploadSelect(): Promise<void> {
        if (this.editorialSidebarService.editorialSidebar) {
            this.nodeEntriesGlobalService.getPrimaryInstance()?.selection.clear();
            this.editorialSidebarService.showOption({
                option: 'SORT_INTO',
                trap: false,
                optionConfig: {
                    state: TabType.UPLOAD,
                    allowCreate: true,
                    autoClose: false,
                    upload: 'default',
                } as NodesSelectorConfig,
            });
            return;
        }
        const nodes = await this.uploadDialog.openUploadDialog({
            parent: await this.getParent(),
            chooseParent: this.showPicker,
        });
        if (nodes && Array.isArray(nodes)) {
            this.localEventsService.nodesCreated.emit(nodes);
            this.createElement.emit(nodes);
        }
    }

    public hasUsableOptions() {
        return this.options.some((o) => o.isEnabled);
    }

    private async getParent() {
        return this._parent && !this.nodeHelper.isNodeCollection(this._parent)
            ? this._parent
            : this.nodeHelper.getDefaultInboxFolder().toPromise();
    }

    async addFolder(folder: AddFolderDialogResult) {
        this.toast.showProgressSpinner();
        const properties = RestHelper.createNameProperty(folder.name);
        if (folder.metadataSet) {
            properties[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] = [folder.metadataSet];
            properties[RestConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET] = ['true'];
        }
        this.nodeService
            .createNode(
                (await this.getParent()).ref.id,
                RestConstants.CM_TYPE_FOLDER,
                [],
                properties,
            )
            .subscribe(
                (data) => {
                    this.toast.closeProgressSpinner();
                    this.createElement.emit([data.node]);
                    this.toast.toast('WORKSPACE.TOAST.FOLDER_ADDED');
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    if (
                        this.nodeHelper.handleNodeError(folder.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        void this.openAddFolderDialog(folder.name);
                    }
                },
            );
    }

    onFileDrop(fileList: FileList) {
        if (!this.isDropAllowed()) {
            return;
        }
        if (!this.allowed) {
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_GENERAL');
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
        const files: File[] = [];
        for (let file of fileList) {
            files.push(file);
        }
        void this.openUpload(files);
    }

    private async openUpload(files: File[]): Promise<void> {
        const nodes = await this.uploadDialog.uploadFilesAndCreateNodes({
            parent: await this.getParent(),
            files,
        });
        if (nodes) {
            if (this.params.reurl) {
                this.nodeHelper.addNodeToLms(nodes[0], this.params.reurl);
            }
            this.createElement.emit(nodes);
        }
    }

    async showCreateConnector(details: AddWithConnectorDialogData) {
        const user = await this.iamService.getCurrentUserAsync();
        if (
            user.person.quota.enabled &&
            user.person.quota.sizeCurrent >= user.person.quota.sizeQuota
        ) {
            await this.dialogs.openGenericDialog({
                title: 'CONNECTOR_QUOTA_REACHED_TITLE',
                message: 'CONNECTOR_QUOTA_REACHED_MESSAGE',
                buttons: OK,
            });
        } else {
            const dialogRef = await this.dialogs.openAddWithConnectorDialog(details);
            dialogRef.afterClosed().subscribe((result) => {
                if (result) {
                    void this.createConnector(details.connector, result);
                }
            });
        }
    }

    async showCreateLtiTool(tool: Tool) {
        const parent = await this.getParent();
        const dialogRef = await this.dialogs.openCreateLtiToolDialog({ tool, parent });
        dialogRef.afterClosed().subscribe(async (result) => {
            if (!result) {
                return;
            }
            const nodes = await this.ltiToolOptionsService.createFromDialogResult(
                tool,
                result,
                () => parent,
            );
            nodes.forEach((node) => this.createElement.emit([node]));
        });
    }

    private openCamera() {
        this.bridge.getCordova().getPhotoFromCamera(
            async (data: string) => {
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
                const fakeFileList = [blob];
                await this.openUpload(fakeFileList);
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
        win: Window = null,
        parameters: { [key in string]: string[] } = null,
        connectorType: Connector = null,
    ) {
        const preferEdit = parameters?.['preferEdit']?.[0] === 'true';
        void this.uiService.editConnector(node, {
            type,
            win,
            connectorType,
            preferEdit,
            data: parameters,
        });
    }

    pickMaterialFromSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this._parent.ref.id;
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    private async createConnector(connector: Connector, event: AddWithConnectorDialogResult) {
        const prop = this.nodeHelper.propertiesFromConnector(event);
        this.nodeService
            .createNode((await this.getParent()).ref.id, RestConstants.CCM_TYPE_IO, [], prop, false)
            .subscribe(
                (data) => {
                    this.editConnector(
                        data.node,
                        event.type as Filetype,
                        event.window,
                        event.data,
                        connector,
                    );
                    const node: VirtualNode = { ...data.node, virtual: true };
                    node.observe = true;
                    this.createElement.emit([node]);
                },
                (error: any) => {
                    event.window?.close();
                    if (
                        this.nodeHelper.handleNodeError(event.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        void this.showCreateConnector({
                            connector,
                            name: event.name,
                            data: event.data,
                        });
                    }
                },
            );
    }

    dropEnabled() {
        return this.mainNavConfig.create?.globalDrop !== false;
    }
    isDropAllowed() {
        return (
            this.allowed &&
            this.dropEnabled() &&
            this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            )
        );
    }
}
