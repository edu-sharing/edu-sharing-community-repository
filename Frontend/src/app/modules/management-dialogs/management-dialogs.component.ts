import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, HostListener} from '@angular/core';
import {DialogButton, NodeVersions, RestNodeService, Version} from "../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {RestSearchService} from "../../core-module/core.module";
import {Toast} from "../../core-ui-module/toast";
import {RestConstants} from "../../core-module/core.module";
import {NodeWrapper, Node, Collection} from "../../core-module/core.module";
import {RestHelper} from "../../core-module/core.module";
import {RestToolService} from "../../core-module/core.module";
import {ConfigurationService} from "../../core-module/core.module";
import {MdsComponent} from "../../common/ui/mds/mds.component";
import {RestCollectionService} from "../../core-module/core.module";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../core-module/ui/ui-animation";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {Router} from '@angular/router';
import {UIConstants} from "../../core-module/ui/ui-constants";
import {ClipboardObject, TemporaryStorageService} from '../../core-module/core.module';
import {RestUsageService} from "../../core-module/core.module";
import {Observable} from 'rxjs';
import {BridgeService} from '../../core-bridge-module/bridge.service';
import {LinkData, NodeHelper} from '../../core-ui-module/node-helper';

@Component({
  selector: 'workspace-management',
  templateUrl: 'management-dialogs.component.html',
  styleUrls: ['management-dialogs.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('fromLeft', UIAnimation.fromLeft()),
    trigger('fromRight',UIAnimation.fromRight())
  ]
})
export class WorkspaceManagementDialogsComponent  {
  public globalProgress = false;
  @ViewChild('mds') mdsRef : MdsComponent;
  @Input() showLtiTools = false;
  @Input() uploadShowPicker = false;
  @Input() uploadMultiple = true;
  @Input() fileIsOver = false;
  @Input() addToCollection:Node[];
  @Output() addToCollectionChange = new EventEmitter();
  @Input() filesToUpload : Node[];
  @Output() filesToUploadChange = new EventEmitter();
  @Input() parent : Node;
  @Output() showLtiToolsChange = new EventEmitter();
  @Input() nodeLicense : Node[];
  @Output() nodeLicenseChange = new EventEmitter();
  @Input() addPinnedCollection: Node;
  @Output() addPinnedCollectionChange = new EventEmitter();
  @Input() set nodeDelete (nodeDelete: Node[]){
        if(nodeDelete==null)
            return;
        this.dialogTitle='WORKSPACE.DELETE_TITLE'+(nodeDelete.length === 1 ? '_SINGLE' : '');
        this.dialogMessage='WORKSPACE.DELETE_MESSAGE'+(nodeDelete.length === 1 ? '_SINGLE' : '');
        if(nodeDelete.length === 1 && nodeDelete[0].collection) {
            this.dialogTitle='WORKSPACE.DELETE_TITLE_COLLECTION';
            this.dialogMessage='WORKSPACE.DELETE_MESSAGE_COLLECTION';
        }
      this.dialogCancelable=true;
        this.dialogMessageParameters = {name:RestHelper.getName(nodeDelete[0])};
        this.dialogNode=nodeDelete;
        this.dialogButtons=DialogButton.getCancel(()=> {this.dialogTitle = null});
        this.dialogButtons.push(new DialogButton('YES_DELETE',DialogButton.TYPE_PRIMARY,()=>{this.deleteConfirmed(nodeDelete)}));

        // check for usages and warn user
        if(nodeDelete.length === 1 && !nodeDelete[0].isDirectory) {
            this.usageService.getNodeUsages(nodeDelete[0].ref.id,nodeDelete[0].ref.repo).subscribe((usages)=>{
                if(usages.usages.length>0) {
                    this.dialogMessage='WORKSPACE.DELETE_MESSAGE_SINGLE_USAGES';
                    this.dialogMessageParameters = {name:nodeDelete[0].name,usages:usages.usages.length};
                }
            });
        }
    }
    @Output() nodeDeleteChange = new EventEmitter();
    @Output() onDelete = new EventEmitter<{objects: Node[]|any,error: boolean, count: number}>();
  @Input() nodeShare : Node[];
  @Output() nodeShareChange = new EventEmitter<Node[]>();
    @Input() nodeDebug : Node[];
    @Output() nodeDebugChange = new EventEmitter<Node[]>();
    @Input() nodeShareLink : Node;
    @Output() nodeShareLinkChange = new EventEmitter();
    @Input() nodeWorkflow : Node;
    @Output() nodeWorkflowChange = new EventEmitter();
  @Input() nodeReport : Node;
  @Output() nodeReportChange = new EventEmitter();
  @Input() addNodesStream : Node[];
  @Output() addNodesStreamChange = new EventEmitter();
    @Input() nodeVariant : Node;
    @Output() nodeVariantChange = new EventEmitter();
  @Input() nodeMetadata : Node[];
    @Output() nodeMetadataChange = new EventEmitter<Node[]>();
    @Input() nodeTemplate : Node;
    @Output() nodeTemplateChange = new EventEmitter();
    @Input() nodeContributor : Node;
    @Output() nodeContributorChange = new EventEmitter();
    @Input() set nodeSimpleEdit (nodeSimpleEdit: Node[]) {
        this._nodeSimpleEdit = nodeSimpleEdit;
        this._nodeSimpleFromUpload = false;
    }
    @Input() nodeSimpleEditChange = new EventEmitter<Node[]>();
    @Input() collectionWriteFeedback: Node;
    @Output() collectionWriteFeedbackChange = new EventEmitter<Node>();
    @Input() collectionViewFeedback: Node;
    @Output() collectionViewFeedbackChange = new EventEmitter<Node>();
    @Input() nodeSidebar: Node;
    @Output() nodeSidebarChange = new EventEmitter<Node>();
    @Input() showUploadSelect=false;
  @Output() showUploadSelectChange = new EventEmitter();
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onRefresh=new EventEmitter<Node[]|void>();
  @Output() onUploadFilesProcessed=new EventEmitter<Node[]>();
  @Output() onCloseMetadata=new EventEmitter();
  @Output() onUploadFileSelected=new EventEmitter();
  @Output() onUpdateLicense=new EventEmitter();
  @Output() onCloseAddToCollection=new EventEmitter();
  @Output() onStoredAddToCollection=new EventEmitter();
  _nodeSimpleEdit: Node[];
  _nodeSimpleFromUpload = false;
  public createMetadata: string;
  public editorPending = false;
  public metadataParent: Node;
  public ltiToolConfig : Node;
  public ltiObject: Node;
  public dialogTitle:string;
  public dialogMessage:string;
  public dialogMessageParameters:any;
  public dialogCancelable:boolean;
  public dialogNode:Node|Node[];
  public dialogButtons:DialogButton[];
  private currentLtiTool: Node;
  private ltiToolRefresh: Boolean;
  @Input() nodeDeleteOnCancel: boolean;
  @Output() nodeDeleteOnCancelChange = new EventEmitter();
  private nodeLicenseOnUpload = false;
    /**
     * QR Code object data to print
     * @node: Reference to the node (for header title)
     * @data: The string to display inside the qr code (e.g. an url)
     */
  @Input() qr: {node: Node, data: string};

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.key === 'Escape') {
      if(this.nodeMetadata!=null || this.createMetadata){
        if(this.mdsRef.handleKeyboardEvent(event))
          return;
        this.closeEditor(false);
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.nodeSidebar!=null){
        this.closeSidebar();
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.addToCollection!=null) {
          this.cancelAddToCollection();
          event.preventDefault();
          event.stopPropagation();
          return;
      }
        if(this.nodeTemplate!=null){
            this.closeTemplate();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
      if(this.nodeLicense!=null){
        this.closeLicense();
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.showLtiTools){
        this.closeLtiTools();
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.nodeReport!=null){
        this.closeReport();
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.ltiObject){
        this.ltiObject=null;
        event.preventDefault();
        event.stopPropagation();
        return;
      }
    }
  }
  public constructor(
    private nodeService:RestNodeService,
    private usageService:RestUsageService,
    private toolService:RestToolService,
    private temporaryStorage:TemporaryStorageService,
    private collectionService:RestCollectionService,
    private translate:TranslateService,
    private config:ConfigurationService,
    private searchService:RestSearchService,
    private toast:Toast,
    private bridge:BridgeService,
    private router:Router,
  ){
   }
 private closeLtiToolConfig(){
    this.ltiToolConfig=null;
    this.ltiToolRefresh=new Boolean();
 }
    closeShareLink() {
        this.nodeShareLink = null
        this.nodeShareLinkChange.emit(null);
    }
 closeShare() {
     this.nodeShare = null
     this.nodeShareChange.emit(null);
 }
    public closeWorkflow(node: Node = null){
        this.nodeWorkflow = null;
        this.nodeWorkflowChange.emit(null);
        if (node) {
            this.onRefresh.emit([node]);
        }
    }
    private deleteConfirmed(nodes : Node[],position=0,error=false) : void {
        if (position >= nodes.length) {
            this.globalProgress = false;
            if (!error)
                this.toast.toast("WORKSPACE.TOAST.DELETE_FINISHED");
            this.onDelete.emit({objects: nodes,error:error,count:position});
            return;
        }
        this.dialogTitle=null;
        this.globalProgress = true;
        this.nodeService.deleteNode(nodes[position].ref.id).subscribe(data => {
            this.removeNodeFromClipboard(nodes[position]);
            this.deleteConfirmed(nodes, position + 1, error);
        }, (error: any) => {
            this.toast.error(error);
            this.deleteConfirmed(nodes, position + 1, true);
        });
    }
    private removeNodeFromClipboard(node: Node) {
        let clip=(this.temporaryStorage.get("workspace_clipboard") as ClipboardObject);
        if(clip==null)
            return;

        for(let n of clip.nodes){
            if(n.ref.id==node.ref.id){
                clip.nodes.splice(clip.nodes.indexOf(n),1);
            }
            if(clip.nodes.length==0){
                this.temporaryStorage.remove("workspace_clipboard");
            }
        }
    }
 public uploadDone(event: Node[]){
    if (this.config.instant('licenseDialogOnUpload', false)) {
         this.nodeLicense = event;
         this.nodeLicenseOnUpload = true;
    } else {
        this.showMetadataAfterUpload(event);
    }
    this.filesToUpload = null;
    this.filesToUploadChange.emit(null);
  }
  public refresh(){
    this.onRefresh.emit();
  }
 public uploadFile(event:any){
   this.onUploadFileSelected.emit(event);
 }
  createUrlLink(link : LinkData) {
    const urlData = NodeHelper.createUrlLink(link);
    this.closeUploadSelect();
    this.toast.showProgressDialog();
    this.nodeService.createNode(this.parent.ref.id,RestConstants.CCM_TYPE_IO,urlData.aspects,urlData.properties,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
      (data) => {
        this.showMetadataAfterUpload([data.node]);
        this.toast.closeModalDialog();
      });
  }
 private openLtiConfig(event:Node){
   this.ltiToolConfig=event;
 }
 public closeUploadSelect(){
   this.showUploadSelect=false
   this.showUploadSelectChange.emit(false);
 }
 public closeContributor(){
     if(this.editorPending){
         this.editorPending=false;
         this.nodeMetadata=[this.nodeContributor];
     }
   this.nodeContributor=null;
   this.nodeContributorChange.emit(null);
 }
  private closeLtiTools() {
    this.showLtiTools = false;
    this.showLtiToolsChange.emit(false);
  }
  private closeLicense() {
    if(this.nodeLicenseOnUpload){
      this.showMetadataAfterUpload(this.nodeLicense);
    }
    else {
        if(this._nodeSimpleFromUpload)
            this.onUploadFilesProcessed.emit(this.nodeLicense);
        this._nodeSimpleFromUpload = false;
    }
      if(this.editorPending){
          this.editorPending=false;
          this.nodeMetadata=this.nodeLicense;
      }
    this.nodeLicense=null;
    this.nodeLicenseOnUpload=false;
    this.nodeLicenseChange.emit(null);
  }
  private updateLicense(nodes:Node[]) {
      console.log(nodes);
    this.closeLicense();
    this.onUpdateLicense.emit();
    this.onRefresh.emit(nodes);
  }
  deleteNodes(nodes: Node[]) {
      this.toast.showProgressDialog();
      Observable.forkJoin(nodes.map((n) => this.nodeService.deleteNode(n.ref.id, false)))
          .subscribe(() => {
              this.nodeDeleteOnCancel = false;
              this.nodeDeleteOnCancelChange.emit(false);
              this.toast.closeModalDialog();
              this.closeEditor(true);
          });
  }
  private closeEditor(refresh:boolean,node: Node[]=null){
      if (this.nodeDeleteOnCancel && node == null) {
          this.deleteNodes(this.nodeMetadata);
          return;
      }
    this.nodeDeleteOnCancel=false;
    this.nodeDeleteOnCancelChange.emit(false);
    this.nodeMetadata=null;
    this.nodeMetadataChange.emit(null);
    this.createMetadata=null;
    this.onCloseMetadata.emit(node);
    if(refresh) {
      this.onRefresh.emit(node);
      if(node && node.length === 1 && node[0].aspects.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION) !== -1) {
        this.currentLtiTool = node[0];
      }
      else{
        this.ltiToolRefresh=new Boolean();
      }
    }
  }
  public editLti(event:Node){
    //this.closeLtiTools();
    this.nodeMetadata=[event];
  }
  public createLti(event:any){
    //this.closeLtiTools();
    this.createMetadata=event.type;
    this.metadataParent=event.node;
  }
  public createLtiObject(event:Node){
    this.closeLtiTools();
    this.ltiObject=event;
  }
  public createLtiNodeObject(event:any){
    let win=window.open("",'_blank');
    let properties=RestHelper.createNameProperty(event.name);
    properties[RestConstants.CCM_PROP_TOOL_INSTANCE_REF]=[RestHelper.createSpacesStoreRef(this.ltiObject)];
    this.nodeService.createNode(event.parent.ref.id,
      RestConstants.CCM_TYPE_IO,
      [RestConstants.CCM_ASPECT_TOOL_OBJECT],
      properties,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe((data:NodeWrapper)=>{
      this.ltiObject=null;
      this.toolService.openLtiObject(data.node,win);
      this.onRefresh.emit();
    },(error:any)=>{
      this.toast.error(error);
      win.close();
    });
  }
  public closeStream() {
    this.addNodesStream=null;
    this.addNodesStreamChange.emit(null);
  }
  public closeReport() {
    this.nodeReport=null;
    this.nodeReportChange.emit(null);
  }
    public closeVariant() {
        this.nodeVariant=null;
        this.nodeVariantChange.emit(null);
    }
  private cancelAddToCollection(){
    this.dialogTitle=null;
    this.addToCollection=null;
    this.addToCollectionChange.emit(null);
    this.onCloseAddToCollection.emit();
  }
  public addToCollectionCreate(parent:Node=null){
      this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES,this.addToCollection);
      this.router.navigate([UIConstants.ROUTER_PREFIX,"collections","collection","new",parent ? parent.ref.id : RestConstants.ROOT]);
      this.addToCollection=null;
      this.addToCollectionChange.emit(null);
  }
  public addToCollectionList(collection:Node,list:Node[]=this.addToCollection,close=true,callback:Function=null,force=false){
    if(!force && (collection.collection.scope!=RestConstants.COLLECTIONSCOPE_MY)){
      this.dialogTitle='DIALOG.COLLECTION_SHARE_PUBLIC';
      this.dialogMessage='DIALOG.COLLECTION_SHARE_PUBLIC_INFO';
      this.dialogCancelable=true;
      this.dialogMessageParameters={collection:RestHelper.getTitle(collection)};
      this.dialogButtons=DialogButton.getNextCancel(()=>{this.dialogTitle=null},()=>{
        this.addToCollectionList(collection,list,close,callback,true);
      });
      return;
    }
    if(close)
      this.cancelAddToCollection();
    else{
      this.dialogTitle=null;
    }
    this.globalProgress=true;
    UIHelper.addToCollection(this.collectionService,this.router,this.bridge,collection,list,()=>{
      this.globalProgress=false;
       this.onStoredAddToCollection.emit(collection);
      if(callback)
        callback();
    });
  }

    private showMetadataAfterUpload(event: Node[]) {
        this._nodeSimpleEdit = event;
        this.nodeSimpleEditChange.emit(event);
        this._nodeSimpleFromUpload = true;

        this.nodeDeleteOnCancel=true;
        this.nodeDeleteOnCancelChange.emit(true);
    }

    closeTemplate() {
        this.nodeTemplate = null;
        this.nodeTemplateChange.emit(null);
    }

    closeDebug() {
      this.nodeDebug = null;
      this.nodeDebugChange.emit(null);
    }

    closePinnedCollection() {
        this.addPinnedCollection = null;
        this.addPinnedCollectionChange.emit(null);
    }

    closeCollectionWriteFeedback() {
        this.collectionWriteFeedback = null;
        this.collectionWriteFeedbackChange.emit(null);
    }

    addCollectionFeedback(feedback: any) {
        if (!feedback) {
            return;
        }
        delete feedback[RestConstants.CM_NAME];
        this.toast.showProgressDialog();
        this.collectionService
            .addFeedback(this.collectionWriteFeedback.ref.id, feedback)
            .subscribe(
                () => {
                    this.toast.closeModalDialog();
                    this.closeCollectionWriteFeedback();
                    this.toast.toast('COLLECTIONS.FEEDBACK_TOAST');
                },
                error => {
                    this.toast.closeModalDialog();
                    this.toast.error(error);
                },
            );
    }
    private restoreVersion(restore:{version: Version,node: Node}) {
        this.toast.showConfigurableDialog({
            title: 'WORKSPACE.METADATA.RESTORE_TITLE',
            message: 'WORKSPACE.METADATA.RESTORE_MESSAGE',
            buttons: DialogButton.getYesNo(() => this.toast.closeModalDialog(), () => this.doRestoreVersion(restore.version)),
            node: restore.node,
            isCancelable: true,
            onCancel: () => this.toast.closeModalDialog(),
        });
    }
    private doRestoreVersion(version: Version): void {
        this.toast.showProgressDialog();
        this.nodeService.revertNodeToVersion(version.version.node.id, version.version.major, version.version.minor)
            .subscribe(
                (data: NodeVersions) => {
                    this.toast.closeModalDialog();
                    this.refresh();
                    this.closeSidebar();
                    // @TODO type is not compatible
                    this.nodeService.getNodeMetadata(version.version.node.id, [RestConstants.ALL]).subscribe((node) => {
                        this.nodeSidebar = node.node;
                        this.nodeSidebarChange.emit(node.node);
                        this.toast.toast('WORKSPACE.REVERTED_VERSION');
                    },
                (error: any) => this.toast.error(error));
                },
            (error: any) => this.toast.error(error));
    }


    closeCollectionViewFeedback() {
        this.collectionViewFeedback = null;
        this.collectionViewFeedbackChange.emit(null);
    }

    closeSidebar() {
        this.nodeSidebar = null;
        this.nodeSidebarChange.emit(null);
    }

    displayNode(node: Node) {
      if(node.version) {
          this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id, node.version]);
      } else {
          this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
      }
    }

    closeSimpleEdit(saved: boolean, nodes: Node[] = null) {
        if (saved && this._nodeSimpleFromUpload) {
            this.onUploadFilesProcessed.emit(nodes);
        } else if(!saved && this._nodeSimpleFromUpload) {
            this.deleteNodes(this._nodeSimpleEdit);
            this.onUploadFilesProcessed.emit(null);
        }
        if (nodes) {
            this.onRefresh.emit(nodes);
        }
        this._nodeSimpleEdit = null;
        this.nodeSimpleEditChange.emit(null);
    }
}
