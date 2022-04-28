import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AccessibilityComponent } from '../../common/ui/accessibility/accessibility.component';
import { CookieInfoComponent } from '../../common/ui/cookie-info/cookie-info.component';
import { FrameEventsService, Node } from '../../core-module/core.module';
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
     * Show and enables the search field
     */
    searchEnabled?: boolean;
    /**
     * Shows the current location
     */
    showScope? = true;
    /**
     * Shows and enables the user menu
     */
    showUser? = true;
    /**
     * The placeholder text for the search field, will be translated
     */
    searchPlaceholder?: string;
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
    searchQuery?: string;
    currentScope: string;

    /**
     * If create is allowed, this event will fire the new nodes
     */
    onCreate?: (node: Node[]) => void;
    onCreateNotAllowed?: () => void;
    /**
     * Called when a search event happened, emits the search string and additional event info.
     */
    onSearch?: (query: string, cleared: boolean) => void;
    searchQueryChange?: (searchQuery: string) => void;
}

@Injectable({
    providedIn: 'root',
})
export class MainNavService {
    private mainnav: MainNavComponent;
    private cookieInfo: CookieInfoComponent;
    private accessibility: AccessibilityComponent;
    private mainNavConfigSubject = new BehaviorSubject<MainNavConfig>(new MainNavConfig());

    constructor(
        private dialogs: ManagementDialogsService,
        private event: FrameEventsService,
        private skipNav: SkipNavService,
    ) {}

    getDialogs() {
        return this.dialogs.getDialogsComponent();
    }

    getCookieInfo() {
        return this.cookieInfo;
    }

    getAccessibility() {
        return this.accessibility;
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

    /**
     * Configures the `MainNavComponent`, using defaults for omitted values.
     */
    setMainNavConfig(config: MainNavConfig): void {
        this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED, config.currentScope);
        this.mainNavConfigSubject.next({
            ...new MainNavConfig(),
            ...config,
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
