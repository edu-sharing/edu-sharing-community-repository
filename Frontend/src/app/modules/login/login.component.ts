import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Toast} from '../../common/ui/toast';
import {ActivatedRoute, Params, Router, UrlSerializer} from '@angular/router';
import {AccessScope, LoginResult} from '../../common/rest/data-object';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../common/translation';
import {RestConnectorService} from '../../common/rest/services/rest-connector.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {ConfigurationService} from '../../common/services/configuration.service';
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../common/ui/ui-helper';
import {SessionStorageService} from '../../common/services/session-storage.service';
import {OPEN_URL_MODE, UIConstants} from '../../common/ui/ui-constants';
import {Helper} from '../../common/helper';
import {RestHelper} from '../../common/rest/rest-helper';
import {PlatformLocation} from '@angular/common';

import {CordovaService} from '../../common/services/cordova.service';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../common/ui/ui-animation';
import {InputPasswordComponent} from '../../common/ui/input-password/input-password.component';
import {RouterHelper} from '../../common/router.helper';
import {HttpClient} from "@angular/common/http";
import {FormControl} from '@angular/forms';
import {map, startWith} from "rxjs/operators";
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {DialogButton} from '../../common/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'workspace-login',
  templateUrl: 'login.component.html',
  styleUrls: ['login.component.scss'],
    animations: [
        trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST)),
    ]
})
export class LoginComponent  implements OnInit{
  loginUrl: any;
  @ViewChild('mainNav') mainNavRef : MainNavComponent;
  @ViewChild('passwordInput') passwordInput : InputPasswordComponent;
  @ViewChild('usernameInput') usernameInput : ElementRef;
  @ViewChild('loginForm') loginForm : ElementRef;

  public isLoading=true;
  private disabled=false;
  private showUsername=true;
  private username='';
  private password='';
  private scope='';
  private next='';
  public mainnav=true;
  public showProviders=false;
  private caption='LOGIN.TITLE';
  private config: any={};
  private providers: any;
  providerControl = new FormControl();
  currentProvider:any;
  private buttons: DialogButton[];

  currentProviderDisplay(provider:any){
    return provider ? provider.name : null;
  }
  private filteredProviders: any;
  private checkConditions(){
    this.disabled=!this.username;// || !this.password;
      this.updateButtons();
  }
  private recoverPassword(){
      if(this.config.register.local){
          this.router.navigate([UIConstants.ROUTER_PREFIX+'register','request']);
      }
      else {
          window.location.href = this.config.register.recoverUrl;
      }
  }
  private register(){
      if(this.config.register.local){
          this.router.navigate([UIConstants.ROUTER_PREFIX+'register']);
      }
      else {
          window.location.href = this.config.register.registerUrl;
      }
  }
  openLoginUrl(){
      window.location.href=this.loginUrl;
  }
  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private platformLocation: PlatformLocation,
              private urlSerializer:UrlSerializer,
              private router:Router,
              private http:HttpClient,
              private translate:TranslateService,
              private configService:ConfigurationService,
              private title:Title,
              private storage : SessionStorageService,
              private route : ActivatedRoute,
              private cordova: CordovaService
            ){
    this.updateButtons();
    Translation.initialize(translate,this.configService,this.storage,this.route).subscribe(()=>{
      UIHelper.setTitle('LOGIN.TITLE',title,translate,configService);
      this.configService.getAll().subscribe((data:any)=>{
        this.config=data;
        if(!this.config.register) {
            // default register mode: allow local registration if not disabled
            this.config.register = {local: true};
        }
        this.updateButtons();
        this.username=this.configService.instant('defaultUsername','');
        this.password=this.configService.instant('defaultPassword','');
        this.route.queryParams.forEach((params: Params) => {
          if(params['username'])
              this.username=params['username'];

          this.connector.onAllRequestsReady().subscribe(()=>{
            this.isLoading=false;
            this.mainNavRef.finishPreloading();
              setTimeout(()=>{
                  if (this.username && this.passwordInput)
                      this.passwordInput.nativeInput.nativeElement.focus();
                  else if(this.usernameInput){
                      this.usernameInput.nativeElement.focus();
                  }
              },100);
          });
          this.scope=params['scope'];
          if(!this.scope)
            this.scope=null;
          this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
            if(data.currentScope){
              this.connector.logout().subscribe(()=>{}); // just to make sure there is no scope still set // NO: We need a valid session when login to scope!!!
              data.statusCode=null;
            }
            else if(data.currentScope==this.scope){
              if(data.statusCode==RestConstants.STATUS_CODE_OK && params['local']!='true'){
                this.goToNext();
              }
            }
            if(configService.instant('loginProvidersUrl')){
              this.showProviders=true;
              this.updateButtons();
              this.http.get(configService.instant('loginProvidersUrl')).subscribe((providers)=>{
                  this.processProviders(providers);
              });
            }
              this.loginUrl=configService.instant('loginUrl');
              const allowLocal=configService.instant('loginAllowLocal',false);
              if(params['local']!='true' && !allowLocal && this.loginUrl && data.statusCode!=RestConstants.STATUS_CODE_OK){
                this.openLoginUrl();
                return;
            }
          });
          this.showUsername=this.scope!=RestConstants.SAFE_SCOPE;
          this.next=params['next'];
          this.mainnav=params['mainnav'] != 'false';
          if(this.scope==RestConstants.SAFE_SCOPE){
            this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
              if(data.statusCode!=RestConstants.STATUS_CODE_OK){
                RestHelper.goToLogin(this.router,this.configService);
              }
              else{
                this.connector.hasAccessToScope(RestConstants.SAFE_SCOPE).subscribe((scope:AccessScope)=>{
                  if(scope.hasAccess){
                    this.username=data.authorityName;
                  }
                  else{
                    this.toast.error(null,'LOGIN.NO_ACCESS');
                    this.router.navigate([UIConstants.ROUTER_PREFIX+'workspace']);
                    //window.history.back();
                  }
                },(error:any)=>{
                  this.toast.error(error);
                });
              }
            },(error:any)=>RestHelper.goToLogin(this.router,this.configService));
          }


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

    this.filteredProviders = this.providerControl.valueChanges
        .pipe(
            startWith(''),
            map((value:string) => this.filterProviders(value))
        );
    console.log(this.filteredProviders);

  }
  private login(){
    
    this.isLoading=true;

      this.connector.login(this.username,this.password,this.scope).subscribe(
        (data:string) => {
          if(data==RestConstants.STATUS_CODE_OK) {
            this.goToNext();
          }
          else if(data==RestConstants.STATUS_CODE_PREVIOUS_SESSION_REQUIRED || data==RestConstants.STATUS_CODE_PREVIOUS_USER_WRONG){
            this.toast.error(null,'LOGIN.SAFE_PREVIOUS');
            this.password="";
            this.isLoading=false;
          }
          else{
            this.toast.error(null,'LOGIN.ERROR');
            this.password="";
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
      this.next=Helper.addGetParameter('fromLogin','true',this.next);
      RouterHelper.navigateToAbsoluteUrl(this.platformLocation,this.router,this.next);
      //window.location.assign(this.next);
    }
    else {
      UIHelper.goToDefaultLocation(this.router,this.configService);
    }
  }

    updateButtons(): any {
      this.buttons = [];
      if(this.showProviders){
        return;
      }
      if(this.config.register && (this.config.register.local || this.config.register.recoverUrl)) {
          this.buttons.push(new DialogButton('LOGIN.RECOVER_PASSWORD', DialogButton.TYPE_CANCEL, () => this.recoverPassword()));
      }
      let login=new DialogButton('LOGIN.LOGIN',DialogButton.TYPE_PRIMARY,()=>this.login());
      login.disabled=this.disabled;
      this.buttons.push(login);
    }
  private processProviders(providers: any) {
    let data:any={};
    for(let provider in providers.wayf_idps){
      let object=providers.wayf_idps[provider];
      object.url=provider;
      let type=object.type;
      if(!data[type]) {
        data[type] = {
          group: providers.wayf_categories[type],
          providers: []
        };
      }
      data[type].providers.push(object);
    }
    this.providers = [];
    for(let key in data){
      this.providers.push(data[key]);
    }
    console.log(this.providers);
  }

  private filterProviders(filter:any="") {
    console.log(filter);
    let filtered=[];
    // an object was detected, abort
    if(filter.name){
      return this.providers;
    }
    this.currentProvider=null;
    for(let p of Helper.deepCopy(this.providers)){
      p.providers=p.providers.filter((p:any) => p.name.toLowerCase().includes(filter.toLowerCase()));
      if(p.providers.length)
        filtered.push(p);
    }
    return filtered;
  }
  goToProvider(){
    console.log(this.currentProvider);
    if(!this.currentProvider){
      this.toast.error(null,'LOGIN.NO_PROVIDER_SELECTED');
    }
    let url=this.configService.instant('loginProviderTargetUrl');
    if(!url){
      this.toast.error(null,'No configuration for loginProviderTargetUrl found.');
      return;
    }
    let target=this.connector.getAbsoluteServerUrl()+this.configService.instant('loginUrl');
    url=url.
      replace(':target',encodeURIComponent(target)).
      replace(':entity',encodeURIComponent(this.currentProvider.url));
    //@TODO: Redirect to shibboleth provider
    UIHelper.openUrl(url,this.cordova,OPEN_URL_MODE.Current);
  }
}
