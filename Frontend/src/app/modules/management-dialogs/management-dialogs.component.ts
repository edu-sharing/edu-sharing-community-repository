import {Component, Input, EventEmitter, Output, ViewChild, ElementRef, HostListener} from '@angular/core';
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {TranslateService} from "@ngx-translate/core";
import {RestSearchService} from "../../common/rest/services/rest-search.service";
import {Toast} from "../../common/ui/toast";
import {RestConstants} from "../../common/rest/rest-constants";
import {NodeWrapper, Node, Collection} from "../../common/rest/data-object";
import {RestHelper} from "../../common/rest/rest-helper";
import {RestToolService} from "../../common/rest/services/rest-tool.service";
import {ConfigurationService} from "../../common/services/configuration.service";
import {MdsComponent} from "../../common/ui/mds/mds.component";
import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../common/ui/ui-animation";
import {UIHelper} from "../../common/ui/ui-helper";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {Router} from '@angular/router';
import {UIConstants} from "../../common/ui/ui-constants";
import {ClipboardObject, TemporaryStorageService} from '../../common/services/temporary-storage.service';

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
  @Input() fileIsOver = false;
  @Input() addToCollection:Node[];
  @Output() addToCollectionChange = new EventEmitter();
  @Input() filesToUpload : Node[]
  @Output() filesToUploadChange = new EventEmitter();
  @Input() parent : Node;
  @Output() showLtiToolsChange = new EventEmitter();
  @Input() nodeLicense : Node[];
  @Output() nodeLicenseChange = new EventEmitter();
    @Input() set nodeDelete (nodeDelete: Node[]){
        if(nodeDelete==null)
            return;
        this.dialogTitle="WORKSPACE.DELETE_TITLE"+(nodeDelete.length==1 ? "_SINGLE" : "");
        this.dialogCancelable=true;
        this.dialogMessage="WORKSPACE.DELETE_MESSAGE"+(nodeDelete.length==1 ? "_SINGLE" : "");
        this.dialogMessageParameters={name:nodeDelete[0].name};
        this.dialogButtons=DialogButton.getOkCancel(() => {
            this.dialogTitle = null;
        }, ()=>this.deleteConfirmed(nodeDelete));
    }
    @Output() nodeDeleteChange = new EventEmitter();
    @Output() onDelete = new EventEmitter();
  @Input() nodeShare : Node;
  @Output() nodeShareChange = new EventEmitter();
    @Input() nodeShareLink : Node;
    @Output() nodeShareLinkChange = new EventEmitter();
    @Input() nodeWorkflow : Node;
    @Output() nodeWorkflowChange = new EventEmitter();
    @Input() nodeReport : Node;
    @Output() nodeReportChange = new EventEmitter();
    @Input() nodeVariant : Node;
    @Output() nodeVariantChange = new EventEmitter();
  @Input() nodeMetadata : Node;
    @Output() nodeMetadataChange = new EventEmitter();
    @Input() nodeTemplate : Node;
    @Output() nodeTemplateChange = new EventEmitter();
    @Input() nodeContributor : Node;
    @Output() nodeContributorChange = new EventEmitter();
  @Input() showUploadSelect=false;
  @Output() showUploadSelectChange = new EventEmitter();
  @Input() nodeMetadataAllowReplace : Boolean;
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onRefresh=new EventEmitter();
  @Output() onUploadFilesProcessed=new EventEmitter();
  @Output() onCloseMetadata=new EventEmitter();
  @Output() onUploadFileSelected=new EventEmitter();
  @Output() onUpdateLicense=new EventEmitter();
  @Output() onCloseAddToCollection=new EventEmitter();
  public createMetadata: string;
  public editorPending = false;
  public metadataParent: Node;
  public ltiToolConfig : Node;
  public ltiObject: Node;
  public dialogTitle:string;
  public dialogMessage:string;
  public dialogMessageParameters:any;
  public dialogCancelable:boolean;
  public dialogButtons:DialogButton[];
  private currentLtiTool: Node;
  private ltiToolRefresh: Boolean;
  @Input() nodeDeleteOnCancel: boolean;
  @Output() nodeDeleteOnCancelChange = new EventEmitter();
  private nodeLicenseOnUpload = false;
  private wasUploaded: boolean;

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.key=="Escape"){
      if(this.nodeMetadata!=null || this.createMetadata){
        if(this.mdsRef.handleKeyboardEvent(event))
          return;
        this.closeEditor(false);
        event.preventDefault();
        event.stopPropagation();
        return;
      }
      if(this.addToCollection!=null){
        this.cancelAddToCollection();
        event.preventDefault();
        event.stopPropagation();
        return;
      }
        if(this.nodeWorkflow!=null){
            this.closeWorkflow();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(this.nodeShare!=null){
            this.closeShare();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        if(this.nodeShareLink!=null){
            this.closeShareLink();
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
      if(this.nodeContributor!=null){
        this.closeContributor();
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
        if(this.nodeVariant!=null){
            this.closeVariant();
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
    private toolService:RestToolService,
    private temporaryStorage:TemporaryStorageService,
    private collectionService:RestCollectionService,
    private translate:TranslateService,
    private config:ConfigurationService,
    private searchService:RestSearchService,
    private toast:Toast,
    private router:Router,
  ){
   }
   private closeAddToCollection(){
      this.addToCollection=null;
      this.addToCollectionChange.emit(null);
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
    public closeWorkflow(refresh=false){
        this.nodeWorkflow=null;
        this.nodeWorkflowChange.emit(null);
        if(refresh)
            this.onRefresh.emit();
    }
    private deleteConfirmed(nodes : Node[],position=0,error=false) : void {
        if (position >= nodes.length) {
            this.globalProgress = false;
            if (!error)
                this.toast.toast("WORKSPACE.TOAST.DELETE_FINISHED");
            this.onDelete.emit({error:error,count:position});
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
                console.log("all items in clipboard removed");
                this.temporaryStorage.remove("workspace_clipboard");
            }
        }
    }
 public uploadDone(event : Node[]){
    if(this.config.instant('licenseDialogOnUpload',false)){
         this.nodeLicense=event;
         this.nodeLicenseOnUpload=true;
    }
    else if(this.filesToUpload.length==1){
        this.showMetadataAfterUpload(event);
    }
    else{
        this.onUploadFilesProcessed.emit(event);
    }
    this.wasUploaded=true;
    this.filesToUpload=null;
    this.filesToUploadChange.emit(null);
    this.onRefresh.emit();
  }
 public uploadFile(event:any){
   this.onUploadFileSelected.emit(event);
 }
  private createUrlLink(link : any){
    let prop:any={};
    let aspects:string[]=[];
    let url=UIHelper.addHttpIfRequired(link.link);
    prop[RestConstants.CCM_PROP_IO_WWWURL]=[url];
    if(link.lti){
        aspects.push(RestConstants.CCM_ASPECT_TOOL_INSTANCE_LINK);
        prop[RestConstants.CCM_PROP_TOOL_INSTANCE_KEY]=[link.consumerKey];
        prop[RestConstants.CCM_PROP_TOOL_INSTANCE_SECRET]=[link.sharedSecret];
    }
    prop[RestConstants.CCM_PROP_LINKTYPE]=[RestConstants.LINKTYPE_USER_GENERATED];
    this.closeUploadSelect();
    this.globalProgress=true;
    this.nodeService.createNode(this.parent.ref.id,RestConstants.CCM_TYPE_IO,aspects,prop,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
      (data:NodeWrapper)=>{
        this.globalProgress=false;
        this.nodeDeleteOnCancel=true;
        this.nodeDeleteOnCancelChange.emit(true);
        this.nodeMetadata=data.node;
        this.nodeMetadataAllowReplace=true;
        this.onRefresh.emit();
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
         this.nodeMetadata=this.nodeContributor;
     }
   this.nodeContributor=null;
   this.nodeContributorChange.emit(null);
 }
  private closeLtiTools() {
    this.showLtiTools = false;
    this.showLtiToolsChange.emit(false);
  }
  private closeLicense() {
    if(this.nodeLicenseOnUpload && this.nodeLicense.length==1){
      this.showMetadataAfterUpload(this.nodeLicense);
    }
    else {
        if(this.wasUploaded)
            this.onUploadFilesProcessed.emit(this.nodeLicense);
        this.wasUploaded = false;
    }
      if(this.editorPending){
          this.editorPending=false;
          this.nodeMetadata=this.nodeLicense[0];
      }
    this.nodeLicense=null;
    this.nodeLicenseOnUpload=false;
    this.nodeLicenseChange.emit(null);
  }
  private updateLicense(){
    this.closeLicense();
    this.onUpdateLicense.emit();
    this.onRefresh.emit();
  }
  private closeEditor(refresh:boolean,node:Node=null){
      if(node!=null && this.wasUploaded){
          this.onUploadFilesProcessed.emit([node]);
      }
      this.wasUploaded=false;
      if(this.nodeDeleteOnCancel && node==null){
          this.globalProgress=true;
          this.nodeService.deleteNode(this.nodeMetadata.ref.id,false).subscribe(()=>{
            this.nodeDeleteOnCancel=false;
            this.nodeDeleteOnCancelChange.emit(false);
            this.globalProgress=false;
            this.closeEditor(true);
          });
          return;
    }
    this.nodeDeleteOnCancel=false;
    this.nodeDeleteOnCancelChange.emit(false);
    this.nodeMetadata=null;
    this.nodeMetadataChange.emit(null);
    this.createMetadata=null;
    this.onCloseMetadata.emit();
    if(refresh) {
      this.onRefresh.emit();
      if(node && node.aspects.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION)!=-1) {
        this.currentLtiTool = node;
      }
      else{
        this.ltiToolRefresh=new Boolean();
      }
    }
  }
  public editLti(event:Node){
    //this.closeLtiTools();
    this.nodeMetadata=event;
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
  public addToCollectionCreate(){
      this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES,this.addToCollection);
      this.router.navigate([UIConstants.ROUTER_PREFIX,"collections","collection","new",RestConstants.ROOT]);
      this.cancelAddToCollection();
  }
  public addToCollectionList(collection:Collection,list:Node[]=this.addToCollection,close=true,callback:Function=null,force=false){
    console.log(collection);
    if(!force && (collection.scope!=RestConstants.COLLECTIONSCOPE_MY)){
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
    UIHelper.addToCollection(this.collectionService,this.router,this.toast,collection,list,()=>{
      this.globalProgress=false;
      if(callback)
        callback();
    });
  }

    private showMetadataAfterUpload(event: Node[]) {
        this.nodeMetadata=event[0];
        this.nodeMetadataAllowReplace=false;
        this.nodeDeleteOnCancel=true;
        this.nodeDeleteOnCancelChange.emit(true);
    }

    closeTemplate() {
        this.nodeTemplate = null;
        this.nodeTemplateChange.emit(null);
    }
}
