import {Component, ViewChild} from "@angular/core";

@Component({
  selector: 'global-container',
  templateUrl: 'global-container.component.html',
  styleUrls: ['global-container.component.scss'],
})
/**
 * Global components (always visible regardless of route
 */
export class GlobalContainerComponent{
  private static preloading=true;
  static instance:GlobalContainerComponent;
  @ViewChild('rocketchat') rocketchat :any; // using any to bypass Circular Dependency issues

  constructor(){
    GlobalContainerComponent.instance=this;
  }
  public getPreloading(){
    return GlobalContainerComponent.preloading;
  }
  public static getPreloading(){
    return GlobalContainerComponent.preloading;
  }
  public static finishPreloading(){
    GlobalContainerComponent.preloading=false;
  }
}
