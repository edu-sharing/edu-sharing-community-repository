import { Injectable } from '@angular/core';
import { WorkspaceManagementDialogsComponent } from '../../modules/management-dialogs/management-dialogs.component';
import { MainNavComponent } from '../ui/main-nav/main-nav.component';
import {CookieInfoComponent} from '../ui/cookie-info/cookie-info.component';
import {ActivatedRoute, Params, Router} from "@angular/router";

@Injectable()
export class MainNavService {
    private mainnav: MainNavComponent;
    private managementDialogs: WorkspaceManagementDialogsComponent;
    private cookieInfo: CookieInfoComponent;

    constructor(private router: Router,
                private route: ActivatedRoute
    ) {
   }
    getDialogs() {
        return this.managementDialogs;
    }
    getCookieInfo() {
        return this.cookieInfo;
    }
    registerDialogs(managementDialogs: WorkspaceManagementDialogsComponent) {
        this.managementDialogs = managementDialogs;
        this.subscribeChanges();
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

    private subscribeChanges() {
        this.managementDialogs.signupGroupChange.subscribe((value: boolean) => {
            this.router.navigate(['./'], {
                relativeTo: this.route,
                queryParamsHandling: 'merge',
                queryParams: {
                    signupGroup : value || null
                }
            })
        });
        this.route.queryParams.subscribe((params: Params) => {
            this.managementDialogs.signupGroup = params.signupGroup;
        });
    }
}
