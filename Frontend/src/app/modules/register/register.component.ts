import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from "../../common/ui/toast";
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from "@angular/router";
import {OAuthResult, LoginResult, AccessScope} from "../../common/rest/data-object";
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {RestConstants} from "../../common/rest/rest-constants";
import {ConfigurationService} from "../../common/services/configuration.service";
import {FrameEventsService} from "../../common/services/frame-events.service";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {Helper} from "../../common/helper";
import {RestHelper} from "../../common/rest/rest-helper";
import {PlatformLocation} from "@angular/common";

import {CordovaService} from "../../common/services/cordova.service";
import {InputPasswordComponent} from "../../common/ui/input-password/input-password.component";

@Component({
  selector: 'app-register',
  templateUrl: 'register.component.html',
  styleUrls: ['register.component.scss']
})
export class RegisterComponent{
  public isLoading=true;
  public firstName="";
  public lastName="";
  public mail="";
  public password="";
  public org="";
  public news = true;
  public agree = false;

  public checkConditions(){
    //  TODO: @Simon;
  }

  public checkMail(){

      const EMAIL_REGEXP = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

      if (this.mail && !EMAIL_REGEXP.test(this.mail)) {
          return false;
      } else {
          return true;
      }
  }
  public checkPasswort(){
    //  TODO: @Simon;
     /* Das Password muss mindestens 5 Zeichen lang sein
      Das Passwort muss Großbuchstaben, Kleinbuchstaben und Zahlen beinhalten
      Wenn es nicht der Fall ist dann Rot markieren und den Hinweis anzeigen lassen.   */

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
  private openImprint(){
      //TODO: @Simon
      // Link zur Impressum
      // window.document.location.href=this.config.imprintUrl;
  }


  public canRegister(){
      return this.firstName.trim() && this.mail.trim() && this.password
          && this.agree;
  }

  private register(){
      //  TODO: @Simon;
  }

  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private platformLocation: PlatformLocation,
              private urlSerializer:UrlSerializer,
              private router:Router,
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
