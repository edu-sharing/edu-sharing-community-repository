import {
    AfterViewInit,
    Component,
    DoCheck,
    ElementRef,
    HostListener,
    Injector,
    NgZone,
    OnInit,
    ViewChild,
    inject,
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ApiStateService, AuthenticationService } from 'ngx-edu-sharing-api';
import {
    AccessibilityService,
    AppContainerService,
    TranslationsService,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../environments/environment';
import { ConfigurationService, RestHelper, RestNetworkService } from './core-module/core.module';
import { WorkspaceManagementDialogsComponent } from './features/management-dialogs/management-dialogs.component';
import { ManagementDialogsService } from './features/management-dialogs/management-dialogs.service';
import { CookieInfoComponent } from './main/cookie-info/cookie-info.component';
import { LoadingScreenService } from './main/loading-screen/loading-screen.service';
import { MainNavService } from './main/navigation/main-nav.service';
import { printCurrentTaskInfo } from './main/track-change-detection';
import { BridgeService } from './services/bridge.service';
import { LicenseAgreementService } from './services/license-agreement.service';
import { ScrollPositionRestorationService } from './services/scroll-position-restoration.service';
import { ThemeService } from './services/theme.service';
import { PlatformLocation } from '@angular/common';

@Component({
    selector: 'es-app',
    templateUrl: 'app.component.html',
    providers: [],
    standalone: false,
})
export class AppComponent implements OnInit, DoCheck, AfterViewInit {
    private appContainer = inject(AppContainerService);
    private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private mainNavService = inject(MainNavService);
    private dialogs = inject(ManagementDialogsService);
    private ngZone = inject(NgZone);
    private bridge = inject(BridgeService);
    private injector = inject(Injector);
    private accessibilityService = inject(AccessibilityService);
    private ui = inject(UIService);
    private translations = inject(TranslationsService);
    private loadingScreen = inject(LoadingScreenService);
    private licenseAgreement = inject(LicenseAgreementService);
    private themeService = inject(ThemeService);
    private authentication = inject(AuthenticationService);
    private configuration = inject(ConfigurationService);
    private scrollPositionRestoration = inject(ScrollPositionRestorationService);
    private legacyRestService = inject(RestNetworkService);
    private apiState = inject(ApiStateService);

    private static readonly CHECKS_PER_SECOND_WARNING_THRESHOLD = 0;
    private static readonly CONSECUTIVE_TRANSGRESSION_THRESHOLD = 10;
    static history = new BehaviorSubject<string[]>([]);

    public static isRedirectedFromLogin() {
        const history = AppComponent.history.value;
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

    isLoading = this.loadingScreen.isLoading;

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

    constructor() {
        this.injector.get(Router).events.subscribe((event) => {
            // if (event instanceof NavigationStart) {
            //     console.log('NavigationStart', event.url);
            // }
            if (event instanceof NavigationEnd) {
                AppComponent.history.value.push(event.url);
                AppComponent.history.next(AppComponent.history.value);
            }
        });
        this.ngZone.runOutsideAngular(() => {
            // Do not trigger change detection with setInterval.
            this.checksMonitorInterval = window.setInterval(() => this.monitorChecks(), 1000);
        });
    }

    ngOnInit(): void {
        this.elementRef.nativeElement.removeAttribute('ng-version');
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
        this.appContainer.init(this.elementRef.nativeElement);
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
        if (this.numberOfChecks > AppComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD) {
            this.consecutiveTransgression++;
            if (this.consecutiveTransgression >= AppComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD) {
                console.warn(
                    'Change detection triggered more than ' +
                        AppComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD +
                        ' times per second for the past ' +
                        AppComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD +
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
        if (this.bridge.isRunningCordova()) {
            return;
        }
        // The backend announces the authentication state of every rest response via the
        // `X-Edu-Authenticated` header. If a session silently expired (e.g. repository restart),
        // the user is degraded to guest without any error status, so we have to react to the
        // reported state change ourselves.
        this.apiState.observeAuthenticationLost().subscribe(() => {
            console.warn('backend reports a lost authentication, redirecting to login');
            this.authentication.forceLoginInfoRefresh();
            if (!this.isLoginExemptRoute(this.getCurrentRoute())) {
                this.ui.goToLogin();
            }
        });
        this.authentication.observeLoginInfo().subscribe(async (loginInfo) => {
            const route = this.getCurrentRoute();
            if (!loginInfo.isValidLogin && !this.isLoginExemptRoute(route)) {
                this.ui.goToLogin();
            } else if (loginInfo.isGuest) {
                this.configuration.get('loginSilentMode').subscribe((mode: string) => {
                    if ('iframe' === mode) {
                        const iframe_id: string = 'sso_check';
                        if (!document.getElementById(iframe_id)) {
                            let iframe = document.createElement('iframe');
                            iframe.style.display = 'none';
                            iframe.src =
                                window.location.origin +
                                '/edu-sharing/rest/authentication/v1/validateSSOSession';
                            iframe.id = iframe_id;
                            iframe.onload = (ev) => {
                                try {
                                    var y = iframe.contentDocument;
                                    var pre_info = y.getElementsByTagName('pre')[0].innerHTML;
                                    var o = JSON.parse(pre_info);
                                    if (o.error && o.error === 'login_required') {
                                    } else {
                                        this.ui.goToLogin();
                                    }
                                } catch (error) {
                                    console.error('check session iframe fails:' + error);
                                }
                            };
                            document.body.appendChild(iframe);
                        }
                    }
                });

                /*this.authentication.validateSSOSession().subscribe(async (loginInfo) => {
                    console.log("got a login will reload page");
                    window.location.reload();
                }, error => {
                    console.error(error);
                })*/
            }
        });
    }

    /** dirty hack: location + router components return null values */
    private getCurrentRoute(): string {
        return window.location.pathname.substring(
            this.injector.get(PlatformLocation).getBaseHrefFromDOM()?.length ?? 0,
        );
    }

    /** Routes that are reachable without a valid session and must not redirect to the login. */
    private isLoginExemptRoute(route: string): boolean {
        return (
            route.startsWith(UIConstants.ROUTER_PREFIX + 'login') ||
            route.startsWith(UIConstants.ROUTER_PREFIX + 'register') ||
            route.startsWith(UIConstants.ROUTER_PREFIX + 'error') ||
            route.startsWith(UIConstants.ROUTER_PREFIX + 'message') ||
            // public link sharing
            route.startsWith(UIConstants.ROUTER_PREFIX + 'sharing') ||
            route.startsWith('shibboleth')
        );
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
