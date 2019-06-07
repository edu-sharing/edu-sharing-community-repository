import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {
  NodeWrapper, Node, IamUsers, WorkflowEntry, NodePermissions,
  Permission, UserSimple, WorkflowDefinition
} from "../../../common/rest/data-object";
import {VCard} from "../../../common/VCard";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {Translation} from "../../../common/translation";
import {TranslateService} from "@ngx-translate/core";
import any = jasmine.any;
import {SuggestItem} from "../../../common/ui/autocomplete/autocomplete.component";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {NodeHelper} from "../../../common/ui/node-helper";
import {AuthorityNamePipe} from "../../../common/ui/authority-name.pipe";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {UIHelper} from "../../../common/ui/ui-helper";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {RestHelper} from "../../../common/rest/rest-helper";

@Component({
  selector: 'workspace-workflow',
  templateUrl: 'workflow.component.html',
  styleUrls: ['workflow.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceWorkflowComponent  {
  private _nodeId: string;
  public dialogTitle:string;
  public dialogMessage:string;
  public dialogMessageParameters:any;
  public dialogButtons:DialogButton[];
  public loading=true;
  public node: Node;
  public receivers:UserSimple[]=[];
  public status=RestConstants.WORKFLOW_STATUS_UNCHECKED;
  public initialStatus=RestConstants.WORKFLOW_STATUS_UNCHECKED;
  public chooseStatus = false;
  public comment:string;
  public validStatus:WorkflowDefinition[];
  public history: WorkflowEntry[];
  public globalAllowed: boolean;
  public globalSearch = false;
  public TYPE_EDITORIAL=RestConstants.GROUP_TYPE_EDITORIAL;
  @Input() set nodeId(nodeId : string){
    this._nodeId=nodeId;
    this.loading=true;
    this.nodeService.getNodeMetadata(nodeId,[RestConstants.ALL]).subscribe((data:NodeWrapper)=> {
      this.nodeService.getWorkflowHistory(nodeId).subscribe((workflow:WorkflowEntry[])=>{
        this.history=workflow;
        this.node = data.node;
        this.loading=false;
        //if(this.node.properties[RestConstants.CCM_PROP_WF_RECEIVER])
        //  this.receivers=JSON.parse(JSON.stringify(this.node.properties[RestConstants.CCM_PROP_WF_RECEIVER]));
        if(workflow.length)
          this.receivers = workflow[0].receiver;
        if(!this.receivers || (this.receivers.length==1 && !this.receivers[0]))
          this.receivers=[];
        this.status=NodeHelper.getWorkflowStatus(this.config,this.node);
        this.initialStatus=this.status;
      },(error:any)=>{
        this.toast.error(error);
        this.cancel();
      });

    });
  }
  @Output() onDone=new EventEmitter();
  @Output() onClose=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  public isAllowedAsNext(status:WorkflowDefinition){
    if(!this.initialStatus.next)
      return true;
    if(this.initialStatus.id==status.id)
      return true;
    return this.initialStatus.next.indexOf(status.id)!=-1;
  }
  public setStatus(status:WorkflowDefinition){
    if(!this.isAllowedAsNext(status)){
      return;
    }
    this.status=status;
    this.chooseStatus=false;
  }
  private addSuggestion(data: UserSimple) {
    /*if(this.receivers.indexOf(data.item.id)==-1)
      this.receivers.push(data.item.id);*/
    this.receivers=[data];
  }
  public getWorkflowForId(id:string){
    return NodeHelper.getWorkflowStatusById(this.config,id);
  }
  public removeReceiver(data : UserSimple){
    let pos=this.receivers.indexOf(data);
    if(pos!=-1){
      this.receivers.splice(pos,1);
    }
  }
  public hasChanges(){
    return this.statusChanged() || this.receiversChanged();
  }
  public saveWorkflow(){
    if(!this.comment && this.receiversChanged()){
      this.toast.error(null,'WORKSPACE.WORKFLOW.NO_COMMENT');
      return;
    }
    let receivers=this.status.hasReceiver ? this.receivers : [];
    if(receivers.length){
      this.nodeService.getNodePermissionsForUser(this._nodeId,receivers[0].authorityName).subscribe((data:string[])=>{
        if(data.indexOf(RestConstants.PERMISSION_COORDINATOR)==-1){
          this.dialogTitle='WORKSPACE.WORKFLOW.USER_NO_PERMISSION';
          this.dialogMessage='WORKSPACE.WORKFLOW.USER_NO_PERMISSION_INFO';
          this.dialogMessageParameters={user:new AuthorityNamePipe(this.translate).transform(receivers[0],null)};
          this.dialogButtons=[
            new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>{this.dialogTitle=null;}),
            new DialogButton('WORKSPACE.WORKFLOW.PROCEED',DialogButton.TYPE_PRIMARY,()=>{
              this.addWritePermission(receivers[0].authorityName);
              this.dialogTitle=null;
              this.loading=true;
            })
            ];
          return;
        }
        else{
          this.saveWorkflowFinal(receivers);
        }
     },(error:any)=>{this.toast.error(error)});
      return;
    }
    this.saveWorkflowFinal(receivers);
  }
  private saveWorkflowFinal(receivers:UserSimple[]){
    let entry=new WorkflowEntry();
    let receiversClean:any[]=[];
    for(let r of receivers){
      receiversClean.push({authorityName:r.authorityName});
    }
    entry.receiver=receiversClean;
    entry.comment=this.comment;
    entry.status=this.status.id;
    this.onLoading.emit(true);
    this.nodeService.addWorkflow(this._nodeId,entry).subscribe(()=>{
      this.toast.toast('WORKSPACE.TOAST.WORKFLOW_UPDATED');
      this.onDone.emit();
      this.onLoading.emit(false);
    },(error:any)=>{
      this.toast.error(error);
      this.onLoading.emit(false);
    });
  }
  public cancel(){
    this.onClose.emit();
  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private iam:RestIamService,
    private config:ConfigurationService,
    private connector:RestConnectorService,
    private toast:Toast,
  ){
    this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH).subscribe((has:boolean)=>this.globalAllowed=has);
    this.config.getAll().subscribe(()=> {
      this.validStatus = NodeHelper.getWorkflows(this.config);
    });
  }
  private receiversChanged(){
      let prop=this.node.properties[RestConstants.CCM_PROP_WF_RECEIVER];
      if(prop){
        if(prop.length!=this.receivers.length)
          return true;
        for(let receiver of prop){
          if(this.receivers.indexOf(receiver)==-1)
            return true;
        }
        return false;
      }
    return this.receivers.length>0;
  }
  private statusChanged() {
    if(this.node.properties[RestConstants.CCM_PROP_WF_STATUS])
      return this.status!=this.node.properties[RestConstants.CCM_PROP_WF_STATUS][0];
    return this.status!=RestConstants.WORKFLOW_STATUS_UNCHECKED;
  }

  private addWritePermission(authority: string) {
    this.nodeService.getNodePermissions(this._nodeId).subscribe((data:NodePermissions)=>{
      let permission=new Permission();
      permission.authority={authorityName:authority,authorityType:RestConstants.AUTHORITY_TYPE_USER};
      permission.permissions=[RestConstants.PERMISSION_COORDINATOR];
      data.permissions.localPermissions.permissions.push(permission);
      let permissions=RestHelper.copyAndCleanPermissions(data.permissions.localPermissions.permissions,data.permissions.localPermissions.inherited);
      this.nodeService.setNodePermissions(this._nodeId,permissions,false).subscribe(()=>{
        this.saveWorkflow();
      },(error:any)=>this.toast.error(error));
    },(error:any)=>this.toast.error(error));
  }
}
