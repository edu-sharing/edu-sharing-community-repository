import { TestBed } from '@angular/core/testing';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { ModuleInfoControllerService } from 'ngx-rendering-service-api';

import { ModuleInfoService } from './module-info.service';
import { UrlEmbeddings } from './lib/dto/FrontendModuleConfig';

describe('ModuleInfoService', () => {
    let service: ModuleInfoService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ModuleInfoService, { provide: ModuleInfoControllerService, useValue: {} }],
        });
        service = TestBed.inject(ModuleInfoService);
    });

    function serloNode(): Node {
        return {
            properties: {
                [RestConstants.CCM_PROP_REPLICATIONSOURCE]: ['serlo'],
                [RestConstants.CCM_PROP_REPLICATIONSOURCEID]: ['12345'],
                'ccm:wwwurl': ['https://de.serlo.org/mathe/12345/title'],
            },
        } as unknown as Node;
    }

    it('routes a serlo node to the url module with the SERLO embedding', () => {
        const config = service.getFrontendModuleSetting(serloNode());

        expect(config.module).toBe('url');
        expect(config.urlModuleConfig?.embedding).toBe(UrlEmbeddings.SERLO);
        expect(config.urlModuleConfig?.externalId).toBe('12345');
    });

    it('prefers the serlo detector over the generic link detector', () => {
        // a serlo node also has a wwwurl, which would otherwise match checkLink()
        const config = service.getFrontendModuleSetting(serloNode());

        expect(config.urlModuleConfig?.embedding).not.toBe(UrlEmbeddings.LINK);
        expect(config.urlModuleConfig?.embedding).toBe(UrlEmbeddings.SERLO);
    });

    function vimeoNode(url: string): Node {
        return {
            properties: {
                'ccm:wwwurl': [url],
            },
        } as unknown as Node;
    }

    it('extracts the vimeo id from a plain vimeo.com/<id> url', () => {
        const config = service.getFrontendModuleSetting(vimeoNode('https://vimeo.com/12345'));

        expect(config.urlModuleConfig?.embedding).toBe(UrlEmbeddings.VIMEO);
        expect(config.urlModuleConfig?.externalId).toBe('12345');
    });

    it('extracts id and query-param privacy hash from a player.vimeo.com url', () => {
        const config = service.getFrontendModuleSetting(
            vimeoNode('https://player.vimeo.com/video/12345?h=abcdef'),
        );

        expect(config.urlModuleConfig?.embedding).toBe(UrlEmbeddings.VIMEO);
        expect(config.urlModuleConfig?.externalId).toBe('12345?h=abcdef');
    });

    it('extracts id and path-form privacy hash from a vimeo.com/<id>/<token> url', () => {
        const config = service.getFrontendModuleSetting(
            vimeoNode('https://vimeo.com/12345/abcdef'),
        );

        expect(config.urlModuleConfig?.embedding).toBe(UrlEmbeddings.VIMEO);
        expect(config.urlModuleConfig?.externalId).toBe('12345?h=abcdef');
    });
});
