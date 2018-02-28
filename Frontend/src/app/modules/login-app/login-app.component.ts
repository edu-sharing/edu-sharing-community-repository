import {
    Component,
    OnInit
} from '@angular/core';

import { Toast } from "../../common/ui/toast";
import { Router, Route } from "@angular/router";
import { OAuthResult, LoginResult, AccessScope } from "../../common/rest/data-object";
import { UIConstants } from "../../common/ui/ui-constants";
import { CordovaService } from "../../common/services/cordova.service";
import {ConfigurationService} from '../../common/services/configuration.service';
import {UIHelper} from '../../common/ui/ui-helper';
import {Translation} from '../../common/translation';
import {TranslateService} from '@ngx-translate/core';

// possible states this UI component can be in
enum StateUI { SERVERLIST = 0, LOGIN = 1, SERVERURL = 2};

@Component({
    selector: 'app-login',
    templateUrl: 'login-app.component.html',
    styleUrls: ['login-app.component.scss']
})
export class LoginAppComponent  implements OnInit{

    private state:StateUI = StateUI.SERVERLIST;

    public isLoading=true;
    public disabled=true;
    private username="";
    private password="";
    private serverurl="https://";
    servers: any;
    currentServer: any;

    constructor(
        private toast:Toast,
        private router:Router,
        private translation: TranslateService,
        private cordova: CordovaService,
        private config : ConfigurationService
    ){
        this.isLoading=true;
        if (!this.cordova.isRunningCordova()) {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'login']);
            return;
        }
        Translation.initializeCordova(this.translation,this.cordova).subscribe(()=>{
            this.cordova.getPublicServerList().subscribe((servers:any)=>{
                this.servers=servers;
                console.log(servers);
                // WHEN RUNNING ON DESKTOP --> FORWARD TO BASIC LOGIN PAGE


                /*
                 * APP Start Setup
                 */

                // 1. Wait until Cordova is Ready
                this.cordova.setDeviceReadyCallback(()=>{
                    // app startup, cordova has valid data ? -> go to default location (this will check oauth)
                    if(this.cordova.hasValidConfig()){
                        UIHelper.goToDefaultLocation(this.router,this.config);
                        return;
                    }
                    this.isLoading=false;
                });
            });
        });


    }
    getServerIcon(server:any){
        return server.url+'assets/images/logo.svg';
    }
    ngOnInit() {
    }
    private checkConditions(){
        this.disabled=!this.username;// || !this.password;
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
    chooseServer(server:any){
        this.currentServer=server;
        this.state=StateUI.LOGIN;
    }
    private textInputFokus() : void {
    }

    private buttonRegister() {
        alert("TODO");
    }

    private login(){
        this.isLoading=true;
        this.cordova.setServerURL(this.currentServer.url+"rest",true).subscribe(()=> {

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
