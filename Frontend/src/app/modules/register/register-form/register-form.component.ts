import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { Toast } from '../../../core-ui-module/toast';
import { Router } from '@angular/router';
import {
    ConfigurationService,
    RegisterInformation,
    RestConnectorService,
    RestRegisterService,
} from '../../../core-module/core.module';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { PlatformLocation } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ReplaySubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'es-register-form',
    templateUrl: 'register-form.component.html',
    styleUrls: ['register-form.component.scss'],
})
export class RegisterFormComponent implements OnDestroy {
    @Output() onRegisterDone = new EventEmitter();
    @Output() onStateChanged = new EventEmitter<void>();
    password: string;
    registerForm: FormGroup;
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    info: RegisterInformation;
    public news = true;
    public agree = false;
    public privacyUrl: string;
    requiredFields: string[] = [];

    public register() {
        this.toast.showProgressDialog();
        this.info = this.registerForm.getRawValue();
        delete (this.info as any).agree;
        this.info.password = this.password;
        this.registerService.register(this.info).subscribe(
            () => {
                this.toast.closeModalDialog();
                this.onRegisterDone.emit();
                // this.toast.toast("REGISTER.TOAST");
            },
            (error) => {
                if (UIHelper.errorContains(error, 'DuplicateAuthorityException')) {
                    this.registerForm.setErrors({ incorrect: true });
                    this.toast.error(null, 'REGISTER.TOAST_DUPLICATE');
                } else {
                    this.toast.error(error);
                }
                this.toast.closeModalDialog();
            },
        );
    }

    public openPrivacy() {
        window.open(this.privacyUrl);
    }

    public canRegister() {
        return (
            this.registerForm.valid &&
            this.password &&
            UIHelper.getPasswordStrengthString(this.password) !== 'weak'
        );
    }

    constructor(
        private connector: RestConnectorService,
        private toast: Toast,
        private platformLocation: PlatformLocation,
        private formBuilder: FormBuilder,
        private router: Router,
        private registerService: RestRegisterService,
        private translations: TranslationsService,
        private configService: ConfigurationService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.privacyUrl = this.configService.instant('privacyInformationUrl');
            this.requiredFields = this.configService.instant('register.requiredFields', [
                'firstName',
            ]);

            this.registerForm = this.formBuilder.group({
                firstName: new FormControl(
                    '',
                    this.requiredFields.includes('firstName') ? [Validators.required] : null,
                ),
                lastName: new FormControl(
                    '',
                    this.requiredFields.includes('lastName') ? [Validators.required] : null,
                ),
                organization: new FormControl(
                    '',
                    this.requiredFields.includes('organization') ? [Validators.required] : null,
                ),
                email: new FormControl('', [Validators.required, Validators.email]),
                agree: new FormControl(false, [Validators.requiredTrue]),
                allowNotifications: new FormControl(false),
            });
            this.registerForm.statusChanges
                .pipe(takeUntil(this.destroyed$))
                .subscribe(() => this.onStateChanged.emit());
        });
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
