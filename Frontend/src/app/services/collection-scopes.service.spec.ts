import { TestBed } from '@angular/core/testing';
import {
    AuthenticationService,
    CollectionEntries,
    CollectionService,
    HOME_REPOSITORY,
    Node,
    ROOT,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from 'ngx-edu-sharing-ui';
import { Observable, of, throwError } from 'rxjs';
import { CollectionScopesService } from './collection-scopes.service';

function result<T>(observable: Observable<T>): T {
    let value: T;
    observable.subscribe((next) => (value = next));
    return value;
}

/** A top-level collection owned by `owner`. */
function collectionOf(owner: string): Node {
    return { properties: { 'cm:owner': [owner] } } as unknown as Node;
}

/** Answers the category checks; a scope missing in `totals` holds no collections. */
function answer(totals: Record<string, number>) {
    return (params: { scope: string }) =>
        of({
            collections: [],
            pagination: { total: totals[params.scope] ?? 0 },
        } as unknown as CollectionEntries);
}

describe('CollectionScopesService', () => {
    let service: CollectionScopesService;
    let collectionService: jasmine.SpyObj<CollectionService>;
    let isGuest: boolean;
    let scopeName: string;

    const checkedScopes = () =>
        collectionService.getSubcollections.calls.allArgs().map(([params]) => params.scope);

    beforeEach(() => {
        isGuest = false;
        scopeName = 'UNKNOWN';
        collectionService = jasmine.createSpyObj('CollectionService', ['getSubcollections']);
        TestBed.configureTestingModule({
            providers: [
                CollectionScopesService,
                { provide: CollectionService, useValue: collectionService },
                {
                    provide: AuthenticationService,
                    useValue: { observeLoginInfo: () => of({ isGuest, authorityName: 'me' }) },
                },
                {
                    provide: NodeHelperService,
                    useValue: { getCollectionScopeInfo: () => ({ icon: '', scopeName }) },
                },
            ],
        });
        service = TestBed.inject(CollectionScopesService);
    });

    it('lists editorial and media-center categories only while they hold collections', () => {
        collectionService.getSubcollections.and.callFake(answer({ TYPE_EDITORIAL: 2 }));
        void expect(result(service.getScopes())).toEqual([
            'MY',
            'EDU_GROUPS',
            'TYPE_EDITORIAL',
            'EDU_ALL',
        ]);
        void expect(checkedScopes()).toEqual(['TYPE_EDITORIAL', 'TYPE_MEDIA_CENTER']);
    });

    it('checks the top level of a category for a single collection', () => {
        collectionService.getSubcollections.and.callFake(answer({}));
        result(service.getScopes({ scopes: ['TYPE_EDITORIAL'] }));
        const params = collectionService.getSubcollections.calls.mostRecent().args[0];
        void expect(params.repository).toBe(HOME_REPOSITORY);
        void expect(params.collection).toBe(ROOT);
        void expect(params.maxItems).toBe(1);
    });

    it('lists no personal categories to guests', () => {
        isGuest = true;
        collectionService.getSubcollections.and.callFake(answer({ TYPE_MEDIA_CENTER: 1 }));
        void expect(result(service.getScopes())).toEqual(['TYPE_MEDIA_CENTER', 'EDU_ALL']);
    });

    it('leaves out every empty category with hideEmpty', () => {
        collectionService.getSubcollections.and.callFake(answer({ MY: 3, EDU_ALL: 1 }));
        void expect(result(service.getScopes({ hideEmpty: true }))).toEqual(['MY', 'EDU_ALL']);
        void expect(checkedScopes()).toEqual([
            'MY',
            'EDU_GROUPS',
            'TYPE_EDITORIAL',
            'TYPE_MEDIA_CENTER',
            'EDU_ALL',
        ]);
    });

    it('keeps the order of the given categories', () => {
        void expect(result(service.getScopes({ scopes: ['EDU_ALL', 'MY'] }))).toEqual([
            'EDU_ALL',
            'MY',
        ]);
        void expect(collectionService.getSubcollections).not.toHaveBeenCalled();
    });

    it('lists a category whose check fails, keeping the error from being reported', () => {
        const error = { preventDefault: jasmine.createSpy('preventDefault') };
        collectionService.getSubcollections.and.returnValue(throwError(() => error));
        void expect(result(service.getScopes({ scopes: ['TYPE_EDITORIAL'] }))).toEqual([
            'TYPE_EDITORIAL',
        ]);
        void expect(error.preventDefault).toHaveBeenCalled();
    });

    it('counts the listed collections when the total is missing', () => {
        collectionService.getSubcollections.and.returnValue(
            of({ collections: [{}] } as unknown as CollectionEntries),
        );
        void expect(result(service.getScopes({ scopes: ['TYPE_MEDIA_CENTER'] }))).toEqual([
            'TYPE_MEDIA_CENTER',
        ]);
    });

    it('lists nothing and checks nothing when no category is left', () => {
        isGuest = true;
        void expect(result(service.getScopes({ scopes: ['MY', 'EDU_GROUPS'] }))).toEqual([]);
        void expect(collectionService.getSubcollections).not.toHaveBeenCalled();
    });

    it('lists editorial, media-center and public collections in their own category', () => {
        for (const [name, scope] of [
            ['TYPE_EDITORIAL', 'TYPE_EDITORIAL'],
            ['TYPE_MEDIA_CENTER', 'TYPE_MEDIA_CENTER'],
            ['PUBLIC', 'EDU_ALL'],
        ]) {
            scopeName = name;
            void expect(service.getScopeOf(collectionOf('someone'))).toBe(scope);
        }
    });

    it("lists other collections as the user's own or as shared ones, as the owner tells", () => {
        scopeName = 'SHARED';
        void expect(service.getScopeOf(collectionOf('me'))).toBe('MY');
        void expect(service.getScopeOf(collectionOf('someone'))).toBe('EDU_GROUPS');
        scopeName = 'MY';
        void expect(service.getScopeOf(collectionOf('me'))).toBe('MY');
    });

    it("lists another user's private collection in no category", () => {
        scopeName = 'MY';
        void expect(service.getScopeOf(collectionOf('someone'))).toBeNull();
    });
});
