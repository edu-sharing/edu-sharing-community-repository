import {Component, Input, EventEmitter, Output, ElementRef, ViewChild, OnInit} from '@angular/core';
import {Toast} from "../../../common/ui/toast";
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from "@angular/router";
import {OAuthResult, LoginResult, AccessScope} from "../../../common/rest/data-object";
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../../common/translation";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {FrameEventsService} from "../../../common/services/frame-events.service";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../../common/ui/ui-helper";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {UIConstants} from "../../../common/ui/ui-constants";
import {Helper} from "../../../common/helper";
import {RestHelper} from "../../../common/rest/rest-helper";
import {PlatformLocation} from "@angular/common";

import {CordovaService} from "../../../common/services/cordova.service";
import {InputPasswordComponent} from "../../../common/ui/input-password/input-password.component";
import {RestRegisterService} from "../../../common/rest/services/rest-register.service";

@Component({
  selector: 'app-register-reset-password',
  templateUrl: 'register-reset-password.component.html',
  styleUrls: ['register-reset-password.component.scss']
})
export class RegisterResetPasswordComponent{
    @Output() onLoading=new EventEmitter();
    public new_password ="";
    public key: string;

    public buttonCheck(){
        if (UIHelper.getPasswordStrengthString(this.new_password) != "weak" && this.new_password.trim()){
            return true;
        } else {
            return false;
        }
    }
    public newPassword(){
        this.onLoading.emit(true);
        this.register.resetPassword(this.key,this.new_password).subscribe(()=>{
            this.onLoading.emit(false);
            this.toast.toast("REGISTER.RESET.TOAST");
            this.router.navigate([UIConstants.ROUTER_PREFIX,"login"]);
        },(error)=>{
            this.onLoading.emit(false);
            if(UIHelper.errorContains(error,"InvalidKeyException")) {
                this.toast.error(null,"REGISTER.TOAST_INVALID_RESET_KEY");
                this.router.navigate([UIConstants.ROUTER_PREFIX,"register","request"]);
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
