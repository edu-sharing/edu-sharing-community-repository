import {Component, EventEmitter, Output} from '@angular/core';
import {Toast} from '../../../common/ui/toast';
import {Router, ActivatedRoute, UrlSerializer} from '@angular/router';
import {RegisterInformation} from '../../../common/rest/data-object';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../common/translation';
import {RestConnectorService} from '../../../common/rest/services/rest-connector.service';
import {ConfigurationService} from '../../../common/services/configuration.service';
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../../common/ui/ui-helper';
import {SessionStorageService} from '../../../common/services/session-storage.service';
import {PlatformLocation} from '@angular/common';
import {RestRegisterService} from '../../../common/rest/services/rest-register.service';

@Component({
  selector: 'app-register-form',
  templateUrl: 'register-form.component.html',
  styleUrls: ['register-form.component.scss']
})
export class RegisterFormComponent{
    @Output() onRegisterDone=new EventEmitter();
    @Output() onLoading=new EventEmitter();
    public info : RegisterInformation = {
        firstName: '',
        lastName: '',
        email: '',
        organization: '',
        password: '',
        allowNotifications: false
    };
    public news = true;
    public agree = false;
    public privacyUrl: string;
    public mailValid: boolean;
    requiredFields:string[];

    public checkMail(mail:string) {
        this.mailValid = UIHelper.isEmail(mail);
    }
    public register(){
        this.onLoading.emit(true);
        this.registerService.register(this.info).subscribe(()=>{
            this.onRegisterDone.emit();
            this.onLoading.emit(false);
            this.toast.toast("REGISTER.TOAST");
        },(error)=>{
            if(UIHelper.errorContains(error,"DuplicateAuthorityException")){
                this.mailValid = false;
                this.toast.error(null,"REGISTER.TOAST_DUPLICATE");
            }else {
                this.toast.error(error);
            }
            this.onLoading.emit(false);
        });
    }

  public setAccept(value:boolean){
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
        return (!this.isRequired('firstName') || this.info.firstName.trim())
            && (!this.isRequired('lastName') || this.info.lastName.trim())
            && (!this.isRequired('organization') || this.info.organization.trim())
            && this.mailValid && this.info.password && UIHelper.getPasswordStrengthString(this.info.password) != 'weak'
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
        this.requiredFields = this.configService.instant('register.requiredFields',['firstName']);
    });
  }

  isRequired(field: string) {
      return this.requiredFields.indexOf(field)!=-1;
  }
}
