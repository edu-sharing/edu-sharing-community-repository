import {Component, EventEmitter, Input, Output} from '@angular/core';
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {PlatformLocation} from "@angular/common";
import {ActivatedRoute, Router, UrlSerializer} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {UIConstants} from "../../../common/ui/ui-constants";
import {RestRegisterService} from '../../../common/rest/services/rest-register.service';
import {UIHelper} from "../../../common/ui/ui-helper";

@Component({
  selector: 'app-register-done',
  templateUrl: 'register-done.component.html',
  styleUrls: ['register-done.component.scss']
})
export class RegisterDoneComponent{
    @Output() onModify = new EventEmitter();
    @Input() inputState:string;
    loading=false;
    email = '';
    keyInput = '';
    private _keyUrl = '';
    get keyUrl(){
        return this._keyUrl;
    }
    set keyUrl(keyUrl:string){
        this._keyUrl=keyUrl;
        this.loading=true;
        this.activate(keyUrl);
    }

    public editMail() {
        this.onModify.emit();
    }
    public sendMail(){
        this.register.resendMail(this.email).subscribe(()=>{
            this.toast.toast("REGISTER.TOAST");
        });
    }
    constructor(private connector: RestConnectorService,
                private toast: Toast,
                private register: RestRegisterService,
                private router: Router
    ) {}
    private activate(keyUrl: string) {
        this.register.activate(keyUrl).subscribe(()=>{
            this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
        },(error:any)=>{
            if(UIHelper.errorContains(error,"InvalidKeyException")){
                this.toast.error(null,"REGISTER.TOAST_INVALID_KEY");
            }
            else {
                this.toast.error(error);
            }
            this.loading=false;
        });
    }
}