
import { Injectable } from "@angular/core";
import { setTimeout } from "core-js/library/web/timers";

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable()
export class CordovaService {

  // change this during development for testing true, but false is default
  private forceCordovaMode: boolean = true;

  private deviceIsReady: boolean = false;
  private deviceReadyCallback : Function = null;

  /**
   * CONSTRUCTOR
   */
  constructor() {

    let whenDeviceIsReady = () => {
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

      // call listener if set
      if (this.deviceReadyCallback!=null) this.deviceReadyCallback();
    };

    //adding listener for CordovaReady
    document.addEventListener('deviceready', whenDeviceIsReady, false);

    // just for simulation on forced cordova mode
    if (this.forceCordovaMode) {
      console.log("SIMULATED deviceready event in FORCED CORDOVA MODE (just use during development)");
      setTimeout(whenDeviceIsReady,3000);
    }

  }

  /**********************************************************
   * BASIC CORDOVA
   **********************************************************
   * Tools needed to setup, for info or to react on cordova events
   */

   /**
   * Use to check if angular is running in a cordova environment.
   */
  isRunningCordova():boolean {
    if (this.forceCordovaMode) return true;
    return (typeof (window as any).cordova != "undefined");
  }

  /**
   * Use to check if cordova plugins are ready to use.
   * If angular is running in a cordova environment - make sure that device is ready befor using plugin tools.
   */
  isDeviceReady():boolean {
    return this.deviceIsReady;
  }

  /**
   * Set a callback function to be called then device is ready for codrova action.
   * @param callback callback function (with void parameter)
   */
  setDeviceReadyCallback(callback:Function) {
    if (this.deviceIsReady) {
      // cordova already signaled that it is ready - call on the spot
      callback();
    } else {
      // remember callback and call when ready
      this.deviceReadyCallback = callback;
    }  
  }


  /**********************************************************
   * APP SIDE PRESISTENCE 
   **********************************************************
   * Uses HTML5 storage as a base, but also backups thru the following plugin-in ...
   * https://www.npmjs.com/package/cordova-plugin-nativestorage
   * .. just in case that for example on iOS the HTML5 local storage gets ereased:
   * https://stackoverflow.com/questions/7750857/how-permanent-is-local-storage-on-android-and-ios
   */

  /**
   * load permanent key/value 
   * @param key the key to request value
   * @param callback function callback with value as parameter - null if not available
   */
  getPermanentStorage(key:string, callback:Function) : void {

    // get value from HTML5 local storage
    let value = window.localStorage.getItem(key);

    //just iun case - check if backup is available from nativestorage plugin
    if ((typeof value == 'undefined') || (value==null)) {
      try {
        //window['NativeStorage'].getItem("reference_to_value",<success-callback>, <error-callback>);
        (window as any).NativeStorage.getItem(key,(valueNative:any)=>{
          // WIN 
          if (typeof valueNative == "undefined") valueNative = null;
          callback(valueNative);
        },(error:any)=>{
          // FAIL
          console.error("Fail NativeStorage.setItem",error);
          callback(null);
        });
      } catch (e) {
        console.error("Plugin Fail",e);
        callback(null);
      }
    } else {
      callback(value);
    }

  }

  /**
   * save permament key/value
   * @param key 
   * @param value 
   */
  setPermanentStorage(key:string, value:string) {

    // set on HTML5 storage
    window.localStorage.setItem(key, value);

    // as backup set on native storage
    try {
      (window as any).NativeStorage.setItem(key, value, ()=>{
        // WIN - thats OK
      }, (error:any)=>{
        // FAIL
        console.error("Fail NativeStorage.setItem",error);
      });
    } catch (e) {
      console.error("Plugin Fail",e);
    }

  }

  /**
   * erase all permanent data
   */
  clearPermanentStorage() : void {

    // clear HTML5 local storage
    window.localStorage.clear();

    // clear native storage 
    try {
      (window as any).NativeStorage.clear(()=>{
        // WIN - thats OK
      }, (error:any)=>{
        // FAIL
        console.error("Fail NativeStorage.clear",error);
      });
    } catch (e) {
      console.error("Plugin Fail",e);
    }

  }



}