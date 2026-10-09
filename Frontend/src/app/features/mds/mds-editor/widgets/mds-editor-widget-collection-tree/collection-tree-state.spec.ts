import { fakeAsync, tick } from '@angular/core/testing';
import { Node } from 'ngx-edu-sharing-api';
import { of, Subject, throwError } from 'rxjs';
import { CollectionTreeScope } from './collection-tree-config';
import {
    CollectionCategoryRow,
    CollectionNodeRow,
    CollectionStatusRow,
    CollectionTreeRow,
    CollectionTreeState,
    CollectionTreeStateOptions,
} from './collection-tree-state';
import {
    CollectionPage,
    CollectionTreeService,
    PARENT_ID_PROPERTY,
} from './collection-tree.service';

function collection(id: string, childCollectionsCount = 1): Node {
    return {
        ref: { id },
        title: id,
        name: id,
        access: ['Read'],
        aspects: ['ccm:collection'],
        collection: { childCollectionsCount, level0: false },
    } as unknown as Node;
}

function hit(id: string, title: string, parentId: string): Node {
    return {
        ...collection(id, 0),
        title,
        properties: { [PARENT_ID_PROPERTY]: [parentId] },
    } as unknown as Node;
}

function hits(nodes: Node[]): CollectionPage {
    return { nodes, total: nodes.length };
}

function nodeKeys(rows: CollectionTreeRow[]): string[] {
    return rows.filter((row) => row.kind === 'node').map((row) => row.key);
}

describe('CollectionTreeState', () => {
    let service: jasmine.SpyObj<CollectionTreeService>;

    function createState(options: Partial<CollectionTreeStateOptions> = {}) {
        const state = new CollectionTreeState(service, {
            rootCandidates: [],
            scopes: ['MY'],
            pageSize: 2,
            mdsId: 'mds',
            ...options,
        });
        state.init();
        return state;
    }

    beforeEach(() => {
        service = jasmine.createSpyObj('CollectionTreeService', [
            'getAncestors',
            'getChildren',
            'getCollections',
            'searchCollections',
        ]);
        service.getCollections.and.returnValue(of([]));
    });

    it('lists the top level of the scope without a root', () => {
        service.getChildren.and.returnValue(
            of({ nodes: [collection('a'), collection('b')], total: 2 }),
        );
        const state = createState();
        void expect(service.getChildren).toHaveBeenCalledWith(null, {
            scope: 'MY',
            skipCount: 0,
            maxItems: 2,
        });
        void expect(nodeKeys(state.treeRows())).toEqual(['a', 'b']);
        void expect(state.rootNode()).toBeNull();
    });

    it('starts at the first readable root and lists it expanded as the only top node', () => {
        service.getAncestors.withArgs('missing').and.returnValue(throwError(() => new Error()));
        service.getAncestors
            .withArgs('root')
            .and.returnValue(of([collection('top'), collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [collection('child')], total: 1 }));
        const state = createState({ rootCandidates: ['missing', 'root'] });
        const rows = state.treeRows() as CollectionNodeRow[];
        void expect(state.rootUnavailable()).toBeTrue();
        void expect(state.rootNode().ref.id).toBe('root');
        void expect(nodeKeys(rows)).toEqual(['root', 'root/child']);
        void expect(rows[0].ancestorIds).toEqual(['top']);
        void expect(rows[0].expanded).toBeTrue();
        void expect(rows[1].ancestorIds).toEqual(['top', 'root']);
        void expect(rows[1].parentKey).toBe('root');
        void expect(rows[1].level).toBe(1);
        void expect(service.getChildren.calls.mostRecent().args[0].ref.id).toBe('root');
    });

    it('falls back to the top level when no root can be read', () => {
        service.getAncestors.and.returnValue(throwError(() => new Error()));
        service.getChildren.and.returnValue(of({ nodes: [collection('a')], total: 1 }));
        const state = createState({ rootCandidates: ['missing'] });
        void expect(state.rootUnavailable()).toBeTrue();
        void expect(service.getChildren.calls.mostRecent().args[0]).toBeNull();
        void expect(nodeKeys(state.treeRows())).toEqual(['a']);
    });

    describe('with several categories', () => {
        type ChildrenOptions = { scope?: CollectionTreeScope; skipCount: number; maxItems: number };

        /** Serves `pages` for category pages and a total of 1 for each category check. */
        function serveCategories(
            pages: Partial<Record<CollectionTreeScope, Node[]>>,
            empty: CollectionTreeScope[] = [],
        ) {
            service.getChildren.and.callFake((parent: Node | null, options: ChildrenOptions) =>
                options.maxItems === 1
                    ? of({ nodes: [], total: empty.includes(options.scope) ? 0 : 1 })
                    : of({
                          nodes: pages[options.scope] ?? [],
                          total: pages[options.scope]?.length,
                      }),
            );
        }

        it('lists the categories holding collections, collapsed and not loaded yet', () => {
            serveCategories({}, ['EDU_GROUPS']);
            const state = createState({ scopes: ['MY', 'EDU_GROUPS', 'EDU_ALL'] });
            const rows = state.treeRows() as CollectionCategoryRow[];
            void expect(rows.map((row) => row.key)).toEqual(['category:MY', 'category:EDU_ALL']);
            void expect(rows.map((row) => row.expanded)).toEqual([false, false]);
            const requests = service.getChildren.calls.allArgs();
            void expect(requests.every(([parent]) => parent === null)).toBeTrue();
            void expect(requests.map(([, options]) => options)).toEqual(
                ['MY', 'EDU_GROUPS', 'EDU_ALL'].map((scope) => ({
                    scope: scope as CollectionTreeScope,
                    skipCount: 0,
                    maxItems: 1,
                })),
            );
        });

        it('expands a category into its collections, listing shared ones in each', () => {
            serveCategories({
                MY: [collection('shared'), collection('mine')],
                TYPE_EDITORIAL: [collection('shared')],
            });
            const state = createState({ scopes: ['MY', 'TYPE_EDITORIAL'], pageSize: 10 });
            for (const row of state.treeRows() as CollectionCategoryRow[]) {
                state.expandCategory(row.category);
            }
            const rows = state.treeRows();
            void expect(rows.map((row) => row.key)).toEqual([
                'category:MY',
                'category:MY/shared',
                'category:MY/mine',
                'category:TYPE_EDITORIAL',
                'category:TYPE_EDITORIAL/shared',
            ]);
            const shared = rows[1] as CollectionNodeRow;
            void expect(shared.level).toBe(1);
            void expect(shared.parentKey).toBe('category:MY');
            void expect(shared.ancestorIds).toEqual([]);
            void expect(service.getChildren).toHaveBeenCalledWith(null, {
                scope: 'TYPE_EDITORIAL',
                skipCount: 0,
                maxItems: 10,
            });
        });

        it('lists the only category holding collections without its header', () => {
            serveCategories({ EDU_ALL: [collection('a')] }, ['MY']);
            const state = createState({ scopes: ['MY', 'EDU_ALL'] });
            void expect(state.treeRows().map((row) => row.key)).toEqual(['a']);
        });

        it('reveals a collection in the category holding it, keeping the others closed', () => {
            serveCategories({ MY: [collection('mine')], EDU_ALL: [collection('public')] });
            const state = createState({ scopes: ['MY', 'EDU_ALL'] });
            const revealed: string[] = [];
            state.reveal(['public']).subscribe((key) => revealed.push(key));
            void expect(revealed).toEqual(['category:EDU_ALL/public']);
            const categories = state
                .treeRows()
                .filter((row): row is CollectionCategoryRow => row.kind === 'category');
            void expect(categories.map((row) => row.expanded)).toEqual([false, true]);
        });
    });

    it('offers to load more until the total is reached', () => {
        service.getChildren.and.returnValues(
            of({ nodes: [collection('a'), collection('b')], total: 3 }),
            of({ nodes: [collection('c')], total: 3 }),
        );
        const state = createState();
        const rows = state.treeRows();
        void expect(rows.map((row) => row.kind)).toEqual(['node', 'node', 'more']);
        state.loadMore((rows[2] as CollectionStatusRow).target);
        void expect(service.getChildren.calls.mostRecent().args[1].skipCount).toBe(2);
        void expect(state.treeRows().map((row) => row.key)).toEqual(['a', 'b', 'c']);
    });

    it('ends a level whose total also counts collections that are not returned', () => {
        service.getChildren.and.returnValue(
            of({ nodes: [collection('a'), collection('b')], total: 4 }),
        );
        const state = createState({ pageSize: 10 });
        void expect(state.treeRows().map((row) => row.kind)).toEqual(['node', 'node']);
        void expect(service.getChildren).toHaveBeenCalledTimes(1);
    });

    it('offers no expander for a collection reported without sub-collections', () => {
        service.getChildren.and.returnValue(
            of({ nodes: [collection('parent', 3), collection('leaf', 0)], total: 2 }),
        );
        const state = createState();
        const rows = state.treeRows() as CollectionNodeRow[];
        void expect(rows.map((row) => row.expandable)).toEqual([true, false]);
    });

    it('turns a collection without sub-collections into a leaf once expanded', () => {
        service.getChildren.and.returnValues(
            of({ nodes: [collection('a')], total: 1 }),
            of({ nodes: [], total: 0 }),
        );
        const state = createState();
        const row = state.treeRows()[0] as CollectionNodeRow;
        void expect(row.expandable).toBeTrue();
        state.toggleExpanded(row.item);
        const rows = state.treeRows();
        void expect(rows.length).toBe(1);
        void expect((rows[0] as CollectionNodeRow).expandable).toBeFalse();
    });

    it('shows an error row that retries the level', () => {
        service.getChildren.and.returnValues(
            throwError(() => new Error()),
            of({ nodes: [collection('a')], total: 1 }),
        );
        const state = createState();
        const errorRow = state.treeRows()[0] as CollectionStatusRow;
        void expect(errorRow.kind).toBe('error');
        state.retry(errorRow.target);
        void expect(nodeKeys(state.treeRows())).toEqual(['a']);
    });

    it('reveals a collection by loading the levels on its path below the root', () => {
        const mathChildren = new Subject<CollectionPage>();
        service.getAncestors.and.returnValue(of([collection('top'), collection('root')]));
        service.getChildren.and.callFake((parent: Node) =>
            parent.ref.id === 'root'
                ? of({ nodes: [collection('math'), collection('physics')], total: 2 })
                : mathChildren,
        );
        const state = createState({ rootCandidates: ['root'] });
        const revealed: string[] = [];
        state.reveal(['top', 'root', 'math', 'algebra']).subscribe((key) => revealed.push(key));
        void expect(revealed).toEqual([]);
        mathChildren.next({ nodes: [collection('algebra', 0)], total: 1 });
        void expect(revealed).toEqual(['root/math/algebra']);
        void expect(nodeKeys(state.treeRows())).toEqual([
            'root',
            'root/math',
            'root/math/algebra',
            'root/physics',
        ]);
    });

    it('reveals a collection as far as its path is listed', () => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [collection('a')], total: 3 }));
        const state = createState({ rootCandidates: ['root'] });
        const revealed: string[] = [];
        // `b` is on a page that is not loaded, `other` is not below the root
        state.reveal(['root', 'b', 'b1']).subscribe((key) => revealed.push(key));
        state.reveal(['other', 'c']).subscribe((key) => revealed.push(key));
        void expect(revealed).toEqual(['root', null]);
    });

    it('lists the top level of the scope instead of the root when chosen', () => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.callFake((parent: Node | null) =>
            of({ nodes: [collection(parent ? 'child' : 'top')], total: 1 }),
        );
        const state = createState({ rootCandidates: ['root'] });
        void expect(nodeKeys(state.treeRows())).toEqual(['root', 'root/child']);
        state.setWithinRoot(false);
        void expect(service.getChildren.calls.mostRecent().args[0]).toBeNull();
        void expect(nodeKeys(state.treeRows())).toEqual(['top']);
        state.setWithinRoot(true);
        void expect(nodeKeys(state.treeRows())).toEqual(['root', 'root/child']);
        void expect(service.getChildren).toHaveBeenCalledTimes(2);
    });

    it('searches below the root from two characters on and lists hits below it', fakeAsync(() => {
        service.getAncestors.and.returnValue(of([collection('top'), collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.searchCollections.and.returnValue(of(hits([hit('hit', 'Physik', 'root')])));
        const state = createState({ rootCandidates: ['root'] });
        state.setSearchTerm('p');
        void expect(state.searchActive()).toBeFalse();
        state.setSearchTerm('ph');
        void expect(state.searchRows().map((row) => row.kind)).toEqual(['loading']);
        tick(300);
        void expect(service.searchCollections).toHaveBeenCalledOnceWith('ph', {
            rootId: 'root',
            mdsId: 'mds',
            skipCount: 0,
            maxItems: 2,
        });
        const rows = state.searchRows() as CollectionNodeRow[];
        void expect(nodeKeys(rows)).toEqual(['search/root', 'search/root/hit']);
        void expect(rows[0].expanded).toBeTrue();
        void expect(rows[1].ancestorIds).toEqual(['top', 'root']);
        // the path of the root is known, so nothing has to be looked up
        void expect(service.getAncestors).toHaveBeenCalledOnceWith('root');
        void expect(service.getCollections).not.toHaveBeenCalled();
    }));

    it('searches all collections when not limited to the root', fakeAsync(() => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.searchCollections.and.returnValue(of(hits([])));
        const state = createState({ rootCandidates: ['root'] });
        state.setSearchTerm('physics');
        tick(300);
        state.setWithinRoot(false);
        tick(300);
        void expect(service.searchCollections.calls.mostRecent().args[1].rootId).toBeNull();
        void expect(state.searchRows().map((row) => row.kind)).toEqual(['empty']);
    }));

    it('looks up the collections above hits with one request per level', fakeAsync(() => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.getCollections.and.returnValues(
            of([hit('math', 'math', 'subjects'), hit('physics', 'physics', 'subjects')]),
            of([hit('subjects', 'subjects', 'root')]),
        );
        service.searchCollections.and.returnValue(
            of(
                hits([
                    hit('a', 'Methoden', 'math'),
                    hit('b', 'Methoden', 'physics'),
                    hit('c', 'Methodik', 'math'),
                ]),
            ),
        );
        const state = createState({ rootCandidates: ['root'], pageSize: 10 });
        state.setSearchTerm('meth');
        tick(300);
        const rows = state.searchRows() as CollectionNodeRow[];
        void expect(nodeKeys(rows)).toEqual([
            'search/root',
            'search/root/subjects',
            'search/root/subjects/math',
            'search/root/subjects/math/a',
            'search/root/subjects/math/c',
            'search/root/subjects/physics',
            'search/root/subjects/physics/b',
        ]);
        void expect(rows[3].ancestorIds).toEqual(['root', 'subjects', 'math']);
        void expect(service.getCollections.calls.allArgs()).toEqual([
            [['math', 'physics'], 'mds'],
            [['subjects'], 'mds'],
        ]);
    }));

    it('needs no path request for hits in loaded levels or below other hits', fakeAsync(() => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [collection('math')], total: 1 }));
        service.searchCollections.and.returnValue(
            of(hits([hit('below-hit', 'Algebra 2', 'algebra'), hit('algebra', 'Algebra', 'math')])),
        );
        const state = createState({ rootCandidates: ['root'] });
        state.setSearchTerm('alg');
        tick(300);
        void expect(nodeKeys(state.searchRows())).toEqual([
            'search/root',
            'search/root/math',
            'search/root/math/algebra',
            'search/root/math/algebra/below-hit',
        ]);
        void expect(service.getCollections).not.toHaveBeenCalled();
    }));

    it('lists top-level hits and hits of unknown location on the first level', fakeAsync(() => {
        const topLevelHit = {
            ...collection('top', 0),
            collection: { childCollectionsCount: 0, level0: true },
        } as unknown as Node;
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.searchCollections.and.returnValue(
            of(hits([topLevelHit, hit('lost', 'Lost', 'x')])),
        );
        const state = createState();
        state.setSearchTerm('to');
        tick(300);
        const rows = state.searchRows() as CollectionNodeRow[];
        void expect(nodeKeys(rows)).toEqual(['search/top', 'search/lost']);
        void expect(rows[0].ancestorIds).toEqual([]);
        void expect(rows[1].ancestorIds).toBeNull();
        // `x` cannot be read, so it is not returned
        void expect(service.getCollections).toHaveBeenCalledOnceWith(['x'], 'mds');
    }));

    it('leaves out hits outside of the root', fakeAsync(() => {
        const other = {
            ...collection('other'),
            collection: { childCollectionsCount: 1, level0: true },
        } as unknown as Node;
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getCollections.and.returnValue(of([other]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.searchCollections.and.returnValue(of(hits([hit('outside', 'Outside', 'other')])));
        const state = createState({ rootCandidates: ['root'] });
        state.setSearchTerm('out');
        tick(300);
        void expect(state.searchRows().map((row) => row.kind)).toEqual(['empty']);
    }));

    it('keeps a collection leading to hits from loading all of its children', fakeAsync(() => {
        service.getAncestors.and.returnValue(of([collection('root')]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        service.searchCollections.and.returnValue(of(hits([hit('hit', 'Hit', 'root')])));
        const state = createState({ rootCandidates: ['root'] });
        state.setSearchTerm('hit');
        tick(300);
        const root = state.searchRows()[0] as CollectionNodeRow;
        state.toggleExpanded(root.item);
        void expect(nodeKeys(state.searchRows())).toEqual(['search/root']);
        const callsBefore = service.getChildren.calls.count();
        state.toggleExpanded(root.item);
        void expect(nodeKeys(state.searchRows())).toEqual(['search/root', 'search/root/hit']);
        void expect(service.getChildren.calls.count()).toBe(callsBefore);
    }));

    it('ignores results of a search term that was changed meanwhile', fakeAsync(() => {
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
        const outdated = new Subject<CollectionPage>();
        service.searchCollections.and.returnValues(outdated, of(hits([collection('b')])));
        const state = createState();
        state.setSearchTerm('ab');
        tick(300);
        state.setSearchTerm('abc');
        outdated.next(hits([collection('a')]));
        void expect(state.searchHits()).toEqual([]);
        tick(300);
        void expect(state.searchHits().map((node) => node.ref.id)).toEqual(['b']);
    }));
});

describe('CollectionTreeState search hits', () => {
    let service: jasmine.SpyObj<CollectionTreeService>;

    beforeEach(() => {
        service = jasmine.createSpyObj('CollectionTreeService', [
            'getAncestors',
            'getChildren',
            'getCollections',
            'searchCollections',
        ]);
        service.getCollections.and.returnValue(of([]));
        service.getChildren.and.returnValue(of({ nodes: [], total: 0 }));
    });

    it('expands a hit into its sub-collections below the hit', fakeAsync(() => {
        service.searchCollections.and.returnValue(
            of(hits([collection('optics', 2), collection('colors', 0)])),
        );
        const state = new CollectionTreeState(service, {
            rootCandidates: [],
            scopes: ['MY'],
            pageSize: 10,
            mdsId: 'mds',
        });
        state.init();
        state.setSearchTerm('opt');
        tick(300);
        const [optics, colors] = state.searchRows() as CollectionNodeRow[];
        void expect(optics.expandable).toBeTrue();
        void expect(colors.expandable).toBeFalse();
        service.getChildren.and.returnValue(of({ nodes: [collection('lenses', 0)], total: 1 }));
        state.expand(optics.item);
        void expect(service.getChildren.calls.mostRecent().args[0].ref.id).toBe('optics');
        const rows = state.searchRows() as CollectionNodeRow[];
        void expect(nodeKeys(rows)).toEqual([
            'search/optics',
            'search/optics/lenses',
            'search/colors',
        ]);
        void expect(rows[1].parentKey).toBe('search/optics');
        void expect(rows[1].level).toBe(1);
        // a hit reported without a parent is listed as a top-level collection
        void expect(rows[1].ancestorIds).toEqual(['optics']);
        state.destroy();
    }));
});
