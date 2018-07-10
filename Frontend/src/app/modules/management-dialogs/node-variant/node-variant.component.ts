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
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";

@Component({
  selector: 'node-variant',
  templateUrl: 'node-variant.component.html',
  styleUrls: ['node-variant.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class NodeVariantComponent  {
  public _node: Node;
  private isGuest: boolean;
  @Input() set node(node : Node){
    this._node=node;
  }
  @Output() onCancel=new EventEmitter();
  @Output() onDone=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private translate : TranslateService,
    private config : ConfigurationService,
    private toast : Toast,
    private nodeApi : RestNodeService) {
  }
  public cancel(){
    this.onCancel.emit();
  }

  public create(){
    this.onDone.emit();
  }
}
