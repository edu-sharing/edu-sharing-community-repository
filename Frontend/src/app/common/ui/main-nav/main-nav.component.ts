import {
    animate,
    keyframes,
    style,
    transition,
    trigger,
} from '@angular/animations';
import { HttpClient } from '@angular/common/http';
import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input, OnDestroy,
    Output,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BridgeService } from '../../../core-bridge-module/bridge.service';
import {
    About,
    ConfigurationHelper,
    ConfigurationService,
    DialogButton,
    FrameEventsService,
    IamUser,
    LoginResult,
    Node,
    NodeList,
    NodeTextContent,
    NodeWrapper,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    SessionStorageService,
    TemporaryStorageService, UIService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import {
    OPEN_URL_MODE,
    UIConstants,
} from '../../../core-module/ui/ui-constants';
import { OptionItem, OptionGroup } from '../../../core-ui-module/option-item';
import { Toast } from '../../../core-ui-module/toast';
import { Translation } from '../../../core-ui-module/translation';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { CreateMenuComponent } from '../../../modules/create-menu/create-menu.component';
import { WorkspaceManagementDialogsComponent } from '../../../modules/management-dialogs/management-dialogs.component';
import { MainMenuEntriesService } from '../../services/main-menu-entries.service';
import { GlobalContainerComponent } from '../global-container/global-container.component';
import { MainMenuSidebarComponent } from '../main-menu-sidebar/main-menu-sidebar.component';
import {MainMenuDropdownComponent} from '../main-menu-dropdown/main-menu-dropdown.component';
import {BehaviorSubject} from 'rxjs';
import {MainNavService} from '../../services/main-nav.service';

/**
 * The main nav (top bar + menus)
 */
@Component({
    selector: 'main-nav',
    templateUrl: 'main-nav.component.html',
    styleUrls: ['main-nav.component.scss'],
    providers: [MainMenuEntriesService],
    animations: [
        trigger('overlay', UIAnimation.openOverlay()),
        trigger('overlayBottom', UIAnimation.openOverlayBottom()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('fade', UIAnimation.fade()),
        trigger('nodeStore', [
            transition(':enter', [
                animate(
                    UIAnimation.ANIMATION_TIME_SLOW + 'ms ease-in',
                    keyframes([
                        style({
                            opacity: 0,
                            top: '0',
                            transform: 'scale(0.25)',
                            offset: 0,
                        }),
                        style({
                            opacity: 1,
                            top: '10px',
                            transform: 'scale(1)',
                            offset: 1,
                        }),
                    ]),
                ),
            ]),
            transition(':leave', [
                animate(
                    UIAnimation.ANIMATION_TIME_SLOW + 'ms ease-in',
                    keyframes([
                        style({ opacity: 1, transform: 'scale(1)', offset: 0 }),
                        style({
                            opacity: 0,
                            transform: 'scale(10)',
                            offset: 1,
                        }),
                    ]),
                ),
            ]),
        ]),
    ],
})
export class MainNavComponent implements AfterViewInit, OnDestroy {
    private static readonly ID_ATTRIBUTE_NAME = 'data-banner-id';

    @ViewChild('search') search: ElementRef;
    @ViewChild('topbar') topbar: ElementRef;
    @ViewChild('userRef') userRef: ElementRef;
    @ViewChild('tabNav') tabNav: ElementRef;
    @ViewChild('createMenu') createMenu: CreateMenuComponent;
    @ViewChild('dropdownTriggerDummy') createMenuTrigger: MatMenuTrigger;
    @ViewChild('mainMenuSidebar') mainMenuSidebar: MainMenuSidebarComponent;
    @ViewChild('mainMenuDropdown') mainMenuDropdown: MainMenuDropdownComponent;

    /**
     * Show and enables the search field
     */
    @Input() searchEnabled: boolean;
    /**
     * Shows the current location
     */
    @Input() showScope = true;
    /**
     * Shows and enables the user menu
     */
    @Input() showUser = true;
    /**
     * The placeholder text for the search field, will be translated
     */
    @Input() searchPlaceholder: string;
    /**
     * When true, the sidebar can be clicked to open the menu
     */
    @Input() canOpen = true;
    /**
     * The title on the left side, will be translated
     */
    @Input() title: string;
    /**
     * "add material" options
     */
    @Input() create: {
        allowed?: boolean;
        allowBinary?: boolean;
        parent?: Node;
        folder?: boolean;
    } = {
        // allowed / display new material button
        allowed: false,
        // refer to CreateMenuComponent
        allowBinary: true,
        parent: null,
        folder: false,
    };
    @Input() searchQuery: string;
    @Input() set currentScope(currentScope: string) {
        this._currentScope = currentScope;
        this.event.broadcastEvent(
            FrameEventsService.EVENT_VIEW_OPENED,
            currentScope,
        );
    }

    /**
     * If create is allowed, this event will fire the new nodes
     */
    @Output() onCreate = new EventEmitter<Node[]>();
    /**
     * Called when a search event happened, emits the search string and additional event info.
     */
    @Output() onSearch = new EventEmitter<{
        query: string;
        cleared: boolean;
    }>();
    @Output() searchQueryChange = new EventEmitter<string>();

    visible = false;
    createMenuX: number;
    createMenuY: number;
    timeout: string;
    timeIsValid = false;
    config: any = {};
    nodeStoreAnimation = 0;
    showNodeStore = false;
    acceptLicenseAgreement: boolean;
    licenseAgreement: boolean;
    licenseAgreementHTML: string;
    canEditProfile: boolean;
    userMenuOptions: OptionItem[];
    tutorialElement: ElementRef;
    globalProgress = false;
    showEditProfile: boolean;
    showProfile: boolean;
    _currentScope: string;
    _showUser = false;
    licenseDialog: boolean;
    showScrollToTop = false;
    licenseDetails: string;
    mainMenuStyle: 'sidebar' | 'dropdown' = 'sidebar';

    private editUrl: string;
    private nodeStoreCount = 0;
    private licenseAgreementNode: Node;
    private scrollInitialPositions: any[] = [];
    private lastScroll = -1;
    private elementsTopY = 0;
    private elementsBottomY = 0;
    private fixScrollElements = false;
    private about: About;


    /**
     * @Deprecated
     * Use the mainanv service getDialogs directly
     */
    get management() {
        return this.mainnavService.getDialogs();
    }

    constructor(
        public iam: RestIamService,
        public connector: RestConnectorService,
        private bridge: BridgeService,
        private event: FrameEventsService,
        private nodeService: RestNodeService,
        private configService: ConfigurationService,
        private uiService: UIService,
        private mainnavService: MainNavService,
        private storage: TemporaryStorageService,
        private session: SessionStorageService,
        private http: HttpClient,
        private router: Router,
        private route: ActivatedRoute,
        private toast: Toast,
    ) {
        this.mainnavService.registerMainNav(this);
        this.visible = !this.storage.get(
            TemporaryStorageService.OPTION_HIDE_MAINNAV,
            false,
        );
        this.setMenuStyle();

        this.connector.setRoute(this.route).subscribe(() => {
            this.connector.getAbout().subscribe(about => {
                this.about = about;
                this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
                    if (!data.isValidLogin) {
                        this.canOpen = data.isGuest;
                        this.checkConfig();
                        return;
                    }
                    setInterval(() => this.updateTimeout(), 1000);
                    this.route.queryParams.subscribe(async (params: Params) => {
                        if (params.noNavigation === 'true') {
                            this.canOpen = false;
                        }
                        this.showNodeStore = params.nodeStore === 'true';
                        this._showUser =
                            this._currentScope !== 'login' && this.showUser;
                        this.refreshNodeStore();
                        this.checkConfig();
                        const user = await this.iam.getCurrentUserAsync();
                        this.canEditProfile = user.editProfile;
                        this.configService.getAll().subscribe(() => {});
                    });
                });
            });
        });
        event.addListener(this);
    }

    ngAfterViewInit() {
        this.refreshBanner();
    }

    @HostListener('window:resize')
    onResize(event: any) {
        this.updateUserOptions();
    }

    @HostListener('window:scroll', ['$event'])
    @HostListener('window:touchmove', ['$event'])
    handleScroll(event: any) {
        if (
            this.storage.get(
                TemporaryStorageService.OPTION_DISABLE_SCROLL_LAYOUT,
                false,
            )
        ) {
            return;
        }
        // FIXME: These classes don't work properly when resizing the view causes the banner to
        // change height. To reproduce, load an affected extension (e.g. Lisum) and resize the
        // window to trigger the mobile menu switch. We have two problems:
        //
        // - Some updates happen only on first scroll after resize.
        // - Some updates happen only after the page is reloaded.
        //
        // Interim states are visually broken.
        const elementsScroll = document.getElementsByClassName(
            'scrollWithBanner',
        );
        const elementsAlign = document.getElementsByClassName(
            'alignWithBanner',
        );
        const elements: any = [];
        for (let i = 0; i < elementsScroll.length; i++) {
            elements.push(elementsScroll[i]);
        }
        for (let i = 0; i < elementsAlign.length; i++) {
            elements.push(elementsAlign[i]);
        }
        if (event == null) {
            // Re-init the positions, reset the elements
            this.scrollInitialPositions = [];
            for (let i = 0; i < elements.length; i++) {
                const element: any = elements[i];
                element.style.position = null;
                element.style.top = null;
                // Disable transition for instant refreshes
                element.style.transition = 'none';
            }
            // Give the browser layout engine some time to remove the values, otherwise the elements
            // will have not their initial positions
            setTimeout(() => {
                for (let i = 0; i < elements.length; i++) {
                    const element: any = elements[i];
                    element.style.transition = null;
                    if (
                        !element.getAttribute(
                            MainNavComponent.ID_ATTRIBUTE_NAME,
                        )
                    ) {
                        element.setAttribute(
                            MainNavComponent.ID_ATTRIBUTE_NAME,
                            Math.random(),
                        );
                    }
                    if (
                        this.scrollInitialPositions[
                            element.getAttribute(
                                MainNavComponent.ID_ATTRIBUTE_NAME,
                            )
                        ]
                    )
                        continue;
                    // getComputedStyle does report wrong values in search sidenav
                    this.scrollInitialPositions[
                        element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)
                    ] = window
                        .getComputedStyle(element)
                        .getPropertyValue('top');
                }
                this.posScrollElements(event, elements);
            });
        } else {
            this.handleScrollHide();
            this.posScrollElements(event, elements);
        }
    }

    toggleMenuSidebar() {
        if (this.canOpen) {

            if(this.mainMenuSidebar) {
                this.mainMenuSidebar.toggle();
            } else if(this.mainMenuDropdown) {
                this.mainMenuDropdown.dropdown.menuTrigger.openMenu();
            }
        }
    }

    posScrollElements(event: Event, elements: any[]) {
        let y = 0;
        try {
            const rect = document
                .getElementsByTagName('header')[0]
                .getBoundingClientRect();
            y = rect.bottom - rect.top;
            // Set min height + a small increase of height to prevent flickering in chrome
            document.documentElement.style.minHeight =
                'calc(100% + ' + (y + 10) + 'px)';
        } catch (e) {}
        for (let i = 0; i < elements.length; i++) {
            const element: any = elements[i];
            if (y === 0) {
                element.style.position = null;
                element.style.top = null;
                continue;
            }
            if (element.className.indexOf('alignWithBanner') !== -1) {
                element.style.position = 'relative';
                if (event == null) {
                    element.style.top = y + 'px';
                }
            } else if (
                (window.pageYOffset || document.documentElement.scrollTop) > y
            ) {
                element.style.position = 'fixed';
                element.style.top = this.scrollInitialPositions[
                    element.getAttribute(MainNavComponent.ID_ATTRIBUTE_NAME)
                ];
            } else {
                element.style.position = 'absolute';
                element.style.top =
                    Number.parseInt(
                        this.scrollInitialPositions[
                            element.getAttribute(
                                MainNavComponent.ID_ATTRIBUTE_NAME,
                            )
                        ],
                        10,
                    ) +
                    y +
                    'px';
            }
        }
        this.showScrollToTop =
            (window.pageYOffset || document.documentElement.scrollTop) > 400;
    }

    setNodeStore(value: boolean) {
        UIHelper.changeQueryParameter(
            this.router,
            this.route,
            'nodeStore',
            value,
        );
    }

    refreshNodeStore() {
        this.iam
            .getNodeList(RestConstants.NODE_STORE_LIST)
            .subscribe((data: NodeList) => {
                if (
                    data.nodes.length - this.nodeStoreCount > 0 &&
                    this.nodeStoreAnimation === -1
                ) {
                    this.nodeStoreAnimation =
                        data.nodes.length - this.nodeStoreCount;
                }
                this.nodeStoreCount = data.nodes.length;
                setTimeout(() => {
                    this.nodeStoreAnimation = -1;
                }, 1500);
            });
    }

    onEvent(event: string, data: any) {
        if (event === FrameEventsService.EVENT_PARENT_SEARCH) {
            this.doSearch(data, false);
        }
    }

    openProfileDialog() {
        this.showProfile = true;
    }

    openProfile() {
        this.router.navigate([
            UIConstants.ROUTER_PREFIX + 'profiles',
            RestConstants.ME,
        ]);
    }

    refreshBanner() {
        setTimeout(() => this.handleScroll(null));
    }

    scrollToTop() {
        UIHelper.scrollSmooth(0);
    }

    editProfile() {
        if (this.bridge.isRunningCordova()) {
            window.open(this.editUrl, '_system');
        } else {
            window.location.href = this.editUrl;
        }
    }

    showHelp(url: string) {
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }

    getIconSource() {
        return this.configService.instant(
            'mainnav.icon.url',
            'assets/images/edu-white.svg',
        );
    }

    saveLicenseAgreement() {
        this.licenseAgreement = false;
        if (this.licenseAgreementNode) {
            this.session.set(
                'licenseAgreement',
                this.licenseAgreementNode.content.version,
            );
        } else {
            this.session.set('licenseAgreement', '0.0');
        }
        this.startTutorial();
    }

    async startTutorial() {
        if (this.connector.getCurrentLogin().statusCode === RestConstants.STATUS_CODE_OK) {
            const user = await this.iam.getCurrentUserAsync();
            if (user.editProfile && this.configService.instant('editProfile', false)) {
                this.uiService.waitForComponent(this, 'userRef').subscribe(() => {
                    this.tutorialElement = this.userRef;
                });
            }
        }
    }

    setFixMobileElements(fix: boolean) {
        this.fixScrollElements = fix;
        this.handleScrollHide();
    }

    isSafe() {
        return (
            this.connector.getCurrentLogin()?.currentScope ===
                RestConstants.SAFE_SCOPE
        );
    }

    showLicenses() {
        this.licenseDialog = true;
        this.http
            .get('assets/licenses/en.html', { responseType: 'text' })
            .subscribe(
                text => {
                    this.licenseDetails = text as any;
                },
                error => {
                    console.error(error);
                },
            );
    }

    showChat() {
        return GlobalContainerComponent.instance?.rocketchat?._data;
    }

    getChatCount() {
        return GlobalContainerComponent.instance?.rocketchat?.unread;
    }

    openChat() {
        GlobalContainerComponent.instance.rocketchat.opened = true;
        GlobalContainerComponent.instance.rocketchat.unread = 0;
    }

    getPreloading() {
        return GlobalContainerComponent.getPreloading();
    }

    isCreateAllowed() {
        // @TODO: May Check for more constrains
        return this.create.allowed && !this.connector.getCurrentLogin()?.isGuest;
    }

    openCreateMenu(x: number, y: number) {
        this.createMenuX = x;
        this.createMenuY = y;

        this.createMenu.updateOptions();
        this.createMenuTrigger.openMenu();
        this.createMenuTrigger.onMenuClose;
    }

    clearSearch() {
        this.searchQuery = '';
        this.searchQueryChange.emit('');
        this.onSearch.emit({ query: '', cleared: true });
    }

    logout() {
        this.globalProgress = true;
        this.uiService.handleLogout().subscribe(() => this.finishLogout());
    }

    private login(reurl = false) {
        RestHelper.goToLogin(
            this.router,
            this.configService,
            '',
            reurl ? window.location.href : '',
        );
    }

    doSearch(
        value = this.search.nativeElement.value,
        broadcast = true,
    ) {
        if (broadcast) {
            this.event.broadcastEvent(
                FrameEventsService.EVENT_GLOBAL_SEARCH,
                value,
            );
        }
        this.onSearch.emit({ query: value, cleared: false });
    }

    openImprint() {
        UIHelper.openUrl(
            this.config.imprintUrl,
            this.bridge,
            OPEN_URL_MODE.BlankSystemBrowser,
        );
    }

    openPrivacy() {
        UIHelper.openUrl(
            this.config.privacyInformationUrl,
            this.bridge,
            OPEN_URL_MODE.BlankSystemBrowser,
        );
    }

    private checkConfig() {
        this.configService.getAll().subscribe((data: any) => {
            this.config = data;
            this.editUrl = data.editProfileUrl;
            this.showEditProfile = data.editProfile;
            this.showLicenseAgreement();
            this.updateUserOptions();
        });
    }

    private setMenuStyle() {
        this.configService.get('mainnav.mainMenuStyle').subscribe({
            next: (mainMenuStyle?: string) => {
                switch (mainMenuStyle) {
                    case 'sidebar':
                    case 'dropdown':
                        this.mainMenuStyle = mainMenuStyle;
                        break;
                    case undefined:
                    case null:
                        break;
                    default:
                        console.error(
                            `Unsupported value for config mainMenuStyle: ${mainMenuStyle}`,
                        );
                }
            },
        });
    }

    private finishLogout() {
        if (this.config.logout && this.config.logout.next) {
            window.location.href = this.config.logout.next;
        } else {
            this.login(false);
        }
        this.globalProgress = false;
    }

    private showLicenseAgreement() {
        if (
            !this.config.licenseAgreement ||
            this.connector.getCurrentLogin()?.isGuest ||
            !this.connector.getCurrentLogin().isValidLogin
        ) {
            this.startTutorial();
            return;
        }
        this.session
            .get('licenseAgreement', false)
            .subscribe((version: string) => {
                this.licenseAgreementHTML = null;
                let nodeId: string = null;
                for (const node of this.config.licenseAgreement.nodeId) {
                    if (node.language == null) nodeId = node.value;
                    if (node.language === Translation.getLanguage()) {
                        nodeId = node.value;
                        break;
                    }
                }
                this.nodeService.getNodeMetadata(nodeId).subscribe(
                    (data: NodeWrapper) => {
                        this.licenseAgreementNode = data.node;
                        if (version === data.node.content.version) {
                            this.startTutorial();
                            return;
                        }
                        this.licenseAgreement = true;
                        this.nodeService.getNodeTextContent(nodeId).subscribe(
                            (data: NodeTextContent) => {
                                this.licenseAgreementHTML = data.html
                                    ? data.html
                                    : data.raw
                                    ? data.raw
                                    : data.text;
                            },
                            (error: any) => {
                                this.licenseAgreementHTML = `Error loading content for license agreement node '${nodeId}'`;
                            },
                        );
                    },
                    (error: any) => {
                        if (version === '0.0') {
                            this.startTutorial();
                            return;
                        }
                        this.licenseAgreement = true;
                        this.licenseAgreementHTML = `Error loading metadata for license agreement node '${nodeId}'`;
                    },
                );
            });
    }

    private updateUserOptions() {
        this.userMenuOptions = [];
        if (!this.connector.getCurrentLogin()?.isGuest && !this.connector.getCurrentLogin()?.currentScope) {
            this.userMenuOptions.push(
                new OptionItem('EDIT_ACCOUNT', 'assignment_ind', () =>
                    this.openProfile(),
                ),
            );
            if(this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_SIGNUP_GROUP)) {
                this.userMenuOptions.push(
                    new OptionItem('SIGNUP_GROUP.TITLE', 'group_add', () => {
                        this.management.signupGroup = true;
                        this.management.signupGroupChange.emit(true);
                    })
                );
            }
        }
        if (this.connector.getCurrentLogin()?.isGuest) {
            if (this.config.loginOptions) {
                for (const login of this.config.loginOptions) {
                    this.userMenuOptions.push(
                        new OptionItem(
                            login.name,
                            '',
                            () => (window.location.href = login.url),
                        ),
                    );
                }
            } else {
                this.userMenuOptions.push(
                    new OptionItem('SIDEBAR.LOGIN', 'person', () =>
                        this.login(true),
                    ),
                );
            }
        }
        /*if (
            this._currentScope === 'workspace' ||
            this._currentScope === 'search' ||
            this._currentScope === 'stream' ||
            this._currentScope === 'collections'
        ) {*/
            const boomarkOption = new OptionItem(
                'SEARCH.NODE_STORE.TITLE',
                'bookmark_border',
                () => this.setNodeStore(true),
            );
            this.userMenuOptions.push(boomarkOption);
        // }
        const accessibilityOptions = new OptionItem(
            'OPTIONS.ACCESSIBILITY',
            'accessibility',
            () => {
                this.mainnavService.getAccessibility().show();
            },
        );
        this.userMenuOptions.push(accessibilityOptions);
        for (const option of this.getConfigMenuHelpOptions()) {
            this.userMenuOptions.push(option);
        }
        const infoGroup = new OptionGroup('info', 20);
        if (this.config.imprintUrl) {
            const option = new OptionItem('IMPRINT', 'info_outline', () =>
                this.openImprint(),
            );
            option.group = infoGroup;
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
            this.userMenuOptions.push(option);
        }
        if (this.config.privacyInformationUrl) {
            const option = new OptionItem(
                'PRIVACY_INFORMATION',
                'verified_user',
                () => this.openPrivacy(),
            );
            option.group = infoGroup;
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
            this.userMenuOptions.push(option);
        }
        const option = new OptionItem(
            'LICENSE_INFORMATION',
            'lightbulb_outline',
            () => this.showLicenses(),
        );
        option.group = infoGroup;
        if (this.mainMenuStyle === 'sidebar') {
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
        }
        this.userMenuOptions.push(option);

        if (!this.connector.getCurrentLogin()?.isGuest) {
            this.userMenuOptions.push(
                new OptionItem('LOGOUT', 'undo', () => this.logout()),
            );
        }
    }

    private getConfigMenuHelpOptions() {
        if (!this.config.helpMenuOptions) {
            console.warn(
                'config does not contain helpMenuOptions, will not display any options',
            );
            return [];
        }
        const versionParts = this.about.version.repository.split('.');
        const version = versionParts[0] + versionParts[1];
        const group = new OptionGroup('help', 10);
        return this.config.helpMenuOptions.map(
            (entry: { key: string; icon: string; url: string }) => {
                const option = new OptionItem(entry.key, entry.icon, () =>
                    window.open(entry.url.replace(':version', version)),
                );
                option.group = group;
                return option;
            },
        );
    }

    /**
     * Method to dynamically hide objects when scrolling on mobile
     * Add css class mobile-move-top or mobile-move-bottom for specific items
     */
    private handleScrollHide() {
        if (this.tabNav == null || this.tabNav.nativeElement == null) {
            return;
        }
        // Take the scroll position inside a viewport that was zoomed in using pinch-to-zoom into
        // account. This allows us to scroll bottom elements out of view, but is not really needed
        // for top elements. Using for both as long as no problems come up.
        const scrollY = (window as any).visualViewport?.pageTop ?? window.scrollY;
        if (this.lastScroll === -1) {
            this.lastScroll = scrollY;
            return;
        }
        const elementsTop: any = document.getElementsByClassName(
            'mobile-move-top',
        );
        const elementsBottom: any = document.getElementsByClassName(
            'mobile-move-bottom',
        );
        let top = -1;
        let bottom = -1;
        for (let i = 0; i < elementsTop.length; i++) {
            const rect = elementsTop.item(i).getBoundingClientRect();
            if (bottom === -1 || bottom < rect.bottom) {
                bottom = rect.bottom;
            }
        }
        for (let i = 0; i < elementsBottom.length; i++) {
            const rect = elementsBottom.item(i).getBoundingClientRect();
            if (top === -1 || top > rect.top) {
                top = rect.top;
            }
        }
        let diffTop = scrollY - this.lastScroll;
        let diffBottom = scrollY - this.lastScroll;
        if (diffTop < 0) {
            diffTop *= 2;
        }
        if (diffBottom < 0) {
            diffBottom *= 2;
        }

        // Don't move top elements any further up when they already lie above the screen.
        if (diffTop > 0 && bottom < 0) {
            diffTop = 0;
        }
        // Don't move bottom elements any further down when they already lie below the screen.
        if (diffBottom > 0 && top > window.innerHeight) {
            diffBottom = 0;
        }
        // Don't move bottom elements any further up when the page is zoomed in on mobile.
        if (diffBottom < 0 && (window as any).visualViewport?.scale > 1) {
            diffBottom = 0;
        }
        this.elementsTopY += diffTop;
        this.elementsTopY = Math.max(0, this.elementsTopY);
        this.elementsBottomY += diffBottom;
        this.elementsBottomY = Math.max(0, this.elementsBottomY);
        // For ios elastic scroll
        if (
            window.scrollY < 0 ||
            this.fixScrollElements ||
            !UIHelper.evaluateMediaQuery(
                UIConstants.MEDIA_QUERY_MAX_WIDTH,
                UIConstants.MOBILE_TAB_SWITCH_WIDTH,
            )
        ) {
            this.elementsTopY = 0;
            this.elementsBottomY = 0;
        }
        for (let i = 0; i < elementsTop.length; i++) {
            elementsTop.item(i).style.position = 'relative';
            elementsTop.item(i).style.top = -this.elementsTopY + 'px';
        }
        for (let i = 0; i < elementsBottom.length; i++) {
            elementsBottom.item(i).style.position = 'relative';
            elementsBottom.item(i).style.top = this.elementsBottomY + 'px';
        }
        this.lastScroll = scrollY;
    }

    private showTimeout() {
        return (
            !this.bridge.isRunningCordova() &&
            !this.connector.getCurrentLogin()?.isGuest &&
            this.timeIsValid &&
            this.timeout !== '' &&
            (this.isSafe() ||
                this.configService.instant('sessionExpiredDialog', {
                    show: true,
                }).show)
        );
    }

    private updateTimeout() {
        const time =
            this.connector.logoutTimeout -
            Math.floor(
                (new Date().getTime() - this.connector.lastActionTime) / 1000,
            );
        const min = Math.floor(time / 60);
        const sec = time % 60;
        this.event.broadcastEvent(
            FrameEventsService.EVENT_SESSION_TIMEOUT,
            time,
        );
        if (time >= 0) {
            this.timeout =
                this.formatTimeout(min, 2) + ':' + this.formatTimeout(sec, 2);
            this.timeIsValid = true;
        } else if (this.showTimeout()) {
            this.toast.showModalDialog(
                'WORKSPACE.AUTOLOGOUT',
                'WORKSPACE.AUTOLOGOUT_INFO',
                [
                    new DialogButton(
                        'WORKSPACE.RELOGIN',
                        DialogButton.TYPE_PRIMARY,
                        () => {
                            RestHelper.goToLogin(
                                this.router,
                                this.configService,
                                this.isSafe() ? RestConstants.SAFE_SCOPE : null,
                                null,
                            );
                            this.toast.closeModalDialog();
                        },
                    ),
                ],
                false,
                null,
                { minutes: Math.round(this.connector.logoutTimeout / 60) },
            );
            this.timeout = '';
        } else {
            this.timeout = '';
        }
    }

    private formatTimeout(num: number, size: number) {
        let s = num + '';
        while (s.length < size) {
            s = '0' + s;
        }
        return s;
    }

    getUserName() {
        return ConfigurationHelper.getPersonWithConfigDisplayName(
            this.iam.getCurrentUser(),
            this.configService,
        );
    }

    ngOnDestroy(): void {
        this.mainnavService.registerMainNav(null);
    }
}
