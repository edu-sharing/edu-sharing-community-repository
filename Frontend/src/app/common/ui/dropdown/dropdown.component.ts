import {Component, Input, Output, EventEmitter, OnInit, ViewChild, ElementRef, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../ui-animation";
import {UIService} from "../../services/ui.service";
import {trigger} from "@angular/animations";
import {UIHelper} from "../ui-helper";
import {Helper} from '../../helper';
import {UIConstants} from "../ui-constants";
import {OptionItem} from "../actionbar/option-item";

@Component({
  selector: 'dropdown',
  templateUrl: 'dropdown.component.html',
  styleUrls: ['dropdown.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
/**
 * The dropdown is one base component of the action bar (showing more actions), but can also be used standalone
 */
export class DropdownComponent{
  @ViewChild('dropdownRef') dropdownElement : ElementRef;
  @ViewChild('dropdownContainer') dropdownContainerElement : ElementRef;
  _show: boolean;
  _options: OptionItem[];
  @Input() position = 'left';
  @Input() set show(show:boolean){
    this._show=show;
    if(show)
      this.focusDropdown();
  };
  @Output() showChange=new EventEmitter();
  @Input() set options(options:OptionItem[]) {
    this._options = UIHelper.filterValidOptions(this.ui,Helper.deepCopyArray(options));
  }

  /**
   * the object that should be returned via the option's callback
   * Can be null
   */
  @Input() callbackObject:any;

    /**
     * Should disabled ("greyed out") options be shown or hidden?
     * @type {boolean}
     */
    @Input() showDisabled = true;

  hide(){
    this._show=false;
    this.showChange.emit(false);
  }
  click(option : OptionItem){
      if(!option.isEnabled)
          return;
      option.callback(this.callbackObject);
      this.hide();
  }
  focusDropdown(){
    setTimeout(()=> {
        UIHelper.setFocusOnDropdown(this.dropdownElement);
        UIHelper.scrollSmoothElement(this.dropdownContainerElement.nativeElement.scrollHeight,this.dropdownContainerElement.nativeElement);
    });
  }
  constructor(private ui : UIService){}

}
