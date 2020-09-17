import { Injectable } from '@angular/core';
import { WorkspaceManagementDialogsComponent } from '../../modules/management-dialogs/management-dialogs.component';
import { MainNavComponent } from '../ui/main-nav/main-nav.component';

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
