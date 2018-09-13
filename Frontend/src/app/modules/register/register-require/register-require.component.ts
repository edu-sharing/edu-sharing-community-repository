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

@Component({
  selector: 'app-register-require',
  templateUrl: 'register-require.component.html',
  styleUrls: ['register-require.component.scss']
})
export class RegisterRequireComponent{

}
