import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {NodeWrapper, Node, NodeShare} from "../../../common/rest/data-object";
import {VCard} from "../../../common/VCard";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {Translation} from "../../../common/translation";
import {TranslateService} from "@ngx-translate/core";
import {Helper} from "../../../common/helper";
import { DatepickerOptions } from 'ng2-datepicker';
import {DateHelper} from "../../../common/ui/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../common/ui/ui-animation";

@Component({
  selector: 'workspace-share-link',
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
  public dateOptions: DatepickerOptions;
  public enabled=true;
  public expiry=false;
  public password=false;
  public passwordString:string;
  public _expiryDate : Date;
  private currentDate: number;
  private edit: boolean;
  public set expiryDate(date:Date){
    this._expiryDate=date;
    this.setExpiry(true);
  }
  public get expiryDate(){
    return this._expiryDate;
  }
  private currentShare: NodeShare;
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
        };
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
  //http://stackoverflow.com/questions/25099409/copy-to-clipboard-as-plain-text
  private executeCopy(text:string){
  let copyDiv:any = document.createElement('div');
  copyDiv.contentEditable = true;
  document.body.appendChild(copyDiv);
  copyDiv.innerHTML = text;
  copyDiv.unselectable = "off";
  copyDiv.focus();
  document.execCommand('SelectAll');
  document.execCommand("Copy", false, null);
  document.body.removeChild(copyDiv);
}
  public copyClipboard(){
    if(!this.enabled)
      return;
    try {
      this.executeCopy(this.currentShare.url);
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
      };
      this.updateShare();
  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private toast:Toast,
  ){
    this.dateOptions={};
    this.dateOptions.minDate=new Date(Date.now() - 1000 * 3600 * 24); // Minimal selectable date
    this.dateOptions.minYear=new Date().getFullYear();
    this.dateOptions.maxYear=new Date(new Date().getTime() * 1000 * 3600 * 365).getFullYear();
    //this.dateOptions.format="DD.MM.YYYY";
    Translation.applyToDateOptions(this.translate,this.dateOptions);
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
