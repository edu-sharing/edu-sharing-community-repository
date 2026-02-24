import {
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import {
    ConfigService,
    Node,
    RepositoryMessage,
    SessionStorageService,
    Store,
    User,
    UserService,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { ConfigurationService, RestConnectorService } from '../../../core-module/core.module';
import { OptionItem } from 'ngx-edu-sharing-ui';
import { CreateMenuComponent } from '../create-menu/create-menu.component';
import { MainMenuDropdownComponent } from '../main-menu-dropdown/main-menu-dropdown.component';
import { MainMenuSidebarComponent } from '../main-menu-sidebar/main-menu-sidebar.component';
import { MainNavCreateConfig, MainNavService, TemplateSlot } from '../main-nav.service';
import { debounceTime, map, switchMap, take } from 'rxjs/operators';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { CLOSE } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';

@Component({
    selector: 'es-top-bar',
    templateUrl: './top-bar.component.html',
    styleUrls: ['./top-bar.component.scss'],
    standalone: false,
})
export class TopBarComponent {
    readonly TemplateSlot = TemplateSlot;
    @ContentChild('createButton') createButtonRef: TemplateRef<any>;
    @ViewChild('createMenu') createMenu: CreateMenuComponent;
    @ViewChild('dropdownTriggerDummy') createMenuTrigger: MatMenuTrigger;
    @ViewChild('mainMenuDropdown') mainMenuDropdown: MainMenuDropdownComponent;
    @ViewChild('mainMenuSidebar') mainMenuSidebar: MainMenuSidebarComponent;
    @ViewChild('userRef') userRef: ElementRef;
    @ViewChild('topbar') topbarRef: ElementRef;

    @Input() autoLogoutTimeout$: Observable<string>;
    @Input() canOpen = true;
    @Input() chatCount: number;
    @Input() config: any;
    @Input() create: MainNavCreateConfig;
    @Input() currentScope: string;
    @Input() currentUser: User;
    @Input() isCreateAllowed: boolean;
    @Input() isSafe: boolean;
    @Input() mainMenuStyle: 'sidebar' | 'dropdown' = 'sidebar';
    @Input() searchEnabled: boolean;
    @Input() showChat: boolean;
    @Input() showScope = true;
    @Input() showUser: boolean;
    @Input() title: string;
    @Input() userMenuOptions: OptionItem[];

    @Output() created = new EventEmitter<Node[]>();
    @Output() createNotAllowed = new EventEmitter<void>();
    @Output() openChat = new EventEmitter<void>();
    @Output() showLicenses = new EventEmitter<void>();
    @Output() closeScopeSelector = new EventEmitter<void>();

    createMenuX: number;
    createMenuY: number;
    toggleSidebar = () => this.mainMenuSidebar.toggle();

    constructor(
        // FIXME: Required values should be passed as inputs.
        public connector: RestConnectorService,
        private configService: ConfigurationService,
        public mainNavService: MainNavService,
        public dialogs: DialogsService,
        private user: UserService,
        private sessionStorageService: SessionStorageService,
        private configServiceApi: ConfigService,
        public elementRef: ElementRef,
    ) {
        this.registerSystemMessages();
    }

    getIconSource() {
        return this.configService.instant('mainnav.icon.url', 'assets/images/edu-white.svg');
    }

    private registerSystemMessages() {
        rxjs.combineLatest([
            this.sessionStorageService
                .observe('systemMessage', null, Store.UserProfile)
                .pipe(take(1)),
            this.sessionStorageService.observe('systemMessage', null, Store.Session).pipe(take(1)),
            this.user.observeCurrentUser(),
        ])
            .pipe(
                debounceTime(0),
                switchMap(([configProfile, configSession, user]) =>
                    this.configServiceApi
                        .observeSystemMessage()
                        .pipe(
                            map((newSystemMessage) => [
                                configProfile,
                                configSession,
                                newSystemMessage,
                            ]),
                        ),
                ),
            )
            .subscribe(
                async ([configProfile, configSession, msg]: [
                    string,
                    string,
                    RepositoryMessage,
                ]) => {
                    console.log('new message', msg);
                    if (!msg) {
                        return;
                    }
                    if (configProfile === msg.uuid || configSession === msg.uuid) {
                        console.info('msg already shown', msg);
                        this.mainNavService.setSystemMessage(null);
                        return;
                    }
                    if (msg.repeat === 'once') {
                        void this.sessionStorageService.set('systemMessage', msg.uuid);
                        void this.sessionStorageService.set(
                            'systemMessage',
                            msg.uuid,
                            Store.Session,
                        );
                    }
                    this.mainNavService.setSystemMessage(msg);
                    if (msg.mode === 'modal') {
                        const dialogRef = await this.dialogs.openGenericDialog({
                            title: 'NOTICE',
                            avatar: {
                                kind: 'icon',
                                icon: 'info',
                            },
                            message: msg.message,
                            messageMode: 'html',
                            buttons: CLOSE,
                            minWidth: 600,
                            maxWidth: 800,
                        });
                        dialogRef.afterClosed().subscribe((response) => {
                            if (msg.repeat === 'repeat') {
                                void this.sessionStorageService.set(
                                    'systemMessage',
                                    msg.uuid,
                                    Store.Session,
                                );
                            }
                        });
                    }
                },
            );
    }
    toggleMenuSidebar() {
        if (this.canOpen) {
            if (this.mainMenuSidebar) {
                this.mainMenuSidebar.toggle();
            } else if (this.mainMenuDropdown) {
                this.mainMenuDropdown.dropdown.menuTrigger.openMenu();
            }
        }
    }

    isSidenavOpen() {
        return this.mainMenuSidebar?.show;
    }

    openCreateMenu(x: number, y: number) {
        this.createMenuX = x;
        this.createMenuY = y;

        void this.createMenu.updateOptions();
        this.createMenuTrigger.openMenu();
        this.createMenuTrigger.onMenuClose;
    }

    sizeChanged() {
        this.mainNavService.updateHeight(
            this.topbarRef.nativeElement?.getBoundingClientRect().height,
        );
    }

    hideMessage() {
        void this.sessionStorageService.set(
            'systemMessage',
            this.mainNavService.systemMessage.uuid,
            Store.Session,
        );
    }
}
