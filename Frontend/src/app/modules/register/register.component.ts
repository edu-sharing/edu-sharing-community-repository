import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit, ChangeDetectionStrategy, ChangeDetectorRef} from '@angular/core';
import {Toast} from "../../core-ui-module/toast";
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from "@angular/router";
import {OAuthResult, LoginResult, AccessScope, DialogButton} from "../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../core-ui-module/translation";
import {RestConnectorService} from "../../core-module/core.module";
import {RestConstants} from "../../core-module/core.module";
import {ConfigurationService} from "../../core-module/core.module";
import {FrameEventsService} from "../../core-module/core.module";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {SessionStorageService} from "../../core-module/core.module";
import {UIConstants} from "../../core-module/ui/ui-constants";
import {Helper} from "../../core-module/rest/helper";
import {RestHelper} from "../../core-module/core.module";
import {PlatformLocation} from "@angular/common";
import {CordovaService} from "../../common/services/cordova.service";
import {RegisterFormComponent} from "./register-form/register-form.component";
import {RegisterDoneComponent} from "./register-done/register-done.component";
import {RegisterRequestComponent} from "./register-request/register-request.component";
import {RegisterResetPasswordComponent} from "./register-reset-password/register-reset-password.component";

@Component({
  selector: 'app-register',
  templateUrl: 'register.component.html',
  styleUrls: ['register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterComponent{
    @ViewChild('registerForm') registerForm : RegisterFormComponent;
    @ViewChild('registerDone') registerDone : RegisterDoneComponent;
    @ViewChild('request') request : RegisterRequestComponent;
    @ViewChild('resetPassword') resetPassword : RegisterResetPasswordComponent;
    public isLoading=true;
    state = 'register';
    buttons: DialogButton[];

    public cancel(){
        RestHelper.goToLogin(this.router, this.configService, null, null);
    }

    public requestDone(email: string ){
        this.request.submit();
    }
    public linkRegister() {
        this.router.navigate([UIConstants.ROUTER_PREFIX + "register"]);
    }
    public newPassword(){
        this.resetPassword.newPassword();
    }

  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private platformLocation: PlatformLocation,
              private urlSerializer:UrlSerializer,
              private router:Router,
              private translate:TranslateService,
              private configService:ConfigurationService,
              private changes:ChangeDetectorRef,
              private title:Title,
              private storage : SessionStorageService,
              private route : ActivatedRoute,
            ){
      this.updateButtons();
      this.route.params.subscribe((params)=>{
          if(params['status']){
              if (params['status'] == "done" || params['status'] == "done-reset" || params['status'] == "request" || params['status'] == "reset-password") {
                  this.state = params['status'];
                  this.changes.detectChanges();
              } else{
                  this.router.navigate([UIConstants.ROUTER_PREFIX,"register"]);
              }
          }
      });

    Translation.initialize(this.translate,this.configService,this.storage,this.route).subscribe(()=> {
        UIHelper.setTitle('REGISTER.TITLE', title, translate, configService);
            this.isLoading=false;
            this.changes.detectChanges();
            if(!this.configService.instant("register.local",true)) {
                console.log("no register.local set, will go to login");
                RestHelper.goToLogin(this.router,this.configService,null,null);
            }
            setTimeout(()=>this.setParams());
            this.connector.isLoggedIn().subscribe((data)=>{
                if(data.statusCode=="OK"){
                    UIHelper.goToDefaultLocation(this.router,this.configService);
                }
            });
    });

    }

      onRegisterDone(){
          let email=this.registerForm.info.email;
          this.state='done';
          this.changes.detectChanges();
          // will loose state when going back to register form
          //this.router.navigate([UIConstants.ROUTER_PREFIX,"register","done","-",email]);
          UIHelper.waitForComponent(this,"registerDone").subscribe(()=>{
              this.registerDone.email=email;
              this.changes.detectChanges();
          });
          this.toast.toast("REGISTER.TOAST");
      }

    private setParams() {
        this.route.params.subscribe((params)=>{
            if(params['email'])
                this.registerDone.email=params['email'];
            if(params['key']) {
                if(this.registerDone)
                    this.registerDone.keyUrl = params['key'];
                if(this.resetPassword)
                    this.resetPassword.key = params['key'];
            }
        });
    }

    modifyData() {
        if (this.state == 'done'){
            this.state='register';
        } else {
            this.state='request';
        }
    }

    onPasswordRequested() {
        let email=this.request.emailFormControl.value;
        this.state='done-reset';
        setTimeout(()=>this.registerDone.email=email);
    }
    updateButtons(){
        /*
        <a *ngIf="state=='register'" class="waves-effect waves-light btn" [class.disabled]="!registerForm || !registerForm.canRegister()" tabindex="0" (keyup.enter)="registerForm.register()" (click)="registerForm.register()">{{'REGISTER.BUTTON' | translate }}</a>
        <a *ngIf="state=='request'" [class.disabled]="!request || !request.checkMail()" tabindex="0" (keyup.enter)="requestDone(request.email)" (click)="requestDone(request.email)" class="waves-effect waves-light btn">{{'REGISTER.REQUEST.BUTTON' | translate }}</a>
        <a *ngIf="state=='reset-password'" [class.disabled]="!resetPassword || !resetPassword.buttonCheck()" tabindex="0" (keyup.enter)="newPassword()" (click)="newPassword()" class="waves-effect waves-light btn">{{'REGISTER.RESET.BUTTON' | translate }}</a>
        <a *ngIf="state=='done' || state=='done-reset' && registerDone" class="waves-effect waves-light btn" [class.disabled]="registerDone && !registerDone.keyInput.trim()" tabindex="0" (keyup.enter)="registerDone.activate(registerDone.keyInput)" (click)="registerDone.activate(registerDone.keyInput)">{{(status=='done' ? 'REGISTER.DONE.ACTIVATE' : 'NEXT') | translate}}</a>
         */
        let btn:DialogButton;
        if(this.state=='register'){
            btn=new DialogButton('REGISTER.BUTTON',DialogButton.TYPE_PRIMARY,()=>this.registerForm.register());
            btn.disabled=!this.registerForm || !this.registerForm.canRegister();
        }
        if(this.state=='request'){
            btn=new DialogButton('REGISTER.REQUEST.BUTTON',DialogButton.TYPE_PRIMARY,()=>this.requestDone(this.request.emailFormControl.value));
            btn.disabled=!this.request || !this.request.emailFormControl.valid;
        }
        if(this.state=='reset-password'){
            btn=new DialogButton('REGISTER.RESET.BUTTON',DialogButton.TYPE_PRIMARY,()=>this.newPassword());
            btn.disabled=!this.resetPassword || !this.resetPassword.buttonCheck()
        }
        if((this.state=='done' || this.state=='done-reset') && this.registerDone){
            btn=new DialogButton(this.state=='done' ? 'REGISTER.DONE.ACTIVATE' : 'NEXT',DialogButton.TYPE_PRIMARY,()=>this.registerDone.activate(this.registerDone.keyInput));
            btn.disabled=!this.registerDone || !this.registerDone.keyInput.trim();
        }
        if(btn)
            this.buttons=[btn];
        else
            this.buttons=null;

        return this.buttons;
    }
}
