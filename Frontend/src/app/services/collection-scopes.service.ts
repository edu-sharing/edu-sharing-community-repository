import { inject, Injectable } from '@angular/core';
import {
    AuthenticationService,
    CollectionService,
    HOME_REPOSITORY,
    ROOT,
} from 'ngx-edu-sharing-api';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, first, map, switchMap } from 'rxjs/operators';

/** Top-level categories of collections, as the `scope` of the collections API names them. */
export type CollectionScope =
    | 'MY'
    | 'EDU_GROUPS'
    | 'TYPE_EDITORIAL'
    | 'TYPE_MEDIA_CENTER'
    | 'EDU_ALL';

/** All categories in the order they are listed in. */
export const COLLECTION_SCOPES: readonly CollectionScope[] = [
    'MY',
    'EDU_GROUPS',
    'TYPE_EDITORIAL',
    'TYPE_MEDIA_CENTER',
    'EDU_ALL',
];

/** Categories only signed-in users have collections in. */
const PERSONAL_SCOPES: readonly CollectionScope[] = ['MY', 'EDU_GROUPS'];
/** Categories listed only while they hold collections. */
const OPTIONAL_SCOPES: readonly CollectionScope[] = ['TYPE_EDITORIAL', 'TYPE_MEDIA_CENTER'];

/**
 * Decides which categories of collections the current user gets listed, e.g. as the tabs of the
 * collections page or the top level of a collection tree.
 */
@Injectable({ providedIn: 'root' })
export class CollectionScopesService {
    private readonly authentication = inject(AuthenticationService);
    private readonly collectionService = inject(CollectionService);

    /**
     * The categories of `scopes` listed to the current user, in their order: none of the personal
     * ones for guests, editorial and media-center ones only while they hold collections, and no
     * empty one at all with `hideEmpty`.
     */
    getScopes(
        options: { scopes?: readonly CollectionScope[]; hideEmpty?: boolean } = {},
    ): Observable<CollectionScope[]> {
        return this.authentication.observeLoginInfo().pipe(
            first(),
            switchMap((login) => {
                const scopes = (options.scopes ?? COLLECTION_SCOPES).filter(
                    (scope) => !login?.isGuest || !PERSONAL_SCOPES.includes(scope),
                );
                if (!scopes.length) {
                    return of([]);
                }
                const filled = scopes.map((scope) =>
                    options.hideEmpty || OPTIONAL_SCOPES.includes(scope)
                        ? this.hasCollections(scope)
                        : of(true),
                );
                return forkJoin(filled).pipe(
                    map((isFilled) => scopes.filter((_, index) => isFilled[index])),
                );
            }),
        );
    }

    /** Whether the top level of `scope` holds collections; one that cannot be checked counts. */
    private hasCollections(scope: CollectionScope): Observable<boolean> {
        return this.collectionService
            .getSubcollections({
                repository: HOME_REPOSITORY,
                collection: ROOT,
                scope,
                skipCount: 0,
                maxItems: 1,
            })
            .pipe(
                map((entries) => (entries.pagination?.total ?? entries.collections?.length) > 0),
                catchError((error: unknown) => {
                    // listing the category leaves the error to be reported when it is opened
                    (error as { preventDefault?: () => void })?.preventDefault?.();
                    return of(true);
                }),
            );
    }
}
