import { Injectable, TemplateRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node, SessionStorageService, Store } from 'ngx-edu-sharing-api';

/**
 * Custom templates to replace or extend standard components within the search page.
 */
export interface CollectionsPageCustomTemplates {
    belowTabs?: TemplateRef<unknown>;
}

/**
 * Singleton service for public interfacing with the collections page.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageService {
    constructor(
        private internal: GlobalCollectionsPageServiceInternal,
        private sessionStorageService: SessionStorageService,
    ) {}

    /**
     * Register custom templates to replace or extend standard components within the search page.
     */
    setCustomTemplates(customTemplates: CollectionsPageCustomTemplates): void {
        this.internal.customTemplates.next(customTemplates);
    }

    async removeTemporaryCollections(nodes: Node[]) {
        const collections = await this.sessionStorageService.get<Node[]>(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            [],
            Store.Session,
        );
        await this.sessionStorageService.set(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            collections.filter((c) => !nodes.find((n) => c.ref.id === n.ref.id)),
            Store.Session,
        );
    }
}

/**
 * Internal part of the `GlobalSearchPageService` for use within the search page component and
 * services only.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageServiceInternal {
    readonly customTemplates = new BehaviorSubject<CollectionsPageCustomTemplates>({});
}
