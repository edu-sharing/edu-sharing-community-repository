import { Injectable, TemplateRef, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node, SessionStorageService, Store } from 'ngx-edu-sharing-api';

/**
 * Custom templates to replace or extend standard components within the collections page.
 */
export interface CollectionsPageCustomTemplates {
    belowTabs?: TemplateRef<unknown>;
    /** Replaces the breadcrumbs inside the collection's header bar. */
    breadcrumbs?: TemplateRef<unknown>;
}

/**
 * Singleton service for public interfacing with the collections page.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageService {
    private internal = inject(GlobalCollectionsPageServiceInternal);
    private sessionStorageService = inject(SessionStorageService);

    constructor() {}

    /**
     * Register custom templates to replace or extend standard components within the collections
     * page.
     */
    setCustomTemplates(customTemplates: CollectionsPageCustomTemplates): void {
        this.internal.customTemplates.next(customTemplates);
    }

    /**
     * Keeps new level0 collections client side for 60s, so the start page shows them before the
     * search index has caught up (merged as virtual nodes by the collection content).
     */
    async addTemporaryCollections(nodes: Node[]) {
        if (!nodes?.length) {
            return;
        }
        const collections = await this.sessionStorageService.get<Node[]>(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            [],
            Store.BrowserSessionStorage,
        );
        await this.sessionStorageService.set(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            [...collections.filter((c) => !nodes.find((n) => c.ref.id === n.ref.id)), ...nodes],
            Store.BrowserSessionStorage,
            60,
        );
    }

    async removeTemporaryCollections(nodes: Node[]) {
        const collections = await this.sessionStorageService.get<Node[]>(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            [],
            Store.BrowserSessionStorage,
        );
        await this.sessionStorageService.set(
            SessionStorageService.KEY_ROOT_COLLECTIONS,
            collections.filter((c) => !nodes.find((n) => c.ref.id === n.ref.id)),
            Store.BrowserSessionStorage,
        );
    }
}

/**
 * Internal part of the `GlobalCollectionsPageService` for use within the collections page
 * component and services only.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageServiceInternal {
    readonly customTemplates = new BehaviorSubject<CollectionsPageCustomTemplates>({});
}
