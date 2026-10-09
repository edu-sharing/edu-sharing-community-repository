import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { ScopeQuery, ScopesService } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, InfoMessageComponent, Toast } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { navigateToBaseQuerySearch } from './scope-base-query-navigation';

/**
 * Main component of the editorial page (scope mode): the base query (candidate queries) of the
 * active scope. Queries are edited on the search page (MVP).
 */
@Component({
    selector: 'es-scope-settings',
    templateUrl: 'scope-settings.component.html',
    styleUrls: ['scope-settings.component.scss'],
    imports: [TranslateModule, MatButtonModule, EduSharingUiCommonModule, InfoMessageComponent],
})
export class ScopeSettingsComponent {
    private scopesService = inject(ScopesService);
    private router = inject(Router);
    private toast = inject(Toast);

    readonly scope = this.scopesService.activeScope;
    readonly queries = computed<ScopeQuery[]>(
        () => this.scope()?.definition?.baseQuery?.queries ?? [],
    );
    readonly version = computed(() => this.scope()?.definition?.baseQuery?.version ?? 0);
    readonly saving = signal(false);

    describe(query: ScopeQuery): string[] {
        return Object.entries(query.criteria ?? {}).map(([key, values]) =>
            key === RestConstants.PRIMARY_SEARCH_CRITERIA
                ? '"' + values.join(', ') + '"'
                : key + ': ' + values.join(', '),
        );
    }

    edit(index: number | 'new') {
        const scopeId = this.scope()?.scopeId;
        if (!scopeId) {
            return;
        }
        void navigateToBaseQuerySearch(
            this.router,
            scopeId,
            index,
            index === 'new' ? null : this.queries()[index],
            this.router.url,
        );
    }

    async remove(index: number) {
        const scope = this.scope();
        if (!scope) {
            return;
        }
        this.saving.set(true);
        try {
            await firstValueFrom(
                this.scopesService.setBaseQuery(scope.scopeId, {
                    ...scope.definition.baseQuery,
                    queries: this.queries().filter((_, i) => i !== index),
                }),
            );
            this.toast.toast('EDITORIAL.SCOPE.BASE_QUERY.REMOVED');
        } catch (e) {
            this.toast.error(e);
        } finally {
            this.saving.set(false);
        }
    }
}
