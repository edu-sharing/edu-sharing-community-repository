import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService, ConfigService } from 'ngx-edu-sharing-api';
import { UIConstants, UIService } from 'ngx-edu-sharing-ui';
import { firstValueFrom, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, filter, startWith, takeUntil } from 'rxjs/operators';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { MainNavService } from '../../main/navigation/main-nav.service';
import {
    SearchEvent,
    SearchFieldService,
} from '../../main/navigation/search-field/search-field.service';

export type SwimlaneTypes =
    | 'featured-media'
    | 'collections'
    | 'recent-activities'
    | 'shares'
    | 'assignments';
export type SwimlaneEntry = {
    id: SwimlaneTypes;
    defaultExpanded: boolean;
};
@Component({
    selector: 'es-landing-page',
    templateUrl: 'landing-page.component.html',
    styleUrls: ['landing-page.component.scss'],
    standalone: false,
})
export class LandingPageComponent implements OnInit, OnDestroy {
    private readonly destroyed$ = new Subject<void>();
    readonly i18nPrefix: string = 'LANDING_PAGE.';
    landingPageScope: string = 'LANDING';
    searchEvent$: Observable<SearchEvent>;

    /**
     * displayed swimlanes (in order)
     * are be retrieved from the backend client.config
     */
    swimlanes = signal<SwimlaneEntry[]>([]);

    constructor(
        private authenticationService: AuthenticationService,
        private configService: ConfigService,
        private mainNav: MainNavService,
        private router: Router,
        private searchFieldService: SearchFieldService,
        private ui: UIService,
    ) {
        this.mainNav.setMainNavConfig({
            showUser: true,
            showScope: true,
            currentScope: this.landingPageScope,
            title: 'SIDEBAR.LANDING',
            show: true,
            create: {
                allowed: true,
                allowBinary: true,
            },
            showNavigation: true,
        });
        // enable the search field and observe the search event
        this.searchFieldService.enable(
            {
                enableFiltersAndSuggestions: false,
                showFiltersButton: false,
                placeholder: this.i18nPrefix + 'SEARCH_PLACEHOLDER',
            },
            this.destroyed$,
        );
        this.searchFieldService
            .observeCurrentInstance()
            .pipe(
                takeUntil(this.destroyed$),
                filter((i) => !!i),
            )
            .subscribe((instance) => {
                this.searchEvent$ = instance.onSearchTriggered();
                this.searchEvent$
                    .pipe(
                        startWith({
                            searchString: this.searchFieldService
                                .getCurrentInstance()
                                ?.getSearchString(),
                            cleared: false,
                        }),
                        distinctUntilChanged(),
                    )
                    .subscribe((event) => {
                        if (!event.searchString) {
                            return;
                        }
                        const params = {
                            q: event.searchString,
                        };
                        void this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                            queryParams: params,
                        });
                    });
            });
    }

    async ngOnInit(): Promise<void> {
        const login = await firstValueFrom(this.authenticationService.observeLoginInfo());
        if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
            this.ui.goToLogin();
            return;
        }
        this.swimlanes.set(
            await this.configService.get<SwimlaneEntry[]>('frontpage.dashboard.swimlanes', [
                {
                    id: 'assignments',
                    defaultExpanded: true,
                },
                {
                    id: 'recent-activities',
                    defaultExpanded: true,
                },
                {
                    id: 'collections',
                    defaultExpanded: true,
                },
                {
                    id: 'featured-media',
                    defaultExpanded: true,
                },
                {
                    id: 'shares',
                    defaultExpanded: true,
                },
            ]),
        );
    }

    /**
     * On destruction, complete the subjects.
     */
    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
