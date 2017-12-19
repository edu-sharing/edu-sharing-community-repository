import {
  Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
  QueryList
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../toast";
import {DialogButton} from "../modal-dialog/modal-dialog.component";

@Component({
  selector: 'modal-dialog-toast',
  templateUrl: 'modal-dialog-toast.component.html',
  styleUrls: ['modal-dialog-toast.component.scss']
})
export class ModalDialogToastComponent{
  private buttons: DialogButton;
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="Escape" && this.isCancelable){
      event.preventDefault();
      event.stopPropagation();
      this.cancel();
      return;
    }
  }

  constructor(private toast : Toast){
    toast.onShowModalDialog((data:any)=>{
      this.title=data.title;
      this.message=data.message;
      this.messageParameters=data.translation;
      this.buttons=data.buttons;
      this.visible=true
    });
  }

  public visible=false;
  private isCancelable = true;
  /**
   * The title, will be translated automatically
   */
  private  title : string;
  /**
   * The message, will be translated automatically
   */
  private message : string;
  /**
   * Additional dynamic content for your language string, use an object, e.g.
   * Language String: Hello {{ name }}
   * And use messageParameters={name:'World'}
   */
  private messageParameters : any;


  public click(btn : DialogButton){
    btn.callback();
    this.visible=false;
  }
  private cancel(){
    this.visible=false;
  }
}
