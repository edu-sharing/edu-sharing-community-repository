import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { AuthenticationService, ScopeInfo, ScopesService } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { Toast } from '../../../services/toast';

/**
 * Switches the active scope of the session (see scopes concept §2.1). Doubles as indicator of the
 * active scope in the main nav. Only rendered for signed in users while the feature is enabled.
 */
@Component({
    selector: 'es-scope-switcher',
    templateUrl: './scope-switcher.component.html',
    styleUrls: ['./scope-switcher.component.scss'],
    imports: [
        EduSharingUiCommonModule,
        MatButtonModule,
        MatMenuModule,
        MatTooltipModule,
        TranslateModule,
    ],
})
export class ScopeSwitcherComponent {
    readonly scopesService = inject(ScopesService);
    private authentication = inject(AuthenticationService);
    private toast = inject(Toast);

    private readonly loginInfo = toSignal(this.authentication.observeLoginInfo());
    readonly visible = computed(
        () =>
            this.scopesService.enabled() &&
            !!this.loginInfo()?.isValidLogin &&
            !this.loginInfo()?.isGuest,
    );
    readonly activeScope = this.scopesService.activeScope;
    readonly readOnly = computed(() => {
        const role = this.activeScope()?.role;
        return !!role && role !== 'EDITOR';
    });
    readonly scopes = signal<ScopeInfo[] | null>(null);
    readonly switching = signal(false);

    /** (re)load the switcher entries when the menu is opened, memberships may have changed */
    loadScopes() {
        this.scopesService.getAvailableScopes().subscribe({
            next: (scopes) => this.scopes.set(scopes ?? []),
            error: () => this.scopes.set([]),
        });
    }

    async select(scopeId: string | null) {
        if (scopeId === (this.activeScope()?.scopeId ?? null)) {
            return;
        }
        this.switching.set(true);
        try {
            await this.scopesService.setActiveScope(scopeId);
            // every page derives its data (search filter, permissions, dto values) from the active
            // scope; a full reload is the only way to not leak state of the previous scope from
            // page-level / root services
            window.location.reload();
        } catch (e) {
            this.switching.set(false);
            this.toast.error(e);
        }
    }

    /** temporary domain scopes can not be selected via the switcher, they are active by domain */
    isSelectable(scope: ScopeInfo) {
        return !scope.temporary;
    }
}
