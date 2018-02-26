import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from "../../common/ui/toast";
import {Router, Route, Params, ActivatedRoute} from "@angular/router";
import {OAuthResult, LoginResult, AccessScope} from "../../common/rest/data-object";
import {RouterComponent} from "../../router/router.component";
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {RestConstants} from "../../common/rest/rest-constants";
import {ConfigurationService} from "../../common/services/configuration.service";
import {FrameEventsService} from "../../common/services/frame-events.service";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {Scope} from "@angular/core/src/profile/wtf_impl";
import {UIConstants} from "../../common/ui/ui-constants";
import {Helper} from "../../common/helper";
import {RestHelper} from "../../common/rest/rest-helper";

import {CordovaService} from "../../common/services/cordova.service";

@Component({
  selector: 'workspace-login',
  templateUrl: 'login.component.html',
  styleUrls: ['login.component.scss']
})
export class LoginComponent  implements OnInit{
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
    this.disabled=!this.username;// || !this.password;
  }
  private recoverPassword(){
    window.location.href=this.config.recoverPasswordUrl;
  }
  private register(){
    window.location.href=this.config.registerUrl;
  }
  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private router:Router,
              private translate:TranslateService,
              private configService:ConfigurationService,
              private title:Title,
              private storage : SessionStorageService,
              private route : ActivatedRoute,
              private cordova: CordovaService
            ){

    Translation.initialize(translate,this.configService,this.storage,this.route).subscribe(()=>{
      UIHelper.setTitle('LOGIN.TITLE',title,translate,configService);
      this.configService.getAll().subscribe((data:any)=>{
        this.config=data;

        this.username=this.configService.instant("defaultUsername","");
        this.password=this.configService.instant("defaultPassword","");
        this.route.queryParams.forEach((params: Params) => {
          this.connector.onAllRequestsReady().subscribe(()=>this.isLoading=false);
          this.scope=params['scope'];
          if(!this.scope)
            this.scope=null;
          this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
            if(data.currentScope){
              this.connector.logout().subscribe(()=>{}); // just to make sure there is no scope still set // NO: We need a valid session when login to scope!!!
              data.statusCode=null;
            }
            else if(data.currentScope==this.scope){
              if(data.statusCode==RestConstants.STATUS_CODE_OK && params['local']!="true"){
                this.goToNext();
              }
            }
            if(params['local']!="true" && configService.instant("loginUrl") && data.statusCode!=RestConstants.STATUS_CODE_OK){
              window.location.href=configService.instant("loginUrl");
              return;
            }
          });
          this.showUsername=this.scope!=RestConstants.SAFE_SCOPE;
          this.next=params['next'];
          this.mainnav=params['mainnav']=='false' ? false : true;
          if(this.scope==RestConstants.SAFE_SCOPE){
            this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
              if(data.statusCode!=RestConstants.STATUS_CODE_OK){
                RestHelper.goToLogin(this.router,this.configService);
              }
              else{
                this.connector.hasAccessToScope(RestConstants.SAFE_SCOPE).subscribe((scope:AccessScope)=>{
                  if(scope.hasAccess){
                    this.username=data.authorityName;
                    if (this.passwordInput)
                      this.passwordInput.nativeElement.focus();
                  }
                  else{
                    this.toast.error(null,'LOGIN.NO_ACCESS');
                    this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
                    //window.history.back();
                  }
                },(error:any)=>{
                  this.toast.error(error);
                });
              }
            },(error:any)=>RestHelper.goToLogin(this.router,this.configService));
          }
          setTimeout(()=>{
            if (this.username && this.passwordInput)
              this.passwordInput.nativeElement.focus();
            else if(this.usernameInput){
              this.usernameInput.nativeElement.focus();
            }
          },250);

          if(this.scope==RestConstants.SAFE_SCOPE){
            this.caption='LOGIN.TITLE_SAFE';
          }
          else{
            this.caption='LOGIN.TITLE';
          }
        });

      });

    });
    this.isLoading=true;
  }
  ngOnInit() {


  }
  private login(){
    
    this.isLoading=true;

      this.connector.login(this.username,this.password,this.scope).subscribe(
        (data:string) => {
          if(data==RestConstants.STATUS_CODE_OK) {
            this.goToNext();
          }
          else if(data==RestConstants.STATUS_CODE_PREVIOUS_SESSION_REQUIRED || data==RestConstants.STATUS_CODE_PREVIOUS_USER_WRONG){
            this.toast.error(null,"LOGIN.SAFE_PREVIOUS");
            this.isLoading=false;
          }
          else{
            this.toast.error(null,"LOGIN.ERROR");
            this.isLoading=false;
          }
        },
        (error:any)=>{
          this.toast.error(error);
          this.isLoading=false;
        });

  }

  private goToNext() {
    if(this.next){
      this.next=Helper.addGetParameter("fromLogin","true",this.next);
      window.location.assign(this.next);
    }
    else {
      UIHelper.goToDefaultLocation(this.router,this.configService);
    }
  }
}
