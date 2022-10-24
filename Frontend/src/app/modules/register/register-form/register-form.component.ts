import {Component, EventEmitter, OnDestroy, Output} from '@angular/core';
import {Toast} from '../../../core-ui-module/toast';
import {Router, ActivatedRoute, UrlSerializer} from '@angular/router';
import {RegisterInformation} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../core-ui-module/translation';
import {RestConnectorService} from '../../../core-module/core.module';
import {ConfigurationService} from '../../../core-module/core.module';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {SessionStorageService} from '../../../core-module/core.module';
import {PlatformLocation} from '@angular/common';
import {RestRegisterService} from '../../../core-module/core.module';
import {FormBuilder, FormControl, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import { ReplaySubject } from 'rxjs';
import {takeUntil} from 'rxjs/operators';

@Component({
    selector: 'app-register-form',
    templateUrl: 'register-form.component.html',
    styleUrls: ['register-form.component.scss']
})
export class RegisterFormComponent implements OnDestroy {
    @Output() onRegisterDone=new EventEmitter();
    @Output() onStateChanged=new EventEmitter<void>();
    password: string;
    registerForm: FormGroup;
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    info: RegisterInformation;
    public news = true;
    public agree = false;
    public privacyUrl: string;
    requiredFields:string[] = [];

    public register() {
        this.toast.showProgressDialog();
        this.info = this.registerForm.getRawValue();
        delete (this.info as any).agree;
        this.info.password = this.password;
        this.registerService.register(this.info).subscribe(()=> {
            this.toast.closeModalDialog();
            this.onRegisterDone.emit();
            // this.toast.toast("REGISTER.TOAST");
        },(error)=> {
            if(UIHelper.errorContains(error,'DuplicateAuthorityException')) {
                this.registerForm.setErrors({incorrect: true});
                this.toast.error(null,'REGISTER.TOAST_DUPLICATE');
            } else {
                this.toast.error(error);
            }
            this.toast.closeModalDialog();
        });
    }

    public openPrivacy() {
        window.open(this.privacyUrl);
    }

    public canRegister() {
        return this.registerForm.valid &&
            this.password &&
            UIHelper.getPasswordStrengthString(this.password) !== 'weak';
    }

    constructor(private connector : RestConnectorService,
                private toast:Toast,
                private platformLocation: PlatformLocation,
                private formBuilder: FormBuilder,
                private router:Router,
                private registerService:RestRegisterService,
                private translate:TranslateService,
                private configService:ConfigurationService,
                private storage : SessionStorageService,
                private route : ActivatedRoute,
    ) {
        Translation.initialize(translate,this.configService,this.storage,this.route).subscribe(()=> {
            this.privacyUrl = this.configService.instant('privacyInformationUrl');
            this.requiredFields = this.configService.instant('register.requiredFields',['firstName']);

            this.registerForm = this.formBuilder.group({
                firstName: new FormControl(
                    '',
                    this.requiredFields.includes('firstName') ? [Validators.required] : null
                ),
                lastName: new FormControl(
                    '',
                    this.requiredFields.includes('lastName') ? [Validators.required] : null
                ),
                organization: new FormControl(
                    '',
                    this.requiredFields.includes('organization') ? [Validators.required] : null
                ),
                email: new FormControl('', [
                    Validators.required,
                    Validators.email,
                ]),
                agree: new FormControl(false, [
                    Validators.requiredTrue,
                ]),
                allowNotifications: new FormControl(false)
            });
            this.registerForm.statusChanges.pipe(
                takeUntil(this.destroyed$)
            ).subscribe(() => this.onStateChanged.emit());
        });
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
