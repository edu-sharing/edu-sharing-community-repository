import {
    effect,
    EventEmitter,
    inject,
    Injectable,
    Injector,
    OnDestroy,
    runInInjectionContext,
    WritableSignal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    AssignmentFile,
    AssignmentV1Service,
    AuthenticationService,
    NetworkService,
    Node,
    NodeListErrorResponses,
    NodeListService,
    NodeService,
} from 'ngx-edu-sharing-api';
import {
    ClipboardObject,
    Constrain,
    CustomOptions,
    DefaultGroups,
    ElementType,
    LocalEventsService,
    NodeEntriesDisplayType,
    NodeEntriesGlobalService,
    NodeHelperService as NodeHelperServiceUi,
    OptionData,
    OptionItem,
    OptionItemToggle,
    OptionsHelperComponents,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Target,
    TemporaryStorageService,
} from 'ngx-edu-sharing-ui';
import {
    forkJoin as observableForkJoin,
    lastValueFrom,
    Observable,
    of,
    Subject,
    Subscription,
} from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    RestCollectionService,
    RestConnectorService,
    RestHelper,
} from '../core-module/core.module';
import { LocalPermissions, NodeWrapper } from '../core-module/rest/data-object';
import { Helper } from '../core-module/rest/helper';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestConnectorsService } from '../core-module/rest/services/rest-connectors.service';
import { RestNetworkService } from '../core-module/rest/services/rest-network.service';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { UIService } from '../core-module/rest/services/ui.service';
import { DELETE_OR_CANCEL } from '../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { WorkspaceManagementDialogsComponent } from '../features/management-dialogs/management-dialogs.component';
import { MainNavService } from '../main/navigation/main-nav.service';
import { BridgeService } from './bridge.service';
import { MessageType } from '../util/message-type';
import { forkJoinWithErrors } from '../util/rxjs/forkJoinWithErrors';
import { ConfigOptionItem, NodeHelperService } from './node-helper.service';
import { Toast } from './toast';
import { UIHelper } from '../core-ui-module/ui-helper';
import { GlobalOptionsService } from './global-options.service';
import { EditorialSidebarService } from '../features/editorial-sidebar/editorial-sidebar.service';
import { OptionsContext } from './options/options-context';
import { createPrimaryOptions } from './options/primary-options';
import { createViewOptions } from './options/view-options';
import { createReuseOptions } from './options/reuse-options';
import { createEditOptions } from './options/edit-options';
import { createFileOperationsOptions } from './options/file-operations-options';
import { createDeleteOptions } from './options/delete-options';
import { createToggleOptions } from './options/toggle-options';

@Injectable()
export class OptionsHelperService extends OptionsHelperServiceAbstract implements OnDestroy {
    nodeHelperService = inject(NodeHelperServiceUi);
    authenticationService = inject(AuthenticationService);
    storage = inject(TemporaryStorageService);
    networkService = inject(NetworkService);
    route = inject(ActivatedRoute);
    public nodeHelper = inject(NodeHelperService);
    private bridge = inject(BridgeService);
    private collectionService = inject(RestCollectionService);
    public configService = inject(ConfigurationService);
    private globalOptionsService = inject(GlobalOptionsService);
    public nodeEntriesGlobalService = inject(NodeEntriesGlobalService);
    public connector = inject(RestConnectorService);
    public connectors = inject(RestConnectorsService);
    public dialogs = inject(DialogsService);
    public localEvents = inject(LocalEventsService);
    private mainNavService = inject(MainNavService);
    public editorialSidebarService = inject(EditorialSidebarService);
    private injector = inject(Injector);
    private nodeList = inject(NodeListService);
    public nodeService = inject(NodeService);
    public nodeServiceLegacy = inject(RestNodeService);
    public router = inject(Router);
    public toast = inject(Toast);
    public assignmentV1Service = inject(AssignmentV1Service);
    private translate = inject(TranslateService);
    public uiService = inject(UIService);

    static DownloadElementTypes = [
        ElementType.Node,
        ElementType.NodeChild,
        ElementType.NodeProposal,
        ElementType.NodePublishedCopy,
    ];
    static ElementTypesAddToCollection = [ElementType.Node, ElementType.NodePublishedCopy];

    readonly virtualNodesAdded = new EventEmitter<Node[]>();
    readonly displayTypeChanged = new EventEmitter<NodeEntriesDisplayType>();

    private subscriptions: Subscription[] = [];
    private destroyed = new Subject<void>();

    constructor() {
        const nodeHelperService = inject(NodeHelperServiceUi);
        const authenticationService = inject(AuthenticationService);
        const storage = inject(TemporaryStorageService);
        const networkService = inject(NetworkService);
        const route = inject(ActivatedRoute);
        super(nodeHelperService, authenticationService, storage, networkService, route);
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    override getTypeSingle(node: any): any {
        return super.getTypeSingle(node);
    }
    cutCopyNode(data: OptionData, node: Node, copy: boolean) {
        let list = this.getObjects(node, data);
        if (!list || !list.length) {
            return;
        }
        list = Helper.deepCopy(list);
        const clip: ClipboardObject = { sourceNode: data.parent, nodes: list, copy };
        this.storage.set('workspace_clipboard', clip);
        this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.CUT_COPY', {
            count: list.length,
        });
    }

    pasteNode(
        components: OptionsHelperComponents,
        data: OptionData,
        addVirtualNodes = true,
        nodes: Node[] = [],
    ) {
        const clip = this.storage.get('workspace_clipboard') as ClipboardObject;
        if (!this.canAddObjects(data)) {
            return;
        }
        if (nodes.length === clip.nodes.length) {
            this.bridge.closeModalDialog();
            this.storage.remove('workspace_clipboard');
            const info: any = {
                from: clip.sourceNode
                    ? clip.sourceNode.name
                    : this.translate.instant('WORKSPACE.COPY_SEARCH'),
                to: data.parent.name,
                count: clip.nodes.length,
                mode: this.translate.instant(
                    'WORKSPACE.' + (clip.copy ? 'PASTE_COPY' : 'PASTE_MOVE'),
                ),
            };
            this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.PASTE', info);
            if (addVirtualNodes) {
                this.addVirtualObjects(components, nodes);
            }
            return;
        }
        this.bridge.showProgressSpinner();
        const target = data.parent.ref.id;
        const source = clip.nodes[nodes.length].ref.id;
        if (clip.copy) {
            this.nodeServiceLegacy.copyNode(target, source).subscribe({
                next: (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, addVirtualNodes, nodes.concat(nodeData.node)),
                error: (error: any) => {
                    if (error.error?.error?.indexOf('DAORestrictedAccessException') !== -1) {
                        this.toast.error(null, 'RESTRICTED_ACCESS_COPY_ERROR');
                    } else {
                        this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    }
                    this.bridge.closeModalDialog();
                },
            });
        } else {
            this.nodeServiceLegacy.moveNode(target, source).subscribe({
                next: (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, true, nodes.concat(nodeData.node)),
                error: (error: any) => {
                    this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    this.bridge.closeModalDialog();
                },
            });
        }
    }

    /**
     * refresh all bound components with available menu options
     */
    async refreshComponents(components: OptionsHelperComponents, data: OptionData) {
        if (data == null || components == null) {
            // console.info('options helper refresh called but no data previously bound');
            return;
        }
        this.enabledCache = {};
        if (this.subscriptions?.length) {
            this.subscriptions.forEach((s) => s.unsubscribe());
            this.subscriptions = [];
        }

        if (components.list) {
            components.list.setOptions({
                [Target.List]: await this.getAvailableOptions(Target.List, [], components, data),
                [Target.ListDropdown]: await this.getAvailableOptions(
                    Target.ListDropdown,
                    [],
                    components,
                    data,
                ),
            });
        }
        if (components.dropdown) {
            components.dropdown.options = await this.getAvailableOptions(
                Target.ListDropdown,
                [],
                components,
                data,
            );
            components.dropdown.ngOnChanges();
        }
        if (components.actionbar) {
            components.actionbar.options = await this.getAvailableOptions(
                Target.Actionbar,
                [],
                components,
                data,
            );
            components.actionbar.invalidate();
        }
    }

    /**
     * get all available default options
     * usefull for duplicating specific options for custom use cases
     */
    async getDefaultOptions(data: OptionData) {
        return this.prepareOptions(this.mainNavService.getDialogs(), null, null, data);
    }
    async getAvailableOptions(
        target: Target,
        objects: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
        if (target === Target.List) {
            if (objects == null) {
                // fetch ALL options of ALL items inside list
                // the callback handlers will later decide for the individual node
                objects = null;
            }
        } else if (target === Target.Actionbar) {
            objects = data.selectedObjects || data.activeObjects;
        } else if (target === Target.ListDropdown) {
            if (data.activeObjects) {
                objects = data.activeObjects;
            } else {
                return null;
            }
        }
        let options: OptionItem[] = [];
        if (this.mainNavService.getMainNav()) {
            options = this.prepareOptions(
                this.mainNavService.getDialogs(),
                objects,
                components,
                data,
            );
        } else {
            console.warn(
                'options helper was called without main nav. Can not load default options',
            );
        }
        /*
         // DO NOT DELETE
         // provides a csv-table like structure of all options
        console.info(
            options.map((o) => [
                this.translate.instant(o.name),
                o.scopes?.join(' '),
                o.toolpermissions?.join(' '),
                o.elementType?.join(' '),
                new OptionTooltipPipe(this.translate).getKeyInfo(o)
                ]
            ).map((a) => a.join(',')).join('\n')
        );
         */

        options = this.applyExternalOptions(options, data.customOptions);
        const custom = this.configService.instant<ConfigOptionItem[]>('customOptions');
        void this.nodeHelper.applyCustomNodeOptions(custom, data.allObjects, objects, options);
        // do pre-handle callback options for dropdown + actionbar
        options = await this.filterOptions(options, target, data, objects);
        if (target !== Target.Actionbar) {
            options = options.filter((o) => !(o as OptionItemToggle).isToggle);
            // do not show any actions in the dropdown for no selection, these are reserved for actionbar
            options = options.filter(
                (o) => !o.constrains || o.constrains.indexOf(Constrain.NoSelection) === -1,
            );
        }
        return this.uiService.filterValidOptions(options) as OptionItem[];
    }

    private prepareOptions(
        management: WorkspaceManagementDialogsComponent,
        objects: Node[] | any[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
        const ctx: OptionsContext = {
            service: this,
            management,
            components,
            data,
            queryParams: this.queryParams,
        };
        const options: OptionItem[] = [
            ...createPrimaryOptions(ctx),
            ...createViewOptions(ctx),
            ...createReuseOptions(ctx),
            ...createEditOptions(ctx),
            ...createFileOperationsOptions(ctx),
            ...createDeleteOptions(ctx),
            ...createToggleOptions(ctx),
        ];
        if (data?.postPrepareOptions) {
            data.postPrepareOptions(options, objects);
        }
        if (this.globalOptionsService.postPrepareOptions) {
            this.globalOptionsService.postPrepareOptions(options, data, objects);
        }
        return options;
    }

    getDownloadOption(data: OptionData, safe = false) {
        const downloadNode = new OptionItem(
            'OPTIONS.DOWNLOAD' + (safe ? '_SAFE' : ''),
            'cloud_download',
            (object) => {
                if (data.customDownloadUrl) {
                    this.nodeHelper.downloadUrl(data.customDownloadUrl, 'download', {
                        node: this.getObjects(object, data)?.[0],
                        triggerTrackingEvent: true,
                    });
                    return;
                }
                void this.nodeHelper.downloadNodes(this.getObjects(object, data));
            },
        );
        downloadNode.elementType = OptionsHelperService.DownloadElementTypes;
        downloadNode.constrains = [Constrain.Files];
        downloadNode.group = DefaultGroups.View;
        // downloadNode.key = 'D';
        downloadNode.priority = 40;
        downloadNode.customShowCallback = async (nodes) => {
            return (
                !!data.customDownloadUrl ||
                nodes.some((n) =>
                    (n as Node).properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.includes(
                        RestConstants.SAFE_SCOPE,
                    ),
                ) === safe
            );
        };
        downloadNode.customEnabledCallback = async (nodes) => {
            if (!nodes) {
                return false;
            }

            for (let item of nodes) {
                if ((item as AssignmentFile).referNode) {
                    item = (item as AssignmentFile).referNode;
                }
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                if (
                    item.downloadUrl != null &&
                    item.properties &&
                    (!item.properties[RestConstants.CCM_PROP_IO_WWWURL] ||
                        !RestNetworkService.isFromHomeRepo(item)) &&
                    (item.accessEffective || item.access)?.includes(
                        RestConstants.PERMISSION_DOWNLOAD_CONTENT,
                    ) &&
                    this.nodeHelper.referenceOriginalExists(item)
                ) {
                    // bulk upload is not supported for remote nodes
                    if (!RestNetworkService.isFromHomeRepo(item) && nodes.length !== 1) {
                        continue;
                    }
                    return true;
                }
            }
            return false;
        };
        return downloadNode;
    }

    async revokeNode(object: any, data: OptionData) {
        const dialogRef = await this.dialogs.openRevocationDialog({
            node: this.getObjects(object, data)[0],
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this.localEvents.nodesChanged.emit([result.node]);
            }
        });
    }

    private addVirtualObjects(components: OptionsHelperComponents, objects: any[]) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        this.virtualNodesAdded.emit(objects);
        if (components.list) {
            components.list.addVirtualNodes(objects);
        }
    }

    bookmarkNodes(nodes: Node[]) {
        this.bridge.showProgressSpinner();
        this.addToNodeStore(nodes).subscribe(() => {
            this.bridge.closeModalDialog();
        });
    }

    private addToNodeStore(nodes: Node[]): Observable<void> {
        return this.nodeList
            .addToNodeList(
                RestConstants.NODE_STORE_LIST,
                nodes.map((node) => node.ref.id),
            )
            .pipe(
                tap(() => {
                    this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                        count: nodes.length,
                    });
                }),
                catchError((errors: NodeListErrorResponses) => {
                    const numberSuccessful = nodes.length - errors.length;
                    if (numberSuccessful > 0) {
                        this.toast.toast('SEARCH.ADDED_TO_NODE_STORE', {
                            count: numberSuccessful,
                        });
                    }
                    for (const { nodeId, error } of errors) {
                        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                            this.toast.error(null, 'SEARCH.ADDED_TO_NODE_STORE_EXISTS', {
                                name: RestHelper.getTitle(
                                    nodes.find((node) => node.ref.id === nodeId),
                                ),
                            });
                            error.preventDefault();
                        }
                    }
                    return of(void 0);
                }),
            );
    }

    goToWorkspace(node: Node | any) {
        if (node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
            this.nodeServiceLegacy
                .getNodeMetadata(node.properties[RestConstants.CCM_PROP_IO_ORIGINAL][0])
                .subscribe((org) =>
                    UIHelper.goToWorkspace(
                        this.nodeServiceLegacy,
                        this.router,
                        this.connector.getCurrentLogin(),
                        org.node,
                    ),
                );
        } else {
            UIHelper.goToWorkspace(
                this.nodeServiceLegacy,
                this.router,
                this.connector.getCurrentLogin(),
                node,
            );
        }
    }

    async getObjectsAsync(object: Node | any, data: OptionData, resolveOriginals = false) {
        const nodes = NodeHelperService.getActionbarNodes(
            data.selectedObjects || data.activeObjects,
            object,
        );
        if (resolveOriginals) {
            const originals = await lastValueFrom(
                observableForkJoin(
                    nodes.map((n) => {
                        if (n.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                            return this.nodeServiceLegacy.getNodeMetadata(
                                n.properties[RestConstants.CCM_PROP_IO_ORIGINAL][0],
                                [RestConstants.ALL],
                            );
                        } else if (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
                            return this.nodeServiceLegacy.getNodeMetadata(
                                RestHelper.removeSpacesStoreRef(
                                    n.properties[
                                        RestConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET
                                    ][0],
                                ),
                                [RestConstants.ALL],
                            );
                        } else {
                            return of({
                                node: n,
                            });
                        }
                    }),
                ),
            );
            return originals.map((o) => o.node);
        }
        return nodes;
    }

    applyExternalOptions(options: OptionItem[], customOptionsIn: CustomOptions) {
        if (!customOptionsIn) {
            return options;
        }
        const customOptions = { ...new CustomOptions(), ...customOptionsIn };
        if (!customOptions.useDefaultOptions) {
            options = [];
        }
        if (customOptions.supportedOptions && Array.isArray(customOptions.supportedOptions)) {
            options = options.filter((o) => customOptions.supportedOptions.indexOf(o.name) !== -1);
        } else if (customOptions.removeOptions) {
            for (const option of customOptions.removeOptions) {
                const index = options.findIndex((o) => o.name === option);
                if (index !== -1) {
                    options.splice(index, 1);
                }
            }
        }
        if (customOptions.addOptions) {
            for (const option of customOptions.addOptions) {
                const existing = options.filter((o) => o.name === option.name);
                if (existing.length === 1) {
                    // only replace changed values
                    for (const key of Object.keys(option)) {
                        (existing[0] as any)[key] = (option as any)[key];
                    }
                } else {
                    options.push(option);
                }
            }
        }
        return options;
    }

    /**
     * Shows a confirmation dialog and removes the given nodes from the current collection if
     * confirmed.
     */
    async removeFromCollection(nodes: Node[], data: OptionData) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'OPTIONS.REMOVE_REF',
            message: 'COLLECTIONS.REMOVE_FROM_COLLECTION_DIALOG_TEXT',
            messageParameters: { count: nodes.length.toString() },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef
            .afterClosed()
            .pipe(
                filter((value) => value === 'YES_DELETE'),
                switchMap(() =>
                    forkJoinWithErrors(
                        nodes.map((node: Node) =>
                            this.collectionService
                                .removeFromCollection(node.ref.id, data.parent.ref.id)
                                .pipe(map(() => node)),
                        ),
                    ),
                ),
            )
            .subscribe(({ successes: deletedNodes, errors }) => {
                if (errors.length > 0) {
                    this.toast.error(errors[0]);
                } else {
                    this.toast.toast('COLLECTIONS.REMOVED_FROM_COLLECTION');
                }
                if (deletedNodes.length > 0) {
                    this.localEvents.nodesDeleted.emit(deletedNodes);
                }
            });
    }

    unblockImportedNodes(nodes: Node[]) {
        this.toast.showProgressSpinner();
        observableForkJoin(
            nodes.map((n) => {
                const properties: any = {};
                properties[RestConstants.CCM_PROP_IMPORT_BLOCKED] = [null];
                return new Observable((observer) => {
                    this.nodeServiceLegacy
                        .editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_BLOCKED_IMPORT,
                            properties,
                        )
                        .subscribe(({ node }) => {
                            const permissions = new LocalPermissions();
                            permissions.inherited = true;
                            permissions.permissions = [];
                            this.nodeServiceLegacy
                                .setNodePermissions(node.ref.id, permissions)
                                .subscribe(() => {
                                    observer.next(node);
                                    observer.complete();
                                });
                        });
                });
            }),
        ).subscribe((results: Node[]) => {
            this.toast.closeProgressSpinner();
            this.localEvents.nodesChanged.emit(results);
        });
    }

    /**
     * get the toggle to open or close the right sidebar based on a signal state
     */
    getOptionItemToggleSidebar(state: WritableSignal<boolean>) {
        const toggle = new OptionItemToggle(
            {
                enabled: 'EDITORIAL.OPTION.TOGGLE_SIDEBAR_ENABLED',
                disabled: 'EDITORIAL.OPTION.TOGGLE_SIDEBAR_DISABLED',
            },
            {
                enabled: 'splitscreen_right',
                disabled: 'view_column_2',
            },
            state(),
            () => state.set(!state()),
        );
        runInInjectionContext(this.injector, () => {
            effect(() => (toggle.toggleState = state()));
        });
        toggle.priority = 1000;
        toggle.elementType = [];
        return toggle;
    }
}
