import { PlatformLocation } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CollectionService, Node, NodeService, NodeServiceUnwrapped } from 'ngx-edu-sharing-api';
import { of } from 'rxjs';
import { DialogsService } from '../../../../features/dialogs/dialogs.service';
import { EditorialSidebarService } from '../../../../features/editorial-sidebar/editorial-sidebar.service';
import { PreviewSidebarService } from '../../../../features/editorial-sidebar/preview-sidebar/preview-sidebar.service';
import { Toast } from '../../../../services/toast';
import { GenericWidgetGlobalService } from '../../widgets/generic-widget/generic-widget-global.service';
import { DEFAULT_AI_CONFIG_PROP, DEFAULT_WIDGET_CONFIG_PROP } from '../types/custom-definitions';
import { WidgetConfigUpdatedEvent } from '../types/widget-config-updated-event';
import { TopicPageEventsService } from './topic-page-events.service';
import { TopicPageGlobalService } from './topic-page-global.service';
import { TopicPageHelperService } from './topic-page-helper.service';

const WIDGET_ID = 'aaaaaaaa-0000-0000-0000-000000000001';

const asNode = (id: string, properties: { [key: string]: string[] }): Node =>
    ({ ref: { id }, properties: JSON.parse(JSON.stringify(properties)) } as unknown as Node);

describe('TopicPageHelperService', () => {
    let properties: { [key: string]: string[] };
    // the answer of the cached node API, which lags behind the writes like its short-lived cache
    let cachedNode: Node;
    let helper: TopicPageHelperService;
    let events: TopicPageEventsService;

    beforeEach(() => {
        properties = { [DEFAULT_WIDGET_CONFIG_PROP]: ['{"headline":"before"}'] };
        cachedNode = asNode(WIDGET_ID, properties);
        TestBed.configureTestingModule({
            providers: [
                TopicPageHelperService,
                TopicPageEventsService,
                {
                    provide: NodeService,
                    useValue: {
                        getNode: () => of(cachedNode),
                        setProperty: (
                            _repo: string,
                            _id: string,
                            property: string,
                            value: string[],
                        ) => {
                            if (value) {
                                properties[property] = value;
                            } else {
                                delete properties[property];
                            }
                            return of(null);
                        },
                    },
                },
                {
                    provide: NodeServiceUnwrapped,
                    useValue: { getMetadata: () => of({ node: asNode(WIDGET_ID, properties) }) },
                },
                { provide: CollectionService, useValue: {} },
                { provide: DialogsService, useValue: {} },
                { provide: EditorialSidebarService, useValue: {} },
                { provide: GenericWidgetGlobalService, useValue: {} },
                { provide: PlatformLocation, useValue: {} },
                { provide: PreviewSidebarService, useValue: { getCurrentNode: () => of(null) } },
                { provide: Router, useValue: {} },
                {
                    provide: Toast,
                    useValue: { show: (): void => undefined, error: (): void => undefined },
                },
                { provide: TopicPageGlobalService, useValue: {} },
                { provide: TranslateService, useValue: { instant: (key: string) => key } },
            ],
        });
        helper = TestBed.inject(TopicPageHelperService);
        events = TestBed.inject(TopicPageEventsService);
    });

    it('returns the node as written, not an answer the node cache still holds', async () => {
        const node: Node = await helper.setPropertyAndRetrieveUpdatedNode(
            WIDGET_ID,
            DEFAULT_WIDGET_CONFIG_PROP,
            '{"headline":"after"}',
        );
        void expect(node.properties[DEFAULT_WIDGET_CONFIG_PROP]).toEqual(['{"headline":"after"}']);
    });

    it('reports the values an update of a widget config replaced', async () => {
        const emitted: WidgetConfigUpdatedEvent[] = [];
        events.widgetConfigUpdated.subscribe((event) => emitted.push(event));
        const pageVariantNode: Node = asNode('variant-1', {});

        await helper.persistConfig(
            WIDGET_ID,
            0,
            0,
            pageVariantNode,
            { headline: 'after' } as any,
            null,
            null,
        );

        void expect(emitted.length).toBe(1);
        void expect(emitted[0].pageVariantNode).toBe(pageVariantNode);
        void expect(emitted[0].widgetNodeId).toBe(WIDGET_ID);
        void expect(emitted[0].changes).toEqual([
            {
                property: DEFAULT_WIDGET_CONFIG_PROP,
                before: ['{"headline":"before"}'],
                after: ['{"headline":"after"}'],
            },
            { property: DEFAULT_AI_CONFIG_PROP, before: null, after: null },
        ]);
    });
});
