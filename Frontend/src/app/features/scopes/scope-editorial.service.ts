import { Injectable, signal } from '@angular/core';
import { ScopeQuery } from 'ngx-edu-sharing-api';

/**
 * Target of an editorial change inside the active scope when it is not the current selection of the
 * editorial sidebar: all hits of a search (bulk with `search` instead of `nodeIds`).
 */
export type ScopeEditorialSearchTarget = {
    search: ScopeQuery;
    /** number of hits, shown to the user before the bulk job is started */
    count: number;
};

/**
 * Shared state between the hosts (editorial page, search) and the scope editorial sidebar option
 * (`SCOPE_EDITORIAL`).
 */
@Injectable({
    providedIn: 'root',
})
export class ScopeEditorialService {
    /** id of the editorial sidebar option that edits the entries of the active scope */
    static readonly SIDEBAR_OPTION = 'SCOPE_EDITORIAL';

    /**
     * set by a host to edit all hits of a search instead of the selected nodes; the sidebar option
     * clears it when it is closed
     */
    readonly searchTarget = signal<ScopeEditorialSearchTarget | null>(null);

    /** emits (increments) after entries were changed, so hosts can refresh their lists */
    readonly changed = signal(0);

    /**
     * open the scope editorial sidebar option for all hits of a search. The sidebar reacts on
     * `searchTarget` and activates `SIDEBAR_OPTION` (wired by the sidebar option itself)
     */
    openForSearch(target: ScopeEditorialSearchTarget) {
        this.searchTarget.set(target);
    }

    notifyChanged() {
        this.changed.update((c) => c + 1);
    }
}
