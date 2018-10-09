import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from '../../../common/ui/toast';
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from '@angular/router';
import {OAuthResult, LoginResult, AccessScope, RegisterInformation} from '../../../common/rest/data-object';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../common/translation';
import {RestConnectorService} from '../../../common/rest/services/rest-connector.service';
import {RestConstants} from '../../../common/rest/rest-constants';
import {ConfigurationService} from '../../../common/services/configuration.service';
import {FrameEventsService} from '../../../common/services/frame-events.service';
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../../common/ui/ui-helper';
import {SessionStorageService} from '../../../common/services/session-storage.service';
import {PlatformLocation} from '@angular/common';

import {RestRegisterService} from '../../../common/rest/services/rest-register.service';
import {UIConstants} from "../../../common/ui/ui-constants";

@Component({
  selector: 'app-register-form',
  templateUrl: 'register-form.component.html',
  styleUrls: ['register-form.component.scss']
})
export class RegisterFormComponent{
    @Output() onRegisterDone=new EventEmitter();
    public isLoading=true;
    public info : RegisterInformation = {
        firstName: '',
        lastName: '',
        email: '',
        organization: '',
        password: ''
    };
    public password_strength='';
    public news = true;
    public agree = false;
    public privacyUrl: string;
    public mailValid: boolean;

    public checkMail(){
        this.mailValid = UIHelper.isEmail(this.info.email);
    }
  public checkPassword(){
      this.password_strength = UIHelper.getPasswordStrengthString(this.info.password);
  }

    public register(){
        //  TODO: @Simon;
        this.isLoading=true;
        this.registerService.register(this.info).subscribe(()=>{
            this.onRegisterDone.emit();
            this.isLoading=false;
            this.toast.toast("REGISTER.TOAST");
        },(error)=>{
            if(error._body.indexOf("DuplicateAuthorityException")!=-1){
                this.mailValid = false;
                this.toast.error(null,"REGISTER.TOAST_DUPLICATE");
            }else {
                this.toast.error(error);
            }
            this.isLoading=false;
        });
    }

  public setNews(value:boolean){
      //TODO: @Simon
      if(value){

      } else{

      }
  }
  public setAccept(value:boolean){
      //TODO: @Simon
      if(value){
          this.agree = true;
      } else{
          this.agree = false;
      }
  }
  public openPrivacy(){
      window.open(this.privacyUrl);
  }

    public canRegister(){
        return this.info.firstName.trim() && this.mailValid && this.info.password && this.password_strength != 'weak'
            && this.agree;
    }

  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private platformLocation: PlatformLocation,
              private urlSerializer:UrlSerializer,
              private router:Router,
              private registerService:RestRegisterService,
              private translate:TranslateService,
              private configService:ConfigurationService,
              private title:Title,
              private storage : SessionStorageService,
              private route : ActivatedRoute,
            ){
    Translation.initialize(translate,this.configService,this.storage,this.route).subscribe(()=> {
        UIHelper.setTitle('REGISTER.TITLE', title, translate, configService);
        this.privacyUrl = this.configService.instant("privacyInformationUrl");
    });
    this.isLoading=true;
  }
}
