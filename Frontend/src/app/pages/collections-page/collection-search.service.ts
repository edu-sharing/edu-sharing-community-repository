import { inject, Injectable } from '@angular/core';
import { DEFAULT, Node, PROPERTY_FILTER_ALL } from 'ngx-edu-sharing-api';
import { firstValueFrom, from, Observable } from 'rxjs';
import {
    CollectionTreeService,
    PARENT_ID_PROPERTY,
} from '../../features/mds/mds-editor/widgets/mds-editor-widget-tree/collection-tree/collection-tree.service';
import { CollectionScope, CollectionScopesService } from '../../services/collection-scopes.service';

/** Most hits requested at once; the hits are filtered by category afterwards, so no paging. */
export const COLLECTION_SEARCH_MAX_HITS = 500;
/** Guards against a cycle in the parent links of corrupt data. */
const MAX_DEPTH = 50;

export interface CollectionSearchRequest {
    /** Category (tab) whose top-level collections the hits have to belong to. */
    scope?: string;
    /** Collection whose descendants are searched; takes the place of the category. */
    parentId?: string;
    /** Whether collections below the searched level are hits as well; defaults to `true`. */
    includeSubLevels?: boolean;
    sortProperties?: string[];
    sortAscending?: boolean[];
}

export interface CollectionSearchResult {
    hits: Node[];
    /** The collection each hit is placed in, by id of the hit; missing for top-level hits. */
    parents: Map<string, Node>;
    /**
     * The collections that lead from the searched level to the hits without being hits
     * themselves, so the hits can be shown below them.
     */
    ancestors: Node[];
    /**
     * The id of the collection a hit or an ancestor is placed in within the result, by id;
     * missing for the ones directly on the searched level.
     */
    parentIds: Map<string, string>;
    /** Whether more collections match than were requested. */
    truncated: boolean;
}

/**
 * Searches collections by name for the collections page.
 *
 * The search query knows no categories, but each category is defined by its top-level
 * collections. A hit belongs to a category when the top-most collection above it is one of them.
 */
@Injectable()
export class CollectionSearchService {
    private readonly treeService = inject(CollectionTreeService);
    private readonly collectionScopesService = inject(CollectionScopesService);
    /** Ids of the top-level collections per category, loaded once until `reset`. */
    private readonly topLevelIds = new Map<string, Promise<Set<string>>>();

    /** Forgets the loaded top-level collections, e.g. after a collection was created. */
    reset(): void {
        this.topLevelIds.clear();
    }

    search(term: string, request: CollectionSearchRequest): Observable<CollectionSearchResult> {
        return from(this.load(term, request));
    }

    private async load(
        term: string,
        request: CollectionSearchRequest,
    ): Promise<CollectionSearchResult> {
        const page = await firstValueFrom(
            this.treeService.searchCollectionsPage(term, {
                rootId: request.parentId ?? null,
                mdsId: DEFAULT,
                maxItems: COLLECTION_SEARCH_MAX_HITS,
                propertyFilter: [PROPERTY_FILTER_ALL, PARENT_ID_PROPERTY],
                sortProperties: request.sortProperties,
                sortAscending: request.sortAscending,
            }),
        );
        const placed = await firstValueFrom(this.treeService.getWithAncestors(page.nodes, ''));
        const getParentId = (id: string) => placed?.get(id)?.parentId ?? null;
        // A metadata set that cannot limit the search to a subtree is searched without the limit,
        // so the hits are checked against the collections above them as well.
        const isBelow = (id: string, ancestorId: string): boolean => {
            let current = getParentId(id);
            for (let depth = 0; current && depth < MAX_DEPTH; depth++) {
                if (current === ancestorId) {
                    return true;
                }
                current = getParentId(current);
            }
            return false;
        };
        const hits = page.nodes.filter(
            (node) => !request.parentId || isBelow(node.ref.id, request.parentId),
        );
        const parents = new Map<string, Node>();
        for (const hit of hits) {
            const parent = placed?.get(getParentId(hit.ref.id));
            if (parent) {
                parents.set(hit.ref.id, parent.node);
            }
        }
        let filtered = hits;
        // TODO: The search query has no notion of categories, so they are derived here from the
        // top-level collections of the category. Move this into the backend (a scope parameter
        // on the collections search) to get exact paging and totals and to drop the extra requests.
        if (!request.parentId && request.scope) {
            const topLevel = await this.getTopLevelIds(request.scope);
            filtered = hits.filter((hit) => {
                let current = hit.ref.id;
                for (let depth = 0; depth < MAX_DEPTH; depth++) {
                    const parentId = getParentId(current);
                    if (!parentId) {
                        // a collection above that is not readable ends the chain without a top
                        return !!placed?.has(current) && topLevel.has(current);
                    }
                    current = parentId;
                }
                return false;
            });
        }
        if (request.includeSubLevels === false) {
            const topLevel =
                !request.parentId && request.scope
                    ? await this.getTopLevelIds(request.scope)
                    : null;
            filtered = filtered.filter((hit) =>
                request.parentId
                    ? getParentId(hit.ref.id) === request.parentId
                    : !topLevel || topLevel.has(hit.ref.id),
            );
        }
        const hitIds = new Set(filtered.map((hit) => hit.ref.id));
        const ancestors = new Map<string, Node>();
        const parentIds = new Map<string, string>();
        for (const hit of filtered) {
            let id = hit.ref.id;
            for (let depth = 0; depth < MAX_DEPTH; depth++) {
                const parentId = getParentId(id);
                const parent = parentId ? placed?.get(parentId) : null;
                if (!parent || parentId === request.parentId) {
                    break;
                }
                parentIds.set(id, parentId);
                if (!hitIds.has(parentId)) {
                    ancestors.set(parentId, parent.node);
                }
                id = parentId;
            }
        }
        return {
            hits: filtered,
            parents,
            ancestors: [...ancestors.values()],
            parentIds,
            truncated: page.total > page.nodes.length,
        };
    }

    private getTopLevelIds(scope: string): Promise<Set<string>> {
        let ids = this.topLevelIds.get(scope);
        if (!ids) {
            ids = this.collectionScopesService.loadTopLevelIds(scope as CollectionScope);
            this.topLevelIds.set(scope, ids);
            // a failed request must not be remembered
            ids.catch(() => this.topLevelIds.delete(scope));
        }
        return ids;
    }
}
