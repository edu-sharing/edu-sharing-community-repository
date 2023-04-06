import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { CollectionV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node } from '../models';
import { cachedShareReplay, KeyCache } from '../utils/decorators/cached-share-replay';
import { ReferenceEntries } from '../api/models/reference-entries';
import { CollectionEntries } from '../api/models/collection-entries';

@Injectable({
    providedIn: 'root',
})
export class CollectionService {
    private static readonly collectionCache = new KeyCache();

    constructor(private collectionV1: CollectionV1Service) {}

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

    @cachedShareReplay(CollectionService.collectionCache, getCacheKey)
    observeCollection(id: string, { repository = HOME_REPOSITORY } = {}): Observable<Node> {
        return this.collectionV1
            .getCollection({ collectionId: id, repository })
            .pipe(map((entry) => entry.collection));
    }
}

function getCacheKey(id: string, { repository = HOME_REPOSITORY }): string {
    return JSON.stringify({ id, repository });
}
