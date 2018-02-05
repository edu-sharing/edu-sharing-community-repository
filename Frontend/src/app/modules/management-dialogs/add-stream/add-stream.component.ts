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
  selector: 'add-stream',
  templateUrl: 'add-stream.component.html',
  styleUrls: ['add-stream.component.scss']
})
export class AddStreamComponent  {
  private _nodes: any;
  @Input() set nodes(nodes : Node[]){
    this._nodes=nodes;
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

    });
  }
  public cancel(){
    this.onCancel.emit();
  }
  public done(){
    this.onDone.emit();
  }
  public save(){
    this.onLoading.emit(true);

  }
}
