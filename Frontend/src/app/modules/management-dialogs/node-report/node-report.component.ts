import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
  NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
  LoginResult
} from "../../../common/rest/data-object";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {UIHelper} from "../../../common/ui/ui-helper";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'node-report',
  templateUrl: 'node-report.component.html',
  styleUrls: ['node-report.component.scss']
})
export class NodeReportComponent  {
  public reasons = [
    "UNAVAILABLE","INAPPROPRIATE_CONTENT","INVALID_METADATA","OTHER"
  ]
  public selectedReason : string;
  public comment : string;
  public email : string;
  public _node: Node;
  private isGuest: boolean;
  @Input() set node(node : Node){
    this._node=node;
  }
  @Output() onCancel=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  @Output() onDone=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private translate : TranslateService,
    private config : ConfigurationService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
    this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
      this.isGuest=data.isGuest;
      if(!data.isGuest){
        this.iam.getUser().subscribe((data)=>{
          this.email=data.person.profile.email;
        });
      }
    });
  }
  public cancel(){
    this.onCancel.emit();
  }
  public done(){
    this.onDone.emit();
  }
  public report(){
    if(!this.selectedReason){
      this.toast.error(null,'NODE_REPORT.REASON_REQUIRED');
      return;
    }
    if(!UIHelper.isEmail(this.email)){
      this.toast.error(null,'NODE_REPORT.EMAIL_REQUIRED');
      return;
    }
    this.onLoading.emit(true);
    this.nodeApi.reportNode(this._node.ref.id,this.getReasonAsString(),this.email,this.comment,this._node.ref.repo).subscribe(()=>{
      this.toast.toast('NODE_REPORT.DONE');
      this.onLoading.emit(false);
      this.onDone.emit();
    },(error:any)=>{
      this.onLoading.emit(false);
      this.toast.error(error);
    });
  }

  private getReasonAsString() {
    return this.translate.instant("NODE_REPORT.REASONS."+this.selectedReason)+" ("+this.selectedReason+")";
  }
}
