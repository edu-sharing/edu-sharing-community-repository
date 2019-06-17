import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DialogButton, RestConnectorService} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {RestNodeService} from "../../../core-module/core.module";
import {Connector, Node} from "../../../core-module/core.module";
import {ConfigurationService} from "../../../core-module/core.module";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {RestIamService} from "../../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {RestConstants} from '../../../core-module/core.module';
import {Router} from '@angular/router';
import {RestHelper} from '../../../core-module/core.module';
import {RestConnectorsService} from "../../../core-module/core.module";
import {FrameEventsService} from "../../../core-module/core.module";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {OPEN_URL_MODE} from "../../../core-module/ui/ui-constants";

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
                  UIHelper.openConnector(this.connectors,this.iam,this.events,this.toast,edited.node,null,win);
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
      UIHelper.openUrl(this.getLicenseUrl(),this.connector.getBridgeService(),OPEN_URL_MODE.BlankSystemBrowser);
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
