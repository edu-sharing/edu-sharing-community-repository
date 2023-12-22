import { Component, Input, EventEmitter, Output } from '@angular/core';
import { Toast } from '../../../core-ui-module/toast';
import { Router, Params } from '@angular/router';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { UIConstants } from '../../../core-module/ui/ui-constants';
import { RegisterService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-register-reset-password',
    templateUrl: 'register-reset-password.component.html',
    styleUrls: ['register-reset-password.component.scss'],
})
export class RegisterResetPasswordComponent {
    @Output() onStateChanged = new EventEmitter<void>();
    public new_password = '';
    @Input() params: Params;
    public buttonCheck() {
        if (
            UIHelper.getPasswordStrengthString(this.new_password) != 'weak' &&
            this.new_password.trim()
        ) {
            return true;
        } else {
            return false;
        }
    }
    public newPassword() {
        this.toast.showProgressDialog();
        this.register.resetPassword(this.params.key, this.new_password).subscribe(
            () => {
                this.toast.closeModalDialog();
                this.toast.toast('REGISTER.RESET.TOAST');
                this.router.navigate([UIConstants.ROUTER_PREFIX, 'login']);
            },
            (error) => {
                console.log('error', error);
                this.toast.closeModalDialog();
                if (error?.error?.error?.includes('DAOInvalidKeyException')) {
                    this.toast.error(null, 'REGISTER.TOAST_INVALID_RESET_KEY');
                    this.router.navigate([UIConstants.ROUTER_PREFIX, 'register', 'request']);
                } else {
                    this.toast.error(error);
                }
            },
        );
    }
    constructor(private toast: Toast, private register: RegisterService, private router: Router) {}
}
