import {
    AfterViewInit,
    Component,
    DoCheck,
    HostListener,
    Injector,
    NgZone,
    OnInit,
    ViewChild,
} from '@angular/core';
import { NavigationEnd, Router, Routes } from '@angular/router';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { AccessibilityService, TranslationsService, UIConstants } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';
import { ThemeService } from '../common/services/theme.service';
import { MdsTestComponent } from '../common/test/mds-test/mds-test.component';
import { ApplyToLmsComponent } from '../common/ui/apply-to-lms/apply-to-lms.component';
import { CookieInfoComponent } from '../common/ui/cookie-info/cookie-info.component';
import { BridgeService } from '../core-bridge-module/bridge.service';
import { ConfigurationService, RestHelper, RestNetworkService } from '../core-module/core.module';
import { extensionRoutes } from '../extension/extension-routes';
import { DialogsNavigationGuard } from '../features/dialogs/dialogs-navigation.guard';
import { LoadingScreenService } from '../main/loading-screen/loading-screen.service';
import { MainNavService } from '../main/navigation/main-nav.service';
import { FileUploadComponent } from '../modules/file-upload/file-upload.component';
import { LtiComponent } from '../modules/lti/lti.component';
import { WorkspaceManagementDialogsComponent } from '../modules/management-dialogs/management-dialogs.component';
import { ManagementDialogsService } from '../modules/management-dialogs/management-dialogs.service';
import { OerComponent } from '../modules/oer/oer.component';
import { ServicesComponent } from '../modules/services/services.components';
import { ShareAppComponent } from '../modules/share-app/share-app.component';
import { SharingComponent } from '../modules/sharing/sharing.component';
import { StartupComponent } from '../modules/startup/startup.component';
import { StreamComponent } from '../modules/stream/stream.component';
import { LicenseAgreementService } from '../services/license-agreement.service';
import { ScrollPositionRestorationService } from '../services/scroll-position-restoration.service';
import { printCurrentTaskInfo } from './track-change-detection';

@Component({
    selector: 'es-router',
    templateUrl: 'router.component.html',
    providers: [],
})
export class RouterComponent implements OnInit, DoCheck, AfterViewInit {
    private static readonly CHECKS_PER_SECOND_WARNING_THRESHOLD = 0;
    private static readonly CONSECUTIVE_TRANSGRESSION_THRESHOLD = 10;
    private static history = new BehaviorSubject<string[]>([]);

    public static isRedirectedFromLogin() {
        const history = RouterComponent.history.value;
        if (history.length < 2) {
            return false;
        }
        return (
            history[history.length - 1].indexOf(UIConstants.ROUTER_PREFIX + 'login') !== -1 ||
            history[history.length - 1].indexOf(UIConstants.ROUTER_PREFIX) === -1
        );
    }

    @ViewChild('management') management: WorkspaceManagementDialogsComponent;
    @ViewChild('cookie') cookie: CookieInfoComponent;

    private numberOfChecks = 0;
    private consecutiveTransgression = 0;
    private checksMonitorInterval: number;

    /**
     * Adds a prefix to all routes for compatibility with tomcat.
     */
    static transformRoute(route: any): any {
        const result: any = []; // we need a deep copy
        for (const r of route) {
            const a: any = {
                path: r.path,
                component: r.component,
                children: r.children,
            };

            if (a.path) a.path = UIConstants.ROUTER_PREFIX + r.path;
            result.push(a);
        }
        return result;
    }

    // FIXME: should we really do this?
    // > Warning: The beforeunload event should only be used to alert the user of unsaved changes.
    // > Once those changes are saved, the event should be removed. It should never be added
    // > unconditionally to the page, as doing so can hurt performance in some cases. See the legacy
    // > APIs section for details.
    // --- https://developer.chrome.com/blog/page-lifecycle-api/
    @HostListener('window:beforeunload', ['$event'])
    interceptRoute(event: BeforeUnloadEvent) {
        // console.log(event);
    }

    constructor(
        private mainNavService: MainNavService,
        private dialogs: ManagementDialogsService,
        private ngZone: NgZone,
        private bridge: BridgeService,
        private injector: Injector,
        private accessibilityService: AccessibilityService,
        private translations: TranslationsService,
        private loadingScreen: LoadingScreenService,
        private licenseAgreement: LicenseAgreementService,
        private themeService: ThemeService,
        private authentication: AuthenticationService,
        private configuration: ConfigurationService,
        private scrollPositionRestoration: ScrollPositionRestorationService,
        private legacyRestService: RestNetworkService,
    ) {
        this.injector.get(Router).events.subscribe((event) => {
            // if (event instanceof NavigationStart) {
            //     console.log('NavigationStart', event.url);
            // }
            if (event instanceof NavigationEnd) {
                RouterComponent.history.value.push(event.url);
                RouterComponent.history.next(RouterComponent.history.value);
            }
        });
        this.ngZone.runOutsideAngular(() => {
            // Do not trigger change detection with setInterval.
            this.checksMonitorInterval = window.setInterval(() => this.monitorChecks(), 1000);
        });
    }

    ngOnInit(): void {
        this.translations
            .initialize()
            .pipe(
                this.loadingScreen.showUntilFinished({
                    // The router component lives as long as the application, so we don't need to
                    // set `until` to anything meaningful.
                    until: rxjs.EMPTY,
                }),
            )
            .subscribe();
        this.setUserScale();
        this.registerRedirectToLogin();
        this.registerContrastMode();
        this.licenseAgreement.setup();
        this.scrollPositionRestoration.setup();
        this.legacyRestService.init();
    }

    ngDoCheck(): void {
        this.numberOfChecks++;
        if (environment.traceChangeDetection) {
            printCurrentTaskInfo('doCheck');
        }
    }

    ngAfterViewInit(): void {
        this.dialogs.registerDialogsComponent(this.management);
        this.mainNavService.registerCookieInfo(this.cookie);
        this.mainNavService.registerAccessibility();
    }

    private monitorChecks(): void {
        // console.log('Change detections run in the past second:', this.numberOfChecks);
        if (this.numberOfChecks > RouterComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD) {
            this.consecutiveTransgression++;
            if (
                this.consecutiveTransgression >= RouterComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD
            ) {
                console.warn(
                    'Change detection triggered more than ' +
                        RouterComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD +
                        ' times per second for the past ' +
                        RouterComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD +
                        ' seconds consecutively.' +
                        ' Not showing any more warnings.',
                );
                window.clearInterval(this.checksMonitorInterval);
            }
        } else {
            this.consecutiveTransgression = 0;
        }
        this.numberOfChecks = 0;
    }

    private setUserScale(): void {
        if (this.bridge.isRunningCordova()) {
            const viewport: HTMLMetaElement = document.head.querySelector('meta[name="viewport"]');
            viewport.content += ', user-scalable=no';
        }
    }

    /**
     * Redirects the user to the login page in case they don't have a valid session.
     */
    private registerRedirectToLogin(): void {
        this.authentication.observeLoginInfo().subscribe((loginInfo) => {
            const router = this.injector.get(Router);
            if (
                !loginInfo.isValidLogin &&
                !(
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'login') ||
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'register')
                )
            ) {
                RestHelper.goToLogin(router, this.configuration);
            }
        });
    }

    private registerContrastMode(): void {
        const contrastModeClass = 'es-contrast-mode';
        this.accessibilityService.observe('contrastMode').subscribe((value) => {
            if (value) {
                document.body.classList.add(contrastModeClass);
            } else {
                document.body.classList.remove(contrastModeClass);
            }
        });
    }
}

// Due to ahead of time, we need to create all routes manually.
const childRoutes: Routes = [
    // overrides and additional routes
    ...extensionRoutes,

    // global
    { path: '', component: StartupComponent },

    {
        path: UIConstants.ROUTER_PREFIX + 'app',
        loadChildren: () =>
            import('../pages/app-login-page/app-login-page.module').then(
                (m) => m.AppLoginPageModule,
            ),
    },
    { path: UIConstants.ROUTER_PREFIX + 'app/share', component: ShareAppComponent },
    { path: UIConstants.ROUTER_PREFIX + 'sharing', component: SharingComponent },
    { path: UIConstants.ROUTER_PREFIX + 'test/mds', component: MdsTestComponent },
    {
        path: UIConstants.ROUTER_PREFIX + 'render',
        loadChildren: () =>
            import('../pages/render-page/render-page.module').then((m) => m.RenderPageModule),
    },
    {
        path: UIConstants.ROUTER_PREFIX + 'apply-to-lms/:repo/:node',
        component: ApplyToLmsComponent,
    },
    // search
    {
        path: UIConstants.ROUTER_PREFIX + 'search',
        loadChildren: () =>
            import('../pages/search-page/search-page.module').then((m) => m.SearchPageModule),
    },
    // workspace
    {
        path: UIConstants.ROUTER_PREFIX + 'workspace',
        loadChildren: () =>
            import('../pages/workspace-page/workspace-page.module').then(
                (m) => m.WorkspacePageModule,
            ),
    },
    // collections
    {
        path: UIConstants.ROUTER_PREFIX + 'collections',
        loadChildren: () =>
            import('../pages/collections-page/collections-page.module').then(
                (m) => m.CollectionsPageModule,
            ),
    },
    // login
    {
        path: UIConstants.ROUTER_PREFIX + 'login',
        loadChildren: () =>
            import('../pages/login-page/login-page.module').then((m) => m.LoginPageModule),
    },
    // register
    {
        path: UIConstants.ROUTER_PREFIX + 'register',
        loadChildren: () =>
            import('../pages/register-page/register-page.module').then((m) => m.RegisterPageModule),
    },
    // file upload
    { path: UIConstants.ROUTER_PREFIX + 'upload', component: FileUploadComponent },
    // admin
    {
        path: UIConstants.ROUTER_PREFIX + 'admin',
        loadChildren: () =>
            import('../pages/admin-page/admin-page.module').then((m) => m.AdminPageModule),
    },
    // permissions
    {
        path: UIConstants.ROUTER_PREFIX + 'permissions',
        loadChildren: () =>
            import('../pages/user-management-page/user-management-page.module').then(
                (m) => m.UserManagementPageModule,
            ),
    },
    // oer
    { path: UIConstants.ROUTER_PREFIX + 'oer', component: OerComponent },
    // stream
    { path: UIConstants.ROUTER_PREFIX + 'stream', component: StreamComponent },
    // profiles
    {
        path: UIConstants.ROUTER_PREFIX + 'profiles',
        loadChildren: () =>
            import('../pages/profile-page/profile-page.module').then((m) => m.ProfilePageModule),
    },

    // link-share
    { path: UIConstants.ROUTER_PREFIX + 'sharing', component: SharingComponent },
    // services
    { path: UIConstants.ROUTER_PREFIX + 'services', component: ServicesComponent },

    // embed
    {
        path: UIConstants.ROUTER_PREFIX + 'embed/:component',
        loadChildren: () => import('../common/ui/embed/embed.module').then((m) => m.EmbedModule),
    },

    { path: UIConstants.ROUTER_PREFIX + 'lti', component: LtiComponent },

    // error page / 404
    {
        path: '',
        loadChildren: () =>
            import('../pages/error-page/error-page.module').then((m) => m.ErrorPageModule),
    },
];

export const ROUTES: Routes = [
    // Add a `canDeactivate` guard to all routes, that closes any open dialogs before allowing
    // navigation.
    {
        path: '',
        canDeactivate: [DialogsNavigationGuard],
        children: childRoutes,
        runGuardsAndResolvers: 'paramsOrQueryParamsChange',
    },
];
