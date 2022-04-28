import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestNodeService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper, Node, NodeShare} from "../../../core-module/core.module";
import {VCard} from "../../../core-module/ui/VCard";
import {Toast} from "../../../core-ui-module/toast";
import {TranslateService} from "@ngx-translate/core";
import {Helper} from "../../../core-module/rest/helper";
import {DateHelper} from "../../../core-ui-module/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {UIHelper} from "../../../core-ui-module/ui-helper";

@Component({
  selector: 'es-workspace-share-link',
  templateUrl: 'share-link.component.html',
  styleUrls: ['share-link.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class WorkspaceShareLinkComponent  {
  public loading=true;
  public _node: Node;
  public enabled=true;
  public expiry=false;
  public password=false;
  public passwordString:string;
  public _expiryDate : Date;
  private currentDate: number;
  edit: boolean;
  public buttons: DialogButton[];
  public today = new Date();
  public set expiryDate(date:Date){
    this._expiryDate=date;
    this.setExpiry(true);
  }
  public get expiryDate(){
    return this._expiryDate;
  }
  currentShare: NodeShare;
  @Input() priority = 1;
  @Input() set node(node : Node){
    this._node=node;
    this.loading=true;
    this.nodeService.getNodeShares(node.ref.id,RestConstants.SHARE_LINK).subscribe((data:NodeShare[])=>{
      this._expiryDate=new Date(new Date().getTime()+3600*24*1000);
      // console.log(data);
        if(data.length){
        this.currentShare=data[0];
        this.expiry=data[0].expiryDate>0;
        this.password=data[0].password;
        if (this.password) {
            this.edit=true;
        }
        this.currentDate=data[0].expiryDate;
        if(this.expiry) {
          this.expiryDate=new Date(data[0].expiryDate);
        }
        if(data[0].expiryDate==0){
          this.enabled=false;
          this.loading=false;
          this.currentShare.url=this.translate.instant('WORKSPACE.SHARE_LINK.DISABLED');
        }
        else{
          this.loading=false;
        }
      }
      else{
        this.createShare();
      }
    },(error:any)=>this.toast.error(error));
  }
  public copyClipboard(){
    if(!this.enabled)
      return;
    try {
      UIHelper.copyToClipboard(this.currentShare.url);
      this.toast.toast('WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD');
    }
    catch(e){
      this.toast.error(null,'WORKSPACE.SHARE_LINK.COPIED_CLIPBOARD_ERROR');
    }
  }
  @Output() onClose=new EventEmitter();
  @Output() onLoading=new EventEmitter();
  public cancel(){
    this.onClose.emit();
  }
  public setEnabled(value:boolean){
    if(value){
      this.createShare();
      //this.updateShare(RestConstants.SHARE_EXPIRY_UNLIMITED);
    }
    else{
      this.deleteShare();
      this.expiry=false;
      this.password=false;
    }
  }
  private updateShare(date=this.currentDate){
    // console.log(date);
    this.currentShare.url=this.translate.instant('LOADING');
    this.nodeService.updateNodeShare(this._node.ref.id,this.currentShare.shareId,date,this.password ? this.passwordString : "").subscribe((data:NodeShare)=>{
      this.currentShare=data;
      if(date==0)
        this.currentShare.url=this.translate.instant('WORKSPACE.SHARE_LINK.DISABLED');
        // console.log(data);
    });
  }
  public setExpiry(value:boolean){
    if(!this.enabled)
      return;
    this.currentDate=value ? DateHelper.getDateFromDatepicker(this.expiryDate).getTime() : RestConstants.SHARE_EXPIRY_UNLIMITED;
    this.updateShare();
  }

  public setPassword(){
      if (!this.password) {
          this.edit=false;
      }
      this.updateShare();
  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private toast:Toast,
  ){
    this.buttons=[new DialogButton('CLOSE',DialogButton.TYPE_PRIMARY,()=>this.cancel())];
  }

    private createShare() {
      this.loading=true;
      this.nodeService.createNodeShare(this._node.ref.id).subscribe((data:NodeShare)=>{
        this.edit=false;
        this.currentShare=data;
        this.loading=false;
      },(error:any)=>this.toast.error(error))
    }

    private deleteShare() {
      this.loading=true;
      this.nodeService.deleteNodeShare(this._node.ref.id,this.currentShare.shareId).subscribe(()=>{
        (this.currentShare as any)={};
        this.loading=false;
      });
    }
}
