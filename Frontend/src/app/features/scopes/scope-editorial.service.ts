import { Injectable, signal } from '@angular/core';
import {
    Node,
    NodeScope,
    NodeWithScopes,
    RestConstants,
    ScopeAssignment,
    ScopeField,
    ScopeQuery,
} from 'ngx-edu-sharing-api';

/**
 * Target of an editorial change inside the active scope when it is not the current selection of the
 * editorial sidebar: all hits of a search (bulk with `search` instead of `nodeIds`).
 */
export type ScopeEditorialSearchTarget = {
    search: ScopeQuery;
    /** number of hits, shown to the user before the bulk job is started */
    count: number;
};

/** a single entry was changed (saved or removed) */
export type ScopeEntryChange = {
    scopeId: string;
    /** original node id */
    nodeId: string;
    /** null if the entry was removed */
    assignment: ScopeAssignment | null;
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

    /**
     * emits after a single entry was changed. Hosts update the node in place instead of searching
     * again: the index follows asynchronously anyway, and the selection (sidebar) is kept
     */
    readonly entryChanged = signal<ScopeEntryChange | null>(null);

    notifyChanged() {
        this.changed.update((c) => c + 1);
    }

    notifyEntryChanged(change: ScopeEntryChange) {
        this.entryChanged.set(change);
    }

    /**
     * copy of the node as the next search would return it: entry of the scope replaced, effective
     * values of the replacing fields in the properties (originals kept in the entry)
     */
    static applyEntryChange(node: Node, change: ScopeEntryChange, fields: ScopeField[]): Node {
        const scopes = (node as NodeWithScopes).scopes ?? [];
        const previous = scopes.find((s) => s.scopeId === change.scopeId);
        const properties = { ...node.properties };
        const originals: Record<string, string[]> = { ...previous?.originals };
        for (const field of fields.filter((f) => f.replaces)) {
            const value = change.assignment?.properties?.[field.id];
            if (value?.length) {
                originals[field.id] ??= node.properties?.[field.id] ?? [];
                properties[field.id] = value;
            } else if (originals[field.id]) {
                properties[field.id] = originals[field.id];
                delete originals[field.id];
            }
        }
        // an editorial change is a change of the content inside the scope (see ScopeNodeView)
        const modified = change.assignment?.modified ? new Date(change.assignment.modified) : null;
        if (modified && !isNaN(modified.getTime())) {
            properties[RestConstants.CM_MODIFIED_DATE] = [String(modified.getTime())];
            properties[RestConstants.CM_MODIFIED_DATE + 'ISO8601'] = [modified.toISOString()];
        }
        const entry: NodeScope | null = change.assignment
            ? {
                  ...previous,
                  scopeId: change.scopeId,
                  properties: change.assignment.properties,
                  published: change.assignment.published,
                  excluded: change.assignment.excluded,
                  originals,
              }
            : null;
        return {
            ...node,
            properties,
            scopes: [
                ...scopes.filter((s) => s.scopeId !== change.scopeId),
                ...(entry ? [entry] : []),
            ],
        } as Node;
    }
}
