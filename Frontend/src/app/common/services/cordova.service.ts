
import { Injectable } from "@angular/core";
import { setTimeout } from "core-js/library/web/timers";

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable()
export class CordovaService {

  private deviceIsReady: boolean = false;

  constructor() {

    //adding listener for CordovaReady
    document.addEventListener('deviceready', ()=>{

      // flag that device is ready
      this.deviceIsReady = true;

      // hide the splashscreen (if still showing)
      setTimeout(()=>{
        try {
          (navigator as any).splashscreen.hide();
        } catch (e) {
          console.error('CordovaService: FAILED to call splashscreen.hide() - is plugin cordova-plugin-splashscreen installed?');
        }
      },1500);

    }, false);

  }

  /**
   * Use to check if angular is running in a cordova environment.
   */
  isRunningCordova():boolean {
    return (typeof (window as any).cordova != "undefined");
  }

  /**
   * Use to check if cordova plugins are ready to use.
   * If angular is running in a cordova environment - make sure that device is ready befor using plugin tools.
   */
  isDeviceReady():boolean {
    return this.deviceIsReady;
  }

  /*
  public isRunningLocal():boolean {
    return (location.hostname === "localhost" || location.hostname === "127.0.0.1" || location.hostname === "");
  }

  public isRunningMobile():boolean { 
    if( navigator.userAgent.match(/Android/i)
      || navigator.userAgent.match(/webOS/i)
      || navigator.userAgent.match(/iPhone/i)
      || navigator.userAgent.match(/iPad/i)
      || navigator.userAgent.match(/iPod/i)
      || navigator.userAgent.match(/BlackBerry/i)
      || navigator.userAgent.match(/Windows Phone/i)
    ){
      return true;
    }
    else {
      return false;
    }
  }

  public isRunningOnDevPort(): boolean {
    return location.port.length>0;
  }
  */

}