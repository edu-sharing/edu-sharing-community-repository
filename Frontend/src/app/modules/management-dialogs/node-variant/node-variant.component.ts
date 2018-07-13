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
import {RestConstants} from '../../../common/rest/rest-constants';
import {Router} from '@angular/router';
import {RestHelper} from '../../../common/rest/rest-helper';

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
  _node: Node;
  chooseDirectory = false;
  breadcrumbs: Node[];
  variantName : string;
  @Input() set node(node : Node){
    this._node=node;
    this.variantName=this.translate.instant('NODE_VARIANT.DEFAULT_NAME',{name:this._node.name});
  }
  @Output() onLoading=new EventEmitter();
  @Output() onCancel=new EventEmitter();
  @Output() onDone=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private translate : TranslateService,
    private config : ConfigurationService,
    private toast : Toast,
    private router : Router,
    private nodeApi : RestNodeService) {
      this.updateBreadcrumbs(RestConstants.INBOX);
  }
  public cancel(){
    this.onCancel.emit();
  }

  public create(){
      if(!this.breadcrumbs || !this.breadcrumbs.length)
          return;
      this.onLoading.emit(true);
      this.nodeApi.forkNode(this.breadcrumbs[this.breadcrumbs.length-1].ref.id,this._node.ref.id).subscribe((created)=>{
          this.nodeApi.editNodeMetadata(created.node.ref.id,RestHelper.createNameProperty(this.variantName)).subscribe(()=>{
              this.onLoading.emit(false);
              let additional={
                  link:{
                      caption:'NODE_VARIANT.CREATED_LINK',
                      callback:()=>{
                          UIHelper.goToWorkspaceFolder(this.nodeApi,this.router,this.connector.getCurrentLogin(),this.breadcrumbs[this.breadcrumbs.length-1].ref.id);
                      }
                  }
              };
              this.toast.toast('NODE_VARIANT.CREATED',{folder:this.breadcrumbs[this.breadcrumbs.length-1].name},null,null,additional);
              this.onDone.emit();
          },(error)=>{
              this.onLoading.emit(false);
              this.toast.error(error);
          });
      },(error)=>{
          this.onLoading.emit(false);
          this.toast.error(error);
      });
  }

    setDirectory(event: Node[]) {
      this.updateBreadcrumbs(event[0].ref.id);
    }

    private updateBreadcrumbs(id: string) {
      this.chooseDirectory=false;
        this.nodeApi.getNodeParents(id).subscribe((parents)=>{
            this.breadcrumbs=parents.nodes.reverse();
        })
    }
}
