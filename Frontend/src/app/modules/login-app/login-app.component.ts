import {
    Component,
    OnInit,
    //ApplicationRef 
} from '@angular/core';

import { Toast } from "../../common/ui/toast";
import {Router, Route, ActivatedRoute} from "@angular/router";
import { OAuthResult, LoginResult, AccessScope } from "../../common/rest/data-object";
import { UIConstants } from "../../common/ui/ui-constants";
import { CordovaService } from "../../common/services/cordova.service";
import { ConfigurationService } from '../../common/services/configuration.service';
import { UIHelper } from '../../common/ui/ui-helper';
import { Translation } from '../../common/translation';
import { TranslateService } from '@ngx-translate/core';
import { RestHelper } from '../../common/rest/rest-helper';
import { STATUS_CODES } from 'http';
import {RestLocatorService} from "../../common/rest/services/rest-locator.service";
import {SessionStorageService} from "../../common/services/session-storage.service";

// possible states this UI component can be in
enum StateUI { SERVERLIST = 0, LOGIN = 1, SERVERURL = 2, NOINTERNET = 3};

@Component({
    selector: 'app-login',
    templateUrl: 'login-app.component.html',
    styleUrls: ['login-app.component.scss']
})
export class LoginAppComponent  implements OnInit {

    private state:StateUI = StateUI.NOINTERNET;

    private instanceTS:number = null;

    public isLoading=true;
    public disabled=true;
    private username="";
    private password="";
    private serverurl = "https://";   
    
    errorURL:string = null;

    servers: any;
    currentServer: any;
    private locationNext: string;

    constructor(
        private toast:Toast,
        private router:Router,
        private route:ActivatedRoute,
        private translation: TranslateService,
        private cordova: CordovaService,
        private config: ConfigurationService,
        private locator: RestLocatorService,
        //private applicationRef: ApplicationRef
    ){

        this.instanceTS = Date.now();
        console.log("CONSTRUCTOR LoginAppComponent",this.instanceTS);

        this.isLoading=true;

        // WHEN RUNNING ON DESKTOP --> FORWARD TO BASIC LOGIN PAGE
        if (!this.cordova.isRunningCordova()) {
            console.log("Not Cordova -> Forward to normal LOGIN");
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'login']);
            return;
        }
        
        this.route.queryParams.subscribe((params)=>{
            this.locationNext=params['next'];
        });

        // 1. Wait until Cordova is Ready
        this.cordova.subscribeServiceReady().subscribe(()=>{

            console.log("Cordova: Service is READY");

            // app startup, cordova has valid data ?
            // -> go to default location (this will check oauth)
            if (this.cordova.hasValidConfig()) {
                console.log("VALID Configuration --> directly go to default");
                this.goToDefaultLocation();
                return;
            }

            // set the self set server url if available from persistence
            // for this value its no problem that result is async
            // init translation service
            console.log("INIT TranslationService .. START");
            this.init();
        });

    }

    buttonExitApp() :void {
        this.cordova.exitApp();
    }


    ngOnInit() {
    }

    private checkConditions() :void  {
        this.disabled=!this.username;// || !this.password;
    }

    private buttonLoginBack() : void {
        //window.history.back();
        //window.location.replace(this.cordova.getIndexPath()+"?reset=true");
        this.cordova.restartCordova();
        //(navigator as any).app.loadUrl(this.cordova.getIndexPath()+"?reset=true");
    }

    private buttonEnterServer() : void {
        this.state = StateUI.SERVERURL;
    }

    private login(){


        /*
        // test camera
        this.cordova.getPhotoFromCamera(
        (win:any)=>{
            console.log("CAMERA WIN",win);
        },
        (error:any, info:any)=>{
            console.log("CAMERA FAIL", error);
            console.dir(info);
        });
        if (1==1) return;
        */

        /*
        // test file download
        this.cordova.downloadContent("http://sample-videos.com/video/mp4/240/big_buck_bunny_240p_20mb.mp4", "test.mp4",(win:any)=>{
            alert("OK "+win);
            this.cordova.openContentNative(win,()=>{
                alert("OK Open");
            }, (text:string, error:any) => {
                console.log(text, error);
                alert("FAIL open");
            });
        }, (text:string,error:any) => {
            console.error(text, error);
            alert("FAIL: "+text);
        });
        if (1==1) return;
        */
        this.isLoading=true;
        // APP: oAuth Login
        this.cordova.loginOAuth(this.locator.endpointUrl,this.username, this.password).subscribe((oauthTokens: OAuthResult) => {
                this.cordova.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS, JSON.stringify(oauthTokens));
                // continue to within the app
                this.goToDefaultLocation();
            },
            (error) => {
                this.isLoading = false;
                if (typeof error == "string") {
                    this.toast.error(null, error);
                } else {
                    this.toast.error(null, "LOGIN.ERROR");
                }

            });
        /*
        this.cordova.setServerURL(this.currentServer.url+"rest/",true).subscribe(()=> {


        });
        */

    }

    private goToDefaultLocation() {
        if(this.locationNext){
            window.location.replace(this.locationNext);
        }
        else {
            this.config.getAll().subscribe(() => {
                UIHelper.goToDefaultLocation(this.router, this.config, {replaceUrl: true});
            });
        }
    }
    getServerIcon(){
        return 'assets/images/app-icon.svg';
    }
    private init() {
        Translation.initializeCordova(this.translation,this.cordova).subscribe(()=>{
            console.log("INIT TranslationService .. OK");
            this.locator.locateApi().subscribe(()=>{
                this.serverurl=this.locator.endpointUrl;
                this.state=StateUI.LOGIN;
                this.isLoading=false;
            });

        });
    }
}
