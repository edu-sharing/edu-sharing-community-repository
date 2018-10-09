import {Component} from '@angular/core';
import {UIHelper} from "../../../common/ui/ui-helper";
import {Router, Route, Params, ActivatedRoute, UrlSerializer} from "@angular/router";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {Toast} from "../../../common/ui/toast";
import {PlatformLocation} from "@angular/common";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {SessionStorageService} from "../../../common/services/session-storage.service";
import {CordovaService} from "../../../common/services/cordova.service";
import {UIConstants} from "../../../common/ui/ui-constants";

@Component({
  selector: 'app-register-request',
  templateUrl: 'register-request.component.html',
  styleUrls: ['register-request.component.scss']
})
export class RegisterRequestComponent {
    email = "";

    public checkMail() {
        return UIHelper.isEmail(this.email);
    }

    constructor(private connector: RestConnectorService,
                private toast: Toast,
                private router: Router
    ) {
    }
}

