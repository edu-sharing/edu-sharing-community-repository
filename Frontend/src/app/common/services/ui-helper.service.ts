import { Injectable } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {AsyncSubject, Observable, Observer} from 'rxjs';
import { BridgeService } from '../../core-bridge-module/bridge.service';
import {
    ConfigurationService,
    FrameEventsService,
    LoginResult,
    RestConnectorService,
    RestConstants,
    UIConstants,
    UIService,
    AccessScope,
    RestOrganizationService,
    OrganizationOrganizations,
    RestMediacenterService,
} from '../../core-module/core.module';
import { OPEN_URL_MODE } from '../../core-module/ui/ui-constants';
import { UIHelper } from '../../core-ui-module/ui-helper';
import {HttpClient} from "@angular/common/http";
import {Toast} from "../../core-ui-module/toast";

@Injectable()
export class UiHelperService {
    constructor(
        private bridge: BridgeService,
        private toast: Toast,
        private http: HttpClient,
        private connector: RestConnectorService,
    ) {}

}
