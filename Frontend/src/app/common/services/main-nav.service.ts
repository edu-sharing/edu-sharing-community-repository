import { Injectable } from '@angular/core';
import { WorkspaceManagementDialogsComponent } from '../../modules/management-dialogs/management-dialogs.component';
import { MainNavComponent } from '../ui/main-nav/main-nav.component';
import {CookieInfoComponent} from '../ui/cookie-info/cookie-info.component';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {AccessibilityComponent} from '../ui/accessibility/accessibility.component';
import { SkipNavService } from '../ui/skip-nav/skip-nav.service';

@Injectable({
    providedIn: 'root',
})
export class MainNavService {
    private mainnav: MainNavComponent;
    private managementDialogs: WorkspaceManagementDialogsComponent;
    private cookieInfo: CookieInfoComponent;
    private accessibility: AccessibilityComponent;

    constructor(private router: Router,
                private route: ActivatedRoute,
                private skipNav: SkipNavService
    ) {
   }
    getDialogs() {
        return this.managementDialogs;
    }
    getCookieInfo() {
        return this.cookieInfo;
    }
    getAccessibility() {
        return this.accessibility;
    }
    registerDialogs(managementDialogs: WorkspaceManagementDialogsComponent) {
        this.managementDialogs = managementDialogs;
        this.subscribeChanges();
    }
    registerCookieInfo(cookieInfo: CookieInfoComponent) {
        this.cookieInfo = cookieInfo;
    }
    registerAccessibility(accessibility: AccessibilityComponent) {
        this.accessibility = accessibility;
        this.skipNav.register('ACCESSIBILITY_SETTINGS', () => accessibility.show());
    }
    getMainNav() {
        return this.mainnav;
    }

    registerMainNav(maiNnav: MainNavComponent) {
        this.mainnav = maiNnav;
    }

    private subscribeChanges() {
        this.managementDialogs.signupGroupChange.subscribe((value: boolean) => {
            this.router.navigate([], {
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
