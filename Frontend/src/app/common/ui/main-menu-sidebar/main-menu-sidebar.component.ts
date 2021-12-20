import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
} from '@angular/core';
import { Observable } from 'rxjs';
import {
    ConfigurationHelper,
    ConfigurationService,
    IamUser,
    LoginResult,
    RestConnectorService,
    RestIamService,
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import {
    MainMenuEntriesService,
    Entry,
} from '../../services/main-menu-entries.service';

@Component({
    selector: 'app-main-menu-sidebar',
    templateUrl: './main-menu-sidebar.component.html',
    styleUrls: ['./main-menu-sidebar.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fromLeft', UIAnimation.fromLeft()),
    ],
})
export class MainMenuSidebarComponent implements OnInit {
    @Input() currentScope: string;

    @Output() showLicenses = new EventEmitter<void>();
    @Output() openProfile = new EventEmitter<void>();

    // Internal state
    show = false;
    entries$: Observable<Entry[]>;

    // Global state, set on init
    loginInfo: LoginResult;
    user: IamUser;
    username: string;

    constructor(
        private configService: ConfigurationService,
        private connector: RestConnectorService,
        private iam: RestIamService,
        mainMenuEntries: MainMenuEntriesService,
    ) {
        this.entries$ = mainMenuEntries.entries$;
    }

    // Public methods, meant for invocation from outside this component.

    /**
     * Toggle the menu.
     */
    toggle() {
        this.show = !this.show;
    }

    // Internal methods, should only be called by this component.

    async ngOnInit() {
        this.loginInfo = await this.connector.isLoggedIn(false).toPromise();
        if (this.loginInfo.isValidLogin) {
            this.user = await this.iam.getCurrentUserAsync();
            this.username = await this.getUsername();
        }
    }

    /**
     * Listen to keyboard events on the sidebar.
     *
     * When the sidebar is shown, there is a focus trap in place that should prevent the focus from
     * ever leaving the sidebar, so this should catch keyboard events if and only if
     * `this.show == true`.
     */
    @HostListener('keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code === 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            this.hide();
        }
    }

    hide() {
        this.show = false;
    }

    onOpenProfile() {
        this.hide();
        this.openProfile.emit();
    }

    onShowLicenses() {
        this.hide();
        this.showLicenses.emit();
    }

    private async getUsername() {
        await this.configService.getAll().toPromise();
        return ConfigurationHelper.getPersonWithConfigDisplayName(
            this.user.person,
            this.configService,
        );
    }
}
