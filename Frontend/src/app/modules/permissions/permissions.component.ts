import { Component, ViewChild, ElementRef, OnInit } from '@angular/core';
import { TranslationsService } from '../../translations/translations.service';
import {
    NodeRef,
    IamUser,
    NodeWrapper,
    Node,
    Version,
    NodeVersions,
    LoginResult,
    IamGroups,
    Group,
    OrganizationOrganizations,
    Organization,
} from '../../core-module/core.module';
import { Router, Params, Routes } from '@angular/router';
import { Toast } from '../../core-ui-module/toast';
import { RestConnectorService } from '../../core-module/core.module';
import { RestOrganizationService } from '../../core-module/core.module';
import { ConfigurationService } from '../../core-module/core.module';
import { RestHelper } from '../../core-module/core.module';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';

@Component({
    selector: 'es-permissions-main',
    templateUrl: 'permissions.component.html',
    styleUrls: ['permissions.component.scss'],
    animations: [],
})
export class PermissionsMainComponent implements OnInit {
    public tab: number = 0;
    public searchQuery: string;
    selected: Organization;
    public isAdmin = false;
    public disabled = false;
    public isLoading = true;
    TABS = ['ORG', 'GROUP', 'USER', 'DELETE'];
    constructor(
        private toast: Toast,
        private router: Router,
        private config: ConfigurationService,
        private translations: TranslationsService,
        private organization: RestOrganizationService,
        private loadingScreen: LoadingScreenService,
        private mainNav: MainNavService,
        private connector: RestConnectorService,
    ) {
        const loadingTask = this.loadingScreen.addLoadingTask();
        this.translations.waitForInit().subscribe(() => {
            this.connector.isLoggedIn().subscribe(
                (data: LoginResult) => {
                    if (data.isValidLogin && !data.isGuest && !data.currentScope) {
                        this.organization
                            .getOrganizations()
                            .subscribe((data: OrganizationOrganizations) => {
                                this.isAdmin = data.canCreate;
                            });
                    } else {
                        this.goToLogin();
                    }
                    this.isLoading = false;
                    loadingTask.done();
                },
                (error: any) => this.goToLogin(),
            );
            this.config.get('hideMainMenu').subscribe((data: string[]) => {
                if (data && data.indexOf('permissions') != -1) {
                    //this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
                    this.disabled = true;
                }
            });
        });
    }

    ngOnInit(): void {
        this.registerMainNav();
    }

    private registerMainNav(): void {
        this.mainNav.setMainNavConfig({
            title: 'PERMISSIONS.TITLE',
            currentScope: 'permissions',
            onSearch: (query) => this.doSearch(query),
        });
        this.updateMainNav();
    }

    private updateMainNav(): void {
        this.mainNav.patchMainNavConfig({
            searchEnabled: this.tab !== 3,
            searchQuery: this.searchQuery,
            searchPlaceholder: 'PERMISSIONS.SEARCH_' + this.TABS[this.tab],
        });
    }

    public doSearch(event: string) {
        this.searchQuery = event;
    }

    setTab(tab: number) {
        if (tab != 0 && !this.selected && !this.isAdmin) {
            this.toast.error(null, 'PERMISSIONS.SELECT_ORGANIZATION');
            this.tab = 0;
        } else if (tab === this.tab) {
            return;
        } else {
            if (tab === 0) {
                this.selected = null;
            }
            this.searchQuery = null;
            this.tab = tab;
        }
        this.updateMainNav();
    }

    private goToLogin() {
        RestHelper.goToLogin(this.router, this.config);
    }
}
