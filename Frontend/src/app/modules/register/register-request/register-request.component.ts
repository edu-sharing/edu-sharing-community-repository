import {Component, EventEmitter, Output} from '@angular/core';
import {UIHelper} from "../../../common/ui/ui-helper";
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from "@angular/router";
import {RestConnectorService} from "../../../core-module/core.module";
import {Toast} from "../../../common/ui/toast";
import {PlatformLocation} from "@angular/common";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../../core-module/core.module";
import {Title} from "@angular/platform-browser";
import {SessionStorageService} from "../../../core-module/core.module";
import {CordovaService} from "../../../common/services/cordova.service";
import {UIConstants} from "../../../core-module/ui/ui-constants";
import {RestRegisterService} from "../../../core-module/core.module";

@Component({
  selector: 'app-register-request',
  templateUrl: 'register-request.component.html',
  styleUrls: ['register-request.component.scss']
})
export class RegisterRequestComponent {
    @Output() onLoading=new EventEmitter();
    @Output() onDone=new EventEmitter();
    email = "";

    public checkMail() {
        return UIHelper.isEmail(this.email);
    }

    constructor(private connector: RestConnectorService,
                private toast: Toast,
                private router: Router,
                private register: RestRegisterService
    ) {
    }

    submit() {
        if(!this.checkMail())
            return;
        this.onLoading.emit(true);
        this.register.recoverPassword(this.email).subscribe(()=>{
            this.onLoading.emit(false);
            this.toast.toast("REGISTER.TOAST");
            this.onDone.emit();
        },(error)=>{
            this.onLoading.emit(false);
            if(UIHelper.errorContains(error,"Invalid mail address")){
                this.toast.error(null,"REGISTER.TOAST_INVALID_MAIL");
            }
            else {
                this.toast.error(error);
            }
        });

        // this.toast.error(null, "");
    }
}

