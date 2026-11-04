import { Router } from '@angular/router';
import { DEFAULT, ScopeQuery } from 'ngx-edu-sharing-api';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';

/** query params of the search page while it edits a base query of a scope */
export const SCOPE_BASE_QUERY_PARAMS = {
    scopeId: 'scopeBaseQuery',
    /** index of the query inside the base query, or `new` */
    index: 'scopeBaseQueryIndex',
    returnUrl: 'scopeBaseQueryReturn',
} as const;

/**
 * Opens the search page to edit one query of the base query of a scope (MVP: the search criteria
 * become the stored criteria). The search page shows a banner to save or cancel and returns to
 * `returnUrl` afterwards.
 */
export function navigateToBaseQuerySearch(
    router: Router,
    scopeId: string,
    index: number | 'new',
    query: ScopeQuery | null,
    returnUrl: string,
) {
    const criteria = { ...(query?.criteria ?? {}) };
    const searchWord = criteria[RestConstants.PRIMARY_SEARCH_CRITERIA]?.[0];
    delete criteria[RestConstants.PRIMARY_SEARCH_CRITERIA];
    return router.navigate([UIConstants.ROUTER_PREFIX, 'search'], {
        queryParams: {
            q: searchWord || null,
            filters: Object.keys(criteria).length ? JSON.stringify(criteria) : null,
            mds: query?.mds && query.mds !== DEFAULT ? query.mds : null,
            [SCOPE_BASE_QUERY_PARAMS.scopeId]: scopeId,
            [SCOPE_BASE_QUERY_PARAMS.index]: String(index),
            [SCOPE_BASE_QUERY_PARAMS.returnUrl]: returnUrl,
        },
    });
}
