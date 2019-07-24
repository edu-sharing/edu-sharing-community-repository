import {
  Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
  QueryList
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Toast} from "../../../core-ui-module/toast";
import {DialogButton} from "../../../core-module/core.module";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {trigger} from "@angular/animations";
import {ProgressType} from "../modal-dialog/modal-dialog.component";

@Component({
  selector: 'modal-dialog-toast',
  templateUrl: 'modal-dialog-toast.component.html',
  styleUrls: ['modal-dialog-toast.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class ModalDialogToastComponent{
  private buttons: DialogButton;
  private onCancel: Function;
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
    this.toast.onShowModalDialog((data:any)=>{
      this.title=data.title;
      this.message=data.message;
      this.input=data.input;
      this.toast.dialogInputValue='';
      this.progressType=data.progressType;
      this.messageParameters=data.translation;
      this.isCancelable=data.isCancelable;
      this.buttons=data.buttons;
      this.onCancel=data.onCancel;
      this.visible=this.title!=null;
    });
  }

  public visible=false;

  private isCancelable = true;
  /**
   * Name/Label of the input that should be displayed
   */
  private input: string;
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
  /**
   * type of the progress to display. Null if this is not an progress dialog
   */
  private progressType:ProgressType;
  /* value stored in the input, if enabled */
  inputValue: string;

  private cancel(){
    this.visible=false;
    if(this.onCancel!=null) this.onCancel();
    this.reset();
  }
  private reset() {
      this.onCancel=null;
  }
}
