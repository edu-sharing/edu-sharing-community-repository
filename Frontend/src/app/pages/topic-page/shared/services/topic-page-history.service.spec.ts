import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import {
    DEFAULT_AI_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_WIDGET_CONFIG_PROP,
} from '../types/custom-definitions';
import { PageStructure } from '../types/page-structure';
import { TopicPageHelperService } from './topic-page-helper.service';
import { TopicPageHistoryHost, TopicPageHistoryService } from './topic-page-history.service';

const VARIANT_ID = 'variant-1';
const WIDGET_A = 'aaaaaaaa-0000-0000-0000-000000000001';
const WIDGET_B = 'bbbbbbbb-0000-0000-0000-000000000002';
const IMAGE_A = 'cccccccc-0000-0000-0000-000000000003';

const withTiles = (...nodeIds: string[]): PageStructure => ({
    swimlanes: [
        {
            id: 's1',
            grid: nodeIds.map((id) => ({
                cols: 6,
                rows: 1,
                nodeId: 'workspace://SpacesStore/' + id,
            })),
        },
    ],
});

// an in-memory repository holding the variant node and its widget children
class FakeRepository {
    nodes = new Map<string, { parent: string; properties: { [key: string]: string[] } }>();
    deleted: string[] = [];

    constructor(structure: PageStructure) {
        this.nodes.set(VARIANT_ID, { parent: 'page', properties: {} });
        this.writeStructure(structure);
        [WIDGET_A, WIDGET_B].forEach((id) =>
            this.nodes.set(id, { parent: VARIANT_ID, properties: {} }),
        );
        this.nodes.set(IMAGE_A, { parent: WIDGET_A, properties: {} });
    }

    readStructure(): PageStructure {
        return JSON.parse(
            this.nodes.get(VARIANT_ID).properties[DEFAULT_PAGE_VARIANT_CONFIG_PROP][0],
        ).structure;
    }

    writeStructure(structure: PageStructure): void {
        this.nodes.get(VARIANT_ID).properties[DEFAULT_PAGE_VARIANT_CONFIG_PROP] = [
            JSON.stringify({ structure }),
        ];
    }

    readonly helper = {
        getNodeUncached: async (id: string): Promise<Node> => {
            const node = this.nodes.get(id);
            if (!node) {
                throw new Error('not found: ' + id);
            }
            return {
                ref: { id },
                parent: { id: node.parent },
                properties: JSON.parse(JSON.stringify(node.properties)),
            } as unknown as Node;
        },
        getNodeIfAvailable: async (id: string): Promise<Node | null> =>
            this.nodes.has(id) ? this.helper.getNodeUncached(id) : null,
        setProperty: async (id: string, property: string, value: string[]): Promise<void> => {
            this.nodes.get(id).properties[property] = value;
        },
        resetProperty: async (id: string, property: string): Promise<void> => {
            delete this.nodes.get(id).properties[property];
        },
        deleteNode: async (id: string): Promise<void> => {
            this.nodes.delete(id);
            this.deleted.push(id);
        },
        deleteNodeIfExists: async (id: string): Promise<void> => {
            if (this.nodes.has(id)) {
                await this.helper.deleteNode(id);
            }
        },
        displayErrorToast: (): void => undefined,
    };
}

describe('TopicPageHistoryService', () => {
    let repository: FakeRepository;
    let history: TopicPageHistoryService;
    let host: TopicPageHistoryHost;
    let restoredNodeIds: string[];
    let variantNodeId: string;
    let recording: boolean;

    // runs a change of the page structure the way the editor does
    const change = (structure: PageStructure, options: { coalesceKey?: string } = {}) =>
        history.record('STEP', async () => repository.writeStructure(structure), options);

    beforeEach(() => {
        repository = new FakeRepository(withTiles(WIDGET_A));
        restoredNodeIds = [];
        variantNodeId = VARIANT_ID;
        recording = true;
        TestBed.configureTestingModule({
            providers: [
                TopicPageHistoryService,
                { provide: TopicPageHelperService, useValue: repository.helper },
                { provide: Toast, useValue: { show: (): void => undefined } },
                { provide: TranslateService, useValue: { instant: (key: string) => key } },
            ],
        });
        history = TestBed.inject(TopicPageHistoryService);
        host = {
            variantNodeId: () => variantNodeId,
            canRecord: () => recording,
            readStructure: () => repository.readStructure(),
            writeStructure: async (structure: PageStructure) =>
                repository.writeStructure(structure),
            nodePropertyRestored: async (nodeId: string) => void restoredNodeIds.push(nodeId),
            setBusy: (): void => undefined,
        };
        history.attach(host);
    });

    it('undoes and redoes a structure change', async () => {
        await change(withTiles(WIDGET_A, WIDGET_B));
        void expect(history.canUndo()).toBeTrue();

        await history.undo();
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_A));
        void expect(history.canRedo()).toBeTrue();

        await history.redo();
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_A, WIDGET_B));
    });

    it('records nothing for a change that leaves the structure as it was', async () => {
        await change(withTiles(WIDGET_A));
        void expect(history.canUndo()).toBeFalse();
    });

    it('merges changes sharing a coalesce key into one step', async () => {
        await change({ swimlanes: [{ id: 's1', heading: 'a' }] }, { coalesceKey: 'heading:s1' });
        await change({ swimlanes: [{ id: 's1', heading: 'ab' }] }, { coalesceKey: 'heading:s1' });

        await history.undo();
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_A));
        void expect(history.canUndo()).toBeFalse();
    });

    it('keeps the node of a removed tile until no step can restore it', async () => {
        await change(withTiles());
        void expect(repository.deleted).toEqual([]);

        await history.undo();
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_A));
        void expect(repository.deleted).toEqual([]);

        // the redo step removing the tile is discarded, so the node is linked again for good
        await change(withTiles(WIDGET_A, WIDGET_B));
        void expect(repository.deleted).toEqual([]);
    });

    it('deletes an orphan once the steps referencing it are gone', async () => {
        await change(withTiles());
        await history.reset();
        void expect(repository.deleted).toEqual([WIDGET_A]);
    });

    it('deletes the node of an undone addition when the redo branch is discarded', async () => {
        await change(withTiles(WIDGET_A, WIDGET_B));
        await history.undo();
        void expect(repository.deleted).toEqual([]);

        await change(withTiles());
        void expect(repository.deleted).toEqual([WIDGET_B]);
    });

    it('never deletes a node that is not a child of the node that linked it', async () => {
        repository.nodes.get(WIDGET_A).parent = 'another-variant';
        await change(withTiles());
        await history.reset();
        void expect(repository.deleted).toEqual([]);
    });

    it('refuses to undo over a change made elsewhere', async () => {
        await change(withTiles(WIDGET_A, WIDGET_B));
        repository.writeStructure(withTiles(WIDGET_B));

        await history.undo();
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_B));
        void expect(history.canUndo()).toBeFalse();
    });

    it('refuses to undo a widget config edit once the widget node was deleted elsewhere', async () => {
        await history.recordApplied('STEP', [
            {
                kind: 'nodeProperty',
                nodeId: WIDGET_A,
                property: DEFAULT_WIDGET_CONFIG_PROP,
                before: null,
                after: [JSON.stringify({})],
            },
        ]);
        repository.nodes.get(WIDGET_A).properties[DEFAULT_WIDGET_CONFIG_PROP] = [
            JSON.stringify({}),
        ];
        await repository.helper.deleteNode(WIDGET_A);

        await history.undo();
        void expect(repository.nodes.has(WIDGET_A)).toBeFalse();
        void expect(history.canUndo()).toBeFalse();
    });

    it('restores the structure a failed change started from', async () => {
        await history.record('STEP', async (tx) => {
            repository.writeStructure(withTiles(WIDGET_A, WIDGET_B));
            tx.fail();
        });
        void expect(repository.readStructure()).toEqual(withTiles(WIDGET_A));
        void expect(history.canUndo()).toBeFalse();
    });

    it('undoes a widget config edit and releases the replaced upload only afterwards', async () => {
        const withUpload = [JSON.stringify({ userUploadedNodeId: IMAGE_A })];
        const withoutUpload = [JSON.stringify({})];
        repository.nodes.get(WIDGET_A).properties[DEFAULT_WIDGET_CONFIG_PROP] = withoutUpload;
        await history.recordApplied('STEP', [
            {
                kind: 'nodeProperty',
                nodeId: WIDGET_A,
                property: DEFAULT_WIDGET_CONFIG_PROP,
                before: withUpload,
                after: withoutUpload,
            },
        ]);

        await history.undo();
        void expect(repository.nodes.get(WIDGET_A).properties[DEFAULT_WIDGET_CONFIG_PROP]).toEqual(
            withUpload,
        );
        void expect(restoredNodeIds).toEqual([WIDGET_A]);

        // the restored config links the upload, so discarding the history keeps it
        await history.reset();
        void expect(repository.deleted).toEqual([]);
    });

    it('reports a restored node once, after all of its properties are written', async () => {
        const configs = { before: [JSON.stringify({})], after: [JSON.stringify({ a: 1 })] };
        const prompts = { before: null as string[] | null, after: ['prompt'] };
        const widget = repository.nodes.get(WIDGET_A).properties;
        widget[DEFAULT_WIDGET_CONFIG_PROP] = configs.after;
        widget[DEFAULT_AI_CONFIG_PROP] = prompts.after;
        const seen: unknown[] = [];
        host.nodePropertyRestored = async (nodeId: string) =>
            void seen.push([
                nodeId,
                widget[DEFAULT_WIDGET_CONFIG_PROP],
                widget[DEFAULT_AI_CONFIG_PROP],
            ]);
        await history.recordApplied('STEP', [
            {
                kind: 'nodeProperty',
                nodeId: WIDGET_A,
                property: DEFAULT_WIDGET_CONFIG_PROP,
                ...configs,
            },
            {
                kind: 'nodeProperty',
                nodeId: WIDGET_A,
                property: DEFAULT_AI_CONFIG_PROP,
                ...prompts,
            },
        ]);

        await history.undo();
        void expect(seen).toEqual([[WIDGET_A, configs.before, undefined]]);
    });

    it('does not merge a change into a step an undo brought back to the top', async () => {
        await change({ swimlanes: [{ id: 's1', heading: 'a' }] }, { coalesceKey: 'heading:s1' });
        await change({ swimlanes: [{ id: 's2' }] });
        await history.undo();
        await change({ swimlanes: [{ id: 's1', heading: 'ab' }] }, { coalesceKey: 'heading:s1' });

        await history.undo();
        void expect(repository.readStructure()).toEqual({
            swimlanes: [{ id: 's1', heading: 'a' }],
        });
        void expect(history.canUndo()).toBeTrue();
    });

    it('keeps the latest 25 steps and releases what only the evicted ones referenced', async () => {
        await change(withTiles());
        for (let i = 0; i < 25; i++) {
            await change({ swimlanes: [{ id: 'step-' + i }] });
        }
        void expect(repository.deleted).toEqual([WIDGET_A]);

        let undone: number = 0;
        while (history.canUndo()) {
            await history.undo();
            undone++;
        }
        void expect(undone).toBe(25);
        void expect(repository.readStructure()).toEqual(withTiles());
    });

    it('starts over when a change switches the page to another variant node', async () => {
        await change(withTiles(WIDGET_A, WIDGET_B));
        await history.record('STEP', async () => {
            variantNodeId = 'variant-2';
        });
        void expect(history.canUndo()).toBeFalse();
    });

    it('records nothing while changes are not recorded', async () => {
        recording = false;
        await change(withTiles(WIDGET_A, WIDGET_B));
        await history.recordApplied('STEP', [
            { kind: 'nodeProperty', nodeId: WIDGET_A, property: 'p', before: null, after: ['x'] },
        ]);
        void expect(history.canUndo()).toBeFalse();
        void expect(history.markOrphaned(WIDGET_B, VARIANT_ID)).toBeFalse();
    });

    it('names a step with its label parameters, keeping those of the step a change merges into', async () => {
        await history.record('ADD', async () => repository.writeStructure(withTiles()), {
            coalesceKey: 'k',
            labelParams: { widget: 'FIRST' },
        });
        await history.record('ADD', async () => repository.writeStructure(withTiles(WIDGET_B)), {
            coalesceKey: 'k',
            labelParams: { widget: 'SECOND' },
        });
        void expect(history.nextUndoStep().labelKey).toBe('ADD');
        void expect(history.nextUndoStep().labelParams).toEqual({ widget: 'FIRST' });
    });
});
