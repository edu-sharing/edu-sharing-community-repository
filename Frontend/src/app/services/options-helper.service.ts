import {
    effect,
    EventEmitter,
    Injectable,
    Injector,
    NgZone,
    OnDestroy,
    runInInjectionContext,
    WritableSignal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import {
    Assignment,
    AssignmentFile,
    AssignmentV1Service,
    AuthenticationService,
    LtiPlatformService,
    NetworkService,
    Node,
    NodeListErrorResponses,
    NodeListService,
    NodeService,
    ROOT,
} from 'ngx-edu-sharing-api';
import {
    AssignmentPipe,
    ClipboardObject,
    Constrain,
    CustomOptions,
    DefaultGroups,
    ElementType,
    HideMode,
    ListEventInterface,
    LocalEventsService,
    NodeEntriesDisplayType,
    NodeHelperService as NodeHelperServiceUi,
    NodesRightMode,
    OptionData,
    OptionItem,
    OptionItemToggle,
    OptionsHelperComponents,
    OptionsHelperService as OptionsHelperServiceAbstract,
    Scope,
    Target,
    TemporaryStorageService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import {
    firstValueFrom,
    forkJoin,
    forkJoin as observableForkJoin,
    Observable,
    of,
    Subject,
    Subscription,
} from 'rxjs';
import { catchError, filter, first, map, switchMap, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    RestCollectionService,
    RestConnectorService,
    RestHelper,
    RestIamService,
} from '../core-module/core.module';
import { LocalPermissions, NodeWrapper } from '../core-module/rest/data-object';
import { Helper } from '../core-module/rest/helper';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestConnectorsService } from '../core-module/rest/services/rest-connectors.service';
import { RestNetworkService } from '../core-module/rest/services/rest-network.service';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { UIService } from '../core-module/rest/services/ui.service';
import {
    DELETE_OR_CANCEL,
    OK_OR_CANCEL,
} from '../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { WorkspaceManagementDialogsComponent } from '../features/management-dialogs/management-dialogs.component';
import { MainNavService } from '../main/navigation/main-nav.service';
import { WorkspaceService } from '../pages/workspace-page/workspace.service';
import { BridgeService } from './bridge.service';
import { KeyboardShortcutsService } from './keyboard-shortcuts.service';
import { MessageType } from '../util/message-type';
import { forkJoinWithErrors } from '../util/rxjs/forkJoinWithErrors';
import { ConfigOptionItem, NodeHelperService } from './node-helper.service';
import { Toast } from './toast';
import { UIHelper } from '../core-ui-module/ui-helper';
import { GlobalOptionsService } from './global-options.service';
import { SelectionModel } from '@angular/cdk/collections';
import { Closable } from '../features/dialogs/card-dialog/card-dialog-config';
import {
    NodesSelectorConfig,
    TabType,
} from '../pages/editorial-page/nodes-selector/nodes-selector.component';
import { EditorialSidebarService } from '../features/editorial-sidebar/editorial-sidebar.service';

@Injectable()
export class OptionsHelperService extends OptionsHelperServiceAbstract implements OnDestroy {
    static DownloadElementTypes = [
        ElementType.Node,
        ElementType.NodeChild,
        ElementType.NodeProposal,
        ElementType.NodePublishedCopy,
    ];
    static ElementTypesAddToCollection = [ElementType.Node, ElementType.NodePublishedCopy];

    readonly virtualNodesAdded = new EventEmitter<Node[]>();
    readonly displayTypeChanged = new EventEmitter<NodeEntriesDisplayType>();

    private keyboardShortcutsSubscription: Subscription;
    private globalOptions: OptionItem[];
    private subscriptions: Subscription[] = [];
    private destroyed = new Subject<void>();

    constructor(
        nodeHelperService: NodeHelperServiceUi,
        authenticationService: AuthenticationService,
        storage: TemporaryStorageService,
        networkService: NetworkService,
        route: ActivatedRoute,
        private nodeHelper: NodeHelperService,
        private bridge: BridgeService,
        private collectionService: RestCollectionService,
        private configService: ConfigurationService,
        private globalOptionsService: GlobalOptionsService,
        private connector: RestConnectorService,
        private connectors: RestConnectorsService,
        private dialogs: DialogsService,
        private eventService: FrameEventsService,
        private iamService: RestIamService,
        private keyboardShortcuts: KeyboardShortcutsService,
        private localEvents: LocalEventsService,
        private mainNavService: MainNavService,
        private editorialSidebarService: EditorialSidebarService,
        private ngZone: NgZone,
        private injector: Injector,
        private nodeList: NodeListService,
        private nodeService: NodeService,
        private nodeServiceLegacy: RestNodeService,
        private router: Router,
        private toast: Toast,
        private assignmentV1Service: AssignmentV1Service,
        private ltiPlatformService: LtiPlatformService,
        private translate: TranslateService,
        private uiService: UIService,
        private workspace: WorkspaceService,
    ) {
        super(nodeHelperService, authenticationService, storage, networkService, route);
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }
    private cutCopyNode(data: OptionData, node: Node, copy: boolean) {
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
            this.nodeServiceLegacy.copyNode(target, source).subscribe(
                (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, addVirtualNodes, nodes.concat(nodeData.node)),
                (error: any) => {
                    if (error.error?.error?.indexOf('DAORestrictedAccessException') !== -1) {
                        this.toast.error(null, 'RESTRICTED_ACCESS_COPY_ERROR');
                    } else {
                        this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    }
                    this.bridge.closeModalDialog();
                },
            );
        } else {
            this.nodeServiceLegacy.moveNode(target, source).subscribe(
                (nodeData: NodeWrapper) =>
                    this.pasteNode(components, data, true, nodes.concat(nodeData.node)),
                (error: any) => {
                    this.nodeHelper.handleNodeError(clip.nodes[nodes.length].name, error);
                    this.bridge.closeModalDialog();
                },
            );
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

    private hasSelection(data: OptionData) {
        return data.selectedObjects && data.selectedObjects.length;
    }

    private prepareOptions(
        management: WorkspaceManagementDialogsComponent,
        objects: Node[] | any[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
        const options: OptionItem[] = [];

        /*
        let apply=new OptionItem('APPLY', 'redo', (node: Node) => this.nodeHelper.addNodeToLms(this.router,this.temporaryStorageService,ActionbarHelperService.getNodes(this.selection,node)[0],this.searchService.reurl));
      apply.enabledCallback=((node:Node)=> {
        return this.nodeHelper.getNodesRight([node],RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
      });
      if(fromList || (nodes && nodes.length==1))
        options.push(apply);
      return options;
         */

        const applyNode = new OptionItem('APPLY', 'redo', (object) =>
            this.nodeHelper.addNodeToLms(this.getObjects(object, data)[0], this.queryParams.reurl),
        );

        applyNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        applyNode.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        applyNode.permissionsRightMode = NodesRightMode.Effective;
        applyNode.permissionsMode = HideMode.Disable;
        applyNode.constrains = [Constrain.NoBulk, Constrain.ReurlMode, Constrain.User];
        applyNode.showAsAction = true;
        applyNode.showAlways = true;
        applyNode.group = DefaultGroups.Primary;
        applyNode.priority = 10;
        applyNode.customShowCallback = async (nodes) => {
            return !this.nodeHelper.isNodeCollection(nodes?.[0]);
        };
        applyNode.customEnabledCallback = (nodes) => {
            // either apply directories is true or it is an file
            return this.nodeHelper.isNodeCollection(nodes?.[0])
                ? false
                : (nodes?.[0].isDirectory ? this.queryParams.applyDirectories === 'true' : true) &&
                      // and either onlyDownloadable is explicitly required or the node has a download url
                      ((this.queryParams.onlyDownloadable ?? 'false') === 'false' ||
                          !!nodes?.[0].downloadUrl ||
                          nodes?.[0].isDirectory);
        };

        /*
       if(nodes && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)==-1) {
       option = new OptionItem("OPTIONS.INVITE", "group_add", callback);
       option.isSeperate = this.nodeHelper.allFiles(nodes);
       option.showAsAction = true;
       option.isEnabled = this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
     }
        */
        const debugNode = new OptionItem('OPTIONS.DEBUG', 'build', async (object) => {
            let nodes = this.getObjects(object, data);
            console.info(nodes);
            if (nodes.some((n) => n.authorityName)) {
                try {
                    nodes = (
                        await forkJoin(
                            nodes.map((n) =>
                                this.nodeServiceLegacy.getNodeMetadata(
                                    n.ref?.id || n.properties?.[RestConstants.NODE_ID]?.[0],
                                    [RestConstants.ALL],
                                ),
                            ),
                        ).toPromise()
                    ).map((n) => n.node);
                } catch (e) {
                    console.info(nodes);
                    console.warn(e);
                }
            }
            void this.dialogs.openNodeInfoDialog({ nodes });
        });
        debugNode.elementType = [];
        debugNode.onlyDesktop = true;
        debugNode.constrains = [Constrain.AdminOrDebug, Constrain.Selection];
        debugNode.group = DefaultGroups.View;
        debugNode.priority = -100;

        const acceptProposal = new OptionItem(
            'OPTIONS.COLLECTION_PROPOSAL_ACCEPT',
            'check',
            (object) => management.addProposalsToCollection(this.getObjects(object, data)),
        );
        /*acceptProposal.customEnabledCallback = (nodes) =>
            nodes.every((n) => (n as ProposalNode).accessible);*/
        acceptProposal.elementType = [ElementType.NodeProposal];
        acceptProposal.constrains = [Constrain.User];
        acceptProposal.group = DefaultGroups.Primary;
        acceptProposal.showAsAction = true;
        acceptProposal.priority = 10;

        const declineProposal = new OptionItem(
            'OPTIONS.COLLECTION_PROPOSAL_DECLINE',
            'clear',
            (object) => management.declineProposals(this.getObjects(object, data)),
        );
        declineProposal.elementType = [ElementType.NodeProposal];
        declineProposal.constrains = [Constrain.User];
        declineProposal.group = DefaultGroups.Primary;
        declineProposal.priority = 20;

        /*
         let openFolder = new OptionItem('SHOW_IN_FOLDER', 'folder', null);
            openFolder.isEnabled = false;
            this.nodeApi.getNodeMetadata(this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]).subscribe((original: NodeWrapper) => {

                this.nodeApi.getNodeParents(original.node.parent.id, false, [], original.node.parent.repo).subscribe(() => {
                    openFolder.isEnabled = true;
                    openFolder.callback=() => this.goToWorkspace(login, original.node);
                    //.isEnabled = data.node.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
                });
            }, (error: any) => {
            });
            options.push(openFolder);
         */
        const revokeNode = new OptionItem('OPTIONS.REVOKE', 'delete', async (object) => {
            await this.revokeNode(object, data);
        });
        revokeNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        revokeNode.scopes = [Scope.Render];
        revokeNode.elementType = [ElementType.NodePublishedCopy];
        revokeNode.group = DefaultGroups.Delete;
        revokeNode.color = 'warn';
        revokeNode.priority = 10;
        revokeNode.permissions = [RestConstants.ACCESS_DELETE];
        revokeNode.permissionsRightMode = NodesRightMode.Effective;
        revokeNode.permissionsMode = HideMode.Hide;

        const editRevocation = new OptionItem('OPTIONS.EDIT_REVOCATION', 'edit', async (object) => {
            await this.revokeNode(object, data);
        });
        editRevocation.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        editRevocation.scopes = [Scope.Render];
        editRevocation.elementType = [ElementType.NodeRevoked];
        editRevocation.group = DefaultGroups.Edit;
        editRevocation.priority = 10;
        editRevocation.permissions = [RestConstants.ACCESS_WRITE];
        editRevocation.permissionsRightMode = NodesRightMode.Effective;
        editRevocation.permissionsMode = HideMode.Hide;

        const openOriginalNode = new OptionItem(
            'OPTIONS.OPEN_ORIGINAL_NODE',
            'description',
            async (object) => {
                const nodeId = RestHelper.removeSpacesStoreRef(
                    this.getObjects(object, data)[0].properties[
                        RestConstants.CCM_PROP_PUBLISHED_ORIGINAL
                    ][0],
                );
                UIHelper.goToNode(this.router, { ref: { id: nodeId } } as Node);
            },
        );
        openOriginalNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        openOriginalNode.toolpermissions = [RestConstants.TOOLPERMISSION_WORKSPACE];
        openOriginalNode.scopes = [Scope.CollectionsReferences, Scope.Search, Scope.Render];
        openOriginalNode.customEnabledCallback = async (nodes: Node[]) => {
            if (nodes && nodes.length === 1) {
                return new Promise<boolean>((resolve) => {
                    const nodeId = RestHelper.removeSpacesStoreRef(
                        nodes[0].properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL][0],
                    );
                    this.nodeServiceLegacy.getNodeMetadata(nodeId).subscribe(
                        () => {
                            resolve(true);
                        },
                        () => {
                            resolve(false);
                        },
                    );
                });
            }
            return false;
        };
        openOriginalNode.elementType = [ElementType.NodePublishedCopy, ElementType.NodeRevoked];
        openOriginalNode.group = DefaultGroups.View;
        openOriginalNode.priority = 13;

        const openParentNode = new OptionItem('OPTIONS.SHOW_IN_FOLDER', 'folder', async (object) =>
            this.goToWorkspace((await this.getObjectsAsync(object, data, true))[0]),
        );
        openParentNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        openParentNode.toolpermissions = [RestConstants.TOOLPERMISSION_WORKSPACE];
        openParentNode.scopes = [
            Scope.CollectionsReferences,
            Scope.Search,
            Scope.Render,
            Scope.EditorialPage,
        ];
        openParentNode.customEnabledCallback = async (nodes: Node[]) => {
            if (nodes && nodes.length === 1) {
                return new Promise<boolean>((resolve) => {
                    let nodeId = nodes[0].ref.id;
                    if (nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                        nodeId = nodes[0].properties[RestConstants.CCM_PROP_IO_ORIGINAL][0];
                    }
                    this.nodeService.getParents(nodeId, { fullPath: false }).subscribe(
                        () => {
                            resolve(true);
                        },
                        (error) => {
                            error.preventDefault();
                            resolve(false);
                        },
                    );
                });
            }
            return false;
        };
        openParentNode.group = DefaultGroups.View;
        openParentNode.priority = 15;

        const openNode = new OptionItem('OPTIONS.SHOW', 'remove_red_eye', (object) =>
            UIHelper.goToNode(this.router, this.getObjects(object, data)[0]),
        );
        openNode.constrains = [Constrain.Files, Constrain.NoBulk];
        openNode.scopes = [Scope.WorkspaceList];
        openNode.group = DefaultGroups.View;
        openNode.priority = 30;

        const editConnectorNode = new OptionItem('OPTIONS.OPEN', 'launch', (node) => {
            void this.uiService.editConnector(this.getObjects(node, data)[0]);
        });
        editConnectorNode.customShowCallback = async (nodes) => {
            return await this.uiService.hasAvailableConnector(nodes ? nodes[0] : null);
        };
        editConnectorNode.elementType = [
            ElementType.Node,
            ElementType.NodePublishedCopy,
            ElementType.NodeChild,
            ElementType.NodeProposal,
        ];
        editConnectorNode.group = DefaultGroups.View;
        editConnectorNode.priority = 20;
        editConnectorNode.showAsAction = true;
        editConnectorNode.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
        ];

        /**
         if (this.connector.getCurrentLogin() && !this.connector.getCurrentLogin().isGuest) {
         option = new OptionItem("OPTIONS.COLLECTION", "layers", callback);
         option.isEnabled = this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
         option.showAsAction = true;
         option.customShowCallback = (node: Node) => {
         let n=ActionbarHelperService.getNodes(nodes,node);
         if(n==null)
         return false;
         return this.nodeHelper.referenceOriginalExists(node) && this.nodeHelper.allFiles(nodes) && n.length>0;
         }
         option.enabledCallback = (node: Node) => {
         let list = ActionbarHelperService.getNodes(nodes, node);
         return this.nodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
         }
         option.disabledCallback = () =>{
         this.connectors.getRestConnector().getBridgeService().showTemporaryMessage(MessageType.error, null,'WORKSPACE.TOAST.ADD_TO_COLLECTION_DISABLED');
         };
         }
         */

        const sortInto = new OptionItem('OPTIONS.SORT_INTO', 'layers', (object) =>
            this.editorialSidebarService.showOption({
                option: 'SORT_INTO',
                trap: true,
                optionConfig: {
                    state: TabType.COLLECTIONS,
                    selection: components.list.getSelection(),
                } as NodesSelectorConfig,
            }),
        );
        sortInto.elementType = OptionsHelperService.ElementTypesAddToCollection;
        sortInto.showAsAction = true;
        sortInto.constrains = [Constrain.Files, Constrain.User, Constrain.NoScope];
        sortInto.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        sortInto.permissionsRightMode = NodesRightMode.Effective;
        sortInto.permissionsMode = HideMode.Disable;
        sortInto.group = DefaultGroups.Reuse;
        sortInto.priority = 10;

        /*
        const addNodeToCollection = new OptionItem(
            'OPTIONS.COLLECTION',
            'layers',
            (object) => (management.addToCollection = this.getObjects(object, data)),
        );
        addNodeToCollection.elementType = OptionsHelperService.ElementTypesAddToCollection;
        addNodeToCollection.showAsAction = true;
        addNodeToCollection.constrains = [Constrain.Files, Constrain.User, Constrain.NoScope];
        addNodeToCollection.customShowCallback = async (nodes) => {
            addNodeToCollection.name =
                data?.scope === Scope.CollectionsReferences
                    ? 'OPTIONS.COLLECTION_OTHER'
                    : 'OPTIONS.COLLECTION';
            return this.nodeHelper.referenceOriginalExists(nodes ? nodes[0] : null);
        };
        addNodeToCollection.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addNodeToCollection.permissionsRightMode = NodesRightMode.Effective;
        addNodeToCollection.permissionsMode = HideMode.Disable;
        // addNodeToCollection.key = 'C';
        addNodeToCollection.group = DefaultGroups.Reuse;
        addNodeToCollection.priority = 10;
        */

        const addNodeToLTIPlatform = new OptionItem('OPTIONS.LTI', 'input', (object) => {
            const nodes: Node[] = this.getObjects(object, data);
            void this.nodeHelper.addNodesToLTIPlatform(nodes);
        });
        addNodeToLTIPlatform.elementType = OptionsHelperService.ElementTypesAddToCollection;
        addNodeToLTIPlatform.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addNodeToLTIPlatform.permissionsRightMode = NodesRightMode.Effective;
        addNodeToLTIPlatform.showAsAction = true;
        addNodeToLTIPlatform.showAlways = true;
        addNodeToLTIPlatform.constrains = [
            Constrain.NoBulk,
            Constrain.Files,
            Constrain.User,
            Constrain.LTIMode,
        ];
        addNodeToLTIPlatform.group = DefaultGroups.Primary;
        addNodeToLTIPlatform.priority = 11;
        addNodeToLTIPlatform.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addNodeToLTIPlatform.customEnabledCallback = async (nodes: Node[]) => {
            const ltiSession = this.connectors.getRestConnector().getCurrentLogin().ltiSession;
            if (!ltiSession) {
                return false;
            }
            if (!ltiSession.acceptMultiple) {
                if (data.selectedObjects && data.selectedObjects.length > 1) {
                    return false;
                }
            }
            /**
             * prevent lti editor as tool with custom content option, embedding nodes as platform created by the same tool
             */
            if (ltiSession.customContentNode && ltiSession.customContentNode.properties) {
                let customContentNodeLtiToolUrl =
                    ltiSession.customContentNode.properties['ccm:ltitool_url'][0];
                return nodes.some((n) => {
                    let nLtiToolUrlArr = ltiSession.customContentNode.properties['ccm:ltitool_url'];
                    if (!Array.isArray(nLtiToolUrlArr) || nLtiToolUrlArr.length == 0) {
                        return true;
                    }
                    let nLtiToolUrl = nLtiToolUrlArr[0];

                    if (
                        n.aspects.includes('ccm:ltitool_node') &&
                        nLtiToolUrl === customContentNodeLtiToolUrl
                    ) {
                        return false;
                    } else return true;
                });
            }
            return true;
        };

        const bookmarkNode = new OptionItem('OPTIONS.ADD_NODE_STORE', 'bookmark_border', (object) =>
            this.bookmarkNodes(this.getObjects(object, data)),
        );
        bookmarkNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        bookmarkNode.constrains = [Constrain.Files, Constrain.HomeRepository, Constrain.NoScope];
        bookmarkNode.group = DefaultGroups.Reuse;
        bookmarkNode.priority = 20;
        bookmarkNode.customShowCallback = async (nodes) => {
            if (nodes) {
                return nodes.every((n) => this.nodeHelper.referenceOriginalExists(n));
            }
            return true;
        };

        const shortcutNode = new OptionItem('OPTIONS.ADD_SHORTCUT', 'star', (object) => {
            const nodes = this.getObjects(object, data);
            void this.dialogs.openShortcutManagementDialog({ node: nodes[0] });
        });
        shortcutNode.elementType = [
            ElementType.Group,
            ElementType.MapRef,
            ElementType.Node,
            ElementType.NodeChild,
            ElementType.NodeProposal,
            ElementType.NodePublishedCopy,
            ElementType.Person,
            ElementType.SavedSearch,
        ];
        shortcutNode.constrains = [Constrain.User];
        shortcutNode.group = DefaultGroups.Reuse;
        shortcutNode.priority = 21;

        const createNodeVariant = new OptionItem('OPTIONS.VARIANT', 'call_split', (object) =>
            this.dialogs.openCreateVariantDialog({ node: this.getObjects(object, data)[0] }),
        );
        createNodeVariant.constrains = [
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        createNodeVariant.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
        createNodeVariant.customEnabledCallback = async (nodes) => {
            if (nodes) {
                // do not show variant if it's a licensed material and user doesn't has change permission rights
                return (
                    nodes[0].properties?.[RestConstants.CCM_PROP_RESTRICTED_ACCESS]?.[0] !==
                        'true' ||
                    this.nodeHelper.getNodesRight(
                        nodes,
                        RestConstants.ACCESS_CHANGE_PERMISSIONS,
                        NodesRightMode.Effective,
                    )
                );
            }
            return true;
        };
        createNodeVariant.customShowCallback = async (nodes) => {
            if (nodes) {
                createNodeVariant.name =
                    'OPTIONS.VARIANT' +
                    (this.connectors.connectorSupportsEdit(nodes[0]) ? '_OPEN' : '');
                return this.nodeHelper.referenceOriginalExists(nodes[0]);
            }
            return false;
        };
        createNodeVariant.group = DefaultGroups.Reuse;
        createNodeVariant.priority = 30;

        const inviteNode = new OptionItem('OPTIONS.INVITE', 'group_add', async (object) =>
            this.dialogs.openShareDialog({ nodes: await this.getObjectsAsync(object, data, true) }),
        );
        inviteNode.elementType = [ElementType.Node, ElementType.SavedSearch];
        inviteNode.showAsAction = true;
        inviteNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        inviteNode.permissionsMode = HideMode.Hide;
        inviteNode.permissionsRightMode = NodesRightMode.Effective;
        // inviteNode.key = 'S';
        inviteNode.constrains = [Constrain.HomeRepository, Constrain.User];
        inviteNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE];
        inviteNode.group = DefaultGroups.Edit;
        inviteNode.priority = 10;
        // invite is not allowed for collections of type editorial
        inviteNode.customShowCallback = async (objects) =>
            objects[0].collection
                ? objects[0].collection.type !== RestConstants.COLLECTIONTYPE_EDITORIAL
                : objects[0].type !== RestConstants.SYS_TYPE_CONTAINER;

        const streamNode = new OptionItem(
            'OPTIONS.STREAM',
            'event',
            (object) => (management.addNodesStream = this.getObjects(object, data)),
        );
        streamNode.elementType = [ElementType.Node];
        streamNode.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        streamNode.permissionsMode = HideMode.Hide;
        streamNode.constrains = [
            Constrain.Files,
            Constrain.NoCollectionReference,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        streamNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE_STREAM];
        streamNode.group = DefaultGroups.Edit;
        streamNode.priority = 15;
        streamNode.customShowCallback = (objects) =>
            this.configService.get('stream.enabled', false).pipe(first()).toPromise();

        const licenseNode = new OptionItem('OPTIONS.LICENSE', 'copyright', (object) => {
            const nodes = this.getObjects(object, data);
            void this.dialogs.openLicenseDialog({ kind: 'nodes', nodes });
        });
        licenseNode.elementType = [ElementType.Node, ElementType.NodeChild];
        licenseNode.constrains = [
            Constrain.Files,
            Constrain.NoCollectionReference,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        licenseNode.permissions = [RestConstants.ACCESS_WRITE];
        licenseNode.permissionsMode = HideMode.Disable;
        licenseNode.toolpermissions = [RestConstants.TOOLPERMISSION_LICENSE];
        // licenseNode.key = 'L';
        licenseNode.group = DefaultGroups.Edit;
        licenseNode.priority = 30;

        const contributorNode = new OptionItem('OPTIONS.CONTRIBUTOR', 'group', (object) => {
            void this.dialogs.openContributorsDialog({
                node: this.getObjects(object, data)[0],
            });
        });
        contributorNode.constrains = [
            Constrain.Files,
            Constrain.NoCollectionReference,
            Constrain.HomeRepository,
            Constrain.NoBulk,
            Constrain.User,
        ];
        contributorNode.permissions = [RestConstants.ACCESS_WRITE];
        contributorNode.permissionsMode = HideMode.Disable;
        contributorNode.onlyDesktop = true;
        contributorNode.group = DefaultGroups.Edit;
        contributorNode.priority = 40;

        const workflowNode = new OptionItem('OPTIONS.WORKFLOW', 'swap_calls', (object) =>
            this.dialogs.openWorkflowDialog({ nodes: this.getObjects(object, data) }),
        );
        workflowNode.constrains = [
            Constrain.Files,
            Constrain.NoCollectionReference,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        workflowNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        workflowNode.permissionsMode = HideMode.Disable;
        workflowNode.group = DefaultGroups.Edit;
        workflowNode.priority = 50;

        /*
        option = new OptionItem("OPTIONS.DOWNLOAD", "cloud_download", callback);
        option.enabledCallback = (node: Node) => {
          let list:any=ActionbarHelperService.getNodes(nodes, node);
          if(!list || !list.length)
            return false;
            let isAllowed=false;
            for(let item of list) {
                if(item.reference)
                    item = item.reference;
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                isAllowed=isAllowed || list && item.downloadUrl!=null && item.properties && !item.properties[RestConstants.CCM_PROP_IO_WWWURL];
            }
            return isAllowed;
         */
        const downloadNode = this.getDownloadOption(data, false);
        const downloadNodeSafe = this.getDownloadOption(data, true);
        const downloadMetadataNode = new OptionItem(
            'OPTIONS.DOWNLOAD_METADATA',
            'format_align_left',
            (object) =>
                this.dialogs.openGenericDialog({
                    title: 'DOWNLOAD_METADATA.TITLE',
                    message: 'DOWNLOAD_METADATA.MESSAGE',
                    closable: Closable.Casual,
                    avatar: {
                        icon: 'format_align_left',
                        kind: 'icon',
                    },
                    buttons: [
                        {
                            label: 'DOWNLOAD_METADATA.TYPE_TEXT',
                            config: DialogButton.TYPE_CANCEL,
                            callback: (ref) => {
                                ref.close();
                                void this.nodeHelper.downloadNode(
                                    this.getObjects(object, data)[0],
                                    RestConstants.NODE_VERSION_CURRENT,
                                    true,
                                );
                                return null;
                            },
                        },
                        {
                            label: 'DOWNLOAD_METADATA.TYPE_PDF',
                            config: DialogButton.TYPE_PRIMARY,
                            callback: async (ref) => {
                                const node = this.getObjects(object, data)[0];
                                void this.router.navigate([
                                    UIConstants.ROUTER_PREFIX + 'pdf-metadata',
                                    node.ref.id,
                                ]);
                                return true;
                            },
                        },
                    ],
                }),
        );
        downloadMetadataNode.elementType = [
            ElementType.Node,
            ElementType.NodeChild,
            ElementType.NodePublishedCopy,
        ];
        downloadMetadataNode.constrains = [Constrain.Files, Constrain.NoBulk];
        downloadMetadataNode.scopes = [Scope.Render];
        downloadMetadataNode.group = DefaultGroups.View;
        downloadMetadataNode.priority = 50;
        downloadMetadataNode.customShowCallback = async (nodes) => {
            if (!nodes) {
                return false;
            }
            return nodes[0].downloadUrl != null;
        };
        const simpleEditNode = new OptionItem(
            'OPTIONS.EDIT_SIMPLE',
            'edu-quick_edit',
            async (object) => {
                const nodes = await this.getObjectsAsync(object, data, true);
                void this.dialogs.openSimpleEditDialog({ nodes, fromUpload: false });
            },
        );
        simpleEditNode.constrains = [Constrain.Files, Constrain.HomeRepository, Constrain.User];
        simpleEditNode.permissions = [RestConstants.ACCESS_WRITE];
        simpleEditNode.permissionsRightMode = NodesRightMode.Effective;
        simpleEditNode.permissionsMode = HideMode.Disable;
        simpleEditNode.group = DefaultGroups.Edit;
        simpleEditNode.priority = 15;

        const editNode = new OptionItem('OPTIONS.EDIT', 'edit', async (object) => {
            const nodes = await this.getObjectsAsync(object, data, true);
            void this.dialogs.openMdsEditorDialogForNodes({ nodes });
        });
        editNode.elementType = [ElementType.Node, ElementType.NodeChild, ElementType.MapRef];
        editNode.constrains = [
            Constrain.FilesAndDirectories,
            Constrain.HomeRepository,
            Constrain.User,
        ];
        editNode.permissions = [RestConstants.ACCESS_WRITE];
        editNode.permissionsMode = HideMode.Disable;
        editNode.permissionsRightMode = NodesRightMode.Effective;
        editNode.group = DefaultGroups.Edit;
        editNode.priority = 20;

        const templateNode = new OptionItem('OPTIONS.TEMPLATE', 'assignment_turned_in', (object) =>
            this.dialogs.openNodeTemplateDialog({ node: this.getObjects(object, data)[0] }),
        );
        templateNode.constrains = [Constrain.NoBulk, Constrain.Directory, Constrain.User];
        templateNode.permissions = [RestConstants.ACCESS_WRITE];
        templateNode.permissionsMode = HideMode.Disable;
        templateNode.onlyDesktop = true;
        templateNode.group = DefaultGroups.Edit;

        const linkMap = new OptionItem('OPTIONS.LINK_MAP', 'link', (node) =>
            this.dialogs.openCreateMapLinkDialog({ node: this.getObjects(node, data)[0] }),
        );
        linkMap.constrains = [
            Constrain.NoBulk,
            Constrain.HomeRepository,
            Constrain.User,
            Constrain.Directory,
        ];
        linkMap.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_MAP_LINK];
        linkMap.toolpermissionsMode = HideMode.Hide;
        linkMap.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
        linkMap.permissionsMode = HideMode.Hide;
        linkMap.group = DefaultGroups.FileOperations;
        linkMap.priority = 5;

        /**
         const cut = new OptionItem('OPTIONS.CUT', 'content_cut', (node: Node) => this.cutCopyNode(node, false));
         cut.isSeperate = true;
         cut.isEnabled = this.nodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE)
         && (this.root === 'MY_FILES' || this.root === 'SHARED_FILES');
         options.push(cut);
         options.push(new OptionItem('OPTIONS.COPY', 'content_copy', (node: Node) => this.cutCopyNode(node, true)));
         */
        const cutNodes = new OptionItem('OPTIONS.CUT', 'content_cut', (node) =>
            this.cutCopyNode(data, node, false),
        );
        cutNodes.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
        cutNodes.constrains = [Constrain.HomeRepository, Constrain.User];
        cutNodes.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
        cutNodes.permissions = [RestConstants.ACCESS_DELETE];
        cutNodes.permissionsMode = HideMode.Disable;
        cutNodes.keyboardShortcut = {
            keyCode: 'KeyX',
            modifiers: ['Ctrl/Cmd'],
        };
        cutNodes.group = DefaultGroups.FileOperations;
        cutNodes.priority = 10;

        const copyNodes = new OptionItem('OPTIONS.COPY', 'content_copy', (node) =>
            this.cutCopyNode(data, node, true),
        );
        // do not allow copy of map links if tp is missing
        copyNodes.customEnabledCallback = async (node) =>
            node.every((n) => !n.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION)) &&
            (node?.some((n) => this.getTypeSingle(n) === ElementType.MapRef)
                ? this.connector.hasToolPermissionInstant(
                      RestConstants.TOOLPERMISSION_CREATE_MAP_LINK,
                  )
                : true);

        copyNodes.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
        copyNodes.constrains = [Constrain.HomeRepository, Constrain.User];
        copyNodes.scopes = [Scope.WorkspaceList, Scope.WorkspaceTree];
        copyNodes.keyboardShortcut = {
            keyCode: 'KeyC',
            modifiers: ['Ctrl/Cmd'],
        };
        copyNodes.group = DefaultGroups.FileOperations;
        copyNodes.priority = 20;

        const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
            this.pasteNode(components, data),
        );
        pasteNodes.elementType = [ElementType.NoneOrUnknown];
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
        pasteNodes.scopes = [Scope.WorkspaceList];
        pasteNodes.keyboardShortcut = {
            keyCode: 'KeyV',
            modifiers: ['Ctrl/Cmd'],
        };
        pasteNodes.group = DefaultGroups.FileOperations;

        const pasteNodeIntoFolder = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
            this.pasteNode(
                components,
                {
                    parent: this.getObjects(node, data)[0],
                    scope: null,
                },
                false,
            ),
        );
        pasteNodeIntoFolder.elementType = [ElementType.Node];
        pasteNodeIntoFolder.constrains = [
            Constrain.ClipboardContent,
            Constrain.Directory,
            Constrain.NoBulk,
            Constrain.AddObjects,
            Constrain.User,
        ];
        pasteNodeIntoFolder.toolpermissions = [
            RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
            RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
        ];
        pasteNodes.scopes = [Scope.WorkspaceList];
        pasteNodes.keyboardShortcut = {
            keyCode: 'KeyV',
            modifiers: ['Ctrl/Cmd'],
        };
        pasteNodeIntoFolder.group = DefaultGroups.Primary;

        const deleteNode = new OptionItem('OPTIONS.DELETE', 'delete', (object) => {
            void this.dialogs.openDeleteNodesDialog({ nodes: this.getObjects(object, data) });
        });
        deleteNode.elementType = [ElementType.Node, ElementType.SavedSearch, ElementType.MapRef];
        deleteNode.constrains = [
            Constrain.HomeRepository,
            Constrain.NoCollectionReference,
            Constrain.User,
        ];
        deleteNode.permissions = [RestConstants.PERMISSION_DELETE];
        deleteNode.permissionsMode = HideMode.Hide;
        deleteNode.keyboardShortcut = {
            keyCode: 'Delete',
        };
        deleteNode.group = DefaultGroups.Delete;
        deleteNode.color = 'warn';
        deleteNode.priority = 10;

        const unblockNode = new OptionItem('OPTIONS.UNBLOCK_IMPORT', 'sync', async (object) => {
            const objects = await this.getObjectsAsync(object, data, true);
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'WORKSPACE.UNBLOCK_TITLE',
                message: 'WORKSPACE.UNBLOCK_MESSAGE',
                buttons: OK_OR_CANCEL,
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'OK') {
                    this.unblockImportedNodes(objects);
                }
            });
        });
        unblockNode.elementType = [ElementType.NodeBlockedImport];
        unblockNode.constrains = [Constrain.HomeRepository, Constrain.User];
        unblockNode.permissions = [RestConstants.PERMISSION_DELETE];
        unblockNode.permissionsMode = HideMode.Hide;
        unblockNode.group = DefaultGroups.Edit;
        unblockNode.priority = 10;

        const removeNodeRef = new OptionItem('OPTIONS.REMOVE_REF', 'do_not_disturb_on', (object) =>
            this.removeFromCollection(this.getObjects(object, data), components, data),
        );
        removeNodeRef.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        removeNodeRef.constrains = [
            Constrain.HomeRepository,
            Constrain.CollectionReference,
            Constrain.User,
        ];
        removeNodeRef.permissions = [RestConstants.PERMISSION_DELETE];
        removeNodeRef.permissionsMode = HideMode.Disable;
        removeNodeRef.scopes = [Scope.CollectionsReferences, Scope.Render];
        removeNodeRef.group = DefaultGroups.Delete;
        removeNodeRef.color = 'warn';
        removeNodeRef.priority = 20;

        /*
        let report = new OptionItem('NODE_REPORT.OPTION', 'flag', (node: Node) => this.nodeReport=this.getCurrentNode(node));
        report.customShowCallback=(node:Node)=>{
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
          return RestNetworkService.allFromHomeRepo(n,this.allRepositories);
        }
        options.push(report);
         */
        const reportNode = new OptionItem('OPTIONS.NODE_REPORT', 'flag', (node) =>
            this.dialogs.openNodeReportDialog({
                node: this.getObjects(node, data)[0],
                mode: 'NODE_REPORT',
                showOptions: true,
            }),
        );
        reportNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        reportNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];
        reportNode.scopes = [Scope.Search, Scope.CollectionsReferences, Scope.Render];
        reportNode.customShowCallback = (objects) =>
            this.configService.get('nodeReport', false).pipe(first()).toPromise();
        reportNode.group = DefaultGroups.View;
        reportNode.priority = 60;

        const qrCodeNode = new OptionItem('OPTIONS.QR_CODE', 'edu-qr_code', (node) => {
            node = this.getObjects(node, data)[0];
            void this.dialogs.openQrDialog({ node });
        });
        qrCodeNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        qrCodeNode.constrains = [Constrain.NoBulk];
        qrCodeNode.scopes = [Scope.Render, Scope.CollectionsCollection];
        qrCodeNode.group = DefaultGroups.View;
        qrCodeNode.priority = 70;

        const embedNode = new OptionItem('OPTIONS.EMBED', 'perm_media', (node) => {
            node = this.getObjects(node, data)[0];
            void this.dialogs.openNodeEmbedDialog({ node });
        });
        embedNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        embedNode.constrains = [Constrain.NoBulk, Constrain.HomeRepository];
        embedNode.scopes = [Scope.Render];
        embedNode.group = DefaultGroups.View;
        embedNode.priority = 80;

        const relationNode = new OptionItem('OPTIONS.RELATIONS', 'swap_horiz', async (node) => {
            const nodes = await this.getObjectsAsync(node, data, true);
            void this.dialogs.openNodeRelationsDialog({ node: nodes[0] });
        });
        relationNode.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        relationNode.constrains = [Constrain.NoBulk, Constrain.User];
        relationNode.scopes = [Scope.Render];
        relationNode.toolpermissions = [RestConstants.TOOLPERMISSION_MANAGE_RELATIONS];
        relationNode.permissions = [RestConstants.PERMISSION_WRITE];
        relationNode.permissionsRightMode = NodesRightMode.Effective;
        relationNode.group = DefaultGroups.Edit;
        relationNode.priority = 70;

        const topicPage = new OptionItem('OPTIONS.SHOW_TOPIC_PAGE', 'menu_book', async (node) =>
            UIHelper.goToTopicPage(this.router, (await this.getObjectsAsync(node, data, false))[0]),
        );
        topicPage.constrains = [
            Constrain.HomeRepository,
            Constrain.Collections,
            Constrain.NoBulk,
            Constrain.User,
        ];
        topicPage.customShowCallback = async (objects) => {
            if (
                this.nodeHelper.getNodesRight(
                    objects as Node[],
                    RestConstants.ACCESS_WRITE,
                    NodesRightMode.Effective,
                )
            ) {
                return true;
            }
            if (objects[0].ref.id === ROOT) {
                return false;
            }
            try {
                if (objects[0].properties?.[RestConstants.CCM_PROP_PAGE_CONFIG_REF]?.[0]) {
                    return true;
                }
                return (
                    await firstValueFrom(
                        this.nodeService.getParents(objects[0].ref.id, { fullPath: false }),
                    )
                ).nodes.some(
                    (n) => n.properties[RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_REF]?.[0],
                );
            } catch (e) {
                e.preventDefault();
                return false;
            }
        };
        topicPage.showAsAction = true;
        topicPage.group = DefaultGroups.View;
        topicPage.priority = 15;

        const submitAssignment = new OptionItem('OPTIONS.ASSIGNMENT_SUBMIT', 'send', (object) =>
            this.uiService.goToAssignment(this.getObjects(object, data)[0], 'submit'),
        );
        submitAssignment.customShowCallback = async (objects) => {
            const assignment = objects[0] as Assignment;
            return (
                assignment.type === 'SUBMISSION' &&
                new AssignmentPipe().transform(assignment, {
                    mode: 'permissions',
                }) !== 'COORDINATOR'
            );
        };
        submitAssignment.elementType = [ElementType.Assignment];
        submitAssignment.constrains = [Constrain.NoBulk, Constrain.User];
        submitAssignment.showAsAction = true;
        submitAssignment.group = DefaultGroups.View;
        submitAssignment.priority = 5;

        const viewAssignmentSubmission = new OptionItem(
            'OPTIONS.ASSIGNMENT_SUBMISSION',
            'inbox',
            (object) =>
                this.uiService.goToAssignment(this.getObjects(object, data)[0], 'submissions'),
        );
        viewAssignmentSubmission.customShowCallback = async (objects) => {
            const assignment = objects[0] as Assignment;
            return (
                assignment.type === 'SUBMISSION' &&
                new AssignmentPipe().transform(assignment, {
                    mode: 'permissions',
                }) === 'COORDINATOR'
            );
        };
        viewAssignmentSubmission.elementType = [ElementType.Assignment];
        viewAssignmentSubmission.constrains = [Constrain.NoBulk, Constrain.User];
        viewAssignmentSubmission.showAsAction = true;
        viewAssignmentSubmission.group = DefaultGroups.View;
        viewAssignmentSubmission.priority = 5;

        const editAssignment = new OptionItem('OPTIONS.ASSIGNMENT_EDIT', 'edit', (object) =>
            this.uiService.goToAssignment(this.getObjects(object, data)[0], 'edit'),
        );
        editAssignment.elementType = [ElementType.Assignment];
        editAssignment.constrains = [Constrain.NoBulk, Constrain.User];
        editAssignment.customShowCallback = async (objects) => {
            const assignment = objects[0] as Assignment;
            // user has access to permissions => so it's a coordinator
            return (
                assignment.type === 'SUBMISSION' &&
                !['FINISHED', 'CANCELED'].includes(assignment.status) &&
                new AssignmentPipe().transform(assignment, {
                    mode: 'permissions',
                }) === 'COORDINATOR'
            );
        };
        editAssignment.showAsAction = true;
        editAssignment.group = DefaultGroups.Edit;
        editAssignment.priority = 5;

        const cancelAssignment = new OptionItem(
            'OPTIONS.ASSIGNMENT_CANCEL',
            'cancel',
            async (object) => {
                const assignment = this.getObjects(object, data)[0] as Assignment;
                const dialogRef = await this.dialogs.openGenericDialog({
                    title: 'OPTIONS.ASSIGNMENT_CANCEL',
                    message: 'OPTIONS.ASSIGNMENT_CANCEL_CONFIRM',
                    buttons: OK_OR_CANCEL,
                });
                dialogRef.afterClosed().subscribe((response) => {
                    if (response === 'OK') {
                        this.assignmentV1Service
                            .createOrUpdateAssignment1({
                                assignmentId: assignment.ref.id,
                                status: 'CANCELED',
                            })
                            .subscribe(() =>
                                this.localEvents.nodesChanged.emit([assignment as any]),
                            );
                    }
                });
            },
        );
        cancelAssignment.elementType = [ElementType.Assignment];
        cancelAssignment.constrains = [Constrain.NoBulk, Constrain.User];
        cancelAssignment.customShowCallback = async (objects) => {
            const assignment = objects[0] as Assignment;
            return (
                assignment.type === 'SUBMISSION' &&
                !['FINISHED', 'CANCELED'].includes(assignment.status) &&
                new AssignmentPipe().transform(assignment, { mode: 'permissions' }) ===
                    'COORDINATOR'
            );
        };
        cancelAssignment.group = DefaultGroups.Delete;
        cancelAssignment.color = 'warn';
        cancelAssignment.priority = 10;

        const finishAssignment = new OptionItem(
            'OPTIONS.ASSIGNMENT_FINISH',
            'done_all',
            async (object) => {
                const assignment = this.getObjects(object, data)[0] as Assignment;
                const dialogRef = await this.dialogs.openGenericDialog({
                    title: 'OPTIONS.ASSIGNMENT_FINISH',
                    message: 'OPTIONS.ASSIGNMENT_FINISH_CONFIRM',
                    buttons: OK_OR_CANCEL,
                });
                dialogRef.afterClosed().subscribe((response) => {
                    if (response === 'OK') {
                        this.assignmentV1Service
                            .createOrUpdateAssignment1({
                                assignmentId: assignment.ref.id,
                                status: 'FINISHED',
                            })
                            .subscribe(() =>
                                this.localEvents.nodesChanged.emit([assignment as any]),
                            );
                    }
                });
            },
        );
        finishAssignment.elementType = [ElementType.Assignment];
        finishAssignment.constrains = [Constrain.NoBulk, Constrain.User];
        finishAssignment.customShowCallback = async (objects) => {
            const assignment = objects[0] as Assignment;
            return (
                assignment.type === 'SUBMISSION' &&
                !['FINISHED', 'CANCELED'].includes(assignment.status) &&
                new AssignmentPipe().transform(assignment, { mode: 'permissions' }) ===
                    'COORDINATOR'
            );
        };
        finishAssignment.group = DefaultGroups.Edit;
        finishAssignment.priority = 10;

        const editCollection = new OptionItem('OPTIONS.COLLECTION_EDIT', 'edit', (object) =>
            this.editCollection(this.getObjects(object, data)[0]),
        );
        editCollection.constrains = [
            Constrain.HomeRepository,
            Constrain.Collections,
            Constrain.NoBulk,
            Constrain.User,
        ];
        editCollection.permissions = [RestConstants.ACCESS_WRITE];
        editCollection.permissionsMode = HideMode.Hide;
        editCollection.showAsAction = true;
        editCollection.group = DefaultGroups.Edit;
        editCollection.priority = 5;

        const pinCollection = new OptionItem('OPTIONS.COLLECTION_PIN', 'edu-pin', (object) =>
            this.dialogs.openPinnedCollectionsDialog({
                collection: this.getObjects(object, data)[0],
            }),
        );
        pinCollection.constrains = [
            Constrain.HomeRepository,
            Constrain.Collections,
            Constrain.NoBulk,
            Constrain.User,
        ];
        pinCollection.permissions = [RestConstants.ACCESS_WRITE];
        pinCollection.permissionsMode = HideMode.Hide;
        pinCollection.toolpermissions = [RestConstants.TOOLPERMISSION_COLLECTION_PINNING];
        pinCollection.group = DefaultGroups.Edit;
        pinCollection.priority = 20;

        const feedbackMaterial = new OptionItem(
            'OPTIONS.MATERIAL_FEEDBACK',
            'chat_bubble',
            (object) =>
                this.dialogs.openSendFeedbackDialog({ node: this.getObjects(object, data)[0] }),
        );
        feedbackMaterial.constrains = [Constrain.HomeRepository, Constrain.Files, Constrain.NoBulk];
        feedbackMaterial.permissions = [RestConstants.PERMISSION_FEEDBACK];
        feedbackMaterial.permissionsRightMode = NodesRightMode.Effective;
        feedbackMaterial.scopes = [Scope.Render];
        feedbackMaterial.permissionsMode = HideMode.Hide;
        feedbackMaterial.toolpermissions = [RestConstants.TOOLPERMISSION_MATERIAL_FEEDBACK];
        feedbackMaterial.group = DefaultGroups.View;
        feedbackMaterial.priority = 15;
        // feedback is only shown for non-managers
        feedbackMaterial.customShowCallback = async (objects) =>
            !this.nodeHelper.getNodesRight(
                objects as Node[],
                RestConstants.ACCESS_WRITE,
                NodesRightMode.Effective,
            );

        const feedbackMaterialView = new OptionItem(
            'OPTIONS.MATERIAL_FEEDBACK_VIEW',
            'speaker_notes',
            (object) => (management.materialViewFeedback = this.getObjects(object, data)[0]),
        );
        feedbackMaterialView.constrains = [
            Constrain.HomeRepository,
            Constrain.Files,
            Constrain.NoBulk,
            Constrain.User,
        ];
        feedbackMaterialView.scopes = [Scope.Render];
        feedbackMaterialView.permissions = [RestConstants.ACCESS_DELETE];
        feedbackMaterialView.permissionsRightMode = NodesRightMode.Effective;
        feedbackMaterialView.permissionsMode = HideMode.Hide;
        feedbackMaterialView.toolpermissions = [RestConstants.TOOLPERMISSION_MATERIAL_FEEDBACK];
        feedbackMaterialView.group = DefaultGroups.View;
        feedbackMaterialView.priority = 20;

        const setDisplayType = (viewType: number, emit = true) => {
            switch (viewType) {
                case NodeEntriesDisplayType.Table:
                    components.list.setDisplayType(NodeEntriesDisplayType.Table);
                    toggleViewType.name = 'OPTIONS.SWITCH_TO_CARDS_VIEW';
                    toggleViewType.icon = 'view_module';
                    break;
                case NodeEntriesDisplayType.Grid:
                    components.list.setDisplayType(NodeEntriesDisplayType.Grid);
                    toggleViewType.name = 'OPTIONS.SWITCH_TO_LIST_VIEW';
                    toggleViewType.icon = 'list';
                    break;
            }
            if (emit) {
                this.displayTypeChanged.emit(components.list.getDisplayType());
            }
        };
        // enabled = table
        // disabled = grid
        const toggleViewType = new OptionItemToggle(
            {
                enabled: 'OPTIONS.SWITCH_TO_CARDS_VIEW',
                disabled: 'OPTIONS.SWITCH_TO_LIST_VIEW',
            },
            {
                enabled: 'view_module',
                disabled: 'list',
            },
            components?.list?.getDisplayType() === NodeEntriesDisplayType.Table,
            () => {
                switch (components.list.getDisplayType()) {
                    case NodeEntriesDisplayType.Table:
                        setDisplayType(NodeEntriesDisplayType.Grid);
                        break;
                    case NodeEntriesDisplayType.Grid:
                        setDisplayType(NodeEntriesDisplayType.Table);
                        break;
                }
            },
        );
        toggleViewType.scopes = [Scope.WorkspaceList, Scope.Search, Scope.CollectionsReferences];
        toggleViewType.constrains = [];
        toggleViewType.group = DefaultGroups.Toggles;
        toggleViewType.elementType = [];
        toggleViewType.priority = 15;
        toggleViewType.togglePosition = 'before';

        /*
        const reorder = new OptionItem('OPTIONS.LIST_SETTINGS', 'settings', (node: Node) => this.reorderDialog = true);
        reorder.isToggle = true;
        options.push(reorder);
        return options;
         */
        /*
        const configureList = new OptionItem('OPTIONS.LIST_SETTINGS', 'settings', (node: Node) =>
            this.list.showReorderColumnsDialog()
        );
        configureList.scopes = [Scope.WorkspaceList];
        configureList.constrains = [Constrain.NoSelection, Constrain.User];
        configureList.group = DefaultGroups.Toggles;
        configureList.elementType = [ElementType.NoneOrUnknown];
        configureList.priority = 20;
        configureList.isToggle = true;
         */

        const infoVersions = new OptionItem('OPTIONS.WORKSPACE_METADATA', 'info', (node: Node) => {
            this.editorialSidebarService.showOption({
                option: 'WORKSPACE_METADATA',
                trap: false,
            });
        });

        infoVersions.scopes = [Scope.WorkspaceList, Scope.Search, Scope.EditorialPage];
        infoVersions.group = DefaultGroups.View;
        infoVersions.priority = 20;

        const registerSelectionChange = (list: ListEventInterface<any>) => {
            const updateVisibility = () => {
                toggleSelection.isToggleVisible =
                    list?.getDisplayType() !== NodeEntriesDisplayType.Table;
            };
            const updateSelectionState = (selection: SelectionModel<any>) => {
                toggleSelection.toggleState = !selection?.isEmpty();
            };
            list?.getSelection()
                .changed.pipe(map((s) => s.source))
                .subscribe(updateSelectionState);
            list?.onDisplayTypeChange().subscribe(updateVisibility);
            updateSelectionState(list?.getSelection());
            updateVisibility();
        };
        const toggleSelection = new OptionItemToggle(
            {
                enabled: 'OPTIONS.DESELECT',
                disabled: 'OPTIONS.SELECT_ALL',
            },
            {
                enabled: 'deselect',
                disabled: 'select_all',
            },
            !components?.list?.getSelection()?.isEmpty(),
            () => {
                if (components.list?.getSelection()?.isEmpty()) {
                    components.list?.selectAll();
                } else {
                    components.list?.getSelection().clear();
                }
            },
        );
        registerSelectionChange(components?.list);
        toggleSelection.scopes = [Scope.WorkspaceList, Scope.Search, Scope.CollectionsReferences];
        toggleSelection.group = DefaultGroups.Toggles;
        toggleSelection.customShowCallback = async () => data?.allObjects?.length > 0;
        toggleSelection.elementType = [];
        toggleSelection.priority = 10;
        toggleSelection.togglePosition = 'before';

        options.push(applyNode);
        options.push(debugNode);
        options.push(acceptProposal);
        options.push(declineProposal);
        options.push(editRevocation);
        options.push(openOriginalNode);
        options.push(openParentNode);
        options.push(openNode);
        options.push(editConnectorNode);
        options.push(bookmarkNode);
        options.push(shortcutNode);
        options.push(topicPage);
        options.push(editCollection);
        options.push(submitAssignment);
        options.push(viewAssignmentSubmission);
        options.push(editAssignment);
        options.push(cancelAssignment);
        options.push(finishAssignment);
        options.push(pinCollection);
        options.push(feedbackMaterial);
        options.push(feedbackMaterialView);
        options.push(simpleEditNode);
        options.push(editNode);
        options.push(infoVersions);
        options.push(sortInto);
        // add to collection
        //options.push(addNodeToCollection);
        options.push(addNodeToLTIPlatform);
        // create variant
        options.push(createNodeVariant);
        options.push(templateNode);
        options.push(inviteNode);
        options.push(streamNode);
        options.push(licenseNode);
        options.push(contributorNode);
        options.push(workflowNode);
        options.push(downloadNode);
        options.push(downloadNodeSafe);
        options.push(downloadMetadataNode);
        options.push(qrCodeNode);
        options.push(relationNode);
        options.push(embedNode);
        options.push(linkMap);
        options.push(cutNodes);
        options.push(copyNodes);
        options.push(pasteNodes);
        options.push(pasteNodeIntoFolder);
        options.push(deleteNode);
        options.push(revokeNode);
        options.push(unblockNode);
        options.push(removeNodeRef);
        options.push(reportNode);
        options.push(toggleViewType);
        options.push(toggleSelection);

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
            (object) => this.nodeHelper.downloadNodes(this.getObjects(object, data)),
        );
        downloadNode.elementType = OptionsHelperService.DownloadElementTypes;
        downloadNode.constrains = [Constrain.Files];
        downloadNode.group = DefaultGroups.View;
        // downloadNode.key = 'D';
        downloadNode.priority = 40;
        downloadNode.customShowCallback = async (nodes) => {
            return (
                nodes.some((n) =>
                    n.properties?.[RestConstants.CCM_PROP_EDUSCOPENAME]?.includes(
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

    private async revokeNode(object: any, data: OptionData) {
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

    private bookmarkNodes(nodes: Node[]) {
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

    private goToWorkspace(node: Node | any) {
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
            const originals = await observableForkJoin(
                nodes.map((n) => {
                    if (n.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                        return this.nodeServiceLegacy.getNodeMetadata(
                            n.properties[RestConstants.CCM_PROP_IO_ORIGINAL][0],
                            [RestConstants.ALL],
                        );
                    } else if (n.type === RestConstants.CCM_TYPE_COLLECTION_PROPOSAL) {
                        return this.nodeServiceLegacy.getNodeMetadata(
                            RestHelper.removeSpacesStoreRef(
                                n.properties[RestConstants.CCM_PROP_COLLECTION_PROPOSAL_TARGET][0],
                            ),
                            [RestConstants.ALL],
                        );
                    } else {
                        return of({
                            node: n,
                        });
                    }
                }),
            ).toPromise();
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
    private async removeFromCollection(
        nodes: Node[],
        components: OptionsHelperComponents,
        data: OptionData,
    ) {
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

    private editCollection(object: Node | any) {
        this.uiService.goToCollection(object, 'edit');
    }

    private unblockImportedNodes(nodes: Node[]) {
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
