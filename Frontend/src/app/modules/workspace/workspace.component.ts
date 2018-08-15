import {Component, ElementRef, HostListener, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {
    NodeRef, IamUser, NodeWrapper, Node, Version, NodeVersions, LoginResult, NodeList,
    OAuthResult, Collection, Connector, ConnectorList, Type, Filetype
} from "../../common/rest/data-object";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {Router, Params, ActivatedRoute, Routes} from "@angular/router";
import {OptionItem} from "../../common/ui/actionbar/option-item";
import {DialogButton, ModalDialogComponent} from "../../common/ui/modal-dialog/modal-dialog.component";
import {RestConstants} from "../../common/rest/rest-constants";
import {RestHelper} from "../../common/rest/rest-helper";
import {Toast} from "../../common/ui/toast";
import {ClipboardObject, TemporaryStorageService} from '../../common/services/temporary-storage.service';
import {UIAnimation} from "../../common/ui/ui-animation";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {NodeHelper} from "../../common/ui/node-helper";
import {UIService} from "../../common/services/ui.service";
import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
import {RestConnectorsService} from "../../common/rest/services/rest-connectors.service";
import {KeyEvents} from "../../common/ui/key-events";
import {ConfigurationService} from "../../common/services/configuration.service";
import {FrameEventsService} from "../../common/services/frame-events.service";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../common/ui/ui-helper";
import {Http,Response} from "@angular/http";
import {trigger} from "@angular/animations";
import {RestToolService} from "../../common/rest/services/rest-tool.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestSearchService} from "../../common/rest/services/rest-search.service";
import {ActionbarHelper} from "../../common/ui/actionbar/actionbar-helper";
import {Helper} from "../../common/helper";
import {RestMdsService} from '../../common/rest/services/rest-mds.service';
import {DateHelper} from '../../common/ui/DateHelper';
import {CordovaService} from "../../common/services/cordova.service";
import {EventListener} from "../../common/services/frame-events.service";

@Component({
    selector: 'workspace-main',
    templateUrl: 'workspace.component.html',
    styleUrls: ['workspace.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('fromLeft', UIAnimation.fromLeft()),
        trigger('fromRight',UIAnimation.fromRight())
    ]
})
export class WorkspaceMainComponent implements EventListener{
    private static VALID_ROOTS=['MY_FILES','SHARED_FILES','MY_SHARED_FILES','TO_ME_SHARED_FILES','WORKFLOW_RECEIVE','RECYCLE'];
    private static VALID_ROOTS_NODES=[RestConstants.USERHOME,'-shared_files-','-my_shared_files-','-to_me_shared_files-'];
    @ViewChild('dropdown') dropdownElement : ElementRef;
    private isRootFolder : boolean;
    private homeDirectory : string;
    private sharedFolders : Node[]=[];
    private path : Node[]=[];
    private parameterNode : Node;
    private metadataNode : String;
    private root = "MY_FILES";

    private explorerOptions : OptionItem[]=[];
    private actionOptions : OptionItem[]=[];
    private selection : Node[]=[];
    public fileIsOver = false;

    private dialogTitle : string;
    private dialogCancelable = false;
    private dialogMessage : string;
    private dialogMessageParameters : any;
    private dialogButtons : DialogButton[];

    private showAddDesktop = false;
    private showAddMobile = false;

    private showSelectRoot = false;
    public showUploadSelect = false;
    private createConnectorName : string;
    private createConnectorType : Connector;
    private addFolderName : string;

    public allowBinary = true;
    private filesToUpload : FileList;
    public globalProgress = false;
    public editNodeMetadata : Node;
    public editNodeTemplate : Node;
    public editNodeDeleteOnCancel = false;
    private createMds : string;
    private editNodeLicense : Node[];
    private editNodeAllowReplace : Boolean;
    private nodeDisplayedVersion : string;
    private createAllowed : boolean;
    private currentFolder : any|Node;
    private currentFolderRef : any|string;
    private user : IamUser;
    public searchQuery : string;
    public isSafe = false;
    private isLoggedIn = false;
    public addNodesToCollection : Node[];
    public addNodesStream : Node[];
    public variantNode : Node;
    private dropdownPosition: string;
    private dropdownLeft: string;
    private dropdownRight: string;
    private dropdownTop: string;
    private dropdownBottom: string;
    private connectorList: ConnectorList;
    private nodeOptions: OptionItem[]=[];
    private currentNode: Node;
    public mainnav=true;
    private timeout: string;
    private timeIsValid = false;
    private viewToggle: OptionItem;
    private isAdmin=false;
    public isBlocked=false;
    private isGuest: boolean;
    private currentNodes: Node[];
    private appleCmd=false;
    public workflowNode: Node;
    public deleteNode: Node[];
    private reurl: string;
    private mdsParentNode: Node;
    public showLtiTools=false;
    private oldParams: Params;
    private selectedNodeTree: string;
    private nodeDebug: Node;
    private sharedNode : Node;
    public contributorNode : Node;
    public shareLinkNode : Node;
    private viewType = 0;
    private infoToggle: OptionItem;
    @HostListener('window:beforeunload', ['$event'])
    beforeunloadHandler(event:any) {
        if(this.isSafe){
            this.connector.logoutSync();
        }
    }
    @HostListener('document:keyup', ['$event'])
    handleKeyboardEventUp(event: KeyboardEvent) {
        if(event.keyCode==91 || event.keyCode==93)
            this.appleCmd=false;
    }
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if(event.keyCode==91 || event.keyCode==93){
            this.appleCmd=true;
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        let clip=(this.storage.get("workspace_clipboard") as ClipboardObject);
        let fromInputField=KeyEvents.eventFromInputField(event);
        let hasOpenWindow=this.hasOpenWindows();
        if(event.code=="KeyX" && (event.ctrlKey || this.appleCmd) && this.selection.length && !hasOpenWindow && !fromInputField){
            this.cutCopyNode(null,false);
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(event.code=="F2" && this.selection.length==1 && !hasOpenWindow && !fromInputField){
            this.editNode(this.selection[0]);
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(event.code=="KeyC" && (event.ctrlKey || this.appleCmd) && this.selection.length && !hasOpenWindow && !fromInputField){
            this.cutCopyNode(null,true);
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(event.code=="KeyV" && (event.ctrlKey || this.appleCmd) && clip && !hasOpenWindow && !fromInputField){
            this.pasteNode();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(event.code=="Delete" && !hasOpenWindow && !fromInputField && this.selection.length){
            this.deleteNodes();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(event.key=="Escape"){
            if(this.addFolderName!=null){
                this.addFolderName=null;
            }
            else if(this.showUploadSelect){
                this.showUploadSelect=false;
            }
            else if(this.createConnectorName!=null){
                this.createConnectorName=null;
            }
            else if(this.metadataNode!=null){
                this.closeMetadata();
            }
            else{
                return;
            }
            event.preventDefault();
            event.stopPropagation();
        }
    }
    onEvent(event: string, data: any): void {
        if(event==FrameEventsService.EVENT_REFRESH){
            this.refresh();
        }
    }
    constructor(private toast : Toast,
                private route : ActivatedRoute,
                private router : Router,
                private translate : TranslateService,
                private storage : TemporaryStorageService,
                private config: ConfigurationService,
                private connectors : RestConnectorsService,
                private collectionApi : RestCollectionService,
                private toolService : RestToolService,
                private session : SessionStorageService,
                private iam : RestIamService,
                private mds : RestMdsService,
                private node : RestNodeService,
                private ui : UIService,
                private title : Title,
                private http : Http,
                private event : FrameEventsService,
                private connector : RestConnectorService,
                private cordova : CordovaService
    ) {
        this.event.addListener(this);
        Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=>{
            UIHelper.setTitle('WORKSPACE.TITLE',title,translate,config);
            this.initialize();
        });
        this.connector.setRoute(this.route);
        this.globalProgress=true;
        this.explorerOptions=this.getOptions([new Node()],true);
        //this.nodeOptions.push(new OptionItem("DOWNLOAD", "cloud_download", (node:Node) => this.downloadNode(node)));
    }
    private uploadCamera(event:any){
        this.filesToUpload=event.target.files;
    }
    private hideDialog() : void{
        this.dialogTitle=null;
    }
    private openCamera(){
        this.cordova.getPhotoFromCamera((data:any)=>{
            console.log(data);
            let name=this.translate.instant('SHARE_APP.IMAGE')+" "+DateHelper.formatDate(this.translate,new Date().getTime(),{showAlwaysTime:true,useRelativeLabels:false})+".jpg";
            let blob:any=Helper.base64toBlob(data,"image/jpeg");
            blob.name=name;
            let list:any={};
            list.item=(i:number)=>{
                return blob;
            }
            list.length=1;
            this.filesToUpload=list;
        },(error:any)=>{
            console.warn(error);
            //this.toast.error(error);
        });
    }
    private showTimeout(){
        return !this.cordova.isRunningCordova() && this.timeIsValid && this.dialogTitle!='WORKSPACE.AUTOLOGOUT' &&
            (this.isSafe || !this.isSafe && this.config.instant('sessionExpiredDialog',{show:true}).show);
    }
    private updateTimeout(){
        let time=this.connector.logoutTimeout - Math.floor((new Date().getTime()-this.connector.lastActionTime)/1000);
        let min=Math.floor(time/60);
        let sec=time%60;
        this.event.broadcastEvent(FrameEventsService.EVENT_SESSION_TIMEOUT,time);
        if(time>=0) {
            this.timeout = this.formatTimeout(min, 2) + ":" + this.formatTimeout(sec, 2);
            this.timeIsValid=true;
        }
        else if(this.showTimeout()){
            this.dialogTitle='WORKSPACE.AUTOLOGOUT';
            this.dialogMessage='WORKSPACE.AUTOLOGOUT_INFO';
            this.dialogCancelable=false;
            this.dialogMessageParameters={minutes:Math.round(this.connector.logoutTimeout/60)};
            this.dialogButtons=[];
            this.dialogButtons.push(new DialogButton("WORKSPACE.RELOGIN",DialogButton.TYPE_PRIMARY,()=>this.goToLogin()));
        }
        else
            this.timeout="";
    }
    private formatTimeout(num:number, size:number) {
        let s = num+"";
        while (s.length < size) s = "0" + s;
        return s;
    }
    private createConnector(event : any){
        let name=event.name+"."+event.type.filetype;
        this.createConnectorName=null;
        let prop=NodeHelper.propertiesFromConnector(event);
        let win:any;
        if(!this.cordova.isRunningCordova())
            win=window.open("");
        this.node.createNode(this.currentFolder.ref.id,RestConstants.CCM_TYPE_IO,[],prop,false).subscribe(
            (data : NodeWrapper)=>{
                this.editConnector(data.node,event.type,win,this.createConnectorType);
                this.refresh();
            },
            (error : any)=>{
                win.close();
                if(NodeHelper.handleNodeError(this.toast,event.name,error)==RestConstants.DUPLICATE_NODE_RESPONSE){
                    this.createConnectorName=event.name;
                }
            }
        )

    }
    private editConnector(node : Node=null,type : Filetype=null,win : any = null,connectorType : Connector = null){
        UIHelper.openConnector(this.connectors,this.event,this.toast,this.connectorList,this.getNodeList(node)[0],type,win,connectorType);
    }
    private handleDrop(event:any){
        for(let s of event.source) {
            if (event.target.ref.id == s.ref.id || event.target.ref.id==s.parent.id) {
                this.toast.error(null, "WORKSPACE.SOURCE_TARGET_IDENTICAL");
                return;
            }
        }
        if(!event.target.isDirectory){
            this.toast.error(null,"WORKSPACE.TARGET_NO_DIRECTORY");
            return;
        }
        if(event.event.altKey){
            this.toast.error(null,"WORKSPACE.FEATURE_NOT_IMPLEMENTED");
        }
        else if(event.type=='copy'){
            this.copyNode(event.target,event.source);
        }
        else{
            this.moveNode(event.target,event.source);
        }
        /*
        this.dialogTitle="WORKSPACE.DRAG_DROP_TITLE";
        this.dialogCancelable=true;
        this.dialogMessage="WORKSPACE.DRAG_DROP_MESSAGE";
        this.dialogMessageParameters={source:event.source.name,target:event.target.name};
        this.dialogButtons=[
          new DialogButton("WORKSPACE.DRAG_DROP_COPY",DialogButton.TYPE_PRIMARY,()=>this.copyNode(event.target,event.source)),
          new DialogButton("WORKSPACE.DRAG_DROP_MOVE",DialogButton.TYPE_PRIMARY,()=>this.moveNode(event.target,event.source)),
        ]
        console.log(event);
        */
    }
    canDropBreadcrumbs = (event:any)=>{return event.target.ref.id!=this.currentFolder.ref.id};
    private moveNode(target:Node,source:Node[],position = 0){
        this.globalProgress=true;
        if(position>=source.length){
            this.finishMoveCopy(target,source,false);
            this.globalProgress=false;
            return;
        }
        this.node.moveNode(target.ref.id,source[position].ref.id).subscribe((data:NodeWrapper)=> {
                this.moveNode(target, source, position+1);
            },
            (error : any)=>{
                NodeHelper.handleNodeError(this.toast,source[position].name,error);
                source.splice(position,1);
                this.moveNode(target, source, position+1);
            });
    }
    private copyNode(target:Node,source:Node[],position = 0){
        this.globalProgress=true;
        if(position>=source.length){
            this.finishMoveCopy(target,source,true);
            this.globalProgress=false;
            return;
        }
        this.node.copyNode(target.ref.id,source[position].ref.id).subscribe((data:NodeWrapper)=> {
                this.copyNode(target, source, position+1);
            },
            (error : any)=>{
                NodeHelper.handleNodeError(this.toast,source[position].name,error);
                source.splice(position,1);
                this.copyNode(target, source, position+1);
            });
    }
    private finishMoveCopy(target:Node,source:Node[],copy:boolean){
        this.dialogTitle=null;
        let info:any={
            to:target.name,
            count:source.length,
            mode:this.translate.instant("WORKSPACE."+(copy ? "PASTE_COPY" : "PASTE_MOVE"))
        }
        if(source.length)
            this.toast.toast("WORKSPACE.TOAST.PASTE_DRAG",info);
        this.globalProgress=false;
        this.refresh();
    }
    private initialize(){
        this.route.params.subscribe((params: Params) => {
            this.isSafe = params['mode'] == 'safe';
            this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
                if(data.statusCode!=RestConstants.STATUS_CODE_OK){
                    RestHelper.goToLogin(this.router,this.config);
                    return;
                }
                this.iam.getUser().subscribe((user : IamUser) => {
                    this.user=user;
                    this.loadFolders(user);

                    let valid=true;
                    this.isGuest=data.isGuest;
                    if(!data.isValidLogin || data.isGuest){
                        valid=false;
                    }
                    this.isBlocked=!this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_WORKSPACE);
                    this.isAdmin=data.isAdmin;
                    if(this.isSafe && data.currentScope!=RestConstants.SAFE_SCOPE)
                        valid=false;
                    if(!this.isSafe && data.currentScope!=null)
                        valid=false;
                    if(!valid){
                        this.goToLogin();
                        return;
                    }
                    this.connector.scope=this.isSafe ? RestConstants.SAFE_SCOPE : null;
                    this.connectors.list().subscribe((data:ConnectorList)=>{
                        this.connectorList=data;
                    });
                    this.isLoggedIn=true;
                    this.node.getHomeDirectory().subscribe((data : NodeRef) => {
                        this.globalProgress=false;
                        this.homeDirectory=data.id;
                        this.route.params.forEach((params: Params) => {
                            //if(this.isSafe)
                            setInterval(()=>this.updateTimeout(),1000);

                            this.route.queryParams.subscribe((params: Params) => {
                                let needsUpdate=false;
                                if(this.oldParams){
                                    for(let key of Object.keys(this.oldParams).concat(Object.keys(params))){
                                        if(params[key]!=this.oldParams[key] && key!='viewType'){
                                            console.log("changed "+key);
                                            needsUpdate=true;
                                        }
                                    }
                                }
                                else{
                                    needsUpdate=true;
                                }
                                this.oldParams=params;
                                if(params['viewType'])
                                    this.viewType=params['viewType'];
                                if(params['root'] && WorkspaceMainComponent.VALID_ROOTS.indexOf(params['root'])!=-1) {
                                    this.root = params['root'];
                                }
                                if(params['reurl']) {
                                    this.reurl = params['reurl'];
                                }
                                this.createAllowed=this.root=='MY_FILES';
                                this.mainnav=params['mainnav']=='false' ? false : true;

                                if(params['file']){
                                    this.node.getNodeMetadata(params['file']).subscribe((data:NodeWrapper)=>{
                                        this.setSelection([data.node]);
                                        this.parameterNode=data.node;
                                        this.metadataNode=params['file'];
                                    });
                                }

                                if(!needsUpdate)
                                    return;

                                this.searchQuery='';
                                if(params['query']) {
                                    this.searchQuery=params['query'];
                                    this.doSearchFromRoute(this.searchQuery);
                                }
                                else if(params['id']) {
                                    this.openDirectoryFromRoute(params['id']);
                                }
                                else{
                                    this.openDirectoryFromRoute("");
                                }
                                if(params['showAlpha']){
                                    this.showAlpha();
                                }
                            });
                        });
                    });
                });
            });
        });
    }
    public resetWorkspace(){
        if(this.metadataNode && this.parameterNode)
            this.setSelection([this.parameterNode]);
    }

    public doSearch(query:any){
        this.routeTo(this.root,null,query.query);
        if(!query.cleared){
            this.ui.hideKeyboardIfMobile();
        }
    }
    private doSearchFromRoute(query:string){
        this.searchQuery=query;
        this.createAllowed=false;
        this.path=[];
        this.selection=[];
        this.currentFolder=null;
        this.actionOptions = this.getOptions(null,false);

        if(this.root=='MY_SHARED_FILES' || this.root=='SHARED_FILES')
            this.root='MY_FILES';
        if(!this.searchQuery){
            this.openDirectory(null);
            return;
        }

    }
    private manageContributorsNode(node: Node) {
        let list=this.getNodeList(node);
        this.contributorNode=list[0];
    }
    private manageWorkflowNode(node: Node) {
        let list=this.getNodeList(node);
        this.workflowNode=list[0];
    }
    private setShareLinkNode(node: Node) {
        let list=this.getNodeList(node);
        this.shareLinkNode=list[0];
    }
    private shareNode(node: Node) {
        let list=this.getNodeList(node);
        this.sharedNode=list[0];
    }
    private editNode(node: Node) {
        let list=this.getNodeList(node);
        this.editNodeMetadata=list[0];
        this.editNodeAllowReplace=new Boolean(true);
    }
    private editLicense(node: Node) {
        let list=this.getNodeList(node);
        this.editNodeLicense=list;
    }
    private addFolder(folder : any){
        this.addFolderName=null;
        this.globalProgress=true;
        let properties=RestHelper.createNameProperty(folder.name);
        if(folder.metadataset) {
            properties[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] = [folder.metadataset];
            properties[RestConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET] = ["true"];
        }
        this.node.createNode(this.currentFolder.ref.id,RestConstants.CM_TYPE_FOLDER,[],properties).subscribe(
            (data : NodeWrapper)=>{
                //this.openNode(data.node.ref.id,false);
                this.globalProgress=false;
                this.refresh();
                this.toast.toast("WORKSPACE.TOAST.FOLDER_ADDED");
            },
            (error : any)=>{
                this.globalProgress=false;
                if(NodeHelper.handleNodeError(this.toast,folder.name,error)==RestConstants.DUPLICATE_NODE_RESPONSE){
                    this.addFolderName=folder.name;
                }
            }
        )
    }
    private afterUpload(node:Node[]){
        if(this.reurl){
            NodeHelper.addNodeToLms(this.router,this.storage,node[0],this.reurl);
        }
    }
    private uploadFiles(files : FileList){
        this.onFileDrop(files);
    }
    public onFileDrop(files : FileList){
        if(!this.showUploadSelect && this.hasOpenWindows())
            return;
        if(this.searchQuery){
            this.toast.error(null,"WORKSPACE.TOAST.NOT_POSSIBLE_IN_SEARCH");
            return;
        }
        if(!this.createAllowed){
            this.toast.error(null,"WORKSPACE.TOAST.NO_WRITE_PERMISSION");
            return;
        }
        if(this.filesToUpload){
            this.toast.error(null,"WORKSPACE.TOAST.ONGOING_UPLOAD");
            return;
        }
        this.showUploadSelect=false;
        this.filesToUpload=files;
    }


    private deleteNodes(node: Node=null) {
        let list=this.getNodeList(node);
        if(list==null)
            return;
        this.deleteNode=list;
    }
    private deleteDone(data:any){
        this.metadataNode=null;
        this.refresh();
    }

    private pasteNode(position=0){
        let clip=(this.storage.get("workspace_clipboard") as ClipboardObject);
        if(this.searchQuery || this.isRootFolder)
            return;
        if(!clip || !clip.nodes.length)
            return;
        if(clip.sourceNode && clip.sourceNode.ref.id==this.currentFolder.ref.id && !clip.copy){
            return;
        }
        if(position>=clip.nodes.length){
            this.globalProgress=false;
            this.storage.remove("workspace_clipboard");
            let info:any={
                from:clip.sourceNode ? clip.sourceNode.name : this.translate.instant('WORKSPACE.COPY_SEARCH'),
                to:this.currentFolder.name,
                count:clip.nodes.length,
                mode:this.translate.instant("WORKSPACE."+(clip.copy ? "PASTE_COPY" : "PASTE_MOVE"))
            }
            this.toast.toast("WORKSPACE.TOAST.PASTE",info);
            this.refresh();
            return;
        }
        this.globalProgress=true;
        let target=this.currentFolder.ref.id;
        console.log(this.currentFolder);
        let source=clip.nodes[position].ref.id;
        if(clip.copy)
            this.node.copyNode(target,source).subscribe(
                (data : NodeWrapper)=> this.pasteNode(position+1),
                (error : any)=> {
                    NodeHelper.handleNodeError(this.toast,clip.nodes[position].name, error);
                    this.globalProgress = false;
                });
        else
            this.node.moveNode(target,source).subscribe(
                (data : NodeWrapper)=> this.pasteNode(position+1),
                (error : any)=>{
                    NodeHelper.handleNodeError(this.toast,clip.nodes[position].name,error);
                    this.globalProgress=false;
                }
            );

    }
    private cutCopyNode(node: Node,copy:boolean) {
        let list=this.getNodeList(node);
        if(!list || !list.length)
            return;
        list=Helper.deepCopy(list);
        let clip : ClipboardObject={sourceNode : this.currentFolder,nodes:list,copy:copy};
        this.storage.set("workspace_clipboard",clip);
        this.toast.toast("WORKSPACE.TOAST.CUT_COPY",{count:list.length});
    }
    private downloadNode(node: Node) {
        let list = this.getNodeList(node);
        NodeHelper.downloadNodes(this.toast,this.connector,list);
    }
    private displayNode(event:Node){
        let list = this.getNodeList(event);
        this.closeMetadata();
        if(list[0].isDirectory){
            this.openDirectory(list[0].ref.id);
        }
        else {
            /*
            this.nodeDisplayed = event;
            this.nodeDisplayedVersion = event.version;
            */
            this.currentNode=list[0];
            this.storage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_OPTIONS,this.nodeOptions);
            this.storage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST,this.currentNodes);
            this.router.navigate([UIConstants.ROUTER_PREFIX+"render", list[0].ref.id,list[0].version ? list[0].version : ""]);
        }
    }
    private restoreVersion(version : Version){
        this.dialogTitle="WORKSPACE.METADATA.RESTORE_TITLE";
        this.dialogCancelable=true;
        this.dialogMessage="WORKSPACE.METADATA.RESTORE_MESSAGE";
        this.dialogButtons=DialogButton.getYesNo(()=>this.hideDialog(),()=>this.doRestoreVersion(version));
    }
    // returns either the passed node as list, or the current selection if the passed node is invalid (actionbar)
    private getNodeList(node : Node) : Node[]{
        if(Array.isArray(node))
            return node;
        let nodes=[node];
        if(node==null)
            nodes=this.selection;
        return nodes;
    }

    private loadFolders(user: IamUser) {
        for(let folder of user.person.sharedFolders){
            this.node.getNodeMetadata(folder.id).subscribe((node : NodeWrapper) => this.sharedFolders.push(node.node));
        }
    }
    private setRoot(root : string){
        this.root=root;
        this.routeTo(root);
    }
    private updateList(nodes : Node[]){
        this.currentNodes=nodes;
    }

    private clickNode(node : Node){
        //if(!this.selection || this.selection.length<2)
        this.setSelection([node]);

        if(!node.isDirectory) {
            if(this.ui.isMobile())
                this.displayNode(node);
            else {
                if(this.metadataNode){
                    this.openMetadata(node);
                }
            }
        }
        else {
            //this.closeMetadata();
            if(this.ui.isMobile())
                this.openDirectory(node.ref.id);
            else if(this.metadataNode)
                this.openMetadata(node)
        }
    }
    private openMetadata(node : Node|string) {
        let old=this.metadataNode;
        if(node==null)
            node=this.selection[0];
        if(typeof node=='string')
            this.metadataNode=new String((node as string));
        else
            this.metadataNode=new String((node as Node).ref.id);
        this.infoToggle.icon='info';
        if(old && this.metadataNode.toString()==old.toString()){
            this.closeMetadata();
        }
    }
    public updateOptions(node : Node) : void{
        this.explorerOptions=this.getOptions([node ? node : new Node()],true);
    }


    public debugNode(node:Node){
        this.nodeDebug=this.getNodeList(node)[0];
        /*
        this.session.set("admin_lucene",{
            query:'@sys\:node-uuid:"'+node.ref.id+'"',
            offset:0,
            count:10,
        });
        this.router.navigate([UIConstants.ROUTER_PREFIX,"admin"],{queryParams:{mode:'BROWSER'}});
        */
    }
    public getOptions(nodes : Node[],fromList:boolean) : OptionItem[] {
        if(nodes && !nodes.length)
            nodes=null;
        let options: OptionItem[] = [];

        let allFiles = NodeHelper.allFiles(nodes);
        let savedSearch = nodes && nodes.length && nodes[0].type==RestConstants.CCM_TYPE_SAVED_SEARCH;
        let clip=(this.storage.get("workspace_clipboard") as ClipboardObject);
        if(this.currentFolder && !nodes && !this.searchQuery && clip && ((!clip.sourceNode || clip.sourceNode.ref.id!=this.currentFolder.ref.id) || clip.copy) && this.createAllowed) {
            options.push(new OptionItem("WORKSPACE.OPTION.PASTE", "content_paste", (node: Node) => this.pasteNode()));
        }
        if (nodes && nodes.length == 1) {
            if(this.reurl && !nodes[0].isDirectory){
                let apply=new OptionItem("APPLY", "redo", (node: Node) => NodeHelper.addNodeToLms(this.router,this.storage,this.getNodeList(node)[0],this.reurl));
                apply.showAsAction=true;
                apply.enabledCallback=((node:Node)=> {
                    return node.access.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1;
                });
                options.push(apply);
            }
            if(this.isAdmin){
                let debug = new OptionItem("WORKSPACE.OPTION.DEBUG", "build", (node: Node) => this.debugNode(node));
                options.push(debug);
            }
            let open = new OptionItem("WORKSPACE.OPTION.SHOW", "remove_red_eye", (node: Node) => this.displayNode(node));
            if (!nodes[0].isDirectory && !savedSearch)
                options.push(open);
        }
        let view = new OptionItem("WORKSPACE.OPTION.VIEW", "launch", (node: Node) => this.editConnector(node));
        if(fromList){
            view.showAlways = true;
            view.showCallback=((node:Node)=>{
                return RestConnectorsService.connectorSupportsEdit(this.connectorList, node) != null;
            });
            options.push(view);
        }
        else if(nodes && nodes.length==1 && RestConnectorsService.connectorSupportsEdit(this.connectorList,nodes[0])){
            options.push(view);
        }
        if(nodes && nodes.length==1 && !savedSearch){
            let edit=new OptionItem("WORKSPACE.OPTION.EDIT", "info_outline", (node: Node) => this.editNode(node));
            edit.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_WRITE);
            edit.isSeperateBottom = true;
            if(edit.isEnabled)
                options.push(edit);
        }
        if(nodes && nodes.length && allFiles) {
            let collection = ActionbarHelper.createOptionIfPossible('ADD_TO_COLLECTION',nodes,this.connector,(node:Node)=>this.addToCollection(node));
            if (collection && !this.isSafe)
                options.push(collection);
            let stream = ActionbarHelper.createOptionIfPossible('ADD_TO_STREAM',nodes,this.connector,(node:Node)=>this.addToStream(node));
            if (stream && !this.isSafe)
                options.push(stream);
        }
        if(nodes && nodes.length && allFiles) {
            /*
            let variant = ActionbarHelper.createOptionIfPossible('CREATE_VARIANT',nodes,this.connector,(node:Node)=>this.createVariant(node));
            if (variant && !this.isSafe)
                options.push(variant);
            */
        }
        let share:OptionItem;
        if (nodes && nodes.length == 1) {
            let template = ActionbarHelper.createOptionIfPossible('NODE_TEMPLATE',nodes,this.connector,(node:Node)=>this.nodeTemplate(node));
            if(template)
                options.push(template);
            share=ActionbarHelper.createOptionIfPossible('INVITE',nodes,this.connector,(node: Node) => this.shareNode(node));
            if(share) {
                share.isEnabled = share.isEnabled && (
                    (this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE) && !this.isSafe)
                    || (this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_SAFE) && this.isSafe)
                );
                //if (this.isSafe && this.root != 'SHARED_FILES')
                //    share.isEnabled = false;
                options.push(share);
            }
            /*let shareLink = ActionbarHelper.createOptionIfPossible('SHARE_LINK',nodes,this.connector,(node: Node) => this.setShareLinkNode(node));
            if (shareLink && !this.isSafe)
                options.push(shareLink);*/
        }
        if(nodes) {
            let license = new OptionItem("WORKSPACE.OPTION.LICENSE", "copyright", (node: Node) => this.editLicense(node));
            license.isEnabled = !this.isSafe && allFiles && NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_DELETE) && this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_LICENSE);
            if (license.isEnabled)
                options.push(license);
        }
        if (nodes && nodes.length == 1 && !savedSearch) {
            let contributor=new OptionItem("WORKSPACE.OPTION.CONTRIBUTOR","group",(node:Node)=>this.manageContributorsNode(node));
            contributor.isEnabled=NodeHelper.getNodesRight(nodes,RestConstants.ACCESS_WRITE);
            if(nodes && !nodes[0].isDirectory && !this.isSafe)
                options.push(contributor);
            let workflow = ActionbarHelper.createOptionIfPossible('WORKFLOW',nodes,this.connector,(node:Node)=>this.manageWorkflowNode(node));
            if(workflow)
                options.push(workflow);


            this.infoToggle=new OptionItem("WORKSPACE.OPTION.METADATA", "info_outline", (node: Node) => this.openMetadata(node));
            this.infoToggle.isToggle=true;
            //info.onlyMobile=!nodes[0].isDirectory;
            options.push(this.infoToggle);
            //options[0].showAlways = true;


        }
        if(fromList || nodes && nodes.length) {
            let download = ActionbarHelper.createOptionIfPossible('DOWNLOAD', nodes, this.connector, (node: Node) => this.downloadNode(node));
            if (download)
                options.push(download);
        }
        if (nodes && nodes.length) {
            let cut=new OptionItem("WORKSPACE.OPTION.CUT", "content_cut", (node: Node) => this.cutCopyNode(node, false));
            cut.isSeperate = true;
            cut.isEnabled=NodeHelper.getNodesRight(nodes,RestConstants.ACCESS_WRITE) && (this.root=='MY_FILES' || this.root=='SHARED_FILES');
            options.push(cut);
            options.push(new OptionItem("WORKSPACE.OPTION.COPY", "content_copy", (node: Node) => this.cutCopyNode(node, true)));
            let del=ActionbarHelper.createOptionIfPossible('DELETE',nodes,this.connector,(node : Node) => this.deleteNodes(node));
            if(del){
                options.push(del);
            }
            let custom=this.config.instant("nodeOptions");
            NodeHelper.applyCustomNodeOptions(this.toast,this.http,this.connector,custom,this.currentNodes, nodes, options,(load:boolean)=>this.globalProgress=load);
        }
        if(!fromList && this.root!='RECYCLE') {
            this.viewToggle = new OptionItem("", this.viewType==0 ? "view_module" : "list", (node: Node) => this.toggleView());
            this.viewToggle.isToggle = true;
            options.push(this.viewToggle);
        }
        return options;
    }
    private setSelection(nodes : Node[]) {
        this.selection=nodes;
        this.actionOptions=this.getOptions(nodes,false);
    }
    private updateLicense(){
        this.closeMetadata();
    }
    private closeMetadata() {
        this.metadataNode=null;
        this.infoToggle.icon='info_outline';
    }
    private openDirectory(id:string){
        this.routeTo(this.root, id ? id : null);
    }
    private openDirectoryFromRoute(id : string,createRoute = true){
        this.selection=[];
        this.closeMetadata();
        this.createAllowed = false;
        this.actionOptions = this.getOptions(null,false);
        let hasId=id;
        if(!id){
            this.path=[];
            id=this.getRootFolderId();
            if(this.root=='RECYCLE')
                return;
        }
        else{
            this.selectedNodeTree=id;
            this.node.getNodeParents(id).subscribe((data : NodeList)=>{
                this.path = data.nodes.reverse();
                this.selectedNodeTree=null;
            },(error:any)=>{
                this.selectedNodeTree=null;
                this.path=[];
            });
        }

        this.searchQuery=null;
        this.currentFolder=null;
        this.allowBinary=true;
        let root=WorkspaceMainComponent.VALID_ROOTS_NODES.indexOf(id)!=-1;
        if(!root || id==RestConstants.USERHOME) {
            this.isRootFolder=false;
            console.log("open path: "+id);
            this.currentFolderRef=id;
            this.node.getNodeMetadata(id).subscribe((data: NodeWrapper) => {
                this.mds.getSet(data.node.metadataset ? data.node.metadataset : RestConstants.DEFAULT).subscribe((mds:any)=>{
                    if(mds.create) {
                        this.allowBinary = !mds.create.onlyMetadata;
                        if(!this.allowBinary)
                            console.log("mds does not allow binary files, will switch mode");
                    }
                });
                this.currentFolder = data.node;
                this.event.broadcastEvent(FrameEventsService.EVENT_NODE_FOLDER_OPENED, this.currentFolder);
                this.createAllowed = NodeHelper.getNodesRight([this.currentFolder], RestConstants.ACCESS_ADD_CHILDREN);
                this.actionOptions = this.getOptions(this.selection, false);
            }, (error: any) => {
                this.currentFolder = {ref: {id: id}};
                this.event.broadcastEvent(FrameEventsService.EVENT_NODE_FOLDER_OPENED, this.currentFolder);
                this.searchQuery = null;
            });
        }
        else{
            this.isRootFolder=true;
            console.log("open root path "+id);
            if(id==RestConstants.USERHOME){
                this.createAllowed = true;
            }
            this.currentFolder = {ref: {id: id}};
            this.currentFolderRef = id;
            this.event.broadcastEvent(FrameEventsService.EVENT_NODE_FOLDER_OPENED, this.currentFolder);
            this.searchQuery = null;
        }

    }
    public createEmptyNode(){
        this.globalProgress=true;
        let prop=RestHelper.createNameProperty(DateHelper.formatDateByPattern(new Date().getTime(),"y-M-d"));
        this.node.createNode(this.currentFolder.ref.id,RestConstants.CCM_TYPE_IO,[],prop,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe((data:NodeWrapper)=>{
            this.editNodeMetadata=data.node;
            this.editNodeDeleteOnCancel=true;
            this.globalProgress=false;
        });
    }
    private openNode(node : Node,useConnector=true) {
        if(!node.isDirectory){
            if(RestSearchService.isSavedSearchObject(node)){
                UIHelper.routeToSearchNode(this.router,node);
            }
            else if(RestToolService.isLtiObject(node)){
                this.toolService.openLtiObject(node);
            }
            else if(useConnector && RestConnectorsService.connectorSupportsEdit(this.connectorList,node)){
                this.editConnector(node);
            }
            else {
                this.displayNode(node);
            }
            return;
        }
        this.openDirectory(node.ref.id);
    }
    private openBreadcrumb(position : number){
        /*this.path=this.path.slice(0,position+1);
        */
        this.searchQuery=null;
        this.actionOptions=null;
        let id="";
        let length=this.path ? this.path.length : 0;
        if(position>0)
            id=this.path[position-1].ref.id;
        else if(length>0){
            id=null;
        }
        else {
            this.showSelectRoot = true;
            return;
        }
        console.log("breadcrumb "+position+" "+id);

        this.openDirectory(id);
    }

    private refresh(refreshPath=true) {
        let search=this.searchQuery;
        let folder=this.currentFolder;
        let ref=this.currentFolderRef;
        this.currentFolder=null;
        this.currentFolderRef=null;
        this.searchQuery=null;
        this.selection=[];
        this.actionOptions=this.getOptions(this.selection,false);
        let path=this.path;
        if(refreshPath)
            this.path=[null];
        setTimeout(()=>{
            this.path=path;
            this.currentFolder=folder;
            this.currentFolderRef=ref;
            this.searchQuery=search;
        });
    }

    private doRestoreVersion(version: Version) : void {
        this.hideDialog();
        this.globalProgress=true;
        this.node.revertNodeToVersion(version.version.node.id,version.version.major,version.version.minor).subscribe((data : NodeVersions)=>{
                this.globalProgress=false;
                this.refresh();
                this.closeMetadata();
                this.openMetadata(version.version.node.id);
                this.toast.toast("WORKSPACE.REVERTED_VERSION");
            },
            (error:any) => this.toast.error(error));
    }

    private refreshRoute(){
        console.log(this.isRootFolder);
        this.routeTo(this.root,!this.isRootFolder && this.currentFolder ? this.currentFolder.ref.id : null,this.searchQuery);
    }
    private routeTo(root: string,node : string=null,search="") {
        console.log("update route "+root+" "+node);
        let params:any={root:root,id:node?node:"",viewType:this.viewType,query:search,mainnav:this.mainnav};
        if(this.reurl)
            params.reurl=this.reurl;
        this.router.navigate(["./"],{queryParams:params,relativeTo:this.route})
            .then((result:boolean)=>{
                if(!result){
                    this.refresh(false);
                }
            });
    }

    private showAlpha() {
        this.dialogTitle='WORKSPACE.ALPHA_TITLE';
        this.dialogMessage='WORKSPACE.ALPHA_MESSAGE';
        this.dialogButtons=DialogButton.getOk(()=>{
            this.dialogTitle=null;
        });
    }

    private nodeTemplate(node: Node){
        this.editNodeTemplate = this.getNodeList(node)[0];
    }
    private addToCollection(node: Node) {
        let nodes=this.getNodeList(node);
        this.addNodesToCollection=nodes;
    }
  private addToStream(node: Node) {
    let nodes=this.getNodeList(node);
    this.addNodesStream=nodes;
  }
    private createVariant(node: Node) {
        let nodes=this.getNodeList(node);
        this.variantNode=nodes[0];
    }
    private createContext(event:any=null){
        if(!this.createAllowed)
            return;
        this.showAddDesktop = true;
        this.dropdownPosition = null;
        this.dropdownTop = null;
        this.dropdownBottom = null;
        this.dropdownLeft = null;
        this.dropdownRight = null;
        if(event) {
            event.preventDefault();
            event.stopPropagation();
            this.dropdownPosition = "fixed";
            this.dropdownLeft = event.clientX + "px";
            if (event.clientY > window.innerHeight / 2) {
                this.dropdownBottom = window.innerHeight - event.clientY + "px";
                this.dropdownTop = "auto";
            }
            else {
                this.dropdownTop = event.clientY + "px";
            }
        }
        setTimeout(()=>UIHelper.setFocusOnDropdown(this.dropdownElement));
    }
    private createMobile(){
        if(!this.createAllowed)
            return;
        this.showAddDesktop = true;
        this.dropdownPosition = "fixed";
        this.dropdownTop = "auto";
        this.dropdownLeft = "auto";
    }

    private goToLogin() {
        RestHelper.goToLogin(this.router,this.config,this.isSafe ? RestConstants.SAFE_SCOPE : "");
    }

    private getRootFolderId() {
        if(this.root=='MY_FILES')
            return RestConstants.USERHOME;
        if(this.root=='SHARED_FILES'){
            return RestConstants.SHARED_FILES;
        }
        if(this.root=='MY_SHARED_FILES'){
            return RestConstants.MY_SHARED_FILES;
        }
        if(this.root=='TO_ME_SHARED_FILES'){
            return RestConstants.TO_ME_SHARED_FILES;
        }
        if(this.root=='WORKFLOW_RECEIVE'){
            return RestConstants.WORKFLOW_RECEIVE;
        }
        return "";
    }

    private toggleView() {
        this.viewType=1-this.viewType;
        this.refreshRoute();
        if(this.viewType==0){
          this.viewToggle.icon='view_module';
        }
        else{
          this.viewToggle.icon='list';
        }

    }

    public listLTI() {
        this.showLtiTools=true;
        this.showAddDesktop=false;
        this.showAddMobile=false;
    }

    hasOpenWindows() {
        return this.editNodeLicense || this.editNodeMetadata || this.createConnectorName || this.showUploadSelect || this.dialogTitle || this.addFolderName || this.sharedNode || this.workflowNode || this.filesToUpload;
    }
}
