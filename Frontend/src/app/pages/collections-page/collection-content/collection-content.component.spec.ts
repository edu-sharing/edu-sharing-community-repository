import { Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Node } from 'ngx-edu-sharing-api';
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
    /**
     * Shows the created collection the way the list does after a reload, with the collection
     * checkboxes visible or not, and returns the options passed to the list.
     */
    function showCreated(checkboxesVisible: boolean): { select: boolean } {
        const addVirtualNodes = jasmine.createSpy('addVirtualNodes');
        const host = {
            createdCollections: [collection('created')],
            createdCollectionsUntil: Date.now() + 5000,
            listCollections: { addVirtualNodes },
            showCollectionCheckboxes: () => checkboxesVisible,
            elementRef: { nativeElement: document.createElement('div') },
            injector: TestBed.inject(Injector),
        };
        (CollectionContentComponent.prototype as any).selectCreatedCollections.call(host);
        expect(addVirtualNodes).toHaveBeenCalledTimes(1);
        const [nodes, options] = addVirtualNodes.calls.mostRecent().args;
        expect(nodes.map((node: Node) => node.ref.id)).toEqual(['created']);
        return options;
    }

    it('selects the created collections while the checkboxes are visible', () => {
        expect(showCreated(true)).toEqual({ select: true });
    });

    it('only shows the created collections while the tiles have no checkboxes', () => {
        expect(showCreated(false)).toEqual({ select: false });
    });
});
