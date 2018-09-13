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
import {UIConstants} from '../../../common/ui/ui-constants';
import {Helper} from '../../../common/helper';
import {RestHelper} from '../../../common/rest/rest-helper';
import {PlatformLocation} from '@angular/common';

import {CordovaService} from '../../../common/services/cordova.service';
import {InputPasswordComponent} from '../../../common/ui/input-password/input-password.component';
import {RestRegisterService} from '../../../common/rest/services/rest-register.service';

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
    /*
    public firstName="";
    public lastName="";
    public mail="";
    public password="";
    public org="";
    */

  public checkConditions(){
    //  TODO: @Simon;
  }

    public checkMail(){

      const EMAIL_REGEXP = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
      if (this.info.email && !EMAIL_REGEXP.test(this.info.email)) {
          return false;
      } else {
          return true;
      }
    }

    public register(){
        //  TODO: @Simon;
        this.isLoading=true;
        this.registerService.register(this.info).subscribe(()=>{
            this.onRegisterDone.emit();
            this.isLoading=false;
        },(error)=>{
            this.toast.error(error);
            this.isLoading=false;
        });
    }

    public getPwStrength(){
    let strengt: any;
    // These are weighting factors
    let flc = 1.0;  // lowercase factor
    let fuc = 1.0;  // uppercase factor
    let fnm = 1.3;  // number factor
    let fsc = 1.5;  // special char factor
    let spc_chars = '^`?()[]{/}+-=Â¦|~!@#$%&*_';

    let regex_sc = new RegExp('['+spc_chars+']', 'g');

    let lcase_count: any = this.info.password.match(/[a-z]/g);
    lcase_count = (lcase_count) ? lcase_count.length : 0;
    let ucase_count: any = this.info.password.match(/[A-Z]/g);
    ucase_count = (ucase_count) ? ucase_count.length : 0;
    let num_count: any = this.info.password.match(/[0-9]/g);
    num_count = (num_count) ? num_count.length : 0;
    let schar_count: any = this.info.password.match(regex_sc);
    schar_count = (schar_count) ? schar_count.length : 0;
    let avg: any = this.info.password.length / 4;

    strengt = ((lcase_count * flc + 1) * (ucase_count * fuc + 1) * (num_count * fnm + 1) * (schar_count * fsc + 1)) / (avg + 1);

    console.log('Strengt: '+strengt);
    return strengt;
    }
    public detectPW(){
      let pw_parts = this.info.password.split('');
      let i;
      let ords = new Array();
      for (i in pw_parts){
          ords[i] = pw_parts[i].charCodeAt(0);
      }
        let accum = 0;
        let lasti = ords.length-1;

        for (let i=0; i < lasti; ++i){
            accum += Math.abs(ords[i] - ords[i+1]);
        }
        console.log('detect: '+accum/lasti);
        return accum/lasti;
    }
  public checkPassword(){
    let min_length = 5;
    if (this.info.password.length >= min_length && this.detectPW() > 0){
        this.password_strength = 'accept';
        if ( this.detectPW() > 5 && this.getPwStrength() > 5){
            this.password_strength = 'medium';
            if (this.detectPW() > 10 && this.getPwStrength() > 10){
                this.password_strength = 'strong';
            }
        }
    } else {
        this.password_strength = 'weak';
    }
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
  public openImprint(){
      this.configService.get('config.imprintUrl').subscribe((url)=>window.open(url));
  }

    public canRegister(){
        return this.info.firstName.trim() && this.info.email.trim() && this.checkMail() && this.info.password && this.password_strength != 'weak'
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
    });
    this.isLoading=true;
  }
}
