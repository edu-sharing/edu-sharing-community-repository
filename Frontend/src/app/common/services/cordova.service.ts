
import { Injectable } from "@angular/core";
import { setTimeout } from "core-js/library/web/timers";
import { Observable, Observer } from "rxjs";
import { Headers, Http, RequestOptions, RequestOptionsArgs, Response } from "@angular/http";

import { OAuthResult, LoginResult } from "../rest/data-object";
import { RestLocatorService } from "./../rest/services/rest-locator.service";

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable()
export class CordovaService {

  // change this during development for testing true, but false is default
  private forceCordovaMode: boolean = true;

  private deviceIsReady: boolean = false;
  private deviceReadyCallback : Function = null;
  private devicePauseCallback : Function = null;
  private deviceResumeCallback : Function = null;

  /**
   * CONSTRUCTOR
   */
  constructor(
    private http : Http,
    private locator : RestLocatorService
  ) {

    // CORDOVA EVENT: Device is Ready (on App StartUp)
    let whenDeviceIsReady = () => {
      console.log("CordovaService: App is Ready");

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

    // CORDOVA EVENT: Pause (App is put into Background)
    let whenDeviceGoesBackground = () => {
      console.log("CordovaService: App goes into Background");
      // call listener if set
      if (this.devicePauseCallback!=null) this.devicePauseCallback();
    };

    // CORDOVA EVENT: Resume (App comes back from Background)
    // always consider that app could have been in background for days
    let whenDeviceGoesForeground = () => {
      console.log("CordovaService: App comes back from Background");
      // call listener if set
      if (this.deviceResumeCallback!=null) this.deviceResumeCallback();
    };

    //adding listener for cordova events
    document.addEventListener('deviceready', whenDeviceIsReady, false);
    document.addEventListener('pause', whenDeviceGoesBackground, false);
    document.addEventListener('resume', whenDeviceGoesForeground, false);

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

  /**
   * Set a callback function to be called then device is paused.
   * @param callback callback function (with void parameter)
   */
  setDevicePauseCallback(callback:Function) {
      this.devicePauseCallback = callback;
  }  

  /**
   * Set a callback function to be called then device resumes from pause.
   * @param callback callback function (with void parameter)
   */
  setDeviceResumeCallback(callback:Function) {
    this.deviceResumeCallback = callback;
}  


  /**********************************************************
   * APP SIDE PRESISTENCE 
   **********************************************************
   * Uses HTML5 storage as a base, but also backups thru the following plugin-in ...
   * https://www.npmjs.com/package/cordova-plugin-nativestorage
   * .. just in case that for example on iOS the HTML5 local storage gets ereased:
   * https://stackoverflow.com/questions/7750857/how-permanent-is-local-storage-on-android-and-ios
   */

  /*
   * KEYS FOR STORAGE
   */
  public STORAGE_OAUTHTOKENS:string = "oauth";

  /**
   * load permanent key/value 
   * @param key the key to request value
   * @param callback function callback with value as parameter - null if not available
   */
  getPermanentStorage(key:string, callback:Function) : void {

    // get value from HTML5 local storage
    let value = window.localStorage.getItem(key);

    //just iun case - check if backup is available from nativestorage plugin
    if (((typeof value == 'undefined') || (value==null)) && ((window as any).NativeStorage)) {
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

  /**********************************************************
   * OAUTH Server Communication
   **********************************************************
   * The REST-Services depend on Configuration Service that already need config from a fixed server.
   * To seperate the app login from those dependencies .. the cordova service provides all the HTTP
   * tools to select a server, make oAuth logins/management, get JSession .. from there on the
   * regular REST-Services can handle all the communication. 
   */

  // errors results that can result when testing a server url
  public static TEST_ERROR_NOTFOUND:string = "APINOTFOUND";
  public static TEST_ERROR_NOINTERNET:string = "NOINTERNET";
  public static TEST_ERROR_INCORRECTVERSION:string = "INCORRECTVERSION";
  public static TEST_ERROR_UNKNOWN:string = "UNKOWN";

  // success results that can result when testing a server url
  public static TEST_WARNING_NOHTTPS:string = "NOHTTPS";
  public static TEST_TESTSKIPPED:string = "TESTSKIPPED";
  public static TEST_OK:string = "OK";

  public setServerURL(url:string, doTesting:boolean): Observable<string> {
    return new Observable<string>((observer: Observer<string>) => {

      if (doTesting) {

        // test URL TO API
        
        alert("TODO: TESTING OF URL NEEDED");
        observer.error(CordovaService.TEST_ERROR_UNKNOWN);
        observer.complete();

      } else {
      
        // simply set API URL and OK
        this.locator.endpointUrl = url;
        observer.next(CordovaService.TEST_TESTSKIPPED);
        observer.complete();
      
      }

    });
  }

  // oAuth login that is used when running as mobile app
  public loginOAuth(username: string = "", password: string = ""): Observable<OAuthResult> {

    let url = this.locator.endpointUrl + RestLocatorService.createUrl("../oauth2/token", null);
    let headers = new Headers();
    headers.append('Content-Type', 'application/x-www-form-urlencoded');
    headers.append('Accept', '*/*');
    let options = { headers: headers, withCredentials: false }

    let data = "client_id=eduApp&grant_type=password&client_secret=secret" +
      "&username=" + encodeURIComponent(username) +
      "&password=" + encodeURIComponent(password);

    return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {
      this.http.post(url, data, options).map((response: Response) => response.json()).subscribe(
        (oauth: OAuthResult) => {

          if (oauth == null) {
            observer.error("INVALID_CREDENTIALS"); "LOGIN.ERROR"
            observer.complete();
            return;
          }

          // set local expire ts on token
          oauth.expires_ts = Date.now() + (oauth.expires_in * 1000);

          observer.next(oauth);
          observer.complete();

        },
        (error: any) => {

          if (error.status == 401) {
            observer.error("LOGIN.ERROR");
            observer.complete();
            return;
          }

          observer.error(error);
          observer.complete();
        });
    });
  }

  // oAuth refresh tokens
  public refreshOAuth(oauth: OAuthResult): Observable<OAuthResult> {

    let url = this.locator.endpointUrl + RestLocatorService.createUrl("../oauth2/token", null);
    let headers = new Headers();
    headers.append('Content-Type', 'application/x-www-form-urlencoded');
    headers.append('Accept', '*/*');
    let options = { headers: headers, withCredentials: false }

    let data = "grant_type=refresh_token&client_id=eduApp&client_secret=secret" +
      "&refresh_token=" + encodeURIComponent(oauth.refresh_token);

    return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {
      this.http.post(url, data, options).map((response: Response) => response.json()).subscribe(
        (oauthNew: OAuthResult) => {

          // set local expire ts on token
          oauthNew.expires_ts = Date.now() + (oauth.expires_in * 1000);

          observer.next(oauthNew);
          observer.complete();

        },
        (error: any) => {
          observer.error(error);
          observer.complete();
        });
    });
  }

  /**
  * If the user has a set of oAuth tokens (after login or when starting the app again)
  * then use this method to get a cookie/session and subscribe on refreshes on oAuth tokens.
  * When the Observable an error mit "INVALID" - oAuth tokens are outdated and new login is needed. 
  */
  public initOAuthSession(oauth: OAuthResult): Observable<OAuthResult> {

    return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {

      let localErrorHandling = (error: any, tag: string = "") => {
        if ((typeof error != "string") && (error.status == 401)) {
          // oauth tokens are invalid
          console.log("INVALID initOAuthSession " + tag);
          observer.error("INVALID");
          observer.complete();
        } else {
          // on all other errors (server, internet, etc)
          console.log("ERROR initOAuthSession " + tag, error);
          observer.error(error);
          observer.complete();
        }
      };

      // make getting session from backend available from multiple points in this method
      let getSessionFromBackend = (firstTry: boolean = true) => {
        // check with backend
        this.isLoggedIn(oauth).subscribe(
          (win) => {

            // you now have a valid session cookie that will work
            // on following request --> FINAL WIN
            observer.next(oauth);
            observer.complete();

          },
          (error) => {

            // just in case oauth is invalid and its first try - try to refresh on time
            // this is for scenarios where expire date of oauth access token is still valid,
            // but in case of a server restart got invalid early
            if ((typeof error != "string") && (error.status == 401)) {

              if (firstTry) {

                // on first try of init session - try the refresh
                console.log("initOAuthSession --> Doing EXCEPTION OAUTH REFRESH");
                this.refreshOAuth(oauth).subscribe(
                  (win) => {
                    oauth = win;
                    getSessionFromBackend(false);
                  },
                  (error) => {
                    localErrorHandling(error, "(on exception refresh)")
                  }
                );

              } else {

                // on second try - give up - oAuth is invalid
                observer.error("INVALID");
                observer.complete();

              }
              return;
            }

            localErrorHandling(error, "(on isLoggedIn firstTry=" + firstTry + ")");
          }
        );
      };

      // check if oauth needs refresh (by timestamp)
      if ((Date.now() + 60000) > oauth.expires_ts) {
        // oAuth needs refresh first
        console.log("initOAuthSession --> Doing PLANNED OAUTH REFRESH");
        this.refreshOAuth(oauth).subscribe(
          (win) => {

            // now oauth is fresh - continue with init session
            oauth = win;
            console.log("FRESH OAUTH", oauth);
            getSessionFromBackend();

          },
          (error) => {
            localErrorHandling(error, "(on planned refresh)");
          }
        );
      } else {
        getSessionFromBackend();
      }

    });

  }

  // also works optional with oAuth tokens for authentication
  public isLoggedIn(oauth: OAuthResult = null): Observable<LoginResult> {

    let url = this.locator.endpointUrl + RestLocatorService.createUrl("authentication/:version/validateSession", null);
    let headers = new Headers();
    headers.append('Accept', 'application/json');
    headers.append('Authorization', 'Bearer ' + oauth.access_token);
    let options: any = { headers: headers, withCredentials: true }

    return new Observable<LoginResult>((observer: Observer<LoginResult>) => {
      this.http.get(url, options).map((response: Response) => response.json()).subscribe(
        (data: LoginResult) => {
          /*
          this.toolPermissions=data.toolPermissions;
          this.event.broadcastEvent(FrameEventsService.EVENT_UPDATE_LOGIN_STATE,data);
          this.storage.set(TemporaryStorageService.SESSION_INFO,data);
          this._logoutTimeout=data.sessionTimeout;
          */
          observer.next(data);
          observer.complete();
        },
        (error: any) => {
          observer.error(error);
          observer.complete();
        }
      );
    });
  }



}