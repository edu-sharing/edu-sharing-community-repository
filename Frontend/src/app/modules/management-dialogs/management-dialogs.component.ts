import {forkJoin as observableForkJoin, observable, Observable} from 'rxjs';
import {
    Component,
    Input,
    EventEmitter,
    Output,
    ViewChild,
    ElementRef,
    HostListener,
    ContentChild, TemplateRef
} from '@angular/core';
import {
    CollectionProposalStatus,
    CollectionReference,
    DialogButton,
    LocalPermissions,
    NodeProperties,
    NodeVersions, ProposalNode,
    RestConnectorService,
    RestNodeService,
    Version
} from '../../core-module/core.module';
import {TranslateService} from "@ngx-translate/core";
import {RestSearchService} from "../../core-module/core.module";
import {Toast} from "../../core-ui-module/toast";
import {RestConstants} from "../../core-module/core.module";
import {NodeWrapper, Node, Collection} from "../../core-module/core.module";
import {RestHelper} from "../../core-module/core.module";
import {RestToolService} from "../../core-module/core.module";
import {ConfigurationService} from "../../core-module/core.module";
import {RestCollectionService} from "../../core-module/core.module";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../core-module/ui/ui-animation";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {Router} from '@angular/router';
import {UIConstants} from "../../core-module/ui/ui-constants";
import {ClipboardObject, TemporaryStorageService} from '../../core-module/core.module';
import {RestUsageService} from "../../core-module/core.module";
import {BridgeService} from '../../core-bridge-module/bridge.service';
import {LinkData, NodeHelperService} from '../../core-ui-module/node-helper.service';
import {WorkspaceLicenseComponent} from './license/license.component';
import {ErrorProcessingService} from '../../core-ui-module/error.processing';
import { BulkBehavior } from '../../features/mds/types/types';
import { MdsEditorWrapperComponent } from '../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MainNavService } from 'src/app/main/navigation/main-nav.service';
import { first } from 'rxjs/operators';


export enum DialogType {
    SimpleEdit = 'SimpleEdit',
    Mds = 'Mds'
}
export enum ManagementEventType {
    AddCollectionNodes,
}
export interface ManagementEvent {
    event: ManagementEventType,
    data?: any
}
@Component({
  selector: 'es-workspace-management',
  templateUrl: 'management-dialogs.component.html',
  styleUrls: ['management-dialogs.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('fromLeft', UIAnimation.fromLeft()),
    trigger('fromRight',UIAnimation.fromRight())
  ]
})
export class WorkspaceManagementDialogsComponent  {
  readonly BulkBehaviour = BulkBehavior;
  @ViewChild(MdsEditorWrapperComponent) mdsEditorWrapperRef : MdsEditorWrapperComponent;
  @ViewChild(WorkspaceLicenseComponent) licenseComponent : WorkspaceLicenseComponent;
  @ContentChild('collectionChooserBeforeRecent') collectionChooserBeforeRecentRef: TemplateRef<any>;
  @Input() showLtiTools = false;
  @Input() uploadShowPicker = false;
  @Input() uploadMultiple = true;
  @Input() fileIsOver = false;
  @Input() addToCollection:Node[];
  @Output() addToCollectionChange = new EventEmitter();
  @Input() filesToUpload : FileList;
  @Output() filesToUploadChange = new EventEmitter();
  @Input() parent : Node;
  @Output() showLtiToolsChange = new EventEmitter();
  @Input() nodeLicense : Node[];
  @Output() nodeLicenseChange = new EventEmitter();
  @Input() addPinnedCollection: Node;
  @Output() addPinnedCollectionChange = new EventEmitter();
  @Output() onEvent = new EventEmitter<ManagementEvent>();
  @Input() linkMap: Node;
  @Output() linkMapChange = new EventEmitter<Node>();
  @Input() set nodeImportUnblock (nodeImportUnblock: Node[]) {
      this.toast.showConfigurableDialog({
          title: 'WORKSPACE.UNBLOCK_TITLE',
          message: 'WORKSPACE.UNBLOCK_MESSAGE',
          buttons: DialogButton.getOkCancel(
              () => this.toast.closeModalDialog(),
              () => this.unblockImportedNodes(nodeImportUnblock)),
          isCancelable: true
      });
  }
  @Input() set nodeDelete (nodeDelete: Node[]){
        this._nodeDelete = nodeDelete;
        if(nodeDelete==null)
            return;
        this.nodeDeleteTitle='WORKSPACE.DELETE_TITLE'+(nodeDelete.length === 1 ? '_SINGLE' : '');
        this.nodeDeleteMessage='WORKSPACE.DELETE_MESSAGE'+(nodeDelete.length === 1 ? '_SINGLE' : '');
        this.nodeDeleteMessageParams = {name:RestHelper.getName(nodeDelete[0])};
        this.nodeDeleteButtons=DialogButton.getCancel(()=> {this._nodeDelete = null});
        this.nodeDeleteButtons.push(new DialogButton('YES_DELETE',DialogButton.TYPE_DANGER,()=>{this.deleteConfirmed(nodeDelete)}));
      if(nodeDelete.length === 1) {
          if (nodeDelete[0].collection) {
              this.nodeDeleteTitle = 'WORKSPACE.DELETE_TITLE_COLLECTION';
              this.nodeDeleteMessage = 'WORKSPACE.DELETE_MESSAGE_COLLECTION';
          } else if (this.nodeHelper.isNodePublishedCopy(nodeDelete[0])) {
              this.nodeDeleteTitle = 'WORKSPACE.DELETE_TITLE_PUBLISHED_COPY';
              this.nodeDeleteMessage = 'WORKSPACE.DELETE_MESSAGE_PUBLISHED_COPY';
          } else if (nodeDelete[0].mediatype === 'folder-link') {
              this.nodeDeleteTitle = 'WORKSPACE.DELETE_TITLE_FOLDER_LINK';
              this.nodeDeleteMessage = 'WORKSPACE.DELETE_MESSAGE_FOLDER_LINK';
          } else if (!nodeDelete[0].isDirectory) {
              // check for usages and warn user
              this.usageService.getNodeUsages(nodeDelete[0].ref.id, nodeDelete[0].ref.repo).subscribe((usages) => {
                  if (usages.usages.length > 0) {
                      this.nodeDeleteMessage = 'WORKSPACE.DELETE_MESSAGE_SINGLE_USAGES';
                      this.nodeDeleteMessageParams = {name: nodeDelete[0].name, usages: usages.usages.length};
                  }
              });
          }
      }
      this.nodeDeleteBlock = this.connector.getCurrentLogin()?.isAdmin &&
          nodeDelete.every((n) => n.properties[RestConstants.CCM_PROP_REPLICATIONSOURCE] != null);
      this.nodeDeleteBlockStatus = this.nodeDeleteBlock;
    }
    @Output() nodeDeleteChange = new EventEmitter();
    @Output() onDelete = new EventEmitter<{objects: Node[]|any,error: boolean, count: number}>();
  @Input() nodeShare : Node[];
  @Output() nodeShareChange = new EventEmitter<Node[]>();
    @Input() nodeDebug : Node[];
    @Output() nodeDebugChange = new EventEmitter<Node[]>();
    @Input() nodeShareLink : Node;
    @Output() nodeShareLinkChange = new EventEmitter();
    @Input() nodeWorkflow : Node[];
    @Output() nodeWorkflowChange = new EventEmitter();
    @Input() signupGroup : boolean;
    @Output() signupGroupChange = new EventEmitter<boolean>();
  @Input() nodeReport : Node;
  @Output() nodeReportChange = new EventEmitter();
  @Input() addNodesStream : Node[];
  @Output() addNodesStreamChange = new EventEmitter();
    @Input() nodeVariant : Node;
    @Output() nodeVariantChange = new EventEmitter();
    @Input() set nodeMetadata (nodeMetadata : Node[]){
      this._nodeMetadata = nodeMetadata;
      this._nodeFromUpload = false;
  }
    @Output() nodeMetadataChange = new EventEmitter<Node[]>();
    @Input() nodeTemplate : Node;
    @Output() nodeTemplateChange = new EventEmitter();
    @Input() nodeContributor : Node;
    @Output() nodeContributorChange = new EventEmitter<Node>();
    @Input() set nodeSimpleEdit (nodeSimpleEdit: Node[]) {
        this._nodeSimpleEdit = nodeSimpleEdit;
        this._nodeFromUpload = false;
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
  @Output() onUploadSelectCanceled = new EventEmitter();
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onRefresh=new EventEmitter<Node[]|void>();
  @Output() onUploadFilesProcessed=new EventEmitter<Node[]>();
  @Output() onCloseMetadata=new EventEmitter();
  @Output() onUploadFileSelected=new EventEmitter<FileList>();
  @Output() onUpdateLicense=new EventEmitter();
  @Output() onCloseAddToCollection=new EventEmitter();
  @Output() onStoredAddToCollection=new EventEmitter<{collection: Node, references: CollectionReference[]}>();
  _nodeDelete: Node[];
  _nodeMetadata: Node[];
  _nodeSimpleEdit: Node[];
  _nodeFromUpload = false;
  nodeDeleteTitle: string;
  nodeDeleteMessage: string;
  nodeDeleteMessageParams: any;
  nodeDeleteBlock: boolean;
  nodeDeleteBlockStatus = true;
  nodeDeleteButtons: DialogButton[];
  public createMetadata: string;
  public editorPending = false;
  public metadataParent: Node;
  public ltiToolConfig : Node;
  public ltiObject: Node;
  currentLtiTool: Node;
  ltiToolRefresh: Boolean;
  @Input() nodeDeleteOnCancel: boolean;
  @Output() nodeDeleteOnCancelChange = new EventEmitter();
  private nodeLicenseOnUpload = false;
    /**
     * QR Code object data to print
     * @node: Reference to the node (for header title)
     * @data: The string to display inside the qr code (e.g. an url)
     */
  @Input() qr: {node: Node, data: string};

    /**
     * @Deprecated, the components should use toast service directly
     */
  set globalProgress (globalProgress: boolean) {
      if(globalProgress) {
          this.toast.showProgressDialog();
      } else {
          this.toast.closeModalDialog();
      }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.key === 'Escape') {
      if(this._nodeMetadata!=null || this.createMetadata){
        if (this.mdsEditorWrapperRef.handleKeyboardEvent(event)) {
          return;
        }
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
    private connector:RestConnectorService,
    private searchService:RestSearchService,
    private mainNavService:MainNavService,
    private toast:Toast,
    private errorProcessing:ErrorProcessingService,
    private nodeHelper: NodeHelperService,
    private bridge:BridgeService,
    private router:Router,
  ){
   }
 closeLtiToolConfig(){
    this.ltiToolConfig=null;
    this.ltiToolRefresh=new Boolean();
 }
    closeShareLink() {
        this.nodeShareLink = null
        this.nodeShareLinkChange.emit(null);
    }
 closeShare() {
      // reload node metadata
     this.toast.showProgressDialog();
     observableForkJoin(this.nodeShare.map((n) => this.nodeService.getNodeMetadata(n.ref.id, [RestConstants.ALL])))
         .subscribe((nodes: NodeWrapper[]) => {
             this.onRefresh.emit(nodes.map(n => n.node));
             this.nodeShare = null
             this.nodeShareChange.emit(null);
             this.toast.closeModalDialog();
         }, error => {
             this.toast.closeModalDialog();
         });
 }
    public closeWorkflow(nodes: Node[] = null){
        this.nodeWorkflow = null;
        this.nodeWorkflowChange.emit(null);
        if (nodes) {
            this.onRefresh.emit(nodes);
        }
    }
    private deleteConfirmed(nodes : Node[],position=0,error=false) : void {
        if (position >= nodes.length) {
            this.toast.closeModalDialog();
            this._nodeDelete = null;
            if (!error) {
                this.toast.toast('WORKSPACE.TOAST.DELETE_FINISHED');
            }
            if (this.nodeDeleteBlockStatus) {
                this.onRefresh.emit(nodes);
            } else {
                this.onDelete.emit({objects: nodes, error: error, count: position});
            }
            return;
        }
        this.toast.showProgressDialog();
        let callback;
        if(this.nodeDeleteBlockStatus) {
            const props: any = {};
            props[RestConstants.CCM_PROP_IMPORT_BLOCKED] = [true];
            callback = new Observable((observer) => {
                this.nodeService.editNodeMetadataNewVersion(
                    nodes[position].ref.id,
                    RestConstants.COMMENT_BLOCKED_IMPORT,
                    props
                ).subscribe(({node}) => {
                    const permissions = new LocalPermissions();
                    permissions.inherited = false;
                    permissions.permissions = [];
                    this.nodeService.setNodePermissions(node.ref.id, permissions).subscribe(() => {
                        observer.next(node);
                        observer.complete();
                    });
                })
            });
        } else {
            callback = this.nodeService.deleteNode(nodes[position].ref.id);
        }
        callback.subscribe((node: Node) => {
            if(node) {
                nodes[position] = node;
            }
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
 public uploadFile(event: FileList){
   this.onUploadFileSelected.emit(event);
 }
  async createUrlLink(link : LinkData) {
    const urlData = this.nodeHelper.createUrlLink(link);
    this.closeUploadSelect();
    this.toast.showProgressDialog();
    const config = await this.mainNavService.observeMainNavConfig().pipe(first()).toPromise();
    this.nodeService.createNode(config.create?.parent?.ref.id,RestConstants.CCM_TYPE_IO,urlData.aspects,urlData.properties,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe(
      (data) => {
        this.showMetadataAfterUpload([data.node]);
        this.toast.closeModalDialog();
      });
  }
 openLtiConfig(event:Node){
   this.ltiToolConfig=event;
 }
 public closeUploadSelect(){
   this.showUploadSelect=false
   this.showUploadSelectChange.emit(false);
 }
  public cancelUploadSelect(){
    this.closeUploadSelect();
    this.onUploadSelectCanceled.emit(false);
  }
 public closeContributor(node: Node){
     if(this.editorPending){
         this.editorPending=false;
         this._nodeMetadata=[this.nodeContributor];
     }
   this.nodeContributor=null;
   this.nodeContributorChange.emit(node);
   if(node) {
       this.onRefresh.emit([node]);
   }
  }
  closeLtiTools() {
    this.showLtiTools = false;
    this.showLtiToolsChange.emit(false);
  }
  closeLicense() {
    if(this.nodeLicenseOnUpload){
      this.showMetadataAfterUpload(this.nodeLicense);
    }
    else {
        if(this._nodeFromUpload)
            this.onUploadFilesProcessed.emit(this.nodeLicense);
        this._nodeFromUpload = false;
    }
      if(this.editorPending){
          this.editorPending=false;
          this._nodeMetadata=this.nodeLicense;
      }
    this.nodeLicense=null;
    this.nodeLicenseOnUpload=false;
    this.nodeLicenseChange.emit(null);
  }
  updateLicense(nodes:Node[]) {
    this.closeLicense();
    this.onUpdateLicense.emit();
    this.onRefresh.emit(nodes);
  }
  deleteNodes(nodes: Node[]) {
      this.toast.showProgressDialog();
      observableForkJoin(nodes.map((n) => this.nodeService.deleteNode(n.ref.id, false)))
          .subscribe(() => {
              this.toast.closeModalDialog();
              this.closeEditor(true);
          });
  }
  closeEditor(refresh:boolean,nodes: Node[]=null){
      if (this.nodeDeleteOnCancel && nodes == null) {
          this.nodeDeleteOnCancel = false;
          this.deleteNodes(this._nodeMetadata);
          return;
      }
    this.setNodeDeleteOnCancel(false);
    this._nodeMetadata=null;
    this.nodeMetadataChange.emit(null);
    this.createMetadata=null;
    this.onCloseMetadata.emit(nodes);
    if(refresh) {
        if(this._nodeFromUpload) {
            this.onUploadFilesProcessed.emit(nodes);
        }
        this.onRefresh.emit(nodes);
      if(nodes?.length === 1 && nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION) !== -1) {
        this.currentLtiTool = nodes[0];
      }
      else{
        this.ltiToolRefresh=new Boolean();
      }
    }
  }
  public editLti(event:Node){
    //this.closeLtiTools();
    this._nodeMetadata=[event];
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
  cancelAddToCollection(){
    this.addToCollection=null;
    this.addToCollectionChange.emit(null);
    this.onCloseAddToCollection.emit();
  }
  public addToCollectionCreate(parent:Node=null){
      this.temporaryStorage.set(TemporaryStorageService.COLLECTION_ADD_NODES,{
          nodes: this.addToCollection,
          callback: this.onStoredAddToCollection
      });
      this.router.navigate([UIConstants.ROUTER_PREFIX,"collections","collection","new",parent ? parent.ref.id : RestConstants.ROOT]);
      this.addToCollection=null;
      this.addToCollectionChange.emit(null);
  }
    public addToCollectionList(collection:Node,list:Node[]=this.addToCollection,close=true,callback:() => void =null, asProposal = false,force=false){
        if(!force) {
            if ((collection.access.indexOf(RestConstants.ACCESS_WRITE) === -1)) {
                this.toast.showConfigurableDialog({
                    title: 'DIALOG.COLLECTION_PROPSE',
                    message: 'DIALOG.COLLECTION_PROPSE_INFO',
                    messageParameters: {collection: RestHelper.getTitle(collection)},
                    buttons: DialogButton.getNextCancel(() => this.toast.closeModalDialog(), () => {
                        this.toast.closeModalDialog();
                        this.addToCollectionList(collection, list, close, callback, true, true);
                    })
                });
                return;
            } else if ((collection.collection.scope !== RestConstants.COLLECTIONSCOPE_MY)) {
                this.toast.showConfigurableDialog({
                    title: 'DIALOG.COLLECTION_SHARE_PUBLIC',
                    message: 'DIALOG.COLLECTION_SHARE_PUBLIC_INFO',
                    messageParameters: {collection: RestHelper.getTitle(collection)},
                    buttons: DialogButton.getNextCancel(() => this.toast.closeModalDialog(), () => {
                        this.toast.closeModalDialog();
                        this.addToCollectionList(collection, list, close, callback, asProposal, true);
                    })
                });
                return;
            }
        }
        if(close)
            this.cancelAddToCollection();
        else {
            this.toast.closeModalDialog();
        }
        this.toast.showProgressDialog();
        UIHelper.addToCollection(this.nodeHelper, this.collectionService,this.router,this.bridge,collection,list, asProposal,(nodes) => {
            this.toast.closeModalDialog();
            this.onStoredAddToCollection.emit({collection, references: nodes});
            if(callback) {
                callback();
            }
        });
    }

    private showMetadataAfterUpload(event: Node[]) {
        const dialog = this.config.instant('upload.postDialog', DialogType.SimpleEdit);
        if(dialog === DialogType.SimpleEdit) {
            this._nodeSimpleEdit = event;
            this.nodeSimpleEditChange.emit(event);
        } else if (dialog === DialogType.Mds){
            this._nodeMetadata = event;
        } else {
            console.error('Invalid configuration for upload.postDialog: ' + dialog);
        }
        this._nodeFromUpload = true;
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
    restoreVersion(restore:{version: Version,node: Node}) {
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
        if (saved && this._nodeFromUpload) {
            this.onUploadFilesProcessed.emit(nodes);
        } else if(!saved && this.nodeDeleteOnCancel) {
            this.deleteNodes(this._nodeSimpleEdit);
            this.onUploadFilesProcessed.emit(null);
        }
        if (nodes) {
            this.onRefresh.emit(nodes);
        }
        this.setNodeDeleteOnCancel(false);
        this._nodeSimpleEdit = null;
        this.nodeSimpleEditChange.emit(null);
    }

    private unblockImportedNodes(nodes: Node[]) {
        this.toast.showProgressDialog();
        observableForkJoin(nodes.map((n) => {
            const properties: any = {};
            properties[RestConstants.CCM_PROP_IMPORT_BLOCKED] = [null];
            return new Observable((observer) => {
                this.nodeService.editNodeMetadataNewVersion(n.ref.id, RestConstants.COMMENT_BLOCKED_IMPORT, properties)
                    .subscribe(({node}) => {
                        const permissions = new LocalPermissions();
                        permissions.inherited = true;
                        permissions.permissions = [];
                        this.nodeService.setNodePermissions(node.ref.id, permissions).subscribe(() => {
                            observer.next(node);
                            observer.complete();
                        })
                    })
            });
        })).subscribe((results: Node[]) => {
            this.toast.closeModalDialog();
            this.onRefresh.emit(results);
        });
    }

    closeLinkMap(node: Node = null) {
      this.linkMap = null;
      this.linkMapChange.emit(null);
    }

    private setNodeDeleteOnCancel(nodeDeleteOnCancel: boolean) {
        this.nodeDeleteOnCancel = nodeDeleteOnCancel;
        this.nodeDeleteOnCancelChange.emit(nodeDeleteOnCancel);
    }

    declineProposals(nodes: ProposalNode[]) {
        this.errorProcessing.handleRestRequest(
            observableForkJoin(nodes.map((n) =>
                this.nodeService.editNodeProperty(n.proposal.ref.id,
                    RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                    [('DECLINED' as CollectionProposalStatus)])
            ))).then(() => {
                this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.DECLINED');
                this.onDelete.emit({objects: nodes, error: false, count: nodes.length});
            });
    }

    addProposalsToCollection(nodes: ProposalNode[]) {
        this.errorProcessing.handleRestRequest(
        observableForkJoin(nodes.map((n) =>
            this.nodeService.editNodeProperty(n.proposal.ref.id,
                RestConstants.CCM_PROP_COLLECTION_PROPOSAL_STATUS,
                [('ACCEPTED' as CollectionProposalStatus)])

        ))).then(() => {
            this.errorProcessing.handleRestRequest(observableForkJoin(nodes.map((n) =>
                this.collectionService.addNodeToCollection(n.proposalCollection.ref.id,
                    n.ref.id,
                    n.ref.repo)
            ))).then((results) => {
                this.toast.toast('COLLECTIONS.PROPOSALS.TOAST.ACCEPTED');
                this.onDelete.emit({objects: nodes, error: false, count: nodes.length});
                this.onRefresh.emit();
                this.onEvent.emit({
                    event: ManagementEventType.AddCollectionNodes,
                    data: {
                        collection: nodes[0].proposalCollection,
                        references: results.map((r) => r.node)
                    }
                });
            });
        });
    }
}
