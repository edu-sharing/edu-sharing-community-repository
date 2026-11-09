import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { retrievePageConfigPropagateRef } from './template-util';

/** What reaches a collection from above: who passes templates on, or who stopped them. */
export type ResolvedInheritance = { source: Node | null; stoppedAt: Node | null };

/** Whether the collection passes no templates on to its sub-collections, its own included. */
export function stopsPropagation(collection: Node): boolean {
    return (
        collection.properties?.[RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_STOP]?.[0] === 'true'
    );
}

/**
 * Resolves the inheritance from the ancestors, nearest first (the collection itself may be
 * included): the nearest one with templates is the source, unless it or one below it stops.
 */
export function resolveInheritance(collection: Node, parents: Node[]): ResolvedInheritance {
    let stoppedAt: Node | null = null;
    for (const parent of parents) {
        if (parent.ref.id === collection.ref.id) {
            continue;
        }
        if (retrievePageConfigPropagateRef(parent)) {
            if (!stoppedAt && stopsPropagation(parent)) {
                stoppedAt = parent;
            }
            return stoppedAt ? { source: null, stoppedAt } : { source: parent, stoppedAt: null };
        }
        if (!stoppedAt && stopsPropagation(parent)) {
            stoppedAt = parent;
        }
    }
    return { source: null, stoppedAt: null };
}

/** The ancestor whose templates the collection inherits, see `resolveInheritance`. */
export function findInheritanceSource(collection: Node, parents: Node[]): Node | null {
    return resolveInheritance(collection, parents).source;
}
