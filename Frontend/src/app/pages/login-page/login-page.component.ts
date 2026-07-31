import { trigger } from '@angular/animations';
import { PlatformLocation } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
    AfterViewInit,
    Component,
    computed,
    ElementRef,
    OnDestroy,
    OnInit,
    Signal,
    signal,
    TemplateRef,
    ViewChild,
    inject,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, UntypedFormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { delay, filter, first, map, startWith, switchMap } from 'rxjs/operators';
import { BridgeService } from '../../services/bridge.service';
import {
    ConfigurationService,
    DialogButton,
    LoginResult,
    RestConnectorService,
    RestConstants,
} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { InputPasswordComponent } from '../../shared/components/input-password/input-password.component';
import { RouterHelper } from '../../util/router.helper';
import { Toast } from '../../services/toast';
import {
    OPEN_URL_MODE,
    TranslationsService,
    UIAnimation,
    UIConstants,
    UIService,
} from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { AuthenticationService, LoginInfo, OAuthEntry, PrimaryLogin } from 'ngx-edu-sharing-api';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { firstValueFrom, Observable, Subject } from 'rxjs';
import { ThemeService } from '../../services/theme.service';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { Closable } from '../../features/dialogs/card-dialog/card-dialog-config';
import {
    GenericDialogData,
    NEXT,
} from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { CardDialogRef } from '../../features/dialogs/card-dialog/card-dialog-ref';
import { CordovaService } from '../../services/cordova.service';

type WafyEntry = {
    name: string;
    url: string;
    type: string;
};

@Component({
    selector: 'es-login-page',
    templateUrl: 'login-page.component.html',
    styleUrls: ['login-page.component.scss'],
    animations: [trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST))],
    standalone: false,
})
export class LoginPageComponent implements OnInit, OnDestroy, AfterViewInit {
    private connector = inject(RestConnectorService);
    private toast = inject(Toast);
    private dialogs = inject(DialogsService);
    private platformLocation = inject(PlatformLocation);
    private ui = inject(UIService);
    private router = inject(Router);
    private http = inject(HttpClient);
    private translations = inject(TranslationsService);
    private configService = inject(ConfigurationService);
    private route = inject(ActivatedRoute);
    bridge = inject(BridgeService);
    private cordova = inject(CordovaService);
    private authentication = inject(AuthenticationService);
    protected themeService = inject(ThemeService);
    private loadingScreen = inject(LoadingScreenService);
    private mainNav = inject(MainNavService);

    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    @ViewChild('loginForm') loginForm: ElementRef;
    @ViewChild('faConfirmRef') faConfirmRef: TemplateRef<unknown>;

    @ViewChild('passwordInput') passwordInput: InputPasswordComponent;
    @ViewChild('usernameInput') usernameInput: ElementRef;

    buttons: DialogButton[] = [];
    caption = 'LOGIN.TITLE';
    config: any = {};
    currentProvider: any;
    disabled = false;
    isSafeLogin = false;
    filteredProviders: Observable<any[]>;
    isLoading = true;
    loginUrl: any;
    password = '';
    providerControl = new UntypedFormControl();
    readonly showProviders = signal(false);
    username = '';
    loginSafeFailed = false;

    show2FaDialog: CardDialogRef<GenericDialogData<'NEXT'>, 'NEXT'>;
    faConfirm = new FormGroup({
        code: new FormControl('', [Validators.required, Validators.pattern(/^\d{6}$/)]),
    });
    readonly providersOnly = signal(false);
    readonly queryParams: Signal<Params>;
    readonly providersOnlyMode = computed(
        () =>
            this.showProviders() &&
            this.providersOnly() &&
            this.queryParams()?.['local'] !== 'true',
    );
    readonly showLocalLogin = computed(
        () => !this.providersOnly() || this.queryParams()?.['local'] === 'true',
    );
    private next = '';
    private providers: any;
    private scope = '';
    private destroyed = new Subject<void>();
    // @TODO change model
    registeredOauthProviders: OAuthEntry[];

    constructor() {
        const configService = this.configService;

        this.queryParams = toSignal(this.route.queryParams, { initialValue: {} as Params });
        // reset the theme in case user was in safe previously
        this.themeService.initWithDefaults();
        const loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
        this.isLoading = true;
        this.updateButtons();
        this.translations
            .waitForInit()
            .pipe(switchMap(() => this.configService.getAll()))
            .subscribe((data: any) => {
                this.config = data;
                if (!this.config.register) {
                    // default register mode: allow local registration if not disabled
                    this.config.register = { local: true };
                }
                this.updateButtons();
                this.username = this.configService.instant('defaultUsername', '');
                this.password = this.configService.instant('defaultPassword', '');
                void this.route.queryParams.forEach(async (params: Params) => {
                    if (params.username) {
                        this.username = params.username;
                    }
                    if (params.device_verification_success === 'true') {
                        this.isLoading = true;
                        try {
                            await this.cordova.finalizeOAuthGrant();
                            this.goToNext();
                        } catch (e) {
                            this.toast.error(e);
                        }
                    }
                    this.next = params.next;
                    this.connector.onAllRequestsReady().subscribe(() => {
                        setTimeout(() => {
                            if (this.username && this.passwordInput) {
                                this.passwordInput.nativeInput.nativeElement.focus();
                            } else if (this.usernameInput) {
                                this.usernameInput.nativeElement.focus();
                            }
                        }, 100);
                    });
                    this.scope = params.scope;
                    if (!this.scope) {
                        this.scope = null;
                    }
                    this.connector.isLoggedIn().subscribe(async (data: LoginResult) => {
                        // @TODO this.registeredOauthProviders
                        this.registeredOauthProviders = (data as PrimaryLogin).oauthEntries; // [{id: "Google"}, {id: "Facebook"}, {id: "Github"}]
                        if (data.currentScope) {
                            // just to make sure there is no scope still set // NO: We need a valid session when login to scope!!!
                            try {
                                await this.connector.logout().toPromise();
                            } catch (e) {
                                console.warn(e);
                            }
                            data.statusCode = null;
                        } else if (data.currentScope === this.scope) {
                            if (
                                data.statusCode === RestConstants.STATUS_CODE_OK &&
                                // force redirect when local was NOT requested or redirectFromSSO was enforced
                                (params.local !== 'true' || params.redirectFromSSO === 'true')
                            ) {
                                if (this.cordova.isRunningCordova()) {
                                    // when there is no valid config -> handle initial oauth grant to get the first token
                                    // this call shall return in the param device_verification_success to be set
                                    if (!(await this.cordova.hasValidConfig())) {
                                        await this.cordova.getOAuthGrant();
                                        return;
                                    }
                                }
                                this.goToNext(data);
                                return;
                            } else if (this.cordova.isRunningCordova()) {
                                if (await this.cordova.hasValidConfig()) {
                                    try {
                                        await this.cordova.startSessionViaOauthRefreshToken();
                                        this.goToNext(data);
                                        return;
                                    } catch (e) {
                                        this.toast.error(e);
                                        this.cordova.oauth = null;
                                    }
                                }
                            }
                        }
                        // when there is a request to go into safe mode, first, the user needs to log in regularly
                        else if (data.statusCode !== RestConstants.STATUS_CODE_OK && this.scope) {
                            // RestHelper.goToLogin()
                        }
                        this.loginUrl = configService.instant('loginUrl');
                        const allowLocal = configService.instant('loginAllowLocal', false);
                        const hasProviders = this.registeredOauthProviders?.length > 1;
                        if (
                            params.local !== 'true' &&
                            !allowLocal &&
                            !hasProviders &&
                            this.loginUrl &&
                            !configService.instant('loginProvidersUrl') &&
                            data.statusCode !== RestConstants.STATUS_CODE_OK
                        ) {
                            this.openLoginUrl();
                            return;
                        }
                        this.isLoading = false;
                        loadingTask.done();
                        if (configService.instant('loginProvidersUrl')) {
                            this.showProviders.set(true);
                            this.providersOnly.set(!configService.instant('loginAllowLocal', true));
                            this.updateButtons();
                            // delay to make sure animation of card has finished
                            // otherwise, overlay gets aligned wrongly
                            const providers = await this.http
                                .get(configService.instant('loginProvidersUrl'))
                                .pipe(delay(UIAnimation.ANIMATION_TIME_NORMAL))
                                .toPromise();
                            this.processProviders(providers);
                        }
                    });
                    this.isSafeLogin = this.scope == RestConstants.SAFE_SCOPE;
                    if (this.scope === RestConstants.SAFE_SCOPE) {
                        this.connector.isLoggedIn(true).subscribe(
                            (data: LoginResult) => {
                                if (data.statusCode !== RestConstants.STATUS_CODE_OK) {
                                    this.ui.goToLogin();
                                } else {
                                    this.authentication
                                        .observeHasAccessToScope(RestConstants.SAFE_SCOPE)
                                        .pipe(first())
                                        .subscribe((hasAccess) => {
                                            if (hasAccess) {
                                                this.username = data.authorityName;
                                            } else {
                                                this.toast.error(null, 'LOGIN.NO_ACCESS');
                                                void this.router.navigate([
                                                    UIConstants.ROUTER_PREFIX + 'workspace',
                                                ]);
                                                // window.history.back();
                                            }
                                        });
                                }
                            },
                            (error: any) => this.ui.goToLogin(),
                        );
                    }

                    if (this.scope === RestConstants.SAFE_SCOPE) {
                        this.caption = 'LOGIN.TITLE_SAFE';
                    } else {
                        this.caption = 'LOGIN.TITLE';
                    }
                });
            });
    }

    ngAfterViewInit(): void {
        setTimeout(() => window.dispatchEvent(new Event('resize')), 100);
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    canRegister(): boolean {
        return (
            this.config.register && (this.config.register.local || this.config.register.registerUrl)
        );
    }

    /** the generic "login via external provider" hint (config.loginUrl) is rendered */
    showLoginUrl(): boolean {
        return !!this.config?.loginUrl && !this.showProviders() && !this.isSafeLogin;
    }

    /**
     * Hide the oauth provider buttons if the generic loginUrl hint is already shown and there is
     * exactly one provider -- both would offer the very same external login.
     */
    showOauthProviders(): boolean {
        const count = this.registeredOauthProviders?.length ?? 0;
        return count > 0 && !(count === 1 && this.showLoginUrl());
    }

    checkConditions(event: Event) {
        this.disabled = !this.username || this.currentProvider; // || !this.password;
        this.updateButtons();
    }

    currentProviderDisplay(provider: any) {
        return provider ? provider.name : '';
    }

    goToProvider() {
        if (!this.currentProvider) {
            this.toast.error(null, 'LOGIN.NO_PROVIDER_SELECTED');
        }
        let url = this.configService.instant('loginProviderTargetUrl');
        if (!url) {
            this.toast.error(null, 'No configuration for loginProviderTargetUrl found.');
            return;
        }
        const target =
            this.connector.getAbsoluteServerUrl() + this.configService.instant('loginUrl');
        url = url
            .replace(':target', encodeURIComponent(target))
            // remove invalid parameters for multiple universities using the same idp
            .replace(
                ':entity',
                encodeURIComponent(this.currentProvider.url.replace(/@_.*?_@/, '')),
            );
        // @TODO: Redirect to shibboleth provider
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Current);
    }

    async login(password = this.password) {
        this.isLoading = true;
        if (this.scope) {
            // before we're converting to a safe session, we need to make sure all previous requests are finished
            // otherwise, we're may getting HTTP_FORBIDDEN/401
            await firstValueFrom(
                this.connector.getCurrentRequestCount().pipe(filter((c) => c === 0)),
            );
        } else {
            if (this.cordova.isRunningCordova()) {
                try {
                    const oauthTokens = await this.cordova.loginOAuth(this.username, this.password);
                } catch (error: any) {
                    console.warn(error);
                    this.isLoading = false;
                    if ((error as Error).message) {
                        this.toast.error(null, error.message);
                    } else {
                        this.toast.error(null, 'LOGIN.ERROR');
                    }
                }
                return;
            }
        }
        this.connector.login(this.username, password, this.scope).subscribe(
            (data) => {
                if (data.statusCode === RestConstants.STATUS_CODE_OK) {
                    this.goToNext(data);
                } else {
                    if (
                        data.statusCode === RestConstants.STATUS_CODE_PREVIOUS_SESSION_REQUIRED ||
                        data.statusCode === RestConstants.STATUS_CODE_PREVIOUS_USER_WRONG
                    ) {
                        this.toast.error(null, 'LOGIN.SAFE_PREVIOUS');
                    } else if (data.statusCode === RestConstants.STATUS_CODE_PASSWORD_EXPIRED) {
                        if (this.isSafeLogin) {
                            this.loginSafeFailed = true;
                        }
                        this.toast.error(
                            null,
                            'LOGIN.PASSWORD_EXPIRED' + (this.isSafeLogin ? '_SAFE' : ''),
                        );
                    } else if (data.statusCode === RestConstants.STATUS_CODE_PERSON_BLOCKED) {
                        this.toast.error(null, 'LOGIN.PERSON_BLOCKED');
                    } else if (data.statusCode === RestConstants.STATUS_CODE_2FA) {
                        void this.show2Fa();
                    } else {
                        if (this.isSafeLogin) {
                            this.loginSafeFailed = true;
                        }
                        this.toast.error(null, 'LOGIN.ERROR' + (this.isSafeLogin ? '_SAFE' : ''));
                    }
                    this.password = '';
                    this.isLoading = false;
                }
            },
            (error: any) => {
                this.toast.error(error);
                this.isLoading = false;
            },
        );
    }

    ngOnInit() {
        this.mainNav.setMainNavConfig({
            currentScope: 'login',
            title: 'SIDEBAR.LOGIN',
        });
    }

    openLoginUrl() {
        window.location.href = this.loginUrl;
    }

    register() {
        if (this.config.register.local) {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'register']);
        } else {
            window.location.href = this.config.register.registerUrl;
        }
    }

    private filterProviders(filter: any = '') {
        const filtered = [];
        if (!this.providers) {
            return null;
        }
        // an object was detected, abort
        if (filter.name) {
            return this.providers;
        }
        this.currentProvider = null;
        for (const p of Helper.deepCopy(this.providers)) {
            p.providers = p.providers.filter(
                (p: any) =>
                    p.name.toLowerCase().includes(filter.toLowerCase()) ||
                    p.data?.toLowerCase().includes(filter.toLowerCase()),
            );
            if (p.providers.length) {
                filtered.push(p);
            }
        }
        return filtered;
    }

    private goToNext(data?: LoginInfo) {
        if (this.next) {
            this.next = Helper.addGetParameter('fromLogin', 'true', this.next);
            RouterHelper.navigateToAbsoluteUrl(this.platformLocation, this.router, this.next);
        } else if (data?.currentScope === RestConstants.SAFE_SCOPE) {
            void this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace', 'safe']);
        } else {
            UIHelper.goToDefaultLocation(this.router, this.platformLocation, this.configService);
        }
    }

    private processProviders(providers: any) {
        const data: {
            [key in string]: {
                group: string;
                providers: WafyEntry[];
            };
        } = {};
        for (const provider of Object.keys(providers.wayf_idps)) {
            const object: WafyEntry = providers.wayf_idps[provider];
            if (object) {
                if (!object.url) {
                    object.url = provider;
                }
                const type = object.type;
                if (!data[type]) {
                    data[type] = {
                        group: providers.wayf_categories[type],
                        providers: [],
                    };
                }
                data[type].providers.push(object);
            }
        }
        this.providers = [];
        for (const key of Object.keys(data)) {
            this.providers.push(data[key]);
        }

        // register observer for autocomplete
        this.filteredProviders = this.providerControl.valueChanges.pipe(
            startWith(''),
            map((value: string) => this.filterProviders(value)),
        );
    }

    private updateButtons() {
        if (this.showProviders()) {
            return;
        }
        const register = new DialogButton('LOGIN.REGISTER_TEXT', { color: 'standard' }, () =>
            this.register(),
        );
        if (this.canRegister()) {
            if (!this.buttons.find((b) => b.label === register.label)) {
                this.buttons.splice(0, 0, register);
            }
        } else {
            this.buttons.splice(this.buttons.findIndex((b) => b.label === register.label));
        }
        let login = new DialogButton('LOGIN.LOGIN', { color: 'primary' }, () => this.login());
        const loginFound = this.buttons.find((b) => b.label === login.label);
        if (!loginFound) {
            this.buttons.push(login);
        } else {
            login = loginFound;
        }
        login.disabled = this.disabled;
    }

    private async show2Fa() {
        this.faConfirm.reset();
        this.show2FaDialog = await this.dialogs.openGenericDialog({
            title: 'LOGIN.2FA.TITLE',
            subtitle: 'LOGIN.2FA.SUBTITLE',
            minWidth: 500,
            closable: Closable.Casual,
            contentTemplate: this.faConfirmRef,
            buttons: NEXT,
            avatar: { kind: 'icon', icon: 'flag' },
        });
        let button = new DialogButton('NEXT', DialogButton.TYPE_PRIMARY, () =>
            this.show2FaDialog.close('NEXT'),
        );
        button.disabled = true;
        setTimeout(() => {
            this.show2FaDialog.patchConfig({
                buttons: [button],
            });
        });
        this.faConfirm.statusChanges.subscribe((status) => {
            button.disabled = status !== 'VALID';
            this.show2FaDialog.patchConfig({
                buttons: [button],
            });
        });
        const result = await firstValueFrom(this.show2FaDialog.afterClosed());
        this.show2FaDialog = null;
        if (result === 'NEXT' && this.faConfirm.status === 'VALID') {
            this.isLoading = true;
            this.connector.verify2Fa(this.faConfirm.get('code').value).subscribe(
                (data) => {
                    if (data.statusCode === RestConstants.STATUS_CODE_OK) {
                        this.goToNext(data);
                    } else if (data.statusCode === RestConstants.STATUS_CODE_2FA) {
                        this.toast.error(null, 'LOGIN.2FA.WRONG_CODE');
                        this.isLoading = false;
                    } else {
                        this.toast.error(null, 'LOGIN.ERROR');
                        this.isLoading = false;
                    }
                },
                (error: any) => {
                    this.toast.error(error);
                    this.isLoading = false;
                },
            );
        }
    }

    protected readonly encodeURIComponent = encodeURIComponent;

    appBack() {
        this.cordova.restartCordova();
    }
}
