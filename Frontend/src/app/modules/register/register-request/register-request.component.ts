import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { Router, Route, Params, ActivatedRoute, UrlSerializer } from '@angular/router';
import { RestConnectorService } from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { PlatformLocation } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '../../../core-module/core.module';
import { Title } from '@angular/platform-browser';
import { SessionStorageService } from '../../../core-module/core.module';
import { CordovaService } from '../../../common/services/cordova.service';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { RestRegisterService } from '../../../core-module/core.module';
import { FormControl, Validators } from '@angular/forms';
import { ReplaySubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'es-register-request',
    templateUrl: 'register-request.component.html',
    styleUrls: ['register-request.component.scss'],
})
export class RegisterRequestComponent implements OnDestroy {
    @Output() onDone = new EventEmitter();
    @Output() onStateChanged = new EventEmitter<void>();
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    emailFormControl = new FormControl('', [
        Validators.required,
        // Validators.email, // also local accounts are allowed for restore
    ]);
    constructor(
        private connector: RestConnectorService,
        private toast: Toast,
        private router: Router,
        private register: RestRegisterService,
    ) {
        this.emailFormControl.statusChanges
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.onStateChanged.emit());
    }
    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
    submit() {
        if (!this.emailFormControl.valid) return;
        this.toast.showProgressDialog();
        this.register.recoverPassword(this.emailFormControl.value).subscribe(
            () => {
                this.toast.closeModalDialog();
                this.toast.toast('REGISTER.TOAST');
                this.onDone.emit();
            },
            (error) => {
                this.toast.closeModalDialog();
                console.log(error);
                if (error?.error?.error?.includes('DAOInvalidKeyException')) {
                    this.toast.error(null, 'REGISTER.TOAST_INVALID_MAIL');
                } else {
                    this.toast.error(error);
                }
            },
        );

        // this.toast.error(null, "");
    }
}
