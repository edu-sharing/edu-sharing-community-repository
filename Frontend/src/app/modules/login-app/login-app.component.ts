import {
    Component,
    OnInit,
    //ApplicationRef 
} from '@angular/core';

import { Toast } from "../../common/ui/toast";
import { Router, Route } from "@angular/router";
import { OAuthResult, LoginResult, AccessScope } from "../../common/rest/data-object";
import { UIConstants } from "../../common/ui/ui-constants";
import { CordovaService } from "../../common/services/cordova.service";
import { ConfigurationService } from '../../common/services/configuration.service';
import { UIHelper } from '../../common/ui/ui-helper';
import { Translation } from '../../common/translation';
import { TranslateService } from '@ngx-translate/core';
import { RestHelper } from '../../common/rest/rest-helper';
import { AnimationKeyframesSequenceMetadata } from '@angular/core/src/animation/dsl';
import { STATUS_CODES } from 'http';

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

    constructor(
        private toast:Toast,
        private router:Router,
        private translation: TranslateService,
        private cordova: CordovaService,
        private config : ConfigurationService,
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

        // 1. Wait until Cordova is Ready
        this.cordova.subscribeServiceReady().subscribe(()=>{

            console.log("Cordova: Service is READY");

            // app startup, cordova has valid data ? 
            // -> go to default location (this will check oauth)
            if (this.cordova.hasValidConfig()) {
                console.log("VALID Configuration --> directly go to default");
                UIHelper.goToDefaultLocation(this.router,this.config);
                return;
            }

            // set the self set server url if available from persistence
            // for this value its no problem that result is async
            this.cordova.getPermanentStorage(CordovaService.STORAGE_SERVER_OWN,(value:string)=>{
                if (value!=null) {
                    this.serverurl = value;
                    this.checkUrl();
                }
            });

            // init translation service
            console.log("INIT TranslationService .. START");
            Translation.initializeCordova(this.translation,this.cordova).subscribe(()=>{
                console.log("INIT TranslationService .. OK");
                this.getServerList();
            });
        });
        
    }

    private getServerList() : void {
        this.isLoading = true;
        this.cordova.getPublicServerList().subscribe((servers:any)=>{
            console.log("OK getServerList()", servers);
            this.servers=servers;
            /*
            for (let server of servers) {
                this.cordova.getServerAbout(server.url).subscribe((about:any)=>{
                    server.version=RestHelper.getRepositoryVersionFromAbout(about);
                }, (error) => {
                    console.log("HTTP FAIL getting about from "+server.url, error);
                });
            }
            */
            this.state = StateUI.SERVERLIST;
            this.isLoading=false;
            console.log("Is iOS ",this.cordova.isIOS());
            console.log("ALL OK - loading is ",this.isLoading);
            //this.applicationRef.tick();
        }, (error)=> {
            this.isLoading=false;
            this.state = StateUI.NOINTERNET;
            console.log("FAILED getServerList()", this.isLoading);
        });
    }

    buttonExitApp() :void {
        this.cordova.exitApp();
    }

    getServerIcon(server:any){
        return server.url+'assets/images/app-icon.svg';
    }

    ngOnInit() {
    }

    private checkConditions() :void  {
        this.disabled=!this.username;// || !this.password;
    }

    private checkUrl() : void {
        this.errorURL = null;
        this.disabled = true;
        this.serverurl = this.serverurl.trim();

        // FORMAT ERRORS
        if (this.serverurl.length<3) {
            this.errorURL = "LOGIN_APP.SERVERURL_TOOSHORT";
            return;
        }

        // JUST WARNINGS
        this.disabled = false;
        if (this.serverurl.startsWith('http:')) {
            this.errorURL = "LOGIN_APP.SERVERURL_NOHTTPS";
            return;
        }

    }

    private chooseServer(server:any) : void {
        if (server==null) {
            this.state=StateUI.SERVERURL;
        } else {
            this.currentServer=server;
            this.state=StateUI.LOGIN;
        }
    }

    private buttonLoginBack() : void {
        this.state = StateUI.SERVERLIST;
    }

    private buttonEnterServer() : void {
        this.state = StateUI.SERVERURL;
    }

    private buttonServerList() : void {
        this.state = StateUI.SERVERLIST;
    }

    private buttonTestServer() : void {
        alert("TODO");
    }

    private buttonRegister() : void {
        alert("TODO");
    }

    private buttonServerUrl() : void {

        this.serverurl = this.serverurl.trim();
        let url2check = this.serverurl;
        if (url2check.toLowerCase().indexOf('http')<0) url2check = "https://" + url2check;

        let whenUrlIsWorking:Function = (win:any) => {

            console.log("WIN",win);
            this.currentServer =     {
                "name" : "Eigener Server",
                "url"  : url2check
            };

            // remember this url
            this.cordova.setPermanentStorage(CordovaService.STORAGE_SERVER_OWN,url2check);

            this.state=StateUI.LOGIN;
            this.isLoading = false;
        };

        this.isLoading = true;
        this.cordova.getServerAbout(url2check).subscribe(
        (win) => {
            whenUrlIsWorking(win);
        },  
        (error) => {

            //console.log("URL was not working: "+url2check);
            //console.log("Try to fix format of URL and try again ..");

            // try to fix url a bit more
            if (url2check.indexOf('/edusharing')>10) url2check = url2check.replace("/edusharing", "/edu-sharing");
            if (url2check.endsWith("/edu-sharing")) url2check = url2check + "/";
            if ((url2check.endsWith("/")) && (!url2check.endsWith("edu-sharing/"))) url2check = url2check + "edu-sharing/";
            if (!url2check.endsWith("/edu-sharing/")) url2check = url2check + "/edu-sharing/";

            this.cordova.getServerAbout(url2check).subscribe(
                (win) => {
                    whenUrlIsWorking(win);
                },
                (error) => {
                    console.log("url check failed for "+url2check);
                    this.isLoading = false;
                    this.toast.error(null, "LOGIN.ERROR");
                }
            );

        });
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
    
        if (this.currentServer==null) {
            this.state=StateUI.SERVERURL;
            return;
        }

        this.isLoading=true;
        this.cordova.setServerURL(this.currentServer.url+"rest/",true).subscribe(()=> {

            // APP: oAuth Login
            this.cordova.loginOAuth(this.username, this.password).subscribe((oauthTokens: OAuthResult) => {
                    this.cordova.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS, JSON.stringify(oauthTokens));
                    // continue to within the app
                    this.goToWorkspace();
                },
                (error) => {
                    this.isLoading = false;
                    if (typeof error == "string") {
                        this.toast.error(null, error);
                    } else {
                        this.toast.error(null, "LOGIN.ERROR");
                    }

                });
        });
 
    }

    private goToWorkspace() {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'workspace']);
    }

}
