import {
    Component, Input, Output, EventEmitter, OnInit, HostListener, ViewChild, ElementRef,
    QueryList, Inject, OnDestroy
} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {trigger} from "@angular/animations";
import {MatDialog, MAT_DIALOG_DATA} from "@angular/material";
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {DialogButton, RestHelper, UIService, Node} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";
import {UIHelper} from "../../../core-ui-module/ui-helper";

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
  @ViewChild('cardContainer') cardContainer: ElementRef;
  @ViewChild('jumpmarksRef') jumpmarksRef: ElementRef;
  private static modalCards: CardComponent[]=[];
    /**
     * the title of the card. Should be pre-translated
     */
  @Input() title:string;
    /**
     * The subtitle of the card (optional)
     * You may also use the "node" binding to automatically fill this field
     */
  @Input() subtitle:string;
    /**
     * Should a "x" appear in the top right (don't forget to bind onCancel as an event)
     */
  @Input() isCancelable=true;
    /**
     * An optional image href that should be appear in the top left corner
     */
  @Input() avatar:string;
  @Input() width="normal";
  @Input() height="normal";
    /**
     * Hint that the layout contains mat-tab-group (relevant for correct scrolling, tabs will be fixed at top)
     */
  @Input() tabbed=false;
    /**
     * Should the dialog be modal (a dark background)
     */
  @Input() modal=true;
    /**
     * Jumpmarks for the left side (used for the mds dialog)
     */
  @Input() jumpmarks:CardJumpmark[];
  @Input() priority=0;
  jumpmarkActive: CardJumpmark;

    /**
     * Optional, bind a Node or Node-Array to this element
     * If this is used, the subtitle and avatar is automatically set depending on the given data
     * @param node
     */
  @Input() set node(node:Node|Node[]){
    if(!node)
      return;
    let nodes:Node[]=(node as any);
    if(!Array.isArray(nodes)){
      nodes=[(node as any)];
    }
    if(nodes && nodes.length){
      if(nodes.length==1 && nodes[0]){
        this.avatar=nodes[0].iconURL;
        this.subtitle=RestHelper.getName(nodes[0]);
      }
      else{
        this.avatar=null;
        this.subtitle=this.translate.instant('CARD_SUBTITLE_MULTIPLE',{count:nodes.length});
      }
    }
  }
  @Output() onCancel = new EventEmitter();
  @Output() onScrolled = new EventEmitter();
  /** A list of buttons, see @DialogButton
   * Also use the DialogButton.getYesNo() and others if applicable!
   */
  public _buttons : DialogButton[];
  @Input() set buttons (buttons :  DialogButton[]){
   this._buttons=buttons;
  }
    @HostListener('window:resize')
    onResize(){
        if(document.activeElement && this.cardContainer && this.cardContainer.nativeElement){
            UIHelper.scrollSmoothElementToChild(document.activeElement,this.cardContainer.nativeElement);
        }
    }
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
      for(let card of CardComponent.modalCards){
          if(card.handleEvent(event))
              return;
      }
    }
    private scrollSmooth(jumpmark:CardJumpmark){
        let pos=document.getElementById(jumpmark.id).offsetTop;
        UIHelper.scrollSmoothElement(pos,this.cardContainer.nativeElement,2);
    }
  constructor(private uiService: UIService,private translate : TranslateService){
      CardComponent.modalCards.splice(0,0,this);
      UIHelper.waitForComponent(this,'jumpmarksRef').subscribe(()=> {
          console.log("jumpmarks ready");
          setInterval(() => {
              try {
                  let jump = this.jumpmarksRef;
                  let height = this.cardContainer.nativeElement.getBoundingClientRect().bottom - this.cardContainer.nativeElement.getBoundingClientRect().top;
                  let pos = this.cardContainer.nativeElement.scrollTop - height - 200;
                  let closest = 999999;
                  for(let jumpmark of this.jumpmarks) {
                      let element=document.getElementById(jumpmark.id);
                      let top = element.getBoundingClientRect().top;
                      if (Math.abs(top - pos) < closest) {
                          closest = Math.abs(top - pos);
                          this.jumpmarkActive = this.jumpmarks[Helper.indexOfObjectArray(this.jumpmarks, 'id', element.id)];
                      }
                  }
              } catch (e) {

              }
          }, 1000 / 20); // 20 FPS
      });
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
  scrolled(){
      this.onScrolled.emit();
  }
}
export class CardJumpmark{
    /**
     *
     * @param id the id (as in html)
     * @param label the pre-translated label
     * @param icon the icon
     */
  constructor(public id:string,public label:string,public icon:string){}
}
