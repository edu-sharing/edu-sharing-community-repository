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
  @Input() nodeComment : Node;
  @Output() nodeCommentChange = new EventEmitter();
  @Input() nodeReport : Node;
  @Output() nodeReportChange = new EventEmitter();
  @Input() nodeMetadata : Node;
  @Input() nodeContributor : Node;
  @Output() nodeContributorChange = new EventEmitter();
  @Output() nodeMetadataChange = new EventEmitter();
  @Input() showUploadSelect=false;
  @Output() showUploadSelectChange = new EventEmitter();
  @Input() nodeMetadataAllowReplace : boolean;
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onRefresh=new EventEmitter();
  @Output() onCloseMetadata=new EventEmitter();
  @Output() onUploadFileSelected=new EventEmitter();
  @Output() onUpdateLicense=new EventEmitter();
  @Output() onCloseAddToCollection=new EventEmitter();
  public createMetadata: string;
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

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.key=="Escape"){
      if(this.nodeMetadata!=null || this.createMetadata){
        if(this.mdsRef.handleKeyboardEvent(event))
          return;
        this.closeEditor(false);
        event.stopPropagation();
        return;
      }
      if(this.addToCollection!=null){
        this.cancelAddToCollection();
        event.stopPropagation();
        return;
      }
      if(this.nodeContributor!=null){
        this.closeContributor();
        event.stopPropagation();
        return;
      }
      if(this.nodeLicense!=null){
        this.closeLicense();
        event.stopPropagation();
        return;
      }
      if(this.showLtiTools){
        this.closeLtiTools();
        event.stopPropagation();
        return;
      }
      if(this.nodeReport!=null){
        this.closeReport();
        event.stopPropagation();
        return;
      }
      if(this.ltiObject){
        this.ltiObject=null;
        event.stopPropagation();
        return;
      }
    }
  }
  public constructor(
    private nodeService:RestNodeService,
    private toolService:RestToolService,
    private collectionService:RestCollectionService,
    private translate:TranslateService,
    private config:ConfigurationService,
    private searchService:RestSearchService,
    private toast:Toast,
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
 public uploadDone(event : Node[]){
    if(this.filesToUpload.length==1){
      this.nodeMetadata=event[0];
      this.nodeMetadataAllowReplace=false;
      this.nodeDeleteOnCancel=true;
      this.nodeDeleteOnCancelChange.emit(true);
    }
    if(this.config.instant('licenseDialogOnUpload',false)){
      this.nodeLicense=event;
    }
    this.filesToUpload=null;
    this.filesToUploadChange.emit(null);

    this.onRefresh.emit();
  }
 public uploadFile(event:any){
   this.onUploadFileSelected.emit(event);
 }
  private createUrlLink(link : string){
    let prop:any={};
    prop[RestConstants.CCM_PROP_IO_WWWURL]=[link];
    this.closeUploadSelect();
    this.globalProgress=true;
    this.nodeService.createNode(this.parent.ref.id,RestConstants.CCM_TYPE_IO,[],prop,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
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
   this.nodeContributor=null
   this.nodeContributorChange.emit(null);
 }
  private closeLtiTools() {
    this.showLtiTools = false;
    this.showLtiToolsChange.emit(false);
  }
  private closeLicense() {
    this.nodeLicense=null;
    this.nodeLicenseChange.emit(null);
  }
  private updateLicense(){
    this.closeLicense();
    this.onUpdateLicense.emit();
    this.onRefresh.emit();
  }
  private closeEditor(refresh:boolean,node:Node=null){
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
  public closeComments() {
    this.nodeComment=null;
    this.nodeCommentChange.emit(null);
  }
  private cancelAddToCollection(){
    this.dialogTitle=null;
    this.addToCollection=null;
    this.addToCollectionChange.emit(null);
    this.onCloseAddToCollection.emit();
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
    UIHelper.addToCollection(this.collectionService,this.toast,collection,list,()=>{
      this.globalProgress=false;
      if(callback)
        callback();
    });
  }

}
