import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { ScopeQuery, ScopesService } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, InfoMessageComponent, Toast } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { EditorialBreadcrumbService } from '../editorial-breadcrumb/editorial-breadcrumb.service';
import { EditorialPageService } from '../editorial-page.service';
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
    private route = inject(ActivatedRoute);
    private toast = inject(Toast);
    private translate = inject(TranslateService);
    private editorialBreadcrumbService = inject(EditorialBreadcrumbService);
    private editorialPageService = inject(EditorialPageService);

    readonly scope = this.scopesService.activeScope;
    readonly queries = computed<ScopeQuery[]>(
        () => this.scope()?.definition?.baseQuery?.queries ?? [],
    );
    readonly version = computed(() => this.scope()?.definition?.baseQuery?.version ?? 0);
    readonly saving = signal(false);

    constructor() {
        // way back to the scope list: breadcrumb (Startseite > Redaktion > Basisabfrage) and close button
        effect(() => {
            const scope = this.scope()?.definition?.displayName;
            this.editorialBreadcrumbService.path.set([
                {
                    title: this.translate.instant('EDITORIAL.SCOPE.BASE_QUERY.TITLE', { scope }),
                },
            ]);
        });
        this.editorialPageService.close.set({ show: true, callback: () => this.close() });
        inject(DestroyRef).onDestroy(() => {
            this.editorialBreadcrumbService.path.set([]);
            this.editorialPageService.close.set({ show: false });
        });
    }

    close() {
        void this.router.navigate(['./'], {
            relativeTo: this.route,
            queryParams: { mainComponent: null },
            queryParamsHandling: 'merge',
        });
    }

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
