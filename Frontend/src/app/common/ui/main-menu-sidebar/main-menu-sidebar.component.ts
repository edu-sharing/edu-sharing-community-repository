import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output
} from '@angular/core';
import {
    ConfigurationService,
    RestConnectorService, RestConstants, RestIamService,
    UIConstants
} from '../../../core-module/core.module';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import {LoginInfo, User, UserService} from 'ngx-edu-sharing-api';
import {takeUntil} from 'rxjs/operators';
import {Subject} from 'rxjs';

@Component({
    selector: 'es-main-menu-sidebar',
    templateUrl: './main-menu-sidebar.component.html',
    styleUrls: ['./main-menu-sidebar.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('fromLeft', UIAnimation.fromLeft()),
    ],
})
export class MainMenuSidebarComponent implements OnInit {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly ME = RestConstants.ME;
    private readonly destroyed$ = new Subject();

    @Input() currentScope: string;

    @Output() showLicenses = new EventEmitter<void>();

    // Internal state
    show = false;

    // Global state, set on init
    loginInfo: LoginInfo;
    currentUser: User;

    constructor(
        private configService: ConfigurationService,
        private connector: RestConnectorService,
        public iam: RestIamService,
        public user: UserService,
    ) {
        this.user
            .getCurrentUser()
            .pipe(takeUntil(this.destroyed$))
            .subscribe(async (currentUser) => {
                this.currentUser = currentUser.person;
            });
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

    onShowLicenses() {
        this.hide();
        this.showLicenses.emit();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
