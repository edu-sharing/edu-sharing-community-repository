import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { Toast } from '../../../core-ui-module/toast';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ReplaySubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { RegisterService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-register-request',
    templateUrl: 'register-request.component.html',
    styleUrls: ['register-request.component.scss'],
})
export class RegisterRequestComponent implements OnDestroy {
    @Output() onDone = new EventEmitter();
    @Output() onStateChanged = new EventEmitter<void>();
    private destroyed$: ReplaySubject<void> = new ReplaySubject(1);
    emailFormControl = new UntypedFormControl('', [
        Validators.required,
        // Validators.email, // also local accounts are allowed for restore
    ]);
    constructor(private toast: Toast, private register: RegisterService) {
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
        this.toast.showProgressSpinner();
        this.register.recoverPassword(this.emailFormControl.value).subscribe(
            () => {
                this.toast.closeProgressSpinner();
                this.toast.toast('REGISTER.TOAST');
                this.onDone.emit();
            },
            (error) => {
                this.toast.closeProgressSpinner();
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
