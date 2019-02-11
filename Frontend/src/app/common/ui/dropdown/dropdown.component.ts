import {Component, Input, Output, EventEmitter, OnInit, ViewChild, ElementRef, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../ui-animation";
import {UIService} from "../../services/ui.service";
import {trigger} from "@angular/animations";
import {UIHelper} from "../ui-helper";
import {Helper} from '../../helper';
import {UIConstants} from "../ui-constants";
import {OptionItem} from "../actionbar/option-item";
import {MatMenu, MatMenuTrigger} from '@angular/material';

@Component({
  selector: 'dropdown',
  templateUrl: 'dropdown.component.html',
  styleUrls: ['dropdown.component.scss']
})
/**
 * The dropdown is one base component of the action bar (showing more actions), but can also be used standalone
 */
export class DropdownComponent{
  @ViewChild('dropdown') menu : MatMenu;
  _options: OptionItem[];
  @Input() position = 'left';
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


  click(option : OptionItem){
      if(!option.isEnabled)
          return;
      option.callback(this.callbackObject);
  }
  constructor(private ui : UIService){

  }
}
