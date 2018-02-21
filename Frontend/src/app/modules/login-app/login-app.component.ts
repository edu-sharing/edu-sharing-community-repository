import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from "../../common/ui/toast";
import {Router, Route } from "@angular/router";
import {OAuthResult, LoginResult, AccessScope} from "../../common/rest/data-object";
import {RestConstants} from "../../common/rest/rest-constants";
import {UIConstants} from "../../common/ui/ui-constants";
import {Helper} from "../../common/helper";
import {RestHelper} from "../../common/rest/rest-helper";

import {CordovaService} from "../../common/services/cordova.service";

@Component({
  selector: 'app-login',
  templateUrl: 'login-app.component.html',
  styleUrls: ['login-app.component.scss']
})
export class LoginAppComponent  implements OnInit{

  @ViewChild('passwordInput') passwordInput : ElementRef;
  @ViewChild('usernameInput') usernameInput : ElementRef;
  @ViewChild('loginForm') loginForm : ElementRef;

  public isLoading=true;
  private disabled=false;
  private showUsername=true;
  private username="";
  private password="";
  private scope="";
  private next="";
  public mainnav=true;
  private caption="LOGIN.TITLE";
  private config: any={};

  private checkConditions(){
    this.disabled=!this.username;
  }

  constructor(
              private toast:Toast,
              private router:Router,
              private cordova: CordovaService
            ){

    this.isLoading=true;

    // WHEN RUNNING ON DESKTOP --> FORWARD TO BASIC LOGIN PAGE
    if (!this.cordova.isRunningCordova()) {
      this.router.navigate([UIConstants.ROUTER_PREFIX + 'login']);
    } else {

      /*
       * APP Start Setup
       */ 

      // 1. Wait until Cordova is Ready
      this.cordova.setDeviceReadyCallback(()=>{

        // 2. Check if server is already set
        // SET AND TEST API URL BY CORDOVA SERVICE - later make select dialog
        this.cordova.setServerURL("http://edu41.edu-sharing.de/edu-sharing/rest/", false).subscribe(
          (win)=>{

            // 3. Check if oAuth tokens are available
            this.cordova.getPermanentStorage(this.cordova.STORAGE_OAUTHTOKENS,(oauthStr:string)=>{

              if (oauthStr!=null) {

                if (confirm('Found existing oAuth tokens - wanna try?')) {

                // get oAuth object from JSON string
                let oauth:OAuthResult = null;
                try {
                  oauth = (JSON.parse(oauthStr) as OAuthResult);
                } catch (e) {
                  console.log("FAIL TO PARSE ("+oauthStr+")");
                  this.isLoading=false;
                  return;
                }

                // got oauth token --> try to login with these
                this.cordova.initOAuthSession(oauth).subscribe(
                  (updatedOAuthTokens)=>{

                    // store oAuth tokens (becuase maybe refreshed)
                    this.cordova.setPermanentStorage(this.cordova.STORAGE_OAUTHTOKENS,JSON.stringify(updatedOAuthTokens));
                    
                    // continue to within the app
                    this.goToWorkspace();

                  },
                  (error)=>{
                    
                    if (error == "INVALID") {
                      // oauth outdated --> show login screen
                      this.isLoading=false;
                      return;
                    }

                    console.log("FAIL initOAuthSession "+oauthStr,error);
                    alert("DEBUG: Was not able to refresh oAuthTokens - check why");
                    this.isLoading=false;
                    return;
                  }
                );

                } else {
                  this.isLoading=false;
                }

              } else {

                // no oauth tokens --> show login screen
                this.isLoading=false;
                return;

              }

            });

          },
          (error)=>{

            this.isLoading=false;
            alert("TODO: Handle ERROR: "+error);
            // TODO: Change Server Selected, retry or try later again (exit app)

          }
        );

      });


    }

  }

  ngOnInit() {
  }

  private login(){
    
    this.isLoading=true;

      // APP: oAuth Login
      this.cordova.loginOAuth(this.username,this.password).subscribe((oauthTokens:OAuthResult)=>{

        // init session with ne tokens
        this.cordova.initOAuthSession(oauthTokens).subscribe(
          (win)=>{

            // store oAuth tokens
            this.cordova.setPermanentStorage(this.cordova.STORAGE_OAUTHTOKENS,JSON.stringify(oauthTokens));

            // continue to within the app
            this.goToWorkspace();

          },
          (error)=>{

            this.isLoading=false;
            this.toast.error(null,"LOGIN.ERROR");
 
          }
        );

      },(error:any)=>{

        this.isLoading=false;
        if (typeof error == "string") {
          this.toast.error(null, error);
        } else {
          this.toast.error(null,"LOGIN.ERROR");
        } 

      });

  }

  private goToWorkspace() {
    this.router.navigate([UIConstants.ROUTER_PREFIX + 'workspace']);
  }



}
