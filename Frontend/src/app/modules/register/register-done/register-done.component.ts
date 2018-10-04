import {Component, Input} from '@angular/core';
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

@Component({
  selector: 'app-register-done',
  templateUrl: 'register-done.component.html',
  styleUrls: ['register-done.component.scss']
})
export class RegisterDoneComponent{
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
        //TODO: @Simon
        // Zum Bearbeitung vom E-Mail
        if (this.inputState == 'done'){
            this.router.navigate([UIConstants.ROUTER_PREFIX + "register"]);
        } else {
            this.router.navigate([UIConstants.ROUTER_PREFIX + "register","request"]);
        }
    }
    public sendMail(){
        //TODO: @Simon
        // E-Mail erneut versenden
        console.log(this.email);
        this.toast.toast("REGISTER.TOAST");

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
            this.toast.error(error);
        });
    }
}