import { computed, Injectable, signal, TemplateRef, inject } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, forkJoin, Observable, of, startWith, Subject } from 'rxjs';
import { debounceTime, filter, map, pairwise, switchMap, take, tap } from 'rxjs/operators';
import {
    ConfigService,
    Node,
    RepositoryMessage,
    SessionStorageService,
    Store,
    UserEntry,
    UserService,
} from 'ngx-edu-sharing-api';
import { FrameEventsService } from '../../core-module/core.module';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { ManagementDialogsService } from '../../features/management-dialogs/management-dialogs.service';
import { MainNavComponent } from '../../main/navigation/main-nav/main-nav.component';
import { CookieInfoComponent } from '../cookie-info/cookie-info.component';
import { SkipNavService } from './skip-nav/skip-nav.service';
import { CustomOptions } from 'ngx-edu-sharing-ui';
import { CLOSE } from '../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';

export class MainNavCreateConfig {
    /** allowed / display new material button */
    allowed?: boolean | 'EMIT_EVENT' = false;
    /**
     * allow the global dropping (drop file anywhere to upload)
     */
    globalDrop?: boolean;
    /** refer to CreateMenuComponent */
    allowBinary?: boolean = true;
    parent?: Node = null;
    folder?: boolean = false;
}
type Status = 'new' | 'other-scope' | 'shown';
export type SystemMessageDetails = {
    storageKey: string;
    message: RepositoryMessage;
    status?: Status;
};

export class MainNavConfig {
    /**
     * Show or hide the entire component including banner and navigation bar
     */
    show? = true;
    /**
     * Show or hide the navigation bar
     */
    showNavigation? = true;
    /**
     * Shows the current location
     */
    showScope? = true;
    /**
     * Shows and enables the user menu
     */
    showUser? = true;
    /**
     * When true, the sidebar can be clicked to open the menu
     */
    canOpen? = true;
    /**
     * The title on the left side, will be translated
     */
    title?: string;
    /**
     * "add material" options
     */
    create?: MainNavCreateConfig = new MainNavCreateConfig();
    currentScope: string;

    /**
     * additional scope info, i.e. for collections this can be "edit" when in edit/create context
     */
    additionalScope?: 'edit';
    /**
     * Hide the search field although it was enabled via `SearchFieldService`.
     *
     * Use if you include the search-field component yourself in your page.
     */
    hideSearchField? = false;
    /**
     * Custom options that should be placed in the "New" menu
     */
    customCreateOptions?: CustomOptions;
    /**
     * custom options / options configuration for the user dropdown menu on the right
     */
    customUserMenuOptions?: CustomOptions;

    /**
     * If create is allowed, this event will fire the new nodes
     */
    onCreate?: (node: Node[]) => void;
    onCreateNotAllowed?: () => void;
}
export enum TemplateSlot {
    MainScopeButton,
    BeforeUserMenu,
    AfterCreateMenu,
    BelowTopBar,
}

@Injectable({
    providedIn: 'root',
})
export class MainNavService {
    private managementDialogs = inject(ManagementDialogsService);
    private event = inject(FrameEventsService);
    private skipNav = inject(SkipNavService);
    private dialogs = inject(DialogsService);
    private sessionStorageService = inject(SessionStorageService);
    private user = inject(UserService);
    private configServiceApi = inject(ConfigService);

    readonly DefaultHeight = 70;
    private mainnav: MainNavComponent;
    private cookieInfo: CookieInfoComponent;
    private mainNavConfigSubject = new BehaviorSubject<MainNavConfig>(new MainNavConfig());
    private mainNavConfigOverrideSubject = new BehaviorSubject<Partial<MainNavConfig> | null>(null);
    private customTemplates: { [key in TemplateSlot]?: TemplateRef<any> } = {};
    /**
     * is triggered when a connector or lti element was successfully created
     * The observable will receive the newly generated node
     */
    onConnectorCreated = new Subject<Node>();
    private _isVisible: boolean;
    private lastHeight = this.DefaultHeight;
    private _systemMessage = signal<SystemMessageDetails>(null);
    showSystemMessage = computed(() => this._systemMessage()?.message?.mode === 'bar');
    readonly DefaultScopes = ['workspace', 'collections', 'search', 'render', 'admin'];
    private customScopes: string[];

    /**
     * register a template to be used in the top bar instead of the default one
     */
    registerCustomTemplateSlot(slot: TemplateSlot, template: TemplateRef<any>) {
        this.customTemplates[slot] = template;
    }
    unregisterCustomTemplateSlot(slot: TemplateSlot) {
        delete this.customTemplates[slot];
    }
    getCustomTemplateSlot(slot: TemplateSlot) {
        return this.customTemplates[slot];
    }
    getDialogs() {
        return this.managementDialogs.getDialogsComponent();
    }

    getCookieInfo() {
        return this.cookieInfo;
    }

    registerCookieInfo(cookieInfo: CookieInfoComponent) {
        this.cookieInfo = cookieInfo;
    }

    registerAccessibility() {
        this.skipNav.register('ACCESSIBILITY_SETTINGS', () =>
            this.dialogs.openAccessibilityDialog(),
        );
    }

    getMainNav() {
        return this.mainnav;
    }

    registerMainNav(maiNnav: MainNavComponent) {
        this.mainnav = maiNnav;
    }

    /**
     * Configures the `MainNavComponent`, using defaults for omitted values.
     */
    setMainNavConfig(config: MainNavConfig): void {
        this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED, config.currentScope);
        this.mainNavConfigSubject.next({
            ...new MainNavConfig(),
            ...config,
        });
        setTimeout(() => {
            void this.getMainNav()?.refreshBanner();
        });
    }

    /**
     * Updates the configuration of `MainNavComponent`, leaving omitted values as they were before.
     */
    patchMainNavConfig(config: Partial<MainNavConfig>): void {
        this.mainNavConfigSubject.next({
            ...this.mainNavConfigSubject.value,
            ...config,
        });
    }

    /**
     * Override individual values for the entire application, independently of what values are given
     * with `setMainNavConfig` and `patchMainNavConfig`.
     */
    globallyOverrideMainNavConfig(config: Partial<MainNavConfig>): void {
        this.mainNavConfigOverrideSubject.next(config);
    }

    observeMainNavConfig(): Observable<MainNavConfig> {
        return rxjs
            .combineLatest([this.mainNavConfigSubject, this.mainNavConfigOverrideSubject])
            .pipe(map(([config, override]) => ({ ...config, ...(override ?? {}) })));
    }

    get isVisible(): boolean {
        return this._isVisible;
    }

    get systemMessage(): SystemMessageDetails {
        return this._systemMessage();
    }

    setVisible(isVisible: boolean) {
        this._isVisible = isVisible;
        this.updateHeight();
    }
    setSystemMessage(systemMessage: SystemMessageDetails) {
        this._systemMessage.set(systemMessage);
    }
    updateHeight(height = 0) {
        if (height) {
            this.lastHeight = height;
        }
        if (this._isVisible) {
            if (!height) {
                height = this.lastHeight;
            }
            document.documentElement.style.setProperty('--mainnavHeight', height + 'px');
            //document.documentElement.style.setProperty('--mainnavCurrentHeight', null);
        } else {
            // Override relevant css variables.
            document.documentElement.style.setProperty('--mainnavHeight', '0');
            //document.documentElement.style.setProperty('--mainnavCurrentHeight', '0');
        }
    }

    /**
     * register additional custom scopes (global areas/pages)
     * Might be used by components that want to offer a list of available scopes for config purposes
     */
    setCustomScopes(scopes: string[]) {
        this.customScopes = scopes;
    }

    getAvailableScopes() {
        return [...this.DefaultScopes, ...(this.customScopes || [])];
    }

    /**
     * observe the current system message that should be displayed (if any)
     */
    observeSystemMessage(): Observable<SystemMessageDetails> {
        return rxjs
            .combineLatest([
                this.observeMainNavConfig().pipe(
                    startWith(null),
                    pairwise(),
                    filter(([a, b]) => a?.currentScope !== b.currentScope),
                    map(([_, c]) => c),
                ),
                this.user
                    .observeCurrentUser()
                    .pipe(switchMap((_) => this.configServiceApi.observeSystemMessages())),
            ])
            .pipe(
                debounceTime(0),
                switchMap(([config, messages]: [MainNavConfig, RepositoryMessage[]]) => {
                    // console.log('new messages', messages, config);
                    if (!messages.length) {
                        return of(null);
                    }
                    const messageObservables = messages.map((message) => {
                        const storageKey = message.components?.length
                            ? 'systemMessage_' + config?.currentScope
                            : 'systemMessage';
                        const details = {
                            message,
                            storageKey,
                        } as SystemMessageDetails;
                        return forkJoin([
                            this.sessionStorageService
                                .observe(storageKey, null, Store.UserProfile)
                                .pipe(take(1)),
                            this.sessionStorageService
                                .observe(storageKey, null, Store.Session)
                                .pipe(take(1)),
                        ]).pipe(
                            map(([userStorage, sessionStorage]) => {
                                let status: Status = 'new';
                                if (
                                    message.components?.length &&
                                    !message.components.includes(config?.currentScope)
                                ) {
                                    status = 'other-scope';
                                }
                                // msg already hidden by user
                                if (
                                    message.uuid === userStorage ||
                                    message.uuid === sessionStorage
                                ) {
                                    status = 'shown';
                                }
                                return { ...details, status };
                            }),
                        );
                    });
                    return forkJoin(messageObservables).pipe(
                        map((results) => results.find((r) => r.status !== 'other-scope')),
                    );
                }),
                tap(async (details) => {
                    // console.log('new message data', details);
                    if (!details || details?.status === 'shown') {
                        this.setSystemMessage(null);
                        return;
                    }
                    if (details.message.repeat === 'once') {
                        void this.sessionStorageService.set(
                            details.storageKey,
                            details.message.uuid,
                        );
                        void this.sessionStorageService.set(
                            details.storageKey,
                            details.message.uuid,
                            Store.Session,
                        );
                    }
                    this.setSystemMessage(details);

                    if (details?.message?.mode === 'modal') {
                        const dialogRef = await this.dialogs.openGenericDialog({
                            title: 'NOTICE',
                            avatar: {
                                kind: 'icon',
                                icon: 'info',
                            },
                            message: details.message.message,
                            messageMode: 'html',
                            buttons: CLOSE,
                            minWidth: 600,
                            maxWidth: 800,
                        });
                        dialogRef.afterClosed().subscribe((response) => {
                            if (details.message.repeat === 'repeat') {
                                void this.sessionStorageService.set(
                                    details.storageKey,
                                    details.message.uuid,
                                    Store.Session,
                                );
                            }
                        });
                    }
                }),
            );
    }

    closeSystemMessage() {
        void this.sessionStorageService.set(
            this.systemMessage.storageKey,
            this.systemMessage.message.uuid,
            Store.Session,
        );
        this.setSystemMessage(null);
    }
}
