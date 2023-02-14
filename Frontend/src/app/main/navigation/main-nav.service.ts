import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CookieInfoComponent } from '../../common/ui/cookie-info/cookie-info.component';
import { FrameEventsService, Node } from '../../core-module/core.module';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { MainNavComponent } from '../../main/navigation/main-nav/main-nav.component';
import { ManagementDialogsService } from '../../modules/management-dialogs/management-dialogs.service';
import { SkipNavService } from './skip-nav/skip-nav.service';

export class MainNavCreateConfig {
    /** allowed / display new material button */
    allowed?: boolean | 'EMIT_EVENT' = false;
    /** refer to CreateMenuComponent */
    allowBinary?: boolean = true;
    parent?: Node = null;
    folder?: boolean = false;
}

export class MainNavConfig {
    /**
     * Show or hide the complete navigation
     */
    show? = true;
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
     * Hide the search field although it was enabled via `SearchFieldService`.
     *
     * Use if you include the search-field component yourself in your page.
     */
    hideSearchField? = false;

    /**
     * If create is allowed, this event will fire the new nodes
     */
    onCreate?: (node: Node[]) => void;
    onCreateNotAllowed?: () => void;
}

@Injectable({
    providedIn: 'root',
})
export class MainNavService {
    private mainnav: MainNavComponent;
    private cookieInfo: CookieInfoComponent;
    private mainNavConfigSubject = new BehaviorSubject<MainNavConfig>(new MainNavConfig());

    constructor(
        private managementDialogs: ManagementDialogsService,
        private event: FrameEventsService,
        private skipNav: SkipNavService,
        private dialogs: DialogsService,
    ) {}

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
            this.getMainNav()?.refreshBanner();
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

    observeMainNavConfig(): Observable<MainNavConfig> {
        return this.mainNavConfigSubject.asObservable();
    }
}
