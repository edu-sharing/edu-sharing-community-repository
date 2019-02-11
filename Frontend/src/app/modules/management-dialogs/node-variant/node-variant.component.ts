import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {
    NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission,
    LoginResult, Connector
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
import {RestConnectorsService} from "../../../common/rest/services/rest-connectors.service";
import {FrameEventsService} from "../../../common/services/frame-events.service";
import {WorkspaceMainComponent} from "../../workspace/workspace.component";
import {NodeHelper} from "../../../common/ui/node-helper";
import {DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";

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
  openViaConnector: Connector;
  licenseWarning: string;
    private buttons: DialogButton[];
  @Input() set node(node : Node){
    this._node=node;
    this.variantName=this.translate.instant('NODE_VARIANT.DEFAULT_NAME',{name:this._node.name});
    this.openViaConnector=this.connectors.connectorSupportsEdit(node);
    let license=node.properties[RestConstants.CCM_PROP_LICENSE] ? node.properties[RestConstants.CCM_PROP_LICENSE][0] : "";
    console.log(license);
    if(license.startsWith('CC_BY') && license.indexOf('ND')!=-1){
      this.licenseWarning='ND';
    }
    else if(license.startsWith("COPYRIGHT")){
      this.licenseWarning='COPYRIGHT';
    }
    else if(!license){
      this.licenseWarning='NO_LICENSE';
    }
    this.updateButtons();
  }
  @Output() onLoading=new EventEmitter();
  @Output() onCancel=new EventEmitter();
  @Output() onDone=new EventEmitter();
  constructor(
    private connector : RestConnectorService,
    private iam : RestIamService,
    private translate : TranslateService,
    private connectors : RestConnectorsService,
    private config : ConfigurationService,
    private toast : Toast,
    private events : FrameEventsService,
    private router : Router,
    private nodeApi : RestNodeService) {
      this.updateBreadcrumbs(RestConstants.INBOX);
      this.updateButtons();
  }
  public cancel(){
    this.onCancel.emit();
  }

  public create(){
      if(!this.breadcrumbs || !this.breadcrumbs.length)
          return;
      let win:any=null;
      if(this.openViaConnector){
          win=UIHelper.getNewWindow(this.connector);
      }
      this.onLoading.emit(true);
      this.nodeApi.forkNode(this.breadcrumbs[this.breadcrumbs.length-1].ref.id,this._node.ref.id).subscribe((created)=>{
          this.nodeApi.editNodeMetadata(created.node.ref.id,RestHelper.createNameProperty(this.variantName)).subscribe((edited)=>{
              this.onLoading.emit(false);
              if(this.openViaConnector){
                  UIHelper.openConnector(this.connectors,this.events,this.toast,edited.node,null,win);
                  UIHelper.goToWorkspaceFolder(this.nodeApi,this.router,this.connector.getCurrentLogin(),this.breadcrumbs[this.breadcrumbs.length-1].ref.id);
              }
              else {
                  let additional={
                      link:{
                          caption:'NODE_VARIANT.CREATED_LINK',
                          callback:()=>{
                              UIHelper.goToWorkspaceFolder(this.nodeApi,this.router,this.connector.getCurrentLogin(),this.breadcrumbs[this.breadcrumbs.length-1].ref.id);
                          }
                      }
                  };
                  this.toast.toast('NODE_VARIANT.CREATED', {folder: this.breadcrumbs[this.breadcrumbs.length - 1].name}, null, null, additional);
              }
              this.onDone.emit();
          },(error)=>{
              this.onLoading.emit(false);
              NodeHelper.handleNodeError(this.toast,this.variantName,error);
              if(win)
                  win.close();
          });
      },(error)=>{
          this.onLoading.emit(false);
          NodeHelper.handleNodeError(this.toast,this.variantName,error);
          if(win)
              win.close();
      });
  }

    setDirectory(event: Node[]) {
      this.updateBreadcrumbs(event[0].ref.id);
    }

    private updateBreadcrumbs(id: string) {
      this.chooseDirectory=false;
        this.nodeApi.getNodeParents(id,false).subscribe((parents)=>{
            this.breadcrumbs=parents.nodes.reverse();
        })
    }
    openLicense(){
      UIHelper.openBlankWindow(this.getLicenseUrl(),this.connector.getCordovaService());
    }

    getLicenseUrl(): string {
        return NodeHelper.getLicenseUrlByString(this._node.properties[RestConstants.CCM_PROP_LICENSE],this._node.properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION])
  }
    updateButtons(): any {
        this.buttons=[
            new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
            new DialogButton('NODE_VARIANT.CREATE'+(this.openViaConnector ? '_EDIT' : ''),DialogButton.TYPE_PRIMARY,()=>this.create())
        ]
    }
}
