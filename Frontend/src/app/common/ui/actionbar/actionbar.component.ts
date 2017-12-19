import {Component, Input, Output, EventEmitter, OnInit, ViewChild, ElementRef} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UIAnimation} from "../ui-animation";
import {UIService} from "../../services/ui.service";
import {trigger} from "@angular/animations";
import {UIHelper} from "../ui-helper";

@Component({
  selector: 'actionbar',
  templateUrl: 'actionbar.component.html',
  styleUrls: ['actionbar.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
/**
 * The action bar provides several icons, usually at the top right, with actions for a current context
 */
export class ActionbarComponent{
  /**
   * The amount of options which are not hidden inside an overflow menu
   * @type {number} (default: depending on mobile (1) or not (2))
   */
  @Input() numberOfAlwaysVisibleOptions=2;
  public optionsAlways : OptionItem[] = [];
  public optionsMenu : OptionItem[] = [];
  public optionsToggle : OptionItem[] = [];
  public dropdown = false;
  /**
   * Set this to true if your action bar is shown on a dark background
   * @type {boolean}
   */
  @Input() darkBackground = false;
  @Input() node:Node = null;

  /**
   * Set the options, see @OptionItem
   * @param options
   */
  @Input() set options(options : OptionItem[]){
    options=OptionItem.filterValidOptions(this.ui,options);
    if(options==null){
      this.optionsAlways=[];
      this.optionsMenu=[];
      return;
    }
    this.optionsToggle=OptionItem.filterToggleOptions(options,true);

    this.optionsAlways=this.getActionOptions(OptionItem.filterToggleOptions(options,false)).slice(0,this.numberOfAlwaysVisibleOptions).reverse();
    if(!this.optionsAlways.length){
      this.optionsAlways=OptionItem.filterToggleOptions(options,false).slice(0,this.numberOfAlwaysVisibleOptions).reverse();
    }
    this.optionsMenu=this.hideActionOptions(OptionItem.filterToggleOptions(options,false),this.optionsAlways);

  }

  @ViewChild('menuElements') menuElements : ElementRef;


  constructor(private ui : UIService, private translate : TranslateService) {
    if(window.innerWidth<UIHelper.MOBILE_WIDTH){
      this.numberOfAlwaysVisibleOptions = 1;
    }
  }
  private click(option : OptionItem){
    if(!option.isEnabled)
      return;
    option.callback(this.node);
    this.dropdown=false;
  }
  private showDropdown(setFocus=true){
    this.dropdown=true;
    if(!setFocus)
      return;

    setTimeout(()=> {
      if (this.menuElements)
        this.menuElements.nativeElement.focus();
    },10);
  }



  private getActionOptions(options: OptionItem[]) {
    let result:OptionItem[]=[];
    for(let option of options){
      if(option.showAsAction)
        result.push(option);
    }
    return result;
  }

  private hideActionOptions(options: OptionItem[], optionsAlways: OptionItem[]) {
    let result:OptionItem[]=[];
    for(let option of options){
      if(optionsAlways.indexOf(option)==-1)
        result.push(option);
    }
    return result;
  }
}
/**
 * A option from the actionbar+dropdown menu
 * Example:
 this.options.push(new OptionItem("RESTORE_SINGLE","undo", (node) => this.restoreSingle(node)));
 this.options.push(new OptionItem("DELETE_SINGLE","archiveDelete", (node) => this.deleteSingle(node)));

 */
export class OptionItem {
  /**
   * If true, this option will be shown all the time in the node table
   * @type {boolean}
   */
  public showAlways = false;
  /**
   * If true, this option will be shown as an action (if room). If none of the items has showAsAction set, the first items will always be shown as action
   * @type {boolean}
   */
  public showAsAction = false;
  /**
   * If true, this option will be shown as a toggle on the right side (provide iconToggle as a toggle icon)
   * @type {boolean}
   */
  public isToggle = false;
  /**
   * If true, shows a line at the top
   * @type {boolean}
   */
  public isSeperate = false;
  /**
   * Like @isSeperate, but shows a line at bottom
   * @type {boolean}
   */
  public isSeperateBottom = false;
  /**
   * If true, only displayed on a mobile device
   * @type {boolean}
   */
  public onlyMobile = false;
  /**
   * Set to false if the action should be shown, but should not be clickable
   * @type {boolean}
   */
  public isEnabled = true;

  /**
   * A function called with the node in content which should return true or false if the option should be shown for this node
   */
  public showCallback : Function;
  /**
   * A function called with the node in content which should return true or false if the option should be enabled or not
   */
  public enabledCallback : Function;
  /**
   *
   * @param name the option name, which is used for the translation
   * @param icon A material icon name
   * @param callback A function callback when this option is choosen. Will get the current node passed as an argument
   */
  constructor(public name: string, public icon: string, public callback: Function) {
  }

  static filterValidOptions(ui: any, options: OptionItem[]) {
    if(options==null)
      return null;
    let optionsFiltered:OptionItem[]=[];
    for(let option of options){
      if(!option.onlyMobile || option.onlyMobile && ui.isMobile())
        optionsFiltered.push(option);
    }
    return optionsFiltered;
  }
  static filterToggleOptions(options: OptionItem[],toggle:boolean) {
    let result:OptionItem[]=[];
    for(let option of options){
      if(option.isToggle==toggle)
        result.push(option);
    }
    return result;
  }
}
