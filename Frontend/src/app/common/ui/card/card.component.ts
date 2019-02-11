import {
    Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
    QueryList, Inject, OnDestroy
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../ui-animation";
import {DialogButton} from "../modal-dialog/modal-dialog.component";
import {MatDialog, MAT_DIALOG_DATA} from "@angular/material";
import {UIService} from '../../services/ui.service';

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
 * A common edu-sharing modal card
 */
export class CardComponent implements OnDestroy{
  private static modalCards: CardComponent[]=[];
  @Input() title:string;
  @Input() subtitle:string;
  @Input() isCancelable=true;
  @Input() avatar:string;
  @Input() width="normal";
  @Input() height="normal";
  @Input() tabbed=false;
  @Input() priority=0;
  @Output() onCancel = new EventEmitter();
  /** A list of buttons, see @DialogButton
   * Also use the DialogButton.getYesNo() and others if applicable!
   */
  public _buttons : DialogButton[];
  @Input() set buttons (buttons :  DialogButton[]){
   this._buttons=buttons;
  }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
      for(let card of CardComponent.modalCards){
          if(card.handleEvent(event))
              return;
      }
    }
  constructor(private uiService: UIService){
      CardComponent.modalCards.splice(0,0,this);
  }
  ngOnDestroy(){
      CardComponent.modalCards.splice(CardComponent.modalCards.indexOf(this),1);
  }
  handleEvent(event:any){
    if(event.key=="Escape"){
      event.stopPropagation();
      event.preventDefault();
      this.cancel();
      return true;
    }
    return false;
  }

  public click(btn : DialogButton){
    btn.callback();
  }
  public cancel(){
    this.onCancel.emit();
  }
}
