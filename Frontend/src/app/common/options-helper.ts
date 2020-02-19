import {RestNetworkService} from '../core-module/rest/services/rest-network.service';
import {RestConnectorsService} from '../core-module/rest/services/rest-connectors.service';
import {RestConstants} from '../core-module/rest/rest-constants';
import {ListTableComponent} from '../core-ui-module/components/list-table/list-table.component';
import {ActionbarComponent} from './ui/actionbar/actionbar.component';
import {Constrain, DefaultGroups, ElementType, HideMode, KeyCombination, OptionItem, Scope, Target} from '../core-ui-module/option-item';
import {UIHelper} from '../core-ui-module/ui-helper';
import {UIService} from '../core-module/rest/services/ui.service';
import {WorkspaceManagementDialogsComponent} from '../modules/management-dialogs/management-dialogs.component';
import {NodeHelper} from '../core-ui-module/node-helper';
import {Connector, Filetype, Node, NodesRightMode, NodeWrapper} from '../core-module/rest/data-object';
import {Helper} from '../core-module/rest/helper';
import {ClipboardObject, TemporaryStorageService} from '../core-module/rest/services/temporary-storage.service';
import {BridgeService} from '../core-bridge-module/bridge.service';
import {MessageType} from '../core-module/ui/message-type';
import {Injectable} from '@angular/core';
import {CardComponent} from '../core-ui-module/components/card/card.component';
import {fromEvent} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {RestNodeService} from '../core-module/rest/services/rest-node.service';
import {ConfigurationService, FrameEventsService, RestConnectorService, RestHelper, RestIamService} from '../core-module/core.module';
import {MainNavComponent} from './ui/main-nav/main-nav.component';
import {Toast} from '../core-ui-module/toast';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute, Params, Router} from '@angular/router';

@Injectable()
export class OptionsHelperService {
    private appleCmd: boolean;
    private globalOptions: OptionItem[];
    private list: ListTableComponent;
    private mainNav: MainNavComponent;
    private queryParams: Params;
    private data: OptionData;

    handleKeyboardEventUp(event: any) {
        if (event.keyCode === 91 || event.keyCode === 93) {
            this.appleCmd = false;
        }
    }
    handleKeyboardEvent(event: any) {
        if (event.keyCode === 91 || event.keyCode === 93) {
            this.appleCmd = true;
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        // do nothing if a modal dialog is still open
        if (CardComponent.getNumberOfOpenCards() > 0) {
            return;
        }
        if (this.globalOptions) {
            const option = this.globalOptions.filter((o: OptionItem) => {
                if (o.key !== event.code) {
                    return false;
                }
                if(o.keyCombination) {
                    if (o.keyCombination.indexOf(KeyCombination.CtrlOrAppleCmd) !== -1) {
                       if (!(event.ctrlKey || this.appleCmd)) {
                           return false;
                       }
                    }
                }
                return true;
            });
            if (option.length === 1) {
                console.log('key', event.code, option);
                option[0].callback(null);
                event.preventDefault();
                event.stopPropagation();
            }
        }
        }

    constructor(
        private networkService: RestNetworkService,
        private connector: RestConnectorService,
        private connectors: RestConnectorsService,
        private iamService: RestIamService,
        private router: Router,
        private route: ActivatedRoute,
        private event: FrameEventsService,
        private http: HttpClient,
        private ui: UIService,
        private toast: Toast,
        private translate: TranslateService,
        private nodeService: RestNodeService,
        private config: ConfigurationService,
        private storage: TemporaryStorageService,
        private bridge: BridgeService,
    ) {
        this.route.queryParams.subscribe((queryParams) => this.queryParams = queryParams);
        // @HostListener decorator unfortunately does not work in services
        fromEvent(document, 'keyup').subscribe((event) =>
            this.handleKeyboardEventUp(event)
        );
        fromEvent(document, 'keydown').subscribe((event) =>
            this.handleKeyboardEvent(event)
        );
    }
    private cutCopyNode(node: Node, copy: boolean) {
        let list = this.getObjects(node);
        if (!list || !list.length) {
            return;
        }
        list = Helper.deepCopy(list);
        const clip: ClipboardObject = { sourceNode: this.data.parent, nodes: list, copy };
        console.log(clip);
        this.storage.set('workspace_clipboard', clip);
        this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.CUT_COPY', { count: list.length });
    }
    private pasteNode(nodes: Node[] = []) {
        const clip = (this.storage.get('workspace_clipboard') as ClipboardObject);
        if (!this.canAddObjects()) {
            return;
        }
        if (nodes.length === clip.nodes.length) {
            this.bridge.closeModalDialog();
            this.storage.remove('workspace_clipboard');
            const info: any = {
                from: clip.sourceNode ? clip.sourceNode.name : this.translate.instant('WORKSPACE.COPY_SEARCH'),
                to: this.data.parent.name,
                count: clip.nodes.length,
                mode: this.translate.instant('WORKSPACE.' + (clip.copy ? 'PASTE_COPY' : 'PASTE_MOVE'))
            };
            this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.PASTE', info);
            this.addVirtualObjects(nodes);
            return;
        }
        this.bridge.showProgressDialog();
        const target = this.data.parent.ref.id;
        const source = clip.nodes[nodes.length].ref.id;
        if (clip.copy) {
            this.nodeService.copyNode(target, source).subscribe(
                (data: NodeWrapper) => this.pasteNode(nodes.concat(data.node)),
                (error: any) => {
                    NodeHelper.handleNodeError(this.bridge, clip.nodes[nodes.length].name, error);
                    this.bridge.closeModalDialog();
                });
        }
        else {
            this.nodeService.moveNode(target, source).subscribe(
                (data: NodeWrapper) => this.pasteNode(nodes.concat(data.node)),
                (error: any) => {
                    NodeHelper.handleNodeError(this.bridge, clip.nodes[nodes.length].name, error);
                    this.bridge.closeModalDialog();
                }
            );
        }

    }
    /**
     * shortcut to simply disable all options on the given compoennts
     * @param mainNav
     * @param actionbar
     * @param list
     */
    clearComponents(mainNav: MainNavComponent,
                      actionbar: ActionbarComponent,
                      list: ListTableComponent = null) {
        if (list) {
            list.options = [];
            list.dropdownOptions = [];
        }
        if (actionbar) {
            actionbar.options = [];
        }
    }
    /**
     * refresh all bound components with available menu options
     */
    refreshComponents(listener: OptionsListener,
                      mainNav: MainNavComponent,
                      actionbar: ActionbarComponent = null,
                      list: ListTableComponent = null) {

        mainNav.management.onRefresh.subscribe((nodes: void | Node[]) =>
            listener.onRefresh(nodes)
        );
        mainNav.management.onDelete.subscribe(
            (result: { error: boolean; count: number; }) => listener.onDelete(result)
        );

        this.list = list;
        this.mainNav = mainNav;
        this.globalOptions = this.getAvailableOptions(mainNav, Target.Actionbar);
        if (list) {
            list.options = this.getAvailableOptions(mainNav, Target.List);
            list.dropdownOptions = this.getAvailableOptions(mainNav, Target.ListDropdown);
        }
        if (actionbar) {
            actionbar.options = this.globalOptions;
        }
    }
    private isOptionEnabled(option: OptionItem, objects: Node[]|any) {
        if(option.permissionsMode === HideMode.Disable &&
            option.permissions && !this.validatePermissions(option, objects)) {
            return false;
        }
        if(option.customEnabledCallback) {
            return option.customEnabledCallback(objects);
        }
        return true;
    }

    private getAvailableOptions(mainNav: MainNavComponent, target: Target) {
        let objects: Node[]|any[];
        if (target === Target.List) {
            objects = this.data.allObjects && this.data.allObjects.length ? [this.data.allObjects[0]] : null;
        } else if (target === Target.Actionbar) {
            objects = this.data.selectedObjects || [this.data.activeObject];
        } else if (target === Target.ListDropdown) {
            if (this.data.activeObject) {
                objects = [this.data.activeObject];
                console.log('active object', objects);
            } else {
                return null;
            }
        }
        let options = this.prepareOptions(mainNav.management, objects);
        options = this.applyExternalOptions(options);
        this.handleCallbacks(options, objects);
        const custom = this.config.instant('customOptions');
        NodeHelper.applyCustomNodeOptions(this.toast, this.http, this.connector, custom, this.data.allObjects, objects, options);
        // do pre-handle callback options for dropdown + actionbar
        if (target === Target.Actionbar || target === Target.ListDropdown) {
            options = options.filter((o) => o.showCallback());
            options.filter((o) => !o.enabledCallback()).forEach((o) => o.isEnabled = false);
        }
        options = this.sortOptionsByGroup(options);
        return (UIHelper.filterValidOptions(this.ui, options) as OptionItem[]);
    }
    private isOptionAvailable(option: OptionItem, objects: Node[]|any[]) {
        if (this.getType(objects) !== option.elementType) {
            console.log('types not matching', this.getType(objects), option);
            return false;
        }
        if(option.scopes) {
            if (option.scopes.indexOf(this.data.scope) === -1) {
                return false;
            }
        }
        if (option.customShowCallback) {
           if (option.customShowCallback(objects) === false) {
               console.log('customShowCallback  was false', option);
               return false;
           }
        }
        if (option.permissions != null && option.permissionsMode === HideMode.Hide) {
           if (!this.validatePermissions(option, objects)) {
               console.log('permissions missing', option.permissions);
               return false;
           }
        }
        if (option.constrains != null) {
           if (option.constrains.indexOf(Constrain.NoCollectionReference) !== -1) {
               if (objects.filter((o) => o.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1).length > 0) {
                   console.log('no collection reference', option);
                   return false;
               }
           }
           if (option.constrains.indexOf(Constrain.NoBulk) !== -1) {
               if (objects.length > 1) {
                   console.log('bulk', option);
                   return false;
               }
           }
           if (option.constrains.indexOf(Constrain.Directory) !== -1) {
                if (objects.filter((o) => o.isDirectory === false).length > 0) {
                    console.log('no directory', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.Files) !== -1) {
                if (objects.filter((o) => o.isDirectory === true).length > 0) {
                    console.log('no file', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.AdminOrDebug) !== -1) {
                if (!this.connectors.getRestConnector().getCurrentLogin().isAdmin &&
                    !(window as any).esDebug === true) {
                    console.log('no admin', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.User) !== -1) {
                if (this.connectors.getRestConnector().getCurrentLogin() &&
                    this.connectors.getRestConnector().getCurrentLogin().statusCode !== RestConstants.STATUS_CODE_OK) {
                    console.log('no user', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.NoSelection) !== -1) {
                if (objects && objects.length) {
                    console.log('selection', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.ClipboardContent) !== -1) {
                if (this.storage.get('workspace_clipboard') == null) {
                    console.log('no clipboard', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.AddObjects) !== -1) {
                if (!this.canAddObjects()) {
                    console.log('no add objects', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.HomeRepository) !== -1) {
                if(!RestNetworkService.allFromHomeRepo(objects)) {
                    console.log('not all from home repo', option);
                    return false;
                }
           }
           if (option.constrains.indexOf(Constrain.ReurlMode) !== -1) {
                if(!this.queryParams.reurl) {
                    console.log('no reurl', option);
                    return false;
                }
           }
        }
        return true;
    }

    private hasSelection() {
        return this.data.selectedObjects && this.data.selectedObjects.length;
    }
    private getType(objects: Node[] | any[]) {
        // @ TODO may combine for all
        if (objects && objects[0]) {
            if (objects[0].ref) {
                return ElementType.Node;
            }
        }
        return ElementType.Unknown;
    }

    private validatePermissions(option: OptionItem, objects: Node[] | any[]) {
        return option.permissions.filter((p) =>
            NodeHelper.getNodesRight(objects, p, option.permissionsRightMode) === false
        ).length === 0;
    }

    private prepareOptions(management: WorkspaceManagementDialogsComponent, objects: Node[] | any[]) {
        const options = [];

        /*
        let apply=new OptionItem('APPLY', 'redo', (node: Node) => NodeHelper.addNodeToLms(this.router,this.temporaryStorageService,ActionbarHelperService.getNodes(this.selection,node)[0],this.searchService.reurl));
      apply.enabledCallback=((node:Node)=> {
        return NodeHelper.getNodesRight([node],RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
      });
      if(fromList || (nodes && nodes.length==1))
        options.push(apply);
      return options;
         */

        const applyNode = new OptionItem('APPLY', 'redo', (object) =>
            NodeHelper.addNodeToLms(this.router, this.storage, this.getObjects(object)[0], this.queryParams.reurl)
        );

        applyNode.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        applyNode.permissionsRightMode = NodesRightMode.Original;
        applyNode.permissionsMode = HideMode.Disable;
        applyNode.constrains = [Constrain.NoBulk, Constrain.ReurlMode];
        applyNode.showAsAction = true;
        applyNode.showAlways = true;
        applyNode.group = DefaultGroups.Primary;
        applyNode.priority = 10;
        applyNode.customShowCallback = ((nodes) => {
            return (this.queryParams.applyDirectories === 'true' ||
                (nodes && !nodes[0].isDirectory));
        });

        /*
       if(nodes && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)==-1) {
       option = new OptionItem("OPTIONS.INVITE", "group_add", callback);
       option.isSeperate = NodeHelper.allFiles(nodes);
       option.showAsAction = true;
       option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
     }
        */
        const debugNode = new OptionItem('OPTIONS.DEBUG', 'build', (object) =>
            management.nodeDebug = this.getObjects(object)[0],
        );
        debugNode.onlyDesktop = true;
        debugNode.constrains = [Constrain.AdminOrDebug, Constrain.NoBulk];
        debugNode.group = DefaultGroups.View;
        debugNode.priority = 10;

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

        const openParentNode = new OptionItem('OPTIONS.SHOW_IN_FOLDER', 'folder', (object) =>
            this.goToWorkspace(this.getObjects(object)[0])
        );
        openParentNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository, Constrain.User];
        openParentNode.scopes = [Scope.Search, Scope.Render];
        openParentNode.customEnabledCallback = (nodes) => {
            if(nodes) {
                openParentNode.customEnabledCallback = null;
                let nodeId = nodes[0].ref.id;
                if (nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1) {
                    nodeId = nodes[0].properties[RestConstants.CCM_PROP_IO_ORIGINAL][0];
                }
                this.nodeService.getNodeParents(nodeId, false, []).subscribe(() => {
                    openParentNode.isEnabled = true;
                }, (error) => {
                    openParentNode.isEnabled = false;
                });
            }
            return true;
        };
        openParentNode.group = DefaultGroups.View;
        openParentNode.priority = 15;

        const openNode = new OptionItem('OPTIONS.SHOW', 'remove_red_eye', (object) =>
            this.list.openNode.emit(this.getObjects(object)[0])
        );
        openNode.constrains = [Constrain.Files, Constrain.NoBulk];
        openNode.scopes = [Scope.Workspace];
        openNode.group = DefaultGroups.View;
        openNode.priority = 30;

        const editConnectorNode = new OptionItem('OPTIONS.VIEW', 'launch', (node: Node) =>
            this.editConnector(node)
        );
        editConnectorNode.customShowCallback = (nodes) => {
            return this.connectors.connectorSupportsEdit(nodes ? nodes[0] : null) != null;
        }
        editConnectorNode.group = DefaultGroups.View;
        editConnectorNode.priority = 20;

        /**
         if (this.connector.getCurrentLogin() && !this.connector.getCurrentLogin().isGuest) {
        option = new OptionItem("OPTIONS.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
        option.showAsAction = true;
        option.customShowCallback = (node: Node) => {
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
            return NodeHelper.referenceOriginalExists(node) && NodeHelper.allFiles(nodes) && n.length>0;
        }
        option.enabledCallback = (node: Node) => {
          let list = ActionbarHelperService.getNodes(nodes, node);
          return NodeHelper.getNodesRight(list,RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
        }
        option.disabledCallback = () =>{
          this.connectors.getRestConnector().getBridgeService().showTemporaryMessage(MessageType.error, null,'WORKSPACE.TOAST.ADD_TO_COLLECTION_DISABLED');
        };
      }
         */

        const addNodeToCollection = new OptionItem('OPTIONS.COLLECTION', 'layers', (object) =>
            management.addToCollection =  this.getObjects(object)
        );
        addNodeToCollection.showAsAction = true;
        addNodeToCollection.constrains = [Constrain.Files, Constrain.User];
        addNodeToCollection.customShowCallback = (nodes) => {
            return NodeHelper.referenceOriginalExists(nodes ? nodes[0] : null);
        };
        addNodeToCollection.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addNodeToCollection.permissionsRightMode = NodesRightMode.Original;
        addNodeToCollection.permissionsMode = HideMode.Disable;
        addNodeToCollection.group = DefaultGroups.Reuse;
        addNodeToCollection.priority = 10;

        const bookmarkNode=new OptionItem('OPTIONS.ADD_NODE_STORE', 'bookmark_border',(object) =>
            this.bookmarkNodes(this.getObjects(object))
        );
        bookmarkNode.constrains = [Constrain.Files, Constrain.HomeRepository];
        bookmarkNode.group = DefaultGroups.Reuse;
        bookmarkNode.priority = 20;

        const createNodeVariant = new OptionItem('OPTIONS.VARIANT', 'call_split', (object) =>
            management.nodeVariant =  this.getObjects(object)[0]
        );
        createNodeVariant.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.User];
        createNodeVariant.customShowCallback = (nodes) => {
            if (nodes) {
                createNodeVariant.name = 'OPTIONS.VARIANT' + (this.connectors.connectorSupportsEdit(nodes[0]) ? '_OPEN' : '');
                return NodeHelper.referenceOriginalExists(nodes[0]);
            }
            return false;
        };
        createNodeVariant.group = DefaultGroups.Reuse;
        createNodeVariant.priority = 30;

        const inviteNode = new OptionItem('OPTIONS.INVITE', 'group_add',(object) =>
            management.nodeShare = this.getObjects(object)
        );
        inviteNode.showAsAction = true;
        inviteNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        inviteNode.permissionsMode = HideMode.Disable;
        inviteNode.constrains = [Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.User];
        inviteNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE];
        inviteNode.group = DefaultGroups.Edit;
        inviteNode.priority = 10;

        const licenseNode = new OptionItem('OPTIONS.LICENSE', 'copyright', (object) =>
            management.nodeLicense = this.getObjects(object)
        );
        licenseNode.constrains = [Constrain.Files, Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.User];
        licenseNode.permissions = [RestConstants.ACCESS_WRITE];
        licenseNode.permissionsMode = HideMode.Disable;
        licenseNode.toolpermissions = [RestConstants.TOOLPERMISSION_LICENSE];
        licenseNode.group = DefaultGroups.Edit;
        licenseNode.priority = 30;

        const contributorNode = new OptionItem('OPTIONS.CONTRIBUTOR', 'group', (object) =>
            management.nodeContributor = this.getObjects(object)[0]
        );
        contributorNode.constrains = [Constrain.Files, Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.NoBulk, Constrain.User];
        contributorNode.permissions = [RestConstants.ACCESS_WRITE];
        contributorNode.permissionsMode = HideMode.Disable;
        contributorNode.onlyDesktop = true;
        contributorNode.group = DefaultGroups.Edit;
        contributorNode.priority = 40;


        /*
        if (nodes && nodes.length==1 && !nodes[0].isDirectory  && nodes[0].type!=RestConstants.CCM_TYPE_SAVED_SEARCH && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)==-1) {
            option = new OptionItem("OPTIONS.WORKFLOW", "swap_calls", callback);
            option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
        }
         */
        const workflowNode = new OptionItem('OPTIONS.WORKFLOW', 'swap_calls', (object) =>
            management.nodeWorkflow =  this.getObjects(object)[0]
        );
        workflowNode.constrains = [Constrain.Files, Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.NoBulk, Constrain.User];
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
        const downloadNode = new OptionItem('OPTIONS.DOWNLOAD', 'cloud_download', (object) =>
            NodeHelper.downloadNodes(this.connector, this.getObjects(object))
        );
        downloadNode.constrains = [Constrain.Files];
        downloadNode.group = DefaultGroups.View;
        downloadNode.priority = 40;
        downloadNode.customEnabledCallback = (nodes) => {
            if (!nodes) {
                return false;
            }
            for (let item of nodes) {
                console.log(item);
                if (item.reference) {
                    item = item.reference;
                }
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                if(item.downloadUrl != null && item.properties && !item.properties[RestConstants.CCM_PROP_IO_WWWURL]) {
                    return true;
                }
            }
            return false;
        };
        const editNode = new OptionItem('OPTIONS.EDIT', 'edit', (object) =>
            management.nodeMetadata = this.getObjects(object)
        );
        editNode.constrains = [Constrain.NoCollectionReference, Constrain.HomeRepository, Constrain.User];
        editNode.permissions = [RestConstants.ACCESS_WRITE];
        editNode.permissionsMode = HideMode.Disable;
        editNode.group = DefaultGroups.Edit;
        editNode.priority = 20;

        const templateNode = new OptionItem('OPTIONS.TEMPLATE', 'assignment_turned_in', (object) =>
            management.nodeTemplate = this.getObjects(object)[0]
        );
        templateNode.constrains = [Constrain.NoBulk, Constrain.Directory, Constrain.User];
        templateNode.permissions = [RestConstants.ACCESS_WRITE];
        templateNode.permissionsMode = HideMode.Disable;
        templateNode.onlyDesktop = true;
        templateNode.group = DefaultGroups.Edit;


        /**
         const cut = new OptionItem('OPTIONS.CUT', 'content_cut', (node: Node) => this.cutCopyNode(node, false));
         cut.isSeperate = true;
         cut.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE)
         && (this.root === 'MY_FILES' || this.root === 'SHARED_FILES');
         options.push(cut);
         options.push(new OptionItem('OPTIONS.COPY', 'content_copy', (node: Node) => this.cutCopyNode(node, true)));
         */
        const cutNodes = new OptionItem('OPTIONS.CUT', 'content_cut', (node) =>
            this.cutCopyNode(node, false)
        );
        cutNodes.constrains = [Constrain.HomeRepository, Constrain.User];
        cutNodes.scopes = [Scope.Workspace];
        cutNodes.permissions = [RestConstants.ACCESS_WRITE];
        cutNodes.permissionsMode = HideMode.Disable;
        cutNodes.key = 'KeyX';
        cutNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];
        cutNodes.group = DefaultGroups.FileOperations;
        cutNodes.priority = 10;

        const copyNodes = new OptionItem('OPTIONS.COPY', 'content_copy', (node) =>
            this.cutCopyNode(node, true)
        );
        copyNodes.constrains = [Constrain.HomeRepository, Constrain.User];
        copyNodes.scopes = [Scope.Workspace];
        copyNodes.key = 'KeyC';
        copyNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];
        copyNodes.group = DefaultGroups.FileOperations;
        copyNodes.priority = 20;

        const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
            this.pasteNode()
        );
        pasteNodes.elementType = ElementType.Unknown;
        pasteNodes.constrains = [Constrain.NoSelection, Constrain.ClipboardContent, Constrain.AddObjects, Constrain.User];
        pasteNodes.scopes = [Scope.Workspace];
        pasteNodes.key = 'KeyV';
        pasteNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];
        pasteNodes.group = DefaultGroups.FileOperations;

        const deleteNode = new OptionItem('OPTIONS.DELETE', 'delete',(object) => {
            management.nodeDelete = this.getObjects(object);
        });
        deleteNode.constrains = [Constrain.HomeRepository, Constrain.User];
        deleteNode.permissions = [RestConstants.PERMISSION_DELETE];
        deleteNode.permissionsMode = HideMode.Disable;
        deleteNode.key = 'Delete';
        deleteNode.group = DefaultGroups.Delete;
        deleteNode.priority = 10;

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
            management.nodeReport = this.getObjects(node)[0]
        );
        reportNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];
        reportNode.scopes = [Scope.Search, Scope.CollectionsReferences, Scope.Render];
        reportNode.customShowCallback = (() => this.config.instant('nodeReport', false));
        reportNode.group = DefaultGroups.View;
        reportNode.priority = 50;

        const qrCodeNode = new OptionItem('OPTIONS.QR_CODE', 'edu-qr_code', (node) =>
            management.qr = {
                node: this.getObjects(node)[0],
                data: window.location.href
            }
        );
        qrCodeNode.constrains = [Constrain.Files, Constrain.NoBulk];
        qrCodeNode.scopes = [Scope.Render];
        qrCodeNode.group = DefaultGroups.View;
        qrCodeNode.priority = 60;


        options.push(applyNode);
        options.push(debugNode);
        options.push(openParentNode);
        options.push(openNode);
        options.push(bookmarkNode);
        options.push(editNode);
        // add to collection
        options.push(addNodeToCollection);
        // create variant
        options.push(createNodeVariant);
        options.push(templateNode);
        options.push(inviteNode);
        options.push(licenseNode);
        options.push(contributorNode);
        options.push(workflowNode);
        options.push(downloadNode);
        options.push(qrCodeNode);
        options.push(cutNodes);
        options.push(copyNodes);
        options.push(pasteNodes);
        options.push(deleteNode);
        options.push(reportNode);
        return options;
    }

    private editConnector(node: Node|any, type: Filetype = null, win: any = null, connectorType: Connector = null) {
        UIHelper.openConnector(this.connectors, this.iamService, this.event, this.toast, node, type, win, connectorType);
    }
    private canAddObjects() {
        return this.data.parent && NodeHelper.getNodesRight([this.data.parent], RestConstants.ACCESS_ADD_CHILDREN);
    }

    private addVirtualObjects(objects: any[]) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        this.list.addVirtualNodes(objects);
    }

    private bookmarkNodes(nodes: Node[]) {
        this.bridge.showProgressDialog();
        RestHelper.addToStore(nodes,this.bridge,this.iamService,()=> {
            this.bridge.closeModalDialog();
            this.mainNav.refreshNodeStore();
        });
    }

    /**
     * overwrite all the show callbacks by using the internal constrains + permission handlers
     * isOptionAvailable will check if customShowCallback exists and will also call it
     */
    private handleCallbacks(options: OptionItem[], objects: Node[]|any) {
        options.forEach((o) => {
            o.showCallback = ((object) => {
                const list = NodeHelper.getActionbarNodes(objects, object);
                if (list == null) {
                    return false;
                }
                return this.isOptionAvailable(o, list);
            });
            o.enabledCallback = ((object) => {
                const list = NodeHelper.getActionbarNodes(objects, object);
                if (list == null) {
                    return false;
                }
                return this.isOptionEnabled(o, list);
            });
        });
    }

    private goToWorkspace(node: Node | any) {
        UIHelper.goToWorkspace(this.nodeService, this.router, this.connector.getCurrentLogin(), node);
    }

    private getObjects(object: Node | any) {
        return NodeHelper.getActionbarNodes(this.data.selectedObjects || [this.data.activeObject], object);
    }

    applyExternalOptions(options: OptionItem[]) {
        if(!this.data.customOptions) {
            return options;
        }
        for (const option of this.data.customOptions) {
            const existing = options.filter((o) => o.name === option.name);
            if(existing.length === 1) {
                // only replace changed values
                for(const key of Object.keys(option)) {
                    (existing[0] as any)[key] = (option as any)[key];
                }
            } else {
                options.push(option);
            }
        }
        return options;
    }
    setData(data: OptionData) {
        this.data = data;
    }
    sortOptionsByGroup(options: OptionItem[]) {
        if(!options) {
            return null;
        }
        let result: OptionItem[] = [];
        let groups=Array.from(new Set(options.map((o) => o.group)));
        groups = groups.sort((o1, o2) => o1.priority > o2.priority ? 1 : -1);
        console.log(groups);
        for (const group of groups) {
            const groupOptions = options.filter((o) => o.group === group);
            if (group == null) {
                console.warn('There are options not assigned to a group. All options should be assigned to a group', groupOptions);
            }
            groupOptions.sort((o1, o2) => o1.priority > o2.priority ? 1 : -1);
            result = result.concat(groupOptions);
        }
        return result;
    }
}
export interface OptionsListener {
    onRefresh?: (nodes: Node[]|void) => void;
    onDelete?: (result: {error: boolean, count: number}) => void;
}
export interface OptionData {
    scope: Scope;
    activeObject: Node|any;
    selectedObjects?: Node[] | any[];
    allObjects?: Node[] | any[];
    parent?: Node|any;
    customOptions?: OptionItem[];
}
