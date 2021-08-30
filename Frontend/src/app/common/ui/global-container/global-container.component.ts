import {Component, ViewChild} from "@angular/core";
import {BehaviorSubject, Observable} from 'rxjs';
import { filter } from "rxjs/operators";

@Component({
  selector: 'global-container',
  templateUrl: 'global-container.component.html',
  styleUrls: ['global-container.component.scss'],
})
/**
 * Global components (always visible regardless of route
 */
export class GlobalContainerComponent{
  private static preloading = new BehaviorSubject<boolean>(true);
  static instance:GlobalContainerComponent;
  @ViewChild('rocketchat') rocketchat :any; // using any to bypass Circular Dependency issues

  constructor(){
    GlobalContainerComponent.instance=this;
  }
  public static subscribePreloading(){
    return GlobalContainerComponent.preloading.pipe(filter((v) => !v));
  }
  public static getPreloading(){
    return GlobalContainerComponent.preloading.value;
  }
  public static finishPreloading(){
    GlobalContainerComponent.preloading.next(false);
  }
}
