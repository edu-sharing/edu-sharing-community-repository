import { Injectable } from '@angular/core';
import { WorkspaceManagementDialogsComponent } from '../../modules/management-dialogs/management-dialogs.component';
import { MainNavComponent } from '../ui/main-nav/main-nav.component';
import {CookieInfoComponent} from '../ui/cookie-info/cookie-info.component';

@Injectable()
export class MainNavService {
    private mainnav: MainNavComponent;
    private managementDialogs: WorkspaceManagementDialogsComponent;
    private cookieInfo: CookieInfoComponent;

    getDialogs() {
        return this.managementDialogs;
    }
    getCookieInfo() {
        return this.cookieInfo;
    }
    registerDialogs(managementDialogs: WorkspaceManagementDialogsComponent) {
        this.managementDialogs = managementDialogs;
    }
    registerCookieInfo(cookieInfo: CookieInfoComponent) {
        this.cookieInfo = cookieInfo;
    }

    getMainNav() {
        return this.mainnav;
    }

    registerMainNav(maiNnav: MainNavComponent) {
        this.mainnav = maiNnav;
    }
}
