import {RestNetworkService} from '../core-module/rest/services/rest-network.service';
import {RestConnectorsService} from '../core-module/rest/services/rest-connectors.service';
import {RestConstants} from '../core-module/rest/rest-constants';
import {ListTableComponent} from '../core-ui-module/components/list-table/list-table.component';
import {ActionbarComponent} from './ui/actionbar/actionbar.component';
import {OptionItem} from '../core-ui-module/option-item';
import {UIHelper} from '../core-ui-module/ui-helper';
import {UIService} from '../core-module/rest/services/ui.service';
import {WorkspaceManagementDialogsComponent} from '../modules/management-dialogs/management-dialogs.component';
import {NodeHelper, NodesRightMode} from '../core-ui-module/node-helper';
import {Node, NodeWrapper} from '../core-module/rest/data-object';
import {Helper} from '../core-module/rest/helper';
import {ClipboardObject, TemporaryStorageService} from '../core-module/rest/services/temporary-storage.service';
import {BridgeService} from '../core-bridge-module/bridge.service';
import {MessageType} from '../core-module/ui/message-type';
import {Injectable} from '@angular/core';
import {CardComponent} from '../core-ui-module/components/card/card.component';
import {fromEvent} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {RestNodeService} from '../core-module/rest/services/rest-node.service';
import {ActionbarHelperService} from './services/actionbar-helper';
import {ConfigurationService, RestConnectorService, RestHelper, RestIamService} from '../core-module/core.module';
import {MainNavComponent} from './ui/main-nav/main-nav.component';

@Injectable()
export class OptionsHelperService {
    private allObjects: Node[] | any[];
    private selectedObjects: Node[] | any[];
    private activeObject: Node | any;
    private options:Option[] = [];
    private parent: Node|any;
    private appleCmd: boolean;
    private globalOptions: Option[];
    private list: ListTableComponent;
    private mainNav: MainNavComponent;

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
            const option = this.globalOptions.filter((o: Option) => {
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
        private ui: UIService,
        private translate: TranslateService,
        private nodeService: RestNodeService,
        private config: ConfigurationService,
        private storage: TemporaryStorageService,
        private bridge: BridgeService,
    ) {
        // @HostListener decorator unfortunately does not work in services
        fromEvent(document, 'keyup').subscribe((event) =>
            this.handleKeyboardEventUp(event)
        );
        fromEvent(document, 'keydown').subscribe((event) =>
            this.handleKeyboardEvent(event)
        );
    }
    private cutCopyNode(node: Node, copy: boolean) {
        let list = NodeHelper.getActionbarNodes(this.selectedObjects, node);
        if (!list || !list.length) {
            return;
        }
        list = Helper.deepCopy(list);
        const clip: ClipboardObject = { sourceNode: this.parent, nodes: list, copy };
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
                to: this.parent.name,
                count: clip.nodes.length,
                mode: this.translate.instant('WORKSPACE.' + (clip.copy ? 'PASTE_COPY' : 'PASTE_MOVE'))
            };
            this.bridge.showTemporaryMessage(MessageType.info, 'WORKSPACE.TOAST.PASTE', info);
            this.addVirtualObjects(nodes);
            return;
        }
        this.bridge.showProgressDialog();
        const target = this.parent.ref.id;
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
    setAllObjects(allObjects: Node[]|any[]) {
        this.allObjects = allObjects;
    }
    setSelectedObjects(selectedObjects: Node[] | any[]) {
        this.selectedObjects = selectedObjects;
    }

    refreshComponents(management: WorkspaceManagementDialogsComponent,
                      list: ListTableComponent,
                      actionbar: ActionbarComponent,
                      mainNav: MainNavComponent) {
        this.prepareOptions(management);
        this.list = list;
        this.mainNav = mainNav;
        this.globalOptions = this.getAvailableOptions(Target.Actionbar);
        list.options = this.getAvailableOptions(Target.List);
        list.dropdownOptions = this.getAvailableOptions(Target.ListDropdown);
        if (actionbar) {
            actionbar.options = this.globalOptions;
        }
    }
    private disableOptions(options: Option[], objects: Node[]|any) {
        options.filter((o) =>
            o.permissionsMode === HideMode.Disable &&
            o.permissions &&
            !this.validatePermissions(o, objects)
        ).forEach((o) => {
            console.log(o);
            o.isEnabled = false;
        });
    }

    private getAvailableOptions(target: Target) {
        let objects: Node[]|any[];
        if (target === Target.List) {
            objects = this.allObjects && this.allObjects.length ? [this.allObjects[0]] : null;
        } else if (target === Target.Actionbar) {
            objects = this.selectedObjects;
        } else if (target === Target.ListDropdown) {
            if (this.activeObject) {
                objects = [this.activeObject];
            } else {
                return null;
            }
        }
        const result = this.options.filter((o) => this.isAvailable(o, objects));
        console.log(result, objects);
        this.disableOptions(result, objects);
        return (UIHelper.filterValidOptions(this.ui, result) as Option[]);
    }
    private isAvailable(option: Option, objects: Node[]|any[]) {
        if (this.getType(objects) !== option.elementType) {
            // console.log('types not matching', this.getType(objects), option);
            return false;
        }
        if (option.showCallback) {
           if (objects.filter((o) => option.showCallback(o) === false).length > 0) {
               console.log('show callback was false', option);
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
                }
           }
        }
        return true;
    }

    private hasSelection() {
        return this.selectedObjects && this.selectedObjects.length;
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

    private validatePermissions(option: Option, objects: Node[] | any[]) {
        return option.permissions.filter((p) =>
            NodeHelper.getNodesRight(objects, p, option.permissionsRightMode) === false
        ).length === 0;
    }

    setActiveObject(activeObject: Node|any) {
        this.activeObject = activeObject;
    }

    setParentObject(parent: Node|any) {
        this.parent = parent;
    }

    private prepareOptions(management: WorkspaceManagementDialogsComponent) {
        this.options = [];
        /*
       if(nodes && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)==-1) {
       option = new OptionItem("WORKSPACE.OPTION.INVITE", "group_add", callback);
       option.isSeperate = NodeHelper.allFiles(nodes);
       option.showAsAction = true;
       option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
     }
        */
        const debugNode = new Option('WORKSPACE.OPTION.DEBUG', 'build', (object) =>
            management.nodeDebug = NodeHelper.getActionbarNodes(this.selectedObjects, object)[0],
        );
        debugNode.onlyDesktop = true;
        debugNode.constrains = [Constrain.AdminOrDebug, Constrain.NoBulk];

        const openNode = new Option('WORKSPACE.OPTION.SHOW', 'remove_red_eye', (object) =>
            this.list.openNode.emit(NodeHelper.getActionbarNodes(this.selectedObjects, object)[0])
        );
        openNode.constrains = [Constrain.Files, Constrain.NoBulk];


        /**
         if (this.connector.getCurrentLogin() && !this.connector.getCurrentLogin().isGuest) {
        option = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", callback);
        option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH,NodesRightMode.Original);
        option.showAsAction = true;
        option.showCallback = (node: Node) => {
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

        const addNodeToCollection = new Option('WORKSPACE.OPTION.COLLECTION', 'layers', (object) =>
            management.addToCollection =  NodeHelper.getActionbarNodes(this.selectedObjects, object)
        );
        addNodeToCollection.showAsAction = true;
        addNodeToCollection.constrains = [Constrain.Files, Constrain.User];
        addNodeToCollection.showCallback = (node: Node) => {
            return NodeHelper.referenceOriginalExists(node);
        };
        addNodeToCollection.permissions = [RestConstants.ACCESS_CC_PUBLISH];
        addNodeToCollection.permissionsRightMode = NodesRightMode.Original;
        addNodeToCollection.permissionsMode = HideMode.Disable;

        const bookmarkNode=new Option('SEARCH.ADD_NODE_STORE', 'bookmark_border',(object) =>
            this.bookmarkNodes(NodeHelper.getActionbarNodes(this.selectedObjects, object))
        );
        bookmarkNode.constrains = [Constrain.Files, Constrain.HomeRepository];

        const createNodeVariant = new Option('WORKSPACE.OPTION.VARIANT', 'call_split', (object) =>
            management.nodeVariant =  NodeHelper.getActionbarNodes(this.selectedObjects, object)[0]
        );
        createNodeVariant.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository, Constrain.User];
        createNodeVariant.showCallback = (node : Node) => {
            if (node) {
                createNodeVariant.name = 'WORKSPACE.OPTION.VARIANT' + (this.connectors.connectorSupportsEdit(node) ? '_OPEN' : '');
            }
            return true;
        };

        const inviteNode = new Option('WORKSPACE.OPTION.INVITE', 'group_add',(object) =>
            management.nodeShare = NodeHelper.getActionbarNodes(this.selectedObjects, object)
        );
        inviteNode.showAsAction = true;
        inviteNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        inviteNode.permissionsMode = HideMode.Disable;
        inviteNode.constrains = [Constrain.NoCollectionReference, Constrain.User];
        inviteNode.toolpermissions = [RestConstants.TOOLPERMISSION_INVITE];

        const licenseNode = new Option('WORKSPACE.OPTION.LICENSE', 'copyright', (object) =>
            management.nodeLicense = NodeHelper.getActionbarNodes(this.selectedObjects, object)
        );
        licenseNode.constrains = [Constrain.Files, Constrain.User];
        licenseNode.permissions = [RestConstants.ACCESS_WRITE];
        licenseNode.permissionsMode = HideMode.Disable;
        licenseNode.toolpermissions = [RestConstants.TOOLPERMISSION_LICENSE];

        const contributorNode = new Option('WORKSPACE.OPTION.CONTRIBUTOR', 'group', (object) =>
            management.nodeContributor = NodeHelper.getActionbarNodes(this.selectedObjects, object)[0]
        );
        contributorNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.User];
        contributorNode.permissions = [RestConstants.ACCESS_WRITE];
        contributorNode.permissionsMode = HideMode.Disable;
        contributorNode.onlyDesktop = true;
        /*
        if (nodes && nodes.length==1 && !nodes[0].isDirectory  && nodes[0].type!=RestConstants.CCM_TYPE_SAVED_SEARCH && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)==-1) {
            option = new OptionItem("WORKSPACE.OPTION.WORKFLOW", "swap_calls", callback);
            option.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CHANGE_PERMISSIONS);
        }
         */
        const workflowNode = new Option('WORKSPACE.OPTION.WORKFLOW', 'swap_calls', (object) =>
            management.nodeWorkflow =  NodeHelper.getActionbarNodes(this.selectedObjects, object)[0]
        );
        workflowNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.User];
        workflowNode.permissions = [RestConstants.ACCESS_CHANGE_PERMISSIONS];
        workflowNode.permissionsMode = HideMode.Disable;

        /*
        option = new OptionItem("WORKSPACE.OPTION.DOWNLOAD", "cloud_download", callback);
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
        const downloadNode = new Option('WORKSPACE.OPTION.DOWNLOAD', 'cloud_download', (object) =>
            NodeHelper.downloadNodes(this.connector, NodeHelper.getActionbarNodes(this.selectedObjects, object))
        );
        downloadNode.constrains = [Constrain.Files];
        downloadNode.enabledCallback = (node: Node) => {
            let list = NodeHelper.getActionbarNodes(this.selectedObjects, node);
            if (!list || !list.length)
                return false;
            let isAllowed = false;
            for (let item of list) {
                if (item.reference) {
                    item = item.reference;
                }
                // if at least one is allowed -> allow download (download servlet will later filter invalid files)
                isAllowed = isAllowed || list && item.downloadUrl != null &&
                            item.properties && !item.properties[RestConstants.CCM_PROP_IO_WWWURL];
            }
            return isAllowed;
        };
        const editNode = new Option('WORKSPACE.OPTION.EDIT', 'edit', (object) =>
            management.nodeMetadata = NodeHelper.getActionbarNodes(this.selectedObjects, object)
        );
        editNode.permissions = [RestConstants.ACCESS_WRITE];
        editNode.permissionsMode = HideMode.Disable;

        const templateNode = new Option('WORKSPACE.OPTION.TEMPLATE', 'assignment_turned_in', (object) =>
            management.nodeTemplate = NodeHelper.getActionbarNodes(this.selectedObjects, object)[0]
        );
        templateNode.constrains = [Constrain.NoBulk, Constrain.Directory, Constrain.User];
        templateNode.permissions = [RestConstants.ACCESS_WRITE];
        templateNode.permissionsMode = HideMode.Disable;
        templateNode.onlyDesktop = true;


        /**
         const cut = new OptionItem('WORKSPACE.OPTION.CUT', 'content_cut', (node: Node) => this.cutCopyNode(node, false));
         cut.isSeperate = true;
         cut.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE)
         && (this.root === 'MY_FILES' || this.root === 'SHARED_FILES');
         options.push(cut);
         options.push(new OptionItem('WORKSPACE.OPTION.COPY', 'content_copy', (node: Node) => this.cutCopyNode(node, true)));
         */
        const cutNodes = new Option('WORKSPACE.OPTION.CUT', 'content_cut', (node) =>
            this.cutCopyNode(node, false)
        );
        cutNodes.constrains = [Constrain.User];
        cutNodes.permissions = [RestConstants.ACCESS_WRITE];
        cutNodes.permissionsMode = HideMode.Disable;
        cutNodes.key = 'KeyX';
        cutNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];
        const copyNodes = new Option('WORKSPACE.OPTION.COPY', 'content_copy', (node) =>
            this.cutCopyNode(node, true)
        );
        copyNodes.constrains = [Constrain.User];
        copyNodes.key = 'KeyC';
        copyNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];
        const pasteNodes = new Option('WORKSPACE.OPTION.PASTE', 'content_paste', (node) =>
            this.pasteNode()
        );
        pasteNodes.elementType = ElementType.Unknown;
        pasteNodes.constrains = [Constrain.NoSelection, Constrain.ClipboardContent, Constrain.AddObjects, Constrain.User];
        pasteNodes.key = 'KeyV';
        pasteNodes.keyCombination = [KeyCombination.CtrlOrAppleCmd];

        const deleteNode = new Option('WORKSPACE.OPTION.DELETE', 'delete',(object) => {
            management.nodeDelete = NodeHelper.getActionbarNodes(this.selectedObjects, object);
        });
        deleteNode.constrains = [Constrain.User];
        deleteNode.permissions = [RestConstants.PERMISSION_DELETE];
        deleteNode.permissionsMode = HideMode.Disable;
        deleteNode.key = 'Delete';

        /*
        let report = new OptionItem('NODE_REPORT.OPTION', 'flag', (node: Node) => this.nodeReport=this.getCurrentNode(node));
        report.showCallback=(node:Node)=>{
            let n=ActionbarHelperService.getNodes(nodes,node);
            if(n==null)
                return false;
          return RestNetworkService.allFromHomeRepo(n,this.allRepositories);
        }
        options.push(report);
         */
        const reportNode = new Option('NODE_REPORT.OPTION', 'flag', (node) =>
            management.nodeReport = NodeHelper.getActionbarNodes(this.selectedObjects, node)[0]
        );
        reportNode.constrains = [Constrain.Files, Constrain.NoBulk, Constrain.HomeRepository];
        reportNode.showCallback = (() => this.config.instant('nodeReport', false));

        const qrCodeNode = new Option('WORKSPACE.OPTION.QR_CODE', 'edu-qr_code', (node) =>
            management.qr = {
                node: NodeHelper.getActionbarNodes(this.selectedObjects, node)[0],
                data: window.location.href
            }
        );
        qrCodeNode.constrains = [Constrain.Files, Constrain.NoBulk];



        this.options.push(debugNode);
        this.options.push(openNode);
        this.options.push(bookmarkNode);
        this.options.push(editNode);
        // add to collection
        this.options.push(addNodeToCollection);
        // create variant
        this.options.push(createNodeVariant);

        this.options.push(templateNode);
        this.options.push(inviteNode);
        this.options.push(licenseNode);
        this.options.push(contributorNode);
        this.options.push(workflowNode);
        this.options.push(downloadNode);
        this.options.push(qrCodeNode);
        this.options.push(cutNodes);
        this.options.push(copyNodes);
        this.options.push(pasteNodes);
        this.options.push(deleteNode);
        this.options.push(reportNode);
    }

    private canAddObjects() {
        console.log(this.parent);
        return this.parent && NodeHelper.getNodesRight([this.parent], RestConstants.ACCESS_ADD_CHILDREN);
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
}
class Option extends OptionItem {
    public key: string;
    public keyCombination: KeyCombination[];
    public elementType = ElementType.Node;
    public permissions: string[];
    public constrains: Constrain[];
    public permissionsMode = HideMode.Disable;
    public permissionsRightMode = NodesRightMode.Local;
    public toolpermissions: string[];
}
enum HideMode {
    Disable,
    Hide
}
enum ElementType {
    Node,
    Person,
    Group,
    Unknown
}
enum Constrain {
    NoCollectionReference, // option is only visible for non-collection references
    Directory, // only visible for directories (ccm:map)
    Files, // only visible for files (ccm:io)
    AdminOrDebug, // only visible if user is admin or esDebug is enabled on window component
    NoBulk, // No support for bulk (multiple objects)
    NoSelection, // Only visible when currently no element is selected
    ClipboardContent, // Only visible when the clipboard has content
    AddObjects, // Only visible when it is possible to add objects into the current list
    HomeRepository, // Only visible when the nodes are from the local (home) repository
    User, // Only visible when a user is present and logged in
}
enum KeyCombination {
    CtrlOrAppleCmd
}
export enum Target {
    List, // Target is the ListTableComponent
    ListDropdown,
    Actionbar
}

