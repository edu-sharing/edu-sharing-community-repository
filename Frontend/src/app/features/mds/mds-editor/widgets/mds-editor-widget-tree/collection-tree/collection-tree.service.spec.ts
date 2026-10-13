import { TestBed } from '@angular/core/testing';
import {
    CollectionEntries,
    CollectionService,
    DEFAULT,
    Node,
    NodeServiceUnwrapped,
    ROOT,
    SearchService,
} from 'ngx-edu-sharing-api';
import { NodeHelperService } from 'ngx-edu-sharing-ui';
import { Observable, of, throwError } from 'rxjs';
import { CollectionTreeService } from './collection-tree.service';

function collection(id: string, parentId: string | null = null): Node {
    return {
        ref: { id },
        title: id,
        name: id,
        aspects: ['ccm:collection'],
        collection: { level0: !parentId },
        properties: parentId ? { 'virtual:primaryparent_nodeid': [parentId] } : {},
    } as unknown as Node;
}

function result<T>(observable: Observable<T>): T {
    let value: T;
    observable.subscribe((next) => (value = next));
    return value;
}

describe('CollectionTreeService', () => {
    let service: CollectionTreeService;
    let collectionService: jasmine.SpyObj<CollectionService>;
    let nodeService: jasmine.SpyObj<NodeServiceUnwrapped>;
    let searchService: jasmine.SpyObj<SearchService>;

    beforeEach(() => {
        collectionService = jasmine.createSpyObj('CollectionService', ['getSubcollections']);
        nodeService = jasmine.createSpyObj('NodeService', ['getParents']);
        searchService = jasmine.createSpyObj('SearchService', ['search', 'requestSearch']);
        TestBed.configureTestingModule({
            providers: [
                CollectionTreeService,
                { provide: CollectionService, useValue: collectionService },
                { provide: NodeServiceUnwrapped, useValue: nodeService },
                { provide: SearchService, useValue: searchService },
                {
                    provide: NodeHelperService,
                    useValue: {
                        getSortByForCollection: (parent: Node | typeof ROOT) =>
                            parent === ROOT
                                ? { active: 'cm:modified', direction: 'desc' }
                                : { active: 'ccm:collection_ordered_position', direction: 'asc' },
                    },
                },
            ],
        });
        service = TestBed.inject(CollectionTreeService);
    });

    it('lists the top level of the scope with pinned collections first', () => {
        collectionService.getSubcollections.and.returnValue(
            of({ collections: [collection('a')], pagination: { total: 3 } } as CollectionEntries),
        );
        const page = result(service.getChildren(null, { scope: 'MY', skipCount: 0, maxItems: 50 }));
        const params = collectionService.getSubcollections.calls.mostRecent().args[0];
        void expect(params.collection).toBe(ROOT);
        void expect(params.scope).toBe('MY');
        void expect(params.fetchCounts).toBeTrue();
        void expect(params.sortProperties).toEqual([
            'ccm:collection_pinned_status',
            'ccm:collection_pinned_order',
            'cm:modified',
        ]);
        void expect(params.sortAscending).toEqual([false, true, false]);
        void expect(page).toEqual({ nodes: [collection('a')], total: 3 });
    });

    it('searches below the root without changing the state of the search page', () => {
        searchService.requestSearch.and.returnValue(
            of({ nodes: [collection('a')], pagination: { total: 1 } } as never),
        );
        const hits = result(
            service.searchCollections('phys', {
                rootId: 'root',
                mdsId: DEFAULT,
                maxItems: 25,
            }),
        );
        void expect(searchService.search).not.toHaveBeenCalled();
        const params = searchService.requestSearch.calls.mostRecent().args[0];
        void expect(params.metadataset).toBe(DEFAULT);
        void expect(params.query).toBe('collections');
        void expect(params.body.criteria).toEqual([
            { property: 'ngsearchword', values: ['phys'] },
            { property: 'virtual:parent_recursive', values: ['root'] },
        ]);
        void expect(params.propertyFilter).toContain('virtual:primaryparent_nodeid');
        void expect(hits).toEqual([collection('a')]);
    });

    it('searches all collections when no root is given', () => {
        searchService.requestSearch.and.returnValue(
            of({ nodes: [], pagination: { total: 0 } } as never),
        );
        result(
            service.searchCollections('phys', {
                rootId: null,
                mdsId: DEFAULT,
                maxItems: 25,
            }),
        );
        const params = searchService.requestSearch.calls.mostRecent().args[0];
        void expect(params.body.criteria).toEqual([{ property: 'ngsearchword', values: ['phys'] }]);
    });

    it('retries without restriction in the default metadata set when the search fails', () => {
        spyOn(console, 'warn');
        searchService.requestSearch.and.returnValues(
            throwError(() => new Error('unknown parameter')),
            of({ nodes: [], pagination: { total: 0 } } as never),
        );
        const hits = result(
            service.searchCollections('phys', {
                rootId: 'root',
                mdsId: DEFAULT,
                maxItems: 25,
            }),
        );
        const retry = searchService.requestSearch.calls.mostRecent().args[0];
        void expect(searchService.requestSearch).toHaveBeenCalledTimes(2);
        void expect(retry.metadataset).toBe(DEFAULT);
        void expect(retry.body.criteria).toEqual([{ property: 'ngsearchword', values: ['phys'] }]);
        void expect(hits).toEqual([]);
    });

    it('keeps the application from reporting a failed search it recovers from', () => {
        spyOn(console, 'warn');
        const error = { status: 400, preventDefault: jasmine.createSpy('preventDefault') };
        searchService.requestSearch.and.returnValues(
            throwError(() => error),
            of({ nodes: [], pagination: { total: 0 } } as never),
        );
        result(
            service.searchCollections('phys', {
                rootId: 'root',
                mdsId: DEFAULT,
                maxItems: 25,
            }),
        );
        void expect(error.preventDefault).toHaveBeenCalled();
    });

    it('returns the path of a collection top-most first and caches it', () => {
        nodeService.getParents.and.returnValue(
            of({ nodes: [collection('c'), collection('b'), collection('a')] } as never),
        );
        const path = result(service.getAncestors('c'));
        result(service.getAncestors('c'));
        void expect(path.map((node) => node.ref.id)).toEqual(['a', 'b', 'c']);
        void expect(nodeService.getParents).toHaveBeenCalledOnceWith({
            repository: '-home-',
            node: 'c',
            propertyFilter: [
                'ccm:collectionsubcollectionordermode',
                'virtual:primaryparent_nodeid',
            ],
        });
    });

    it('requests a path again after it failed', () => {
        nodeService.getParents.and.returnValues(
            throwError(() => new Error('forbidden')),
            of({ nodes: [collection('c')] } as never),
        );
        service.getAncestors('c').subscribe({ error: () => undefined });
        const path = result(service.getAncestors('c'));
        void expect(path.map((node) => node.ref.id)).toEqual(['c']);
        void expect(nodeService.getParents).toHaveBeenCalledTimes(2);
    });

    it('looks up several collections by id in one search', () => {
        searchService.requestSearch.and.returnValue(of({ nodes: [collection('a')] } as never));
        const found = result(service.getCollections(['a', 'b'], DEFAULT));
        const params = searchService.requestSearch.calls.mostRecent().args[0];
        void expect(params.metadataset).toBe(DEFAULT);
        void expect(params.query).toBe('collections');
        void expect(params.maxItems).toBe(2);
        void expect(params.body.criteria).toEqual([
            { property: 'virtual:node_id', values: ['a', 'b'] },
        ]);
        void expect(found).toEqual([collection('a')]);
    });

    it('loads paths instead where the metadata set cannot look collections up by id', () => {
        spyOn(console, 'warn');
        const folder = { ...collection('folder'), aspects: [] } as unknown as Node;
        searchService.requestSearch.and.returnValue(throwError(() => ({ status: 400 })));
        nodeService.getParents.and.returnValue(
            of({ nodes: [collection('a'), collection('top'), folder] } as never),
        );
        const found = result(service.getCollections(['a'], DEFAULT));
        void expect(found.map((node) => node.ref.id)).toEqual(['top', 'a']);
        result(service.getCollections(['b'], DEFAULT));
        void expect(searchService.requestSearch).toHaveBeenCalledTimes(1);
    });

    it('looks up the collections above search hits one level per request', () => {
        searchService.requestSearch.and.returnValues(
            of({ nodes: [collection('b', 'a')] } as never),
            of({ nodes: [collection('a')] } as never),
        );
        const placed = result(
            service.getWithAncestors([collection('c', 'b'), collection('d', 'b')], DEFAULT),
        );
        const lookups = searchService.requestSearch.calls
            .allArgs()
            .map(([params]) => params.body.criteria[0].values);
        void expect(lookups).toEqual([['b'], ['a']]);
        void expect([...placed.keys()].sort()).toEqual(['a', 'b', 'c', 'd']);
        void expect(placed.get('c').parentId).toBe('b');
        void expect(placed.get('a').parentId).toBeNull();
    });

    it('searches unrestricted right away once the subtree search was rejected', () => {
        spyOn(console, 'warn');
        const options = { rootId: 'root', mdsId: DEFAULT, maxItems: 25 };
        searchService.requestSearch.and.returnValues(
            throwError(() => ({ status: 400 })),
            of({ nodes: [], pagination: { total: 0 } } as never),
            of({ nodes: [], pagination: { total: 0 } } as never),
        );
        result(service.searchCollections('phys', options));
        result(service.searchCollections('physik', options));
        void expect(searchService.requestSearch).toHaveBeenCalledTimes(3);
        const last = searchService.requestSearch.calls.mostRecent().args[0];
        void expect(last.body.criteria).toEqual([{ property: 'ngsearchword', values: ['physik'] }]);
    });
});
