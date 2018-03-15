import { Injectable, HostListener } from "@angular/core";
import { setTimeout } from "core-js/library/web/timers";
import { Observable, Observer, ConnectableObservable } from "rxjs";
import { Headers, Http, RequestOptions, RequestOptionsArgs, Response } from "@angular/http";

import { OAuthResult, LoginResult, NodeRef } from '../rest/data-object';
import { Router } from '@angular/router';
import { RestConstants } from '../rest/rest-constants';

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable()
export class CordovaService {

  // change this during development for testing true, but false is default
  private forceCordovaMode: boolean = false;

  private deviceIsReady: boolean = false;

  private deviceReadyCallback : Function = null;
  private devicePauseCallback : Function = null;
  private deviceResumeCallback : Function = null;

  private observerDeviceReady : Observer<void> = null;

  private deviceReadyObservable: ConnectableObservable<{}>;

  private appGoneBackgroundTS : number = null;

  private _oauth:OAuthResult;
  public endpointUrl:string;
  get oauth(){
    return this._oauth;
  }
  set oauth(oauth: OAuthResult){
    this._oauth=oauth;
    if(oauth) {
        this._oauth.expires_ts = Date.now() + (oauth.expires_in * 1000);
        this.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS,JSON.stringify(this._oauth));
    }

  }

  /**
   * CONSTRUCTOR
   */
  constructor(
    private http : Http,
    private router : Router
  ) {

    console.log("CONSTRUCTOR CordovaService");

    // CORDOVA EVENT: Pause (App is put into Background)
    let whenDeviceGoesBackground = () => {
      // rember time when app went into background
      this.appGoneBackgroundTS = Date.now();
    };

    // CORDOVA EVENT: Resume (App comes back from Background)
    let whenDeviceGoesForeground = () => {

      /*
       * ignore pauses under 1 minute that appear when going into
       * a plugin (camera) or you get a permission request from the OS
       */
      if (this.appGoneBackgroundTS==null) return;
      if ((Date.now()-this.appGoneBackgroundTS)<(60*1000)) return;

      // OK - real pasuse detected
      console.log("CordovaService: App comes back from Background");

      // call listener if set
      if (this.deviceResumeCallback!=null) this.deviceResumeCallback();
    };

    //adding listener for cordova events
    document.addEventListener('deviceready', this.whenDeviceIsReady, false);
    document.addEventListener('pause', whenDeviceGoesBackground, false);
    document.addEventListener('resume', whenDeviceGoesForeground, false);

    // just for simulation on forced cordova mode
    if ((this.forceCordovaMode) && (!this.isReallyRunningCordova())) {
      console.log("SIMULATED deviceready event in FORCED CORDOVA MODE (just use during development)");
      setTimeout(this.whenDeviceIsReady,500+Math.random()*1000);
    }

  }

  // CORDOVA EVENT: Device is Ready (on App StartUp)
  private whenDeviceIsReady = () => {

      console.log("CordovaService: App is Ready");

      // load basic data from storage
      this.loadStorage();

      // hide the splashscreen (if still showing)
      setTimeout(()=>{
        try {
          (navigator as any).splashscreen.hide();
        } catch (e) {
          console.error('CordovaService: FAILED to call splashscreen.hide() - is plugin cordova-plugin-splashscreen installed?');
        }
      },1500);

      // flag that device is ready
      this.deviceIsReady = true;
      console.log("this.deviceIsReady",this.deviceIsReady);

    };

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
    return this.isReallyRunningCordova();
  }

  // just for internal use
  private isReallyRunningCordova() : boolean {
    return (typeof (window as any).cordova != "undefined");
  }

  /**
   * Check if app is running on a iOS device.
   * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-device/index.html
   */
  isIOS() : boolean {
    try {
      let device:any = (window as any).device;
      console.log("cordova-plugin-device", device);
      return device.platform=="iOS";
    } catch (e) {
      console.log("FAIL on Plugin cordova-plugin-device", e);
      return false;
    }
  }

  /**
   * Check if app is running on a Android device.
   * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-device/index.html
   */
  isAnroid() : boolean {
    try {
      let device:any = (window as any).device;
      console.log("cordova-plugin-device", device);
      return device.platform=="Android";
    } catch (e) {
      console.log("FAIL on Plugin cordova-plugin-device", e);
      return false;
    }
  }

  /**
   * Use to check if cordova plugins are ready to use.
   * If angular is running in a cordova environment - make sure that device is ready befor using plugin tools.
   */
  isDeviceReady():boolean {
    return this.deviceIsReady;
  }

  /*  
    * Set a callback function to be called then device is ready for codrova action.
    */
  subscribeDeviceReady() : Observable<void> {
    return new Observable<void>((observer: Observer<void>) => {

      if (this.deviceIsReady) {

        // cordova already signaled that it is ready - call on the spot
        observer.next(null);
        observer.complete();
  
      } else {

        let waitLoop = () => {
          if (this.deviceIsReady) {
            observer.next(null);
            observer.complete();
          } else {
            console.log("Waiting for Device Ready .. waitloop");
            setTimeout(waitLoop,200);
          }
        };
        waitLoop();
  
      } 

    });
  }

  /**
   * after init, load the stored info from the cordova storage and save it as class members for access of other services
   */
  loadStorage(){
      this.getPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS,(data:string)=>{
          this._oauth = (data!=null) ? JSON.parse(data) : null;
          this.getPermanentStorage(CordovaService.STORAGE_SERVER_ENDPOINT,(data:string)=>{
              this.endpointUrl=data;
          });
      });
  }

  /**
   * Set a callback function to be called then device resumes from pause.
   * > 1 Minute in Background
   * @param callback callback function (with void parameter)
   */
  setDeviceResumeCallback(callback:Function) {
    this.deviceResumeCallback = callback;
  }
  
  /**
   * Closes the App when running as real app.
   */
  exitApp() {
    try{
      (navigator as any)['app'].exitApp();
    } catch(e) {
      console.log("FAIL EXIT APP",e);
    }
  }


  /**********************************************************
   * APP PRESISTENCE 
   **********************************************************
   * Uses HTML5 storage as a base, but also backups thru the following plugin-in ...
   * https://www.npmjs.com/package/cordova-plugin-nativestorage
   * .. just in case that for example on iOS the HTML5 local storage gets ereased:
   * https://stackoverflow.com/questions/7750857/how-permanent-is-local-storage-on-android-and-ios
   * Dont use plugin on Android to avoid starting the app with a permission request.
   */

  /*
   * KEYS FOR STORAGE
   */
  public static STORAGE_OAUTHTOKENS:string = "oauth";
  public static STORAGE_SERVER_ENDPOINT:string = "server_endpoint";
  public static STORAGE_SERVER_OWN:string = "server_own";

  /**
   * load permanent key/value 
   * @param key the key to request value
   * @param callback function callback with value as parameter - null if not available
   */
  getPermanentStorage(key:string, callback:Function) : void {

    // get value from HTML5 local storage
    let value = window.localStorage.getItem(key);

    //just iun case - check if backup is available from nativestorage plugin
    if (((typeof value == 'undefined') || (value==null)) && (this.isIOS) && ((window as any).NativeStorage)) {
      try {
        // window['NativeStorage'].getItem("reference_to_value",<success-callback>, <error-callback>);
        (window as any).NativeStorage.getItem(key,(valueNative:any)=>{
          // WIN 
          if (typeof valueNative == "undefined") valueNative = null;
          callback(valueNative);
        },(error:any)=>{
          // FAIL (also when key not available)
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
    if (this.isIOS) {
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

  }

  /**
   * erase all permanent data
   */
  clearPermanentStorage(goToStart=false) : void {

    // clear HTML5 local storage
    window.localStorage.clear();

    // clear native storage 
    if (this.isIOS) {
      try {
        (window as any).NativeStorage.clear(() => {
          if (goToStart) {
            this.goToAppStart();
          }
        }, (error: any) => {
          if (goToStart) {
            this.goToAppStart();
          }
          // FAIL
          console.error("Fail NativeStorage.clear", error);
        });
      } catch (e) {
        console.error("Plugin Fail", e);
      }
    }

  }

  /**********************************************************
   * Permissions Plugin
   **********************************************************
   * Use to wrapp the use of plugin functions, that need certain permissions.
   * https://github.com/NeoLSN/cordova-plugin-android-permissions
   */

  // for 'permission' user values like in this side 
  // https://developer.android.com/reference/android/Manifest.permission.html
  private makeSurePermission(permission:string, successCallback:Function, errorCallback:Function): void {
        
    try {

      let permissions = (window as any).cordova.plugins.permissions;
      let permissionString:string = permissions[permission] as string;

      console.log("permissions",permissions);
      console.log("permissionString",permissionString);

      permissions.checkPermission(permissionString, (status:any) => {

        console.log("status",status);

        if( status.hasPermission ) {

          // permission is available
          successCallback();
      
        } else {

          // try to get permission by request
          permissions.requestPermission(permissionString, (response:any) => {

            console.log("response",response);

            if ( response.hasPermission ) {

              // permission is granted
              successCallback();

            } else {

              // permission denied
              errorCallback("FAIL-PERMISSION-1","permission not granted by user or not part of config.xml");
            
            }

          }, (error:any) => {
            errorCallback("FAIL-PERMISSION-2",error);
          });

        }

      }, (error:any) => {
        errorCallback("FAIL-PERMISSION-3",error);
      });

    } catch(error) {
      errorCallback("FAIL-EXCEPTION",error);
    }

  }

  /**********************************************************
   * Camera Plugin
   **********************************************************
   * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-camera
   */

  getPhotoFromCamera(successCallback:Function, errorCallback:Function, options:any=null ):void {
    try {

      // Default Options
      if (options==null) options = {
        correctOrientation: true,
        destinationType: 0, //Camera.DestinationType.DATA_URL
        sourceType: 1, // Camera.PictureSourceType.CAMERA
        encodingType: 0 //Camera.EncodingType.JPEG
      };

      // Camera PlugIn
      // https://github.com/apache/cordova-plugin-camera
      let runPlugIn:Function = () => {
        (navigator as any).camera.getPicture((result:any)=>{
          successCallback(result);
        },(error:any)=>{
          errorCallback("FAIL-PLUGIN",error);
        }, options);
      }

      // Permissions PlugIn
      // https://github.com/NeoLSN/cordova-plugin-android-permissions
      // TODO: check that the app just asks for photo permission
      this.makeSurePermission("CAMERA",runPlugIn, errorCallback);

    } catch(error) {
      console.log("FAIL-EXCEPTION",error);
    }

  }

  /**********************************************************
   * Basic Server Communication
   **********************************************************
   * Before app starts as app it needs to set server config.
   * To manage this, some basic HTTP requests are needed.
   * These are part of the cordova service, so that it can
   * run seperate from the rest of the app, that needs this
   * config before starting up.
   */

  // get the metadata about what servers that are part of the public listing
  public getPublicServerList() : Observable<any> {
    let url='http://app-registry.edu-sharing.com/public-server-directory.php';
    let headers=new Headers();
    headers.set('Accept','application/json');
    let options={headers:headers};
    return this.http.get(url,options)
        .map((response: Response) => response.json());
  }

  // check connection to server
  public getServerAbout(server:string) : Observable<any> {
      let url=server+'rest/_about'
      let headers=new Headers();
      headers.set('Accept','application/json');
      let options={headers:headers};
      return this.http.get(url,options)
          .map((response: Response) => response.json());
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
        this.setPermanentStorage(CordovaService.STORAGE_SERVER_ENDPOINT,url);
        this.endpointUrl=url;
        console.warn("TODO: TESTING OF URL NEEDED");
        observer.next(CordovaService.TEST_OK);
        observer.complete();

      } else {
      
        // simply set API URL and OK
        this.setPermanentStorage(CordovaService.STORAGE_SERVER_ENDPOINT,url);
        this.endpointUrl=url;
        observer.next(CordovaService.TEST_TESTSKIPPED);
        observer.complete();
      
      }

    });
  }

  // oAuth login that is used when running as mobile app
  public loginOAuth(username: string = "", password: string = ""): Observable<OAuthResult> {

    let url = this.endpointUrl + "../oauth2/token";
    let headers = new Headers();
    headers.append('Content-Type', 'application/x-www-form-urlencoded');
    headers.append('Accept', '*/*');
    let options = { headers: headers, withCredentials: false };

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
          this.oauth=oauth;
          observer.next(this.oauth);
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

  /**
   * Called when the current status is logged out
   * Cordova needs to refresh tokens
   */
  private reiniting=false;
  public reinitStatus(){
    if(this.reiniting)
      return;
    console.log("cordova: refresh oAuth");
    this.reiniting=true;
      this.refreshOAuth(this.oauth).subscribe(()=>{
          this.reiniting=false;
          window.location.reload();
      },(error:any)=>{
        this.clearPermanentStorage();
        console.warn("cordova: invalid oauth, go back to server selection");
        this.goToAppStart();
      });
  }

  // oAuth refresh tokens
  private refreshOAuth(oauth: OAuthResult): Observable<OAuthResult> {

    let url = this.endpointUrl + "../oauth2/token";
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
          this.oauth=oauthNew;
          console.log(oauthNew);
          observer.next(this.oauth);
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
      if ((Date.now() + 60000) > oauth.expires_ts || true) {
          // oAuth needs refresh first
          console.log("initOAuthSession --> Doing PLANNED OAUTH REFRESH");
          this.refreshOAuth(oauth).subscribe(
              (win) => {

                  // now oauth is fresh - continue with init session
                  this.oauth = win;
                  observer.next(this.oauth);
                  observer.complete();

              },
              (error) => {
                  localErrorHandling(error, "(on planned refresh)");
              }
          );
      }
    });

  }

    private goToAppStart() {
        console.log("GO TO START");
        this.router.navigate(['']);
    }

    hasValidConfig() {
        return this._oauth && this.endpointUrl;
    }

    getLanguage() {
      return new Observable<string>((observer: Observer<string>) => {

        try {
          (navigator as any).globalization.getPreferredLanguage(
            (lang:any)=>{
              // WIN
              let code = (lang.value as string).substr(0,2);
              console.log("OK getLanguage()", code);
              observer.next(code);
              observer.complete();
          },(error:any)=>{
              // ERROR - go with default
              console.log("FAIL getLanguage() ---> go with default 'de'",error);
              observer.next("de");
              observer.complete();
          });
        } catch(e) {
          console.log("EXCEPTION getLanguage() ---> go with default 'de'");
          observer.next("de");
          observer.complete();
        }

      });
    }
}