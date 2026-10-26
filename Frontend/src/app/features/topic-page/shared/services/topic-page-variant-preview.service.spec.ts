import { TestBed } from '@angular/core/testing';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../../services/toast';
import {
    CopyOption,
    PageVariantSearchState,
} from '../../editor/add-page-variant-or-template-dialog/add-page-variant-or-template-dialog.component';
import { VariantPreviewRestoreState } from '../types/variant-preview';
import { TopicPageHelperService } from './topic-page-helper.service';
import {
    TopicPageVariantPreviewService,
    VariantPreviewHost,
} from './topic-page-variant-preview.service';

const node = (id: string): Node => ({ ref: { id } } as unknown as Node);

describe('TopicPageVariantPreviewService', () => {
    let service: TopicPageVariantPreviewService;
    let host: jasmine.SpyObj<VariantPreviewHost>;
    let helper: jasmine.SpyObj<TopicPageHelperService>;
    let availableIds: Set<string>;
    const restoreState = { selectedVariables: 'null' } as VariantPreviewRestoreState;
    const nodes: Node[] = [node('a'), node('b'), node('c')];

    beforeEach(() => {
        availableIds = new Set(['a', 'b', 'c']);
        helper = jasmine.createSpyObj<TopicPageHelperService>('TopicPageHelperService', [
            'getNodeIfAvailable',
            'getSelectedVariables',
            'displayErrorToast',
        ]);
        helper.getNodeIfAvailable.and.callFake(async (id: string) =>
            availableIds.has(id) ? node(id) : null,
        );
        helper.getSelectedVariables.and.returnValue(null);
        host = jasmine.createSpyObj<VariantPreviewHost>('VariantPreviewHost', [
            'isGlobalTemplateOpen',
            'enterPreview',
            'showNode',
            'restorePage',
            'reloadPage',
            'createFrom',
        ]);
        host.enterPreview.and.returnValue(restoreState);
        host.showNode.and.resolveTo();
        host.reloadPage.and.resolveTo();
        host.createFrom.and.resolveTo();
        TestBed.configureTestingModule({
            providers: [
                TopicPageVariantPreviewService,
                { provide: TopicPageHelperService, useValue: helper },
                { provide: Toast, useValue: jasmine.createSpyObj('Toast', ['error']) },
            ],
        });
        service = TestBed.inject(TopicPageVariantPreviewService);
        service.attach(host);
    });

    const start = (shown: Node = nodes[1]): Promise<void> =>
        service.start('variant', CopyOption.TopicPage, { nodes } as PageVariantSearchState, shown);

    it('captures the page once and shows the picked node of the listed ones', async () => {
        await start();
        await start(nodes[2]);
        void expect(host.enterPreview).toHaveBeenCalledTimes(1);
        void expect(service.preview().index).toBe(2);
        void expect(service.node().ref.id).toBe('c');
        void expect(host.showNode).toHaveBeenCalledTimes(2);
    });

    it('does not start while a global template is open', async () => {
        host.isGlobalTemplateOpen.and.returnValue(true);
        await start();
        void expect(service.preview()).toBeNull();
        void expect(host.enterPreview).not.toHaveBeenCalled();
    });

    it('drops an unavailable node and shows its neighbour instead', async () => {
        await start(nodes[0]);
        availableIds.delete('b');
        await service.showAt(1);
        void expect(service.preview().nodes.map((n: Node) => n.ref.id)).toEqual(['a', 'c']);
        void expect(service.node().ref.id).toBe('c');
    });

    it('restores the page on end and reloads it only for a changed profiling selection', async () => {
        await start();
        await service.end();
        void expect(host.restorePage).toHaveBeenCalledWith(restoreState);
        void expect(host.reloadPage).not.toHaveBeenCalled();

        await start();
        helper.getSelectedVariables.and.returnValue({ ccm_educationalcontext: ['x'] });
        await service.end();
        void expect(host.reloadPage).toHaveBeenCalledTimes(1);
        void expect(service.preview()).toBeNull();
    });

    it('ends the preview before creating from the shown node', async () => {
        await start();
        await service.use();
        void expect(host.restorePage).toHaveBeenCalled();
        const [kind, created, copyOption] = host.createFrom.calls.mostRecent().args;
        void expect([kind, created.ref.id, copyOption]).toEqual([
            'variant',
            'b',
            CopyOption.TopicPage,
        ]);
    });
});
