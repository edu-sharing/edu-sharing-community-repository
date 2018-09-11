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
import {RegisterFormComponent} from "./register-form/register-form.component";
import {RegisterDoneComponent} from "./register-done/register-done.component";

@Component({
  selector: 'app-register',
  templateUrl: 'register.component.html',
  styleUrls: ['register.component.scss']
})
export class RegisterComponent{
  @ViewChild('registerForm') registerForm : RegisterFormComponent;
  @ViewChild('registerDone') registerDone : RegisterDoneComponent;
  public isLoading=true;

  state = 'register';

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
