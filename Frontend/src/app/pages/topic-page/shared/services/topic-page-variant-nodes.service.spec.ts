import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CopyOption } from '../../editor/add-page-variant-or-template-dialog/add-page-variant-or-template-dialog.component';
import {
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_PROFILING_PROPS,
    DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
} from '../types/custom-definitions';
import { TopicPageHelperService } from './topic-page-helper.service';
import { TopicPageVariantNodesService } from './topic-page-variant-nodes.service';

const node = (id: string, name: string = id, properties: object = {}): Node =>
    ({ ref: { id }, name, properties } as unknown as Node);

describe('TopicPageVariantNodesService', () => {
    let service: TopicPageVariantNodesService;
    let helper: jasmine.SpyObj<TopicPageHelperService>;

    beforeEach(() => {
        helper = jasmine.createSpyObj<TopicPageHelperService>('TopicPageHelperService', [
            'createChild',
            'getNode',
            'getNodeUncached',
            'retrievePageVariantProperties',
            'setProperty',
        ]);
        helper.createChild.and.callFake(async (_parent: string, _type: string, name: string) =>
            node('new', name),
        );
        helper.getNodeUncached.and.callFake(async (id: string) => node(id, 'reloaded'));
        helper.setProperty.and.resolveTo();
        helper.retrievePageVariantProperties.and.resolveTo({});
        TestBed.configureTestingModule({
            providers: [
                TopicPageVariantNodesService,
                { provide: TopicPageHelperService, useValue: helper },
                { provide: TranslateService, useValue: { instant: (key: string) => key } },
            ],
        });
        service = TestBed.inject(TopicPageVariantNodesService);
    });

    describe('createVariantNode', () => {
        it('names the node after its source without the UUIDs of earlier copies', async () => {
            const source = node(
                'source',
                'Variant_0f8fad5b-d9cb-469f-a165-70867728950e_7c9e6679-7425-40de-944b-e07fc1f90ae7',
            );
            const created: Node = await service.createVariantNode('parent', source, {});
            const name: string = helper.createChild.calls.mostRecent().args[2];
            void expect(name).toMatch(/^Variant_[0-9a-f-]{36}$/);
            void expect(created.name).toBe(name);
            void expect(helper.setProperty).not.toHaveBeenCalled();
        });

        it('takes over the profiling of the source', async () => {
            const prop: string = DEFAULT_PAGE_VARIANT_PROFILING_PROPS[0];
            const source = node('source', 'Variant', { [prop]: ['learner'] });
            const created: Node = await service.createVariantNode('parent', source, {});
            void expect(helper.setProperty).toHaveBeenCalledWith('new', prop, ['learner']);
            void expect(created.name).toBe('reloaded');
        });
    });

    describe('templateVariantProperties', () => {
        it('makes a template source the parent of a child template', async () => {
            const source = node('parent-template', 'T', {
                [DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]: ['3'],
            });
            const properties = await service.templateVariantProperties(source, CopyOption.Template);
            void expect(properties[DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]).toBe('true');
            void expect(properties[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]).toBe(
                '3:' + DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
            );
            void expect(properties[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP]).toContain(
                'parent-template',
            );
            void expect(properties[RestConstants.LOM_PROP_TITLE]).toBe(
                'TOPIC_PAGE.DEFAULT_PAGE_TEMPLATE_NAME',
            );
        });

        it('makes any other source a root template keeping its title', async () => {
            helper.retrievePageVariantProperties.and.resolveTo({
                [RestConstants.LOM_PROP_TITLE]: 'Page',
            });
            const properties = await service.templateVariantProperties(
                node('page'),
                CopyOption.TopicPage,
            );
            void expect(properties[DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]).toBe(
                DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION,
            );
            void expect(properties[DEFAULT_PAGE_VARIANT_TEMPLATE_REF_PROP]).toBeUndefined();
            void expect(properties[RestConstants.LOM_PROP_TITLE]).toBe('Page');
        });
    });

    describe('getOrCreatePropagateConfig', () => {
        it('creates an empty folder below the collection without a ref', async () => {
            const result = await service.getOrCreatePropagateConfig(node('collection'), null);
            void expect(helper.createChild.calls.mostRecent().args[0]).toBe('collection');
            void expect(result.pageConfig).toEqual({ variants: [] });
        });

        it('reads the referenced folder', async () => {
            helper.getNode.and.resolveTo(node('folder'));
            const result = await service.getOrCreatePropagateConfig(
                node('collection'),
                'workspace://SpacesStore/folder',
            );
            void expect(helper.getNode).toHaveBeenCalledWith('folder');
            void expect(helper.createChild).not.toHaveBeenCalled();
            void expect(result.pageConfig.variants).toEqual([]);
        });
    });

    describe('applyPersistedParameters', () => {
        it('writes every change except the title', async () => {
            const result: Node | null = await service.applyPersistedParameters(
                'target',
                new Map<string, string | string[]>([
                    [RestConstants.LOM_PROP_TITLE, 'Title'],
                    ['ccm:educationalcontext', ['primary']],
                ]),
            );
            void expect(helper.setProperty.calls.allArgs()).toEqual([
                ['target', 'ccm:educationalcontext', ['primary']],
            ]);
            void expect(result.ref.id).toBe('target');
        });

        it('writes nothing for a title-only change', async () => {
            const result: Node | null = await service.applyPersistedParameters(
                'target',
                new Map([[RestConstants.LOM_PROP_TITLE, 'Title']]),
            );
            void expect(helper.setProperty).not.toHaveBeenCalled();
            void expect(result).toBeNull();
        });
    });
});
