import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { DEFAULT, ScopeQuery, ScopesService } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, Toast, UIConstants } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { SCOPE_BASE_QUERY_PARAMS } from '../../editorial-page/scope-settings/scope-base-query-navigation';

/**
 * Shown on the search page while it is used to edit a query of the base query of a scope
 * (MVP, see `navigateToBaseQuerySearch`): saves the current search criteria as that query or
 * cancels, and returns to the editorial page in both cases.
 */
@Component({
    selector: 'es-scope-base-query-banner',
    templateUrl: 'scope-base-query-banner.component.html',
    styleUrls: ['scope-base-query-banner.component.scss'],
    imports: [TranslateModule, MatButtonModule, EduSharingUiCommonModule],
})
export class ScopeBaseQueryBannerComponent {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private scopesService = inject(ScopesService);
    private toast = inject(Toast);

    private readonly queryParams = toSignal(this.route.queryParams);
    readonly scopeId = computed(() => this.queryParams()?.[SCOPE_BASE_QUERY_PARAMS.scopeId]);
    readonly isNew = computed(() => this.queryParams()?.[SCOPE_BASE_QUERY_PARAMS.index] === 'new');
    readonly scopeName = computed(() => {
        const scope = this.scopesService.activeScope();
        return scope?.scopeId === this.scopeId() ? scope.definition?.displayName : this.scopeId();
    });
    readonly saving = signal(false);

    async save() {
        const scope = this.scopesService.activeScope();
        if (!scope || scope.scopeId !== this.scopeId()) {
            // the base query is part of the active scope's definition; editing another scope
            // would overwrite its queries we don't know
            this.toast.error(null, 'SEARCH.SCOPE_BASE_QUERY.SCOPE_NOT_ACTIVE');
            return;
        }
        const params = this.queryParams() ?? {};
        const criteria: { [key: string]: string[] } = JSON.parse(params.filters || '{}');
        if (params.q) {
            criteria[RestConstants.PRIMARY_SEARCH_CRITERIA] = [params.q];
        }
        const query: ScopeQuery = {
            mds: params.mds || DEFAULT,
            query: RestConstants.DEFAULT_QUERY_NAME,
            criteria,
        };
        const queries = [...(scope.definition?.baseQuery?.queries ?? [])];
        const index = parseInt(params[SCOPE_BASE_QUERY_PARAMS.index], 10);
        if (this.isNew() || isNaN(index) || index >= queries.length) {
            queries.push(query);
        } else {
            queries[index] = query;
        }
        this.saving.set(true);
        try {
            await firstValueFrom(
                this.scopesService.setBaseQuery(scope.scopeId, {
                    ...scope.definition?.baseQuery,
                    queries,
                }),
            );
            this.toast.toast('SEARCH.SCOPE_BASE_QUERY.SAVED');
            void this.goBack();
        } catch (e) {
            this.toast.error(e);
        } finally {
            this.saving.set(false);
        }
    }

    goBack() {
        const returnUrl = this.queryParams()?.[SCOPE_BASE_QUERY_PARAMS.returnUrl];
        // only internal targets
        if (returnUrl?.startsWith('/')) {
            return this.router.navigateByUrl(returnUrl);
        }
        return this.router.navigate([UIConstants.ROUTER_PREFIX, 'editorial', 'scope'], {
            queryParams: { mainComponent: 'scopeSettings' },
        });
    }
}
