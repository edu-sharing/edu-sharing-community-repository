import {Injectable, Injector} from '@angular/core';
import {setTimeout} from 'core-js/library/web/timers';
import {Observable, Observer} from 'rxjs';
import {Headers, Http, Response} from '@angular/http';

import {Location} from '@angular/common';
import {Router} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {HttpClient} from '@angular/common/http';
import {UIConstants} from "../../core-module/ui/ui-constants";
import {FrameEventsService} from "../../core-module/rest/services/frame-events.service"

import {DateHelper} from "../../core-ui-module/DateHelper";
import {OAuthResult} from "../../core-module/rest/data-object";
import {RestConstants} from "../../core-module/rest/rest-constants";

declare var cordova : any;

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable()
export class CordovaService {

  // change this during development for testing true, but false is default
  private forceCordovaMode = false;

  private deviceIsReady = false;

  private deviceResumeCallback : Function = null;

  private observerShareContent : Observer<any> = null;

  private appGoneBackgroundTS : number = null;

  private _oauth:OAuthResult;
  private serviceIsReady = false;

  private lastIntent: any;

  get oauth(){
    return this._oauth;
  }

  set oauth(oauth: OAuthResult){
    this._oauth=oauth;
    if(oauth) {
        this._oauth.expires_ts = Date.now() + (oauth.expires_in * 1000);
        this.setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,JSON.stringify(this._oauth));
    }

  }

  initialHref:string;

  /**
   * CONSTRUCTOR
   */
  constructor(
    private router : Router,
    private http : HttpClient,
    private location: Location,
    private injector:Injector,
    private events : FrameEventsService,
  ) {

    this.initialHref = window.location.href;
    console.log("CONSTRUCTOR CordovaService",this.initialHref);

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

    if(this.isRunningCordova()) {
        // deviceready may not work, because cordova is already loaded, so try to set it ready after some time
        setTimeout(() => this.deviceIsReady = true, 1000);
    }
    //adding listener for cordova events
    document.addEventListener('deviceready', () => {
      this.deviceIsReady = true;
    }, false);
    document.addEventListener('pause', whenDeviceGoesBackground, false);
    document.addEventListener('resume', whenDeviceGoesForeground, false);

    // just for simulation on forced cordova mode
    if ((this.forceCordovaMode) && (!this.isReallyRunningCordova())) {
      console.log("SIMULATED deviceready event in FORCED CORDOVA MODE (just use during development)");
      setTimeout(this.whenDeviceIsReady,500+Math.random()*1000);
    } else if(this.isReallyRunningCordova()) {
      this.deviceReadyLoop(1);
    }

  }

    /**
     * get the last android/ios intent
     */
    public getLastIntent(){
      return this.lastIntent;
    }
  private deviceReadyLoop(counter:number) : void {
    console.log("deviceReadyLoop("+counter+")");
    setTimeout(()=>{
      if (this.deviceIsReady) {
        this.whenDeviceIsReady();
      } else {
        this.deviceReadyLoop(++counter);
      }
    },250);
  }

  // CORDOVA EVENT: Device is Ready (on App StartUp)
  private whenDeviceIsReady = () => {

      //window.open = cordova.InAppBrowser.open;
      console.log("CordovaService: App is Ready");

      // load basic data from storage
      this.loadStorage();

      // --> navigation issues exist anyway, need to check that later
      document.addEventListener("backbutton", ()=>this.onBackKeyDown(), false);
      // when new share contet - go to share screen
      this.onNewShareContent().subscribe(
          (data: any) => {
              // this.router.navigate(['share', URI]);
              if(this.hasValidConfig()) {
                  this.router.navigate([UIConstants.ROUTER_PREFIX, 'app', 'share'], {queryParams: data});
              }
          }, (error) => {
              console.log("ERROR on new share event", error);
          });
      /*
      let shareInterval=setInterval(()=>{
          if(this.hasValidConfig()) {
              console.log("share content register");
              clearInterval(shareInterval);
              this.onNewShareContent().subscribe(
                  (data: any) => {
                      this.router.navigate([UIConstants.ROUTER_PREFIX,'app', 'share'], {queryParams: data});
                  }, (error) => {
                      console.log("ERROR on new share event", error);
                  });
          }
      },1000);
      */


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

      // check if to register on share events
      if (this.observerShareContent!=null) this.registerOnShareContent();

    };
  /**********************************************************
   * Plugin: WebIntent (for Android)
   **********************************************************
   * To receive share content from other apps.
   * https://github.com/cordova-misc/cordova-webintent
   */

   private onNewShareContent() : Observable<any> {
    return new Observable<any>((observer: Observer<any>) => {
      this.observerShareContent = observer;

      // if device is already ready -> register now, otherwise wait
      if (this.deviceIsReady) this.registerOnShareContent();

    });  
   }
   public getFileAsBlob(file:string,mimetype:string){
       return new Observable<Blob>((observer: Observer<Blob>) => {
           (window as any).resolveLocalFileSystemURL(file, (data:any)=>{
             data.file((data2:File)=>{
                 observer.next(data2);
                 observer.complete();
             })

           },(error:any)=>{
               observer.error(error);
               observer.complete();
           });
       });
   }
   private registerOnShareContent() : void {
       if (this.isAndroid()) {
           console.log("register on share intent");

           let handleIntentBase=(intent:any)=>{
                if(intent && intent.extras){
                   let uri=intent.extras["android.intent.extra.TEXT"];
                   if(uri){
                       this.lastIntent=intent;
                       this.observerShareContent.next({uri:uri,mimetype:intent.type});
                       // clear handler to just fire it on first app opening
                       return;
                   }
                   uri = intent.extras["android.intent.extra.STREAM"];
                   // it's a file
                   if(uri){
                       this.lastIntent=intent;
                       (window as any).plugins.intent.getRealPathFromContentUrl(uri,(file:string)=>{
                           this.observerShareContent.next({uri:uri,file:file,mimetype:intent.type});
                       },(error:any)=>{
                           this.observerShareContent.next({uri:uri,mimetype:intent.type});
                       });

                   }
               }
           };
           // only run once. Will loop otherwise if no auth is found and intent was send
           let handleIntent=(intent:any)=> {
               // Do things
               console.log(intent);
               if (intent && intent.action=="android.intent.action.VIEW") {
                   let hit="/edu-sharing";
                   let target=intent.data.substr(0,intent.data.indexOf(hit));
                   let current=window.location.href.substr(0,window.location.href.indexOf(hit));
                   if(target==current){
                       console.log(target+"="+current+", go to request location "+intent.data);
                       window.location.href=intent.data;
                   }
                   else{
                       console.log(target+"!="+current+", logout and go to new location "+intent.data);
                       this.resetAndGoToServerlist('url='+intent.data);
                   }
               }
               else{
                   handleIntentBase(intent);
               }
               (window as any).plugins.intent.getCordovaIntent(null);
           };
           console.log((window as any).plugins);
           (window as any).plugins.intent.getCordovaIntent(handleIntentBase);
           (window as any).plugins.intent.setNewIntentHandler(handleIntent);
           /*
           (window as any).plugins.webintent.onNewIntent((uri:string)=> {
               (window as any).plugins.webintent.getExtra((window as any).plugins.webintent.EXTRA_TEXT,
                   (extra:string)=> {
                       console.log("new intent " + extra+" "+uri);
                       this.observerShareContent.next(extra);
                   },(error:any)=>{
                       console.error(error);
                       (window as any).plugins.webintent.getExtra((window as any).plugins.webintent.EXTRA_STREAM,
                       (extra:string)=> {
                           console.log("new intent " + extra+" "+uri);
                           this.observerShareContent.next(extra);
                       },(error:any)=>{console.error(error);});
               });

           });*/
       }
       if(this.isIOS()){
           // Initialize the plugin
           cordova.openwith.init(()=>{}, ()=>{
               console.warn("failed to init openWith ios");
           });

           // Define your file handler
           cordova.openwith.addHandler((intent:any)=>{
               console.log('intent received');

               console.log('  action: ' + intent.action); // type of action requested by the user
               console.log('  exit: ' + intent.exit); // if true, you should exit the app after processing

               let item = intent.items[0];
               console.log('  type: ', item.type);   // mime type
               console.log('  uri:  ', item.uri);     // uri to the file, probably NOT a web uri
               console.log('  image base64 string length: ', item.base64.length)
               //console.log('  image base64 string: ', item.base64)

               // some optional additional info
               console.log('  text: ', item.text);   // text to share alongside the item, iOS only
               console.log('  file: ', item.file);   // suggested name of the image, iOS 11+ only
               console.log('  utis: ', item.utis);
               console.log('  path: ', item.path);   // path on the device, generally undefined
               item.stream=item.base64; // convert it so it's like on android
               //alert(item.type+" : "+item.name+" : "+item.path+" : "+item.uri);
               item.uri=DateHelper.getDateForNewFile()+".jpg";
               this.lastIntent=item;
               this.observerShareContent.next({uri:item.uri,mimetype:item.type,file:item.name,text:item.text});
           });
       }
   }

   /*
   private resolveFileUri(URI:string, callbackResult:Function) : void {

     try {

       if ((typeof URI !== "undefined") && (URI !== null)) {

         if (URI.indexOf("file://") === 0) {

           // lets resolve to native path
           (window as any).FilePath.resolveNativePath(URI, (localFileUri: string) => {
             callbackResult(localFileUri)
           }, (e: any) => {
             alert("FAILED to resolve ContentURL(" + URI + ")");
           });

         }

         if (URI.indexOf("content://") === 0) {

           // try to resolve CONTENT-URL: https://developer.android.com/guide/topics/providers/content-providers.html
           (window as any).FilePath.resolveNativePath(URI, (win: string) => {

             console.log("Resolved ContentURL(" + URI + ") to FileURL(" + win + ") - go again");
             this.resolveFileUri(win, callbackResult);

           }, (error: any) => {
             alert("FAILED to resolve ContentURL(" + URI + ")");
           });
         }

         else {
           alert("ImageShare ERROR: fileUri unkown " + URI);
         }

       } else {
         alert("ERROR: fileUri undefined or NULL");
       }

     } catch (e) {
       console.error("EXCEPTION resolveFileUri", e);
       callbackResult(URI);
     }

   }
   */


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
      return device.platform=="iOS";
    } catch (e) {
      console.log("FAIL on Plugin cordova-plugin-device (1)");
      return false;
    }
  }

  /**
   * Check if app is running on a Android device.
   * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-device/index.html
   */
  isAndroid() : boolean {
    try {
      let device:any = (window as any).device;
      console.log("cordova-plugin-device", device);
      return device.platform=="Android";
    } catch (e) {
      console.log("FAIL on Plugin cordova-plugin-device (2)");
      return true;
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
    * Set a callback function to be called then device is ready for cordova service action.
    */
  subscribeServiceReady() : Observable<void> {
    return new Observable<void>((observer: Observer<void>) => {

      if (this.serviceIsReady) {

        // cordova already signaled that it is ready - call on the spot
        observer.next(null);
        observer.complete();
  
      } else {

        let waitLoop = () => {
          if (this.serviceIsReady) {
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

  restartCordova(parameters=""):void {
    this.setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,null);
    if(parameters)
        parameters="&"+parameters;
    if(navigator.userAgent.indexOf("ionic / edu-sharing-app")!=-1) {
        // go to ionic local server
        window.location.replace("http://localhost:54361/?reset=true" + parameters);
    }
    else{
        window.location.replace("http://app-registry.edu-sharing.com/ng/?reset=true" + parameters);
    }
    /*
    try {
      (navigator as any).splashscreen.show();
    } catch (e) {}
    document.location.href = this.initialHref;
    */
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


  /**
   * load permanent key/value 
   * @param key the key to request value
   * @param callback function callback with value as parameter - null if not available
   */
  getPermanentStorage(key:string, callback:Function) : void {

    // callback - to sync with ios sharescreen
    let callbackWrapper:Function = (val:any) => {
        /*
      if (this.isReallyRunningCordova() & this.isIOS()) {
        if (key==CordovaService.STORAGE_OAUTHTOKENS) {

          // see what was the last ios share oauth expire date
          this.iosShareScreenLoadValue(CordovaService.IOSSHARE_EXPIRES, (shareExpire:any) => {

            // if there is no value continue with local
            if (shareExpire==null) {
              callback(val);
            } else {

              // check if expire is newer in share then local
              try {
                let oAuthLocal:any = null;
                if (val!=null) oAuthLocal = JSON.parse(val);
                if ((oAuthLocal==null) || (+shareExpire>oAuthLocal.expires_ts)) {

                  // OK share has more up to date oauth 
                  // --> update local from share
                  oAuthLocal.expires_ts = +shareExpire;
                  this.iosShareScreenLoadValue(CordovaService.IOSSHARE_ACCESS,(shareAccess:string)=>{
                    oAuthLocal.access_token = shareAccess;
                    this.iosShareScreenLoadValue(CordovaService.IOSSHARE_REFRESH, (shareRefresh:string)=>{
                      oAuthLocal.refresh_token = shareRefresh;
                      let oAuthLocalJSON = JSON.stringify(oAuthLocal);
                      this.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS, oAuthLocalJSON);
                      callback(oAuthLocalJSON);
                    });
                  });
                
                } else {
                  callback(val);
                }

              } catch (e) {
                console.error("EXCEPTION on sync with ios share extension", e);
                callback(val);
              }

            }

          });

        } else {
          callback(val);
        }
      } else {*/
        callback(val);
      //}
    } 

    // get value from HTML5 local storage
    let value = window.localStorage.getItem(key);

    //just iun case - check if backup is available from nativestorage plugin
    if (((typeof value == 'undefined') || (value==null)) && (this.isIOS()) && ((window as any).NativeStorage)) {
      try {
        // window['NativeStorage'].getItem("reference_to_value",<success-callback>, <error-callback>);
        (window as any).NativeStorage.getItem(key,(valueNative:any)=>{
          // WIN 
          if (typeof valueNative == "undefined") valueNative = null;
          callbackWrapper(valueNative);
        },(error:any)=>{
          // FAIL (also when key not available)
          callbackWrapper(null);
        });
      } catch (e) {
        console.error("Plugin Fail",e);
        callbackWrapper(null);
      }
    } else {
      callbackWrapper(value);
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
    if (this.isIOS()) {
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

    // if a oauth relevant key - sync with sharescreen
    if (key==RestConstants.CORDOVA_STORAGE_OAUTHTOKENS) {
      try {
        let oauthData:any = JSON.parse(value);
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_ACCESS,oauthData.access_token);
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_REFRESH,oauthData.refresh_token);
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_EXPIRES,oauthData.expires_ts);
      } catch (e) {
        console.error("EXCEPTION on storing oauth data for ios sharescreen ",e);
      }
    }

    // if server address - sync with sharescreen
    if (key==RestConstants.CORDOVA_STORAGE_SERVER_OWN && this.isIOS()) {
      try {
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_SERVER,value);
      } catch (e) {
        console.error("EXCEPTION on storing server data for ios sharescreen ",e);
      }
    }

  }

  /**
   * erase all permanent data
   */
  clearPermanentStorage() : void {

    // clear HTML5 local storage
    window.localStorage.clear();

    // clear native storage 
    if (this.isIOS()) {
      try {
        (window as any).NativeStorage.clear(() => {
        }, (error: any) => {
          // FAIL
          console.error("Fail NativeStorage.clear", error);
        });
      } catch (e) {
        console.error("Plugin Fail", e);
      }
    }

    // clear oauth sync with ios share screen
    try {
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_ACCESS,"");
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_REFRESH,"");
        this.iosShareScreenStoreValue(CordovaService.IOSSHARE_EXPIRES, (new Date).getTime()+"");
    } catch (e) {
        console.error("EXCEPTION on storing oauth data for ios sharescreen ",e);
    }

  }

  /**
   * after init, load the stored info from the cordova storage and save it as class members for access of other services
   */
  loadStorage(){
    this.getPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,(data:string)=>{
        this._oauth = (data!=null) ? JSON.parse(data) : null;
        this.serviceIsReady=true;
    });
  }

  clearAllCookies() : void {
    let cookies = document.cookie.split(";");
    for (let i = 0; i < cookies.length; i++) {
        let cookie = cookies[i];
        let eqPos = cookie.indexOf("=");
        let name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT";
    }
  }

  /**********************************************************
   * Sync with iOS Share Screen thru NSUserdefaults Plugin
   **********************************************************
   * Server and oAuth Token need to be synced with the iOS share
   * screen thru the NSUserdefaults storage ny using the following plugin:
   * https://github.com/rootzoll/cordova-plugin-nsuserdefaults-for-app-groups.git
   */

   // keys used to exchange data with ios share extension
   public static IOSSHARE_ACCESS:string = "access_token";
   public static IOSSHARE_REFRESH:string = "refresh_token";
   public static IOSSHARE_EXPIRES:string = "expires_in";
   public static IOSSHARE_SERVER:string = "eduserver";

   /**
    * Use internally every time a value gets loaded from local storage
    * that also needs to be in sync with the iOS share screen.
    * @param key 
    * @param callback returns with string value or null
    */
   private iosShareScreenLoadValue(key:string, callback:Function) : void {
    try {
      if (this.isReallyRunningCordova() && this.isIOS) {
        (window as any).AppGroupsUserDefaults.save({
            suite: "group.edusharing",
            key: key
          }, function(value:any) {
            console.log("PLUGIN OK info.protonet.appgroupsuserdefaults: Key '"+key+"' loaded");
            if (typeof value == "undefined") value = null;
            callback(value);
          }, function(fail:any) {
            console.error("PLUGIN FAIL info.protonet.appgroupsuserdefaults LOAD: ",fail);
            callback(null);
          });
      }
    } catch (e) {
      console.error("PLUGIN EXCEPTION info.protonet.appgroupsuserdefaults LOAD: ",e);
    }    
   } 

   /**
    * Use internally every time a value gets stored to local storage
    * that also needs to be in sync with the iOS share screen.
    * @param key 
    * @param value 
    */
   private iosShareScreenStoreValue(key:string, value:string) :void {
    try {
      if (this.isReallyRunningCordova() && this.isIOS) {
        (window as any).AppGroupsUserDefaults.save({
            suite: "group.edusharing",
            key: key,
            value: value
          }, function(win:any) {
            console.log("PLUGIN OK info.protonet.appgroupsuserdefaults: Key '"+key+"' stored");
          }, function(fail:any) {
            console.error("PLUGIN FAIL info.protonet.appgroupsuserdefaults SAVE: ",fail);
          });
      }
    } catch (e) {
      console.error("PLUGIN EXCEPTION info.protonet.appgroupsuserdefaults SAVE: ",e);
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
        
    if (this.isIOS()) {
      successCallback();
      return;
    }

    try {

      let permissions = (window as any).cordova.plugins.permissions;
      let permissionString:string = permissions[permission] as string;

      // console.log("permissions",permissions);
      // console.log("permissionString",permissionString);

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
        console.error(error);
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
        encodingType: 0, //Camera.EncodingType.JPEG
        quality: 70
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

  public uploadLocalContent(uri:string,endpointUrl:string,headers:any) : Observable<any>{
      return new Observable<string>((observer: Observer<any>) => {
          let fileTransfer: any = new (window as any).FileTransfer();
          fileTransfer.upload(uri, endpointUrl, (result: any) => {
              observer.next(result);
              observer.complete();
          },(error:any)=>{
              observer.error(error);
              observer.complete();
          },{headers:headers});
      });
  }

   /**********************************************************
   * Download PlugIn
   **********************************************************
   * Make it possible to download content to the mobile device.
   * Make sure that the URL given will authenticate the user.
   * iOS: https://github.com/apache/cordova-plugin-file-transfer
   * Android: https://github.com/wymsee/cordova-HTTP
   */

   downloadContent(downloadURL:string, fileName:string, winCallback:Function=null, failCallback:Function=null) : void {
     let status=0;
     let resultPath="";
     try {
       this.makeSurePermission("WRITE_EXTERNAL_STORAGE", (win: any) => {

         console.log("Got Permission");

         // add oauth token if not alreafy in URL
         if ((downloadURL.indexOf('accessToken=') < 0) && (this._oauth != null)) {
           if (downloadURL.indexOf('?') < 0) {
             downloadURL = downloadURL + "?accessToken=" + this._oauth.access_token;
           } else {
             downloadURL = downloadURL + "&accessToken=" + this._oauth.access_token;
           }
         }

         if (this.isIOS()) {

           // iOS: following redirects works automatically - so go direct
           console.log("downloadContent IOS URL: " + downloadURL);
           this.startContentDownload(downloadURL, fileName, (filePath:string)=>{status=1;resultPath=filePath;}, ()=>status=-1);

         } else {

           // Android: resolve redirect (because plugin download can not follow redirect)
           /*console.log("resolving redirects for downloadContent URL ANDROID: " + downloadURL);
           (window as any).CordovaHttpPlugin.head(downloadURL, {}, {}, (response: any) => {
             console.log("200 NOT A REDIRECT URL - use original: " + downloadURL);*/
             this.startContentDownload(downloadURL, fileName,(filePath:string)=>{
                 resultPath=filePath;
                 status=1;
             }, ()=>status=-1);
           /*}, (response: any) => {
             if (response.status == 302) {
               let redirectURL = decodeURIComponent(response.headers.Location);
               console.log("302 Redirect Resolved to: " + redirectURL);
               this.startContentDownload(redirectURL, fileName,()=>status=1, ()=>status=-1);
             } else {
               status=-1;
             }
           });*/

         }

       }, (error: any) => {
           status=-1;
       });


     } catch (e) {
       console.warn(e);
       status=-1;
     }
     let interval=setInterval(()=>{
       if(status==0)
         return;
       clearInterval(interval);
       if(status==1 && winCallback) {
           if(this.isAndroid()) {
               // suggest user to open the file
               console.log("android, openWith", resultPath);
               (window as any).plugins.intent.showOpenWith(resultPath, () => {}, () => {});
           }
           winCallback();
       }
       if(status==-1 && failCallback)
         failCallback();
     },100);
   } 

   private startContentDownload(downloadURL:string, fileName:string, winCallback:Function, failCallback:Function) : void {
     console.log("cordova start download "+downloadURL);
     // set path to store on device
     let targetPath = (window as any).cordova.file.externalRootDirectory + "Download/";
     if (this.isIOS()) targetPath = (window as any).cordova.file.documentsDirectory;
       let localPath = targetPath + fileName;
       let filePath = encodeURI(localPath);
    console.log("target path: "+filePath+" (local "+localPath+")");
     // iOS
     let fileTransfer:any = new (window as any).FileTransfer();
     fileTransfer.download(downloadURL, filePath, (result:any)=>{
         winCallback(localPath);
     }, (err:any) => {
         console.log("FAIL startContentDownload");
         failCallback("FAIL startContentDownload", err);
     }, true, {});
   }

   openInAppBrowser(url:string){
       let params:string;
       if(this.isAndroid()){
           params="location=no,zoom=no";
       }
       else if(this.isIOS()){
           params="toolbar=yes,hideurlbar=yes,hidenavigationbuttons=yes,closebuttoncolor=#ffffff,closebuttoncaption="+this.injector.get(TranslateService).instant("CANCEL");
       }
       let win:any=cordova.InAppBrowser.open(url,"_blank",params);
       win.addEventListener( "loadstop", ()=> {
           // register iframe handling
           win.executeScript({code:`
                var cordovaIframeList=document.getElementsByTagName('iframe');
                function cordovaReceiveMessage(event){
                    for(var i=0;i<cordovaIframeList.length;i++){
                        if (cordovaIframeList[i].contentWindow === event.source) {
                            window.opener.postMessage(event.data);
                            return;
                        }
                    }
                    for(var i=0;i<cordovaIframeList.length;i++){
                        cordovaIframeList[i].contentWindow.postMessage(event.detail,'*');
                    }
                }
                window.addEventListener("message", cordovaReceiveMessage, false);        
           `});
           win.postMessage=(data:any)=>{
               // Redirect post messages to new window
               win.executeScript({
                   code:`
                        var event = new CustomEvent('message',{detail:`+JSON.stringify(data)+`});
                        window.dispatchEvent(event);`
               },null);
           };
           this.events.addWindow(win);
           let loop = setInterval(()=>{

               // Execute JavaScript to fetch message chain
               win.executeScript(
                   {
                       code: `
                        if(!window.messages){
                            window.messages=[];
                        }
                        window.opener={
                            postMessage:function(event,scope){
                                window.messages.push(event);
                            }
                        };
                        var msg=window.messages;
                        window.messages=[];
                        JSON.stringify(msg);`
                   },
                   (values:string[])=>{
                       let events = JSON.parse(values[0]);
                       for(let e of events) {
                           let event={source:win, data:e};
                           console.log(event);
                           this.events.onEvent(event);
                           if(e.event==FrameEventsService.EVENT_CLOSE){
                               clearInterval(loop);
                           }
                           if(e.event==FrameEventsService.EVENT_CORDOVA_CAMERA){
                               this.getPhotoFromCamera((data:any)=>{
                                   this.events.broadcastEvent(FrameEventsService.EVENT_CORDOVA_CAMERA_RESPONSE,data);
                               },()=>{
                                   this.events.broadcastEvent(FrameEventsService.EVENT_CORDOVA_CAMERA_RESPONSE,null);
                               });
                           }
                       }
                   }
               );
           },100);
       });
       return win;
   }
   openBrowser(url:string){
       window.open(url,'_system');
   }
   /**********************************************************
   * FileViewer PlugIn
   **********************************************************
   * To open content native on the app - e.g. after download
   * use this plugin
   * https://github.com/SpiderOak/FileViewerPlugin
   */

   openContentNative(filePath:string, successCallback:Function=null, failCallback:Function=null) : void {
    try {
      (window as any).FileViewerPlugin.view({
              action: (window as any).FileViewerPlugin.ACTION_VIEW,
              url: filePath
          },
          () => {
            // WIN
              if(successCallback) successCallback();
          },
          (error:any) => {
            // FAIL
            if(failCallback) failCallback("FAIL on openContentNative",error);
          }
      );
    } catch (e) {
        if(failCallback) failCallback("EXCEPTION on openContentNative",e);
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

  // oAuth login that is used when running as mobile app
  public loginOAuth(endpointUrl:string, username: string = "", password: string = "",grantType = "password"): Observable<OAuthResult> {

    let url = endpointUrl + "../oauth2/token";
    let headers = {'Content-Type':'application/x-www-form-urlencoded','Accept': '*/*'};
    let options = { headers: headers, withCredentials: true };

    let data = "client_id=eduApp&client_secret=secret" +
      "&grant_type=" + encodeURIComponent(grantType) +
      "&username=" + encodeURIComponent(username) +
      "&password=" + encodeURIComponent(password);

    return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {
      this.http.post<OAuthResult>(url, data, options).subscribe(
        (oauth: OAuthResult) => {

          if (oauth == null) {
            observer.error("INVALID_CREDENTIALS");
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
  public reinitStatus(endpointUrl:string,goToLogin=true,loginNext=window.location.href):Observable<void>{
      return new Observable<void>((observer: Observer<void>) => {

          console.info("cordova: reinit");

          if(this.reiniting) {
              let interval=setInterval(()=>{
                  console.log("cordova: wait for reinit finish");
                  if(!this.reiniting){
                      clearInterval(interval);
                      observer.next(null);
                      observer.complete();
                  }
              },50);
              return;
          }
          console.log("cordova: refresh oAuth");
          if(!this.oauth){
              console.log("cordova: no oAuth, go to Login:"+goToLogin+", next: "+loginNext);
              if(goToLogin) {
                  this.goToLogin(loginNext);
              }
              observer.error(null);
              observer.complete();
              return;
          }
          this.reiniting = true;
          this.refreshOAuth(endpointUrl,this.oauth).subscribe(() => {
              console.info("cordova: oauth OK");
              this.reiniting = false;
              observer.next(null);
              observer.complete();
          }, (error: any) => {
              console.warn(error);
              console.warn("cordova: invalid oauth, go back to server selection");
              this.reiniting = false;
              this.resetAndGoToServerlist();
              observer.error(null);
              observer.complete();
          });
      });
  }

    private resetAndGoToServerlist(parameters="") {
        this.setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS, null);
        this.clearAllCookies();
        this.restartCordova(parameters);
    }

// oAuth refresh tokens
  private refreshOAuth(endpointUrl:string,oauth: OAuthResult): Observable<OAuthResult> {

    let url = endpointUrl + "../oauth2/token";
    let headers = {'Content-Type': 'application/x-www-form-urlencoded','Accept': '*/*'};
    let options = { headers: headers, withCredentials: false }

    let data = "grant_type=refresh_token&client_id=eduApp&client_secret=secret" +
      "&refresh_token=" + encodeURIComponent(oauth.refresh_token);

    return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {
      this.http.post<OAuthResult>(url, data, options).subscribe(
        (oauthNew) => {

          // set local expire ts on token
          this.oauth=oauthNew;
          console.log(oauthNew);
          observer.next(this.oauth);
          observer.complete();

        },
        (error: any) => {
          console.error(error);
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
  /*
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
  */

    hasValidConfig() {
        return this._oauth;
    }

    getLanguage() {
      return new Observable<string>((observer: Observer<string>) => {
          observer.next(navigator.language.split("-")[0]);
          observer.complete();
        /*try {
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
        */

      });
    }

    private onBackKeyDown() {
      console.log("back key pressed");
        let eventDown = new KeyboardEvent('keydown', {key: 'Escape',view: window,bubbles: true,cancelable: true});
        let eventUp = new KeyboardEvent('keyup', {key: 'Escape',view: window,bubbles: true,cancelable: true});
        let down = !window.document.dispatchEvent(eventDown);
        let up = !window.document.dispatchEvent(eventUp);
        console.log("was catched by escape "+down);
        if(down || up){

        } else// if(window.history.length>2) {
            //(navigator as any).app.backHistory();
            this.location.back();
        /*}
        else{
            (navigator as any).app.exitApp();
        }*/
    }
    getIndexPath() {
        return cordova.file.applicationDirectory+'www/';
    }

    private goToLogin(next:string) {
        this.router.navigate([UIConstants.ROUTER_PREFIX,"app"],{queryParams:{next:next}});
    }
}
