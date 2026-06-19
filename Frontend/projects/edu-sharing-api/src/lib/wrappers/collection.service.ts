import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CollectionV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node } from '../models';
import { cachedShareReplay, KeyCache } from '../utils/decorators/cached-share-replay';
import { ReferenceEntries } from '../api/models/reference-entries';
import { CollectionEntries } from '../api/models/collection-entries';
import { Copy } from '../api/models/copy';

@Injectable({
    providedIn: 'root',
})
export class CollectionService {
    private collectionV1 = inject(CollectionV1Service);

    private static readonly collectionCache = new KeyCache();
    private static readonly subCollectionsCache = new KeyCache();

    getCollection(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node> {
        // TODO: Wrap other endpoints and reset not on get, but on modifying actions.
        CollectionService.collectionCache.reset(getCacheKey(id, { repository }));
        return this.observeCollection(id, { repository }).pipe(take(1));
    }

    getReferences(
        params: Parameters<typeof CollectionV1Service.prototype.getCollectionsReferences>[0],
    ): Observable<ReferenceEntries> {
        return this.collectionV1.getCollectionsReferences(params);
    }

    getSubcollections(
        params: Parameters<typeof CollectionV1Service.prototype.getCollectionsSubcollections>[0],
    ): Observable<CollectionEntries> {
        // wrong api data
        return this.collectionV1.getCollectionsSubcollections(
            params,
        ) as Observable<CollectionEntries>;
    }

    @cachedShareReplay(CollectionService.collectionCache, getCacheKey)
    observeCollection(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node> {
        return this.collectionV1
            .getCollection({ collectionId: id, repository })
            .pipe(map((entry) => entry.collection));
    }

    getSubCollections(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node[]> {
        // TODO: Wrap other endpoints and reset not on get, but on modifying actions.
        CollectionService.subCollectionsCache.reset(getCacheKey(id, { repository }));
        return this.observeSubCollections(id, { repository }).pipe(take(1));
    }

    /**
     * Observes child collections of the given collection.
     *
     * Does not use pagination and is not meant to get root-level collections.
     */
    @cachedShareReplay(CollectionService.subCollectionsCache, getCacheKey)
    observeSubCollections(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node[]> {
        return this.collectionV1
            .getCollectionsSubcollections({
                collection: id,
                repository,
                scope: 'EDU_ALL', // value ignored for sub collections
                maxItems: 65535,
            })
            .pipe(map((collectionsEntry) => collectionsEntry.collections));
    }

    /**
     * Copy a collection.
     */
    copyCollection(params: {
        /**
         * ID of repository (or "-home-" for home repository)
         */
        repository: string;

        /**
         * ID of parent collection (or "-root-" for level0 collections)
         */
        sourceCollection: string;

        /**
         * target nodeId. if not set collection will be copied in root
         */
        targetCollection?: string;

        /**
         * if true sourceCollection will be copied, else only the children
         */
        copyRoot?: boolean;

        /**
         * if true reference object will be copied
         */
        copyRefs?: boolean;

        /**
         * if true permissions will be copied
         */
        copyPermissions?: boolean;

        /**
         * if true child collections will be copied
         */
        copyChildCollections?: boolean;
    }): Observable<Copy> {
        return this.collectionV1.copyCollection(params);
    }
}

function getCacheKey(id: string, { repository = HOME_REPOSITORY }): string {
    return JSON.stringify({ id, repository });
}
