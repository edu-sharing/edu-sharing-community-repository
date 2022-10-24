import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from '../../../core-ui-module/toast';
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from '@angular/router';
import {OAuthResult, LoginResult, AccessScope} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../core-ui-module/translation';
import {RestConnectorService} from '../../../core-module/core.module';
import {RestConstants} from '../../../core-module/core.module';
import {ConfigurationService} from '../../../core-module/core.module';
import {FrameEventsService} from '../../../core-module/core.module';
import {Title} from '@angular/platform-browser';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {SessionStorageService} from '../../../core-module/core.module';
import {UIConstants} from '../../../core-module/ui/ui-constants';
import {Helper} from '../../../core-module/rest/helper';
import {RestHelper} from '../../../core-module/core.module';
import {PlatformLocation} from '@angular/common';

import {CordovaService} from '../../../common/services/cordova.service';
import {InputPasswordComponent} from '../../../core-ui-module/components/input-password/input-password.component';
import {RestRegisterService} from '../../../core-module/core.module';

@Component({
  selector: 'app-register-reset-password',
  templateUrl: 'register-reset-password.component.html',
  styleUrls: ['register-reset-password.component.scss']
})
export class RegisterResetPasswordComponent {
    @Output() onStateChanged=new EventEmitter<void>();
    public new_password ='';
    public key: string;

    public buttonCheck() {
        if (UIHelper.getPasswordStrengthString(this.new_password) != 'weak' && this.new_password.trim()) {
            return true;
        } else {
            return false;
        }
    }
    public newPassword() {
        this.toast.showProgressDialog();
        console.log('reset pass');
        this.register.resetPassword(this.key,this.new_password).subscribe(()=> {
            this.toast.closeModalDialog();
            this.toast.toast('REGISTER.RESET.TOAST');
            this.router.navigate([UIConstants.ROUTER_PREFIX,'login']);
        },(error)=> {
            this.toast.closeModalDialog();
            if(error?.error?.error?.includes('DAOInvalidKeyException')) {
                this.toast.error(null,'REGISTER.TOAST_INVALID_RESET_KEY');
                this.router.navigate([UIConstants.ROUTER_PREFIX,'register','request']);
            } else {
                this.toast.error(error);
            }
        });
    }
    constructor(private connector: RestConnectorService,
                private toast: Toast,
                private register: RestRegisterService,
                private router: Router
    ) {
    }
}
