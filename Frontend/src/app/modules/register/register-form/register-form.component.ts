import {Component, EventEmitter, Output} from '@angular/core';
import {Toast} from '../../../core-ui-module/toast';
import {Router, UrlSerializer} from '@angular/router';
import {RegisterInformation} from '../../../core-module/core.module';
import { TranslationsService } from '../../../translations/translations.service';
import {RestConnectorService} from '../../../core-module/core.module';
import {ConfigurationService} from '../../../core-module/core.module';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {PlatformLocation} from '@angular/common';
import {RestRegisterService} from '../../../core-module/core.module';
import {FormControl, ValidationErrors, Validators} from '@angular/forms';

@Component({
  selector: 'es-register-form',
  templateUrl: 'register-form.component.html',
  styleUrls: ['register-form.component.scss']
})
export class RegisterFormComponent{
    @Output() onRegisterDone=new EventEmitter();
    public info : RegisterInformation = {
        firstName: '',
        lastName: '',
        email: '',
        organization: '',
        password: '',
        allowNotifications: false
    };
    emailFormControl = new FormControl('', [
        Validators.required,
        Validators.email,
    ]);
    public news = true;
    public agree = false;
    public privacyUrl: string;
    requiredFields:string[] = [];

    public register(){
        this.info.email=this.emailFormControl.value;
        this.toast.showProgressDialog();
        this.registerService.register(this.info).subscribe(()=>{
            this.toast.closeModalDialog();
            this.onRegisterDone.emit();
            //this.toast.toast("REGISTER.TOAST");
        },(error)=>{
            if(UIHelper.errorContains(error,"DuplicateAuthorityException")){
                this.emailFormControl.setErrors({'incorrect': true});
                this.toast.error(null,"REGISTER.TOAST_DUPLICATE");
            }else {
                this.toast.error(error);
            }
            this.toast.closeModalDialog();
        });
    }

  public openPrivacy(){
      window.open(this.privacyUrl);
  }

    public canRegister(){
        return (!this.isRequired('firstName') || this.info.firstName.trim())
            && (!this.isRequired('lastName') || this.info.lastName.trim())
            && (!this.isRequired('organization') || this.info.organization.trim())
            && this.emailFormControl.valid && this.info.password && UIHelper.getPasswordStrengthString(this.info.password) != 'weak'
            && this.agree;
    }

  constructor(private connector : RestConnectorService,
              private toast:Toast,
              private platformLocation: PlatformLocation,
              private urlSerializer:UrlSerializer,
              private router:Router,
              private registerService:RestRegisterService,
              private translations: TranslationsService,
              private configService:ConfigurationService,
            ){
    this.translations.waitForInit().subscribe(()=> {
        this.privacyUrl = this.configService.instant("privacyInformationUrl");
        this.requiredFields = this.configService.instant('register.requiredFields',['firstName']);
    });
  }

  isRequired(field: string) {
      return this.requiredFields.indexOf(field)!=-1;
  }
}
