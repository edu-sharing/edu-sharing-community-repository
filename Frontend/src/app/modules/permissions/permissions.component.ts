import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslationsService } from '../../translations/translations.service';
import {
    ConfigurationService,
    LoginResult,
    Organization,
    OrganizationOrganizations,
    RestConnectorService,
    RestHelper,
    RestOrganizationService,
} from '../../core-module/core.module';
import { Router } from '@angular/router';
import { Toast } from '../../core-ui-module/toast';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { Subject } from 'rxjs';
import { SearchFieldService } from '../../main/navigation/search-field/search-field.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { PlatformLocation } from '@angular/common';

@Component({
    selector: 'es-permissions-main',
    templateUrl: 'permissions.component.html',
    styleUrls: ['permissions.component.scss'],
    animations: [],
})
export class PermissionsMainComponent implements OnInit, OnDestroy {
    public tab: number = 0;
    public searchQuery: string;
    selected: Organization;
    public isAdmin = false;
    public disabled = false;
    public isLoading = true;
    TABS = ['ORG', 'GROUP', 'USER', 'DELETE'];
    private destroyed = new Subject<void>();

    constructor(
        private toast: Toast,
        private router: Router,
        private platformLocation: PlatformLocation,
        private config: ConfigurationService,
        private translations: TranslationsService,
        private organization: RestOrganizationService,
        private loadingScreen: LoadingScreenService,
        private mainNav: MainNavService,
        private connector: RestConnectorService,
        private searchField: SearchFieldService,
    ) {
        const loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
        this.translations.waitForInit().subscribe(() => {
            this.connector.isLoggedIn().subscribe(
                (data: LoginResult) => {
                    if (data.isValidLogin && !data.isGuest && !data.currentScope) {
                        this.organization
                            .getOrganizations()
                            .subscribe((data: OrganizationOrganizations) => {
                                this.isAdmin = data.canCreate;
                                const hasAccess =
                                    this.isAdmin ||
                                    data.organizations.filter((o) => o.administrationAccess)
                                        .length > 0;
                                if (!hasAccess) {
                                    this.toast.error(null, 'TOAST.API_FORBIDDEN');
                                    UIHelper.goToDefaultLocation(
                                        this.router,
                                        this.platformLocation,
                                        this.config,
                                    );
                                    return;
                                }
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

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerMainNav(): void {
        this.mainNav.setMainNavConfig({
            title: 'PERMISSIONS.TITLE',
            currentScope: 'permissions',
        });
        this.updateSearchField();
    }

    private updateSearchField(): void {
        if (this.tab !== 3) {
            const searchFieldInstance = this.searchField.enable(
                {
                    placeholder: 'PERMISSIONS.SEARCH_' + this.TABS[this.tab],
                },
                this.destroyed,
            );
            searchFieldInstance.setSearchString(this.searchQuery);
            searchFieldInstance
                .onSearchTriggered()
                .subscribe(({ searchString }) => this.doSearch(searchString));
        } else {
            this.searchField.disable();
        }
    }

    private doSearch(searchString: string) {
        this.searchQuery = searchString;
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
        this.updateSearchField();
    }

    private goToLogin() {
        RestHelper.goToLogin(this.router, this.config);
    }
}
