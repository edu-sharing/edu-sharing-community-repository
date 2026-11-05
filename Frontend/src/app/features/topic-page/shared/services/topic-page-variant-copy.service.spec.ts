import { TestBed } from '@angular/core/testing';
import { Node } from 'ngx-edu-sharing-api';
import {
    DEFAULT_PAGE_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_WIDGET_CONFIG_PROP,
} from '../types/custom-definitions';
import { PageVariantConfig } from '../types/page-variant-config';
import { SwimlaneRepeatService } from './swimlane-repeat.service';
import { TopicPageHelperService } from './topic-page-helper.service';
import { TopicPageVariantCopyService } from './topic-page-variant-copy.service';

const node = (id: string, properties: object = {}): Node =>
    ({ ref: { id }, name: id, properties } as unknown as Node);

const variantNode = (id: string, config: object): Node =>
    node(id, { [DEFAULT_PAGE_VARIANT_CONFIG_PROP]: [JSON.stringify(config)] });

describe('TopicPageVariantCopyService', () => {
    let service: TopicPageVariantCopyService;
    let helper: jasmine.SpyObj<TopicPageHelperService>;
    let created: number;
    // properties written per node ID, as JSON parsed where the value is JSON
    let written: Map<string, { [property: string]: unknown }>;

    beforeEach(() => {
        created = 0;
        written = new Map();
        helper = jasmine.createSpyObj<TopicPageHelperService>('TopicPageHelperService', [
            'createChild',
            'copyNodeAsChildIfExists',
            'getNodeIfAvailable',
            'retrievePageVariantProperties',
            'setPropertyAndRetrieveUpdatedNode',
        ]);
        helper.createChild.and.callFake(async () => node('created-' + ++created));
        helper.copyNodeAsChildIfExists.and.callFake(async (sourceId: string) =>
            node('copy-of-' + sourceId.replace(/^.*\//, '')),
        );
        helper.retrievePageVariantProperties.and.resolveTo({});
        helper.setPropertyAndRetrieveUpdatedNode.and.callFake(
            async (id: string, property: string, value: string) => {
                written.set(id, { ...written.get(id), [property]: JSON.parse(value) });
                return node(id);
            },
        );
        TestBed.configureTestingModule({
            providers: [
                TopicPageVariantCopyService,
                { provide: TopicPageHelperService, useValue: helper },
                {
                    provide: SwimlaneRepeatService,
                    useValue: { expandRepeats: async (): Promise<void> => undefined },
                },
            ],
        });
        service = TestBed.inject(TopicPageVariantCopyService);
    });

    const context = { collectionId: 'collection', title: 'Topic' };
    const config = (widgetNodeId: string): object => ({
        structure: { swimlanes: [{ id: 's', grid: [{ nodeId: widgetNodeId }] }] },
    });

    describe('copyPageConfig', () => {
        it('copies each variant with its widgets and keeps the shown one selected', async () => {
            const result = await service.copyPageConfig(
                node('collection'),
                [variantNode('a', config('widget-a')), variantNode('b', config('widget-b'))],
                'workspace://SpacesStore/b',
                0,
                null,
                context,
            );
            // page config folder, then one node per variant
            void expect(result.pageConfigNode.ref.id).toBe('created-1');
            void expect(result.selectedVariantPosition).toBe(1);
            const pageConfig = written.get('created-1')[DEFAULT_PAGE_CONFIG_PROP] as {
                variants: string[];
                default: string;
            };
            void expect(pageConfig.variants.length).toBe(2);
            void expect(pageConfig.default).toBe(pageConfig.variants[1]);
            const copiedB = written.get('created-3')[DEFAULT_PAGE_VARIANT_CONFIG_PROP] as {
                structure: {
                    swimlanes: { grid: { nodeId: string; temporaryNodeId?: string }[] }[];
                };
            };
            void expect(copiedB.structure.swimlanes[0].grid[0].nodeId).toContain(
                'copy-of-widget-b',
            );
            void expect(copiedB.structure.swimlanes[0].grid[0].temporaryNodeId).toBeUndefined();
        });

        it('falls back to the default position when the shown variant is not copied', async () => {
            const result = await service.copyPageConfig(
                node('collection'),
                [variantNode('a', config('widget-a')), variantNode('b', config('widget-b'))],
                '',
                1,
                null,
                context,
            );
            void expect(result.selectedVariantPosition).toBeNull();
            const pageConfig = written.get('created-1')[DEFAULT_PAGE_CONFIG_PROP] as {
                variants: string[];
                default: string;
            };
            void expect(pageConfig.default).toBe(pageConfig.variants[1]);
        });

        it('creates a pending widget in the copy instead of copying the inherited one', async () => {
            await service.copyPageConfig(
                node('collection'),
                [variantNode('a', config('widget-a'))],
                '',
                -1,
                {
                    variantNodeId: 'a',
                    swimlaneIndex: 0,
                    gridIndex: 0,
                    widget: { widgetConfig: { title: 'new' } } as never,
                },
                context,
            );
            // page config folder, variant node, pending widget
            void expect(helper.createChild.calls.count()).toBe(3);
            void expect(helper.createChild.calls.argsFor(2)[4]).toEqual({
                [DEFAULT_WIDGET_CONFIG_PROP]: JSON.stringify({ title: 'new' }),
            });
            void expect(helper.copyNodeAsChildIfExists).not.toHaveBeenCalled();
            const copied = written.get('created-2')[
                DEFAULT_PAGE_VARIANT_CONFIG_PROP
            ] as PageVariantConfig;
            void expect(copied.structure.swimlanes[0].grid[0].nodeId).toContain('created-3');
        });
    });

    describe('persistRelinkedVariantConfig', () => {
        it('turns a template patch without a node into the config of a new widget', async () => {
            const variantConfig = {
                structure: { swimlanes: [{ id: 's', grid: [{ configPatch: { title: 'x' } }] }] },
            } as unknown as PageVariantConfig;
            await service.persistRelinkedVariantConfig(variantConfig, node('target'));
            const tile = variantConfig.structure.swimlanes[0].grid[0];
            void expect(tile.nodeId).toContain('created-1');
            void expect(tile.configPatch).toBeUndefined();
        });

        it('leaves a target without structure untouched', async () => {
            const target: Node = node('target');
            const result: Node = await service.persistRelinkedVariantConfig(
                {} as PageVariantConfig,
                target,
            );
            void expect(result).toBe(target);
            void expect(helper.setPropertyAndRetrieveUpdatedNode).not.toHaveBeenCalled();
        });
    });
});
