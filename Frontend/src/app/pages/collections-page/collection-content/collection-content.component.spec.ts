import { SelectionModel } from '@angular/cdk/collections';
import { Injector, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Node, ProposalNode } from 'ngx-edu-sharing-api';
import { OptionItem } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { CollectionContentComponent } from './collection-content.component';

const OPTIONS = ['OPTIONS.CUT', 'OPTIONS.COPY', 'OPTIONS.INVITE', 'OPTIONS.ADD_SHORTCUT'];

function collection(id: string): Node {
    return { ref: { id }, aspects: [RestConstants.CCM_ASPECT_COLLECTION] } as unknown as Node;
}

function material(id: string): Node {
    return { ref: { id }, aspects: [RestConstants.CCM_ASPECT_IO_REFERENCE] } as unknown as Node;
}

/**
 * Runs the filter of the selection bar's options on the given selection, with the delete right
 * on every node, and returns the names of the options that remain. The regular delete options
 * are left out: each of them only covers one kind of node.
 */
function optionsFor(nodes: Node[]): string[] {
    const options = OPTIONS.map((name) => new OptionItem(name, 'icon', (): void => undefined));
    const host = {
        nodeHelper: {
            isNodeCollection: (node: Node) =>
                node.aspects.includes(RestConstants.CCM_ASPECT_COLLECTION),
            getNodesRight: () => true,
        },
    };
    (CollectionContentComponent.prototype as any).removeOptionsUnsupportedForSelection.call(
        host,
        options,
        nodes,
    );
    return options.map((option) => option.name);
}

describe('CollectionContentComponent selection options', () => {
    it('keeps every option for a single collection', () => {
        expect(optionsFor([collection('a')])).toEqual(OPTIONS);
    });

    it('offers cut and copy for several collections, without invite and shortcut', () => {
        const names = optionsFor([collection('a'), collection('b')]);
        expect(names).toContain('OPTIONS.CUT');
        expect(names).toContain('OPTIONS.COPY');
        expect(names).not.toContain('OPTIONS.INVITE');
        expect(names).not.toContain('OPTIONS.ADD_SHORTCUT');
        expect(names).not.toContain('OPTIONS.DELETE');
    });

    it('offers cut, copy and invite for several materials, without shortcut', () => {
        const names = optionsFor([material('a'), material('b')]);
        expect(names).toContain('OPTIONS.CUT');
        expect(names).toContain('OPTIONS.COPY');
        expect(names).toContain('OPTIONS.INVITE');
        expect(names).not.toContain('OPTIONS.ADD_SHORTCUT');
    });

    it('offers cut, copy and a combined delete for collections and materials together', () => {
        const names = optionsFor([collection('a'), material('b')]);
        expect(names).toContain('OPTIONS.CUT');
        expect(names).toContain('OPTIONS.COPY');
        expect(names).not.toContain('OPTIONS.INVITE');
        expect(names).not.toContain('OPTIONS.ADD_SHORTCUT');
        expect(names).toContain('OPTIONS.DELETE');
    });
});

describe('CollectionContentComponent created collections', () => {
    it('shows the created collections selected', () => {
        const addVirtualNodes = jasmine.createSpy('addVirtualNodes');
        const host = {
            createdCollections: [collection('created')],
            createdCollectionsUntil: Date.now() + 5000,
            listCollections: { addVirtualNodes },
            elementRef: { nativeElement: document.createElement('div') },
            injector: TestBed.inject(Injector),
        };
        (CollectionContentComponent.prototype as any).selectCreatedCollections.call(host);
        expect(addVirtualNodes).toHaveBeenCalledTimes(1);
        const [nodes, options] = addVirtualNodes.calls.mostRecent().args;
        expect(nodes.map((node: Node) => node.ref.id)).toEqual(['created']);
        expect(options).toEqual({ select: true });
    });
});

describe('CollectionContentComponent proposals', () => {
    function proposal(id: string): ProposalNode {
        return {
            ref: { id },
            type: 'ccm:io',
            aspects: [],
            proposal: { ref: { id: 'proposal-' + id } },
        } as unknown as ProposalNode;
    }

    /** a list as the page sees it, backed by a real selection model */
    function list<T>(): { getSelection: () => SelectionModel<T> } {
        const selection = new SelectionModel<T>(true);
        return { getSelection: () => selection };
    }

    /**
     * The page with its lists of sub-collections, materials and proposals in the tile view, and a
     * sidebar that records what it is asked to show.
     */
    function createHost() {
        const host = Object.create(CollectionContentComponent.prototype);
        Object.assign(host, {
            listCollections: list<Node>(),
            listReferences: list<Node>(),
            listTree: undefined,
            proposals: { listProposals: list<ProposalNode>() },
            viewState: { viewType: () => 'grid' },
            nodeHelper: {
                isNodeCollection: (node: Node) =>
                    !!node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION),
            },
            editorialSidebarService: {
                scope: signal(null),
                nodes: signal<Node[]>(null),
                showEntryList: jasmine.createSpy('showEntryList'),
                close: jasmine.createSpy('close'),
            },
        });
        return host;
    }

    const selected = (host: any, name: string): string[] => {
        const target = name === 'proposals' ? host.proposals.listProposals : host[name];
        return target.getSelection().selected.map((node: Node) => node.ref.id);
    };

    it('recognizes a proposal by the proposal it carries or by its type', () => {
        const host = createHost();
        expect(host.isProposal(proposal('p'))).toBeTrue();
        expect(
            host.isProposal({ type: RestConstants.CCM_TYPE_COLLECTION_PROPOSAL } as Node),
        ).toBeTrue();
        expect(host.isProposal(material('m'))).toBeFalse();
    });

    it('selects a clicked proposal alone and shows its actions & options', () => {
        const host = createHost();
        host.listCollections.getSelection().select(collection('c'));
        const p = proposal('p');
        host.selectClickedNode({ element: p });
        expect(selected(host, 'proposals')).toEqual(['p']);
        expect(selected(host, 'listCollections')).toEqual([]);
        expect(host.editorialSidebarService.showEntryList).toHaveBeenCalledWith([p]);
    });

    it('keeps the selection when the only selected proposal is clicked again', () => {
        const host = createHost();
        const p = proposal('p');
        host.selectClickedNode({ element: p });
        host.editorialSidebarService.showEntryList.calls.reset();
        host.selectClickedNode({ element: p });
        expect(selected(host, 'proposals')).toEqual(['p']);
        expect(host.editorialSidebarService.showEntryList).not.toHaveBeenCalled();
    });

    it('drops the selected proposals when a material is clicked', () => {
        const host = createHost();
        host.selectClickedNode({ element: proposal('p') });
        host.selectClickedNode({ element: material('m') });
        expect(selected(host, 'listReferences')).toEqual(['m']);
        expect(selected(host, 'proposals')).toEqual([]);
    });

    it('drops the selected content when proposals are selected', () => {
        const host = createHost();
        host.listCollections.getSelection().select(collection('c'));
        host.listReferences.getSelection().select(material('m'));
        const p = proposal('p');
        host.onProposalsSelectionChange([p]);
        expect(selected(host, 'listCollections')).toEqual([]);
        expect(selected(host, 'listReferences')).toEqual([]);
        expect(host.editorialSidebarService.showEntryList).toHaveBeenCalledWith([p]);
    });

    it('closes the sidebar when the proposals it describes are deselected', () => {
        const host = createHost();
        host.editorialSidebarService.nodes.set([proposal('p')]);
        host.onProposalsSelectionChange([]);
        expect(host.editorialSidebarService.close).toHaveBeenCalled();
    });

    it('keeps the sidebar of other nodes when no proposal is selected', () => {
        const host = createHost();
        host.editorialSidebarService.nodes.set([collection('c')]);
        host.onProposalsSelectionChange([]);
        expect(host.editorialSidebarService.close).not.toHaveBeenCalled();
    });

    it('drops the selected proposals once content is selected, but not without', () => {
        const host = createHost();
        host.proposals.listProposals.getSelection().select(proposal('p'));
        host.clearProposalsWhenContentSelected();
        expect(selected(host, 'proposals')).toEqual(['p']);
        host.listReferences.getSelection().select(material('m'));
        host.clearProposalsWhenContentSelected();
        expect(selected(host, 'proposals')).toEqual([]);
    });
});

describe('CollectionContentComponent range selection of sub-collections and materials', () => {
    const shown = ['a', 'b', 'c', 'd', 'e'].map((id) => collection(id));
    const materials = ['m1', 'm2', 'm3'].map((id) => material(id));

    /**
     * The tile view with the shown sub-collections and materials, and a sidebar that records what
     * it shows.
     */
    function createHost() {
        const host = Object.create(CollectionContentComponent.prototype);
        const selection = new SelectionModel<Node>(true);
        const referencesSelection = new SelectionModel<Node>(true);
        Object.assign(host, {
            interactionType: 0,
            listCollections: { getSelection: () => selection },
            listReferences: { getSelection: () => referencesSelection },
            listTree: undefined,
            proposals: undefined,
            selectionAnchor: null,
            dataSourceCollections: { getData: () => shown },
            dataSourceReferences: { getData: () => materials },
            viewState: { viewType: () => 'grid' },
            nodeHelper: {
                isNodeCollection: (node: Node) =>
                    !!node.aspects?.includes(RestConstants.CCM_ASPECT_COLLECTION),
            },
            editorialSidebarService: {
                scope: signal(null),
                showEntryList: jasmine.createSpy('showEntryList'),
            },
        });
        return host;
    }

    const click = (host: any, id: string, keys: { shiftKey?: boolean; ctrlKey?: boolean } = {}) =>
        host.onCollectionClick({ element: shown.find((node) => node.ref.id === id), ...keys });
    const clickMaterial = (
        host: any,
        id: string,
        keys: { shiftKey?: boolean; ctrlKey?: boolean } = {},
    ) => host.clickElementEvent({ element: materials.find((node) => node.ref.id === id), ...keys });
    const selected = (host: any): string[] =>
        host.listCollections.getSelection().selected.map((node: Node) => node.ref.id);
    const selectedMaterials = (host: any): string[] =>
        host.listReferences.getSelection().selected.map((node: Node) => node.ref.id);

    it('selects the range from the clicked anchor, in the order the tiles are shown', () => {
        const host = createHost();
        click(host, 'a');
        click(host, 'd', { shiftKey: true });
        expect(selected(host)).toEqual(['a', 'b', 'c', 'd']);
    });

    it('moves only the end of the range on further shift clicks', () => {
        const host = createHost();
        click(host, 'a');
        click(host, 'd', { shiftKey: true });
        click(host, 'c', { shiftKey: true });
        expect(selected(host)).toEqual(['a', 'b', 'c']);
    });

    it('ends the range with a regular click, which becomes the new anchor', () => {
        const host = createHost();
        click(host, 'a');
        click(host, 'd', { shiftKey: true });
        click(host, 'e');
        expect(selected(host)).toEqual(['e']);
        click(host, 'c', { shiftKey: true });
        expect(selected(host)).toEqual(['c', 'd', 'e']);
    });

    it('selects like a regular click while there is no anchor', () => {
        const host = createHost();
        click(host, 'c', { shiftKey: true });
        expect(selected(host)).toEqual(['c']);
    });

    it('adds the range to the selection together with the control key', () => {
        const host = createHost();
        click(host, 'e');
        click(host, 'a', { ctrlKey: true });
        click(host, 'b', { ctrlKey: true, shiftKey: true });
        expect(selected(host).sort()).toEqual(['a', 'b', 'e']);
    });

    it('reaches from an anchor among the sub-collections into the materials', () => {
        const host = createHost();
        click(host, 'd');
        clickMaterial(host, 'm2', { shiftKey: true });
        expect(selected(host)).toEqual(['d', 'e']);
        expect(selectedMaterials(host)).toEqual(['m1', 'm2']);
    });

    it('reaches from an anchor among the materials back into the sub-collections', () => {
        const host = createHost();
        clickMaterial(host, 'm1');
        click(host, 'e', { shiftKey: true });
        expect(selected(host)).toEqual(['e']);
        expect(selectedMaterials(host)).toEqual(['m1']);
    });

    it('adds a material to the selection with the control key', () => {
        const host = createHost();
        click(host, 'a');
        clickMaterial(host, 'm3', { ctrlKey: true });
        expect(selected(host)).toEqual(['a']);
        expect(selectedMaterials(host)).toEqual(['m3']);
    });
});

describe('CollectionContentComponent sidebar following the selection', () => {
    function createHost(selection: Node[], shownInSidebar: Node[] = null) {
        const host = Object.create(CollectionContentComponent.prototype);
        Object.assign(host, {
            interactionType: 0,
            addInProgress: false,
            selection,
            editorialSidebarService: {
                scope: signal(null),
                nodes: signal<Node[]>(shownInSidebar),
                sidebarOpened: signal(false),
                handleSelection: jasmine.createSpy('handleSelection'),
                close: jasmine.createSpy('close'),
            },
        });
        return host;
    }

    it('opens the sidebar for several selected nodes and hands them over', () => {
        const host = createHost([collection('a'), material('b')]);
        host.syncSidebarWithSelection();
        expect(host.editorialSidebarService.sidebarOpened()).toBeTrue();
        const [change] = host.editorialSidebarService.handleSelection.calls.mostRecent().args;
        expect(change.source.selected.map((node: Node) => node.ref.id)).toEqual(['a', 'b']);
    });

    it('closes the sidebar once nothing is selected', () => {
        const host = createHost([], [material('b')]);
        host.syncSidebarWithSelection();
        expect(host.editorialSidebarService.close).toHaveBeenCalled();
    });

    it('keeps the sidebar of the selected proposals', () => {
        const proposal = { ref: { id: 'p' }, proposal: { ref: { id: 'x' } } } as unknown as Node;
        const host = createHost([], [proposal]);
        host.syncSidebarWithSelection();
        expect(host.editorialSidebarService.close).not.toHaveBeenCalled();
    });

    it('leaves the sidebar alone while nodes added from it are reloaded', () => {
        const host = createHost([material('b')]);
        host.addInProgress = true;
        host.syncSidebarWithSelection();
        expect(host.editorialSidebarService.handleSelection).not.toHaveBeenCalled();
        expect(host.editorialSidebarService.sidebarOpened()).toBeFalse();
    });
});

describe('CollectionContentComponent selection across views', () => {
    const shown = [collection('a'), collection('b')];
    const materials = [material('m1'), material('m2')];
    const copy = (node: Node) => ({ ...node } as Node);
    const treeNodes = [{ ref: { id: '-root-' } } as Node, ...shown, ...materials].map(copy);

    /**
     * A view of the shown sub-collections and materials, whose lists report every change of their
     * selection like the rendered lists do.
     */
    function createHost(viewType: 'grid' | 'list' | 'tree') {
        const host = Object.create(CollectionContentComponent.prototype);
        const lists = {
            collections: new SelectionModel<Node>(true),
            references: new SelectionModel<Node>(true),
            tree: new SelectionModel<Node>(true),
        };
        Object.values(lists).forEach((list) =>
            list.changed.subscribe(() => host.syncSidebarWithSelection()),
        );
        Object.assign(host, {
            interactionType: 0,
            addInProgress: false,
            restoringSelection: false,
            selection: [],
            shownViewType: viewType,
            listCollections: { getSelection: () => lists.collections },
            listReferences: { getSelection: () => lists.references },
            listTree: { getSelection: () => lists.tree },
            dataSourceCollections: { getData: () => shown },
            dataSourceReferences: { getData: () => materials },
            dataSourceTree: { getData: () => treeNodes },
            viewState: { viewType: () => viewType },
            updateSelection: jasmine.createSpy('updateSelection'),
            editorialSidebarService: {
                scope: signal(null),
                nodes: signal<Node[]>(null),
                sidebarOpened: signal(false),
                handleSelection: jasmine.createSpy('handleSelection'),
                close: jasmine.createSpy('close'),
            },
        });
        return { host, lists };
    }

    const ids = (list: SelectionModel<Node>) => list.selected.map((node) => node.ref.id);

    it('keeps the selection when switching between tiles and list', () => {
        const { host, lists } = createHost('list');
        lists.collections.select(shown[0]);
        lists.references.select(materials[1]);
        host.selection = [shown[0], materials[1]];
        host.editorialSidebarService.close.calls.reset();
        host.onViewTypeChange('grid');
        expect(ids(lists.collections)).toEqual(['a']);
        expect(ids(lists.references)).toEqual(['m2']);
        expect(host.editorialSidebarService.close).not.toHaveBeenCalled();
    });

    it('selects the same nodes in the tree by their ids', () => {
        const { host, lists } = createHost('tree');
        host.restoreSelection(['b', 'm1', '-root-', 'unknown']);
        expect(ids(lists.tree)).toEqual(['b', 'm1']);
        expect(host.updateSelection).toHaveBeenCalled();
    });

    it('splits the selection of the tree into sub-collections and materials', () => {
        const { host, lists } = createHost('grid');
        host.restoreSelection(['a', 'm2']);
        expect(ids(lists.collections)).toEqual(['a']);
        expect(ids(lists.references)).toEqual(['m2']);
        expect(ids(lists.tree)).toEqual([]);
    });

    it('leaves the sidebar as it is while the selection is carried over', () => {
        const { host } = createHost('tree');
        host.restoreSelection(['a', 'm1']);
        expect(host.editorialSidebarService.handleSelection).not.toHaveBeenCalled();
        expect(host.editorialSidebarService.sidebarOpened()).toBeFalse();
        expect(host.restoringSelection).toBeFalse();
    });
});
