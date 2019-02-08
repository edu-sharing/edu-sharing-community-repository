import {
    Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
    QueryList, Inject
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../ui-animation";
import {DialogButton} from "../modal-dialog/modal-dialog.component";
import {MatDialog, MAT_DIALOG_DATA} from "@angular/material";

@Component({
  selector: 'card',
  templateUrl:'card.component.html',
  styleUrls: ['card.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
/**
 * An common edu-sharing modal card
 */
export class CardComponent{
  @Input() title:string;
  @Input() subtitle:string;
  @Input() isCancelable=true;
  @Input() width="normal";
  @Input() height="normal";
  @Input() tabbed=false;
  @Output() onCancel = new EventEmitter();
  /** A list of buttons, see @DialogButton
   * Also use the DialogButton.getYesNo() and others if applicable!
   */
  public _buttons : DialogButton[];
  @Input() set buttons (buttons :  DialogButton[]){
   this._buttons=buttons;
  }
  constructor(){
  }

  public click(btn : DialogButton){
    btn.callback();
  }
  public cancel(){
    this.onCancel.emit();
  }
}
