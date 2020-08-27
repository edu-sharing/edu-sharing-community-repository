import { Injectable } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AsyncSubject, Observable } from 'rxjs';
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
import {MainNavComponent} from '../ui/main-nav/main-nav.component';
import {WorkspaceManagementDialogsComponent} from '../../modules/management-dialogs/management-dialogs.component';

@Injectable()
export class MainNavService {
    private mainnav: MainNavComponent;
    private managementDialogs: WorkspaceManagementDialogsComponent;
    getDialogs() {
        return this.managementDialogs;
    }
    registerDialogs(managementDialogs: WorkspaceManagementDialogsComponent) {
        this.managementDialogs = managementDialogs;
    }
    getMainNav() {
        return this.mainnav;
    }
    registerMainNav(maiNnav: MainNavComponent) {
        this.mainnav = maiNnav;
    }
}
