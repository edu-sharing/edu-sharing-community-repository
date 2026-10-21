import { DestroyRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ConfigService, ConfigV1Service, Context, Node, RestConstants } from 'ngx-edu-sharing-api';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { BridgeService } from 'src/app/services/bridge.service';
import { ImprintPrivacyService } from './imprint-privacy-service';

/** Stands in for the `DestroyRef` of a page component. */
class FakePage implements DestroyRef {
    destroyed = false;
    private callbacks = new Set<() => void>();

    onDestroy(callback: () => void): () => void {
        this.callbacks.add(callback);
        return () => this.callbacks.delete(callback);
    }

    destroy() {
        this.destroyed = true;
        this.callbacks.forEach((callback) => callback());
    }
}

describe('ImprintPrivacyService', () => {
    const DOMAIN_URL = 'https://domain.example/imprint';
    const CONTEXTS: Context[] = [
        { id: 'school', values: { imprintUrl: 'https://school.example/imprint' } },
        { id: 'plain', values: {} },
    ];

    let config$: BehaviorSubject<unknown>;
    let getAvailableContext: jasmine.Spy;
    let service: ImprintPrivacyService;
    let page: FakePage;

    function nodeInContext(contextId?: string): Node {
        return {
            properties: contextId ? { [RestConstants.CCM_PROP_EDUCONTEXT_NAME]: [contextId] } : {},
        } as unknown as Node;
    }

    beforeEach(() => {
        config$ = new BehaviorSubject<unknown>({ imprintUrl: DOMAIN_URL });
        // the backend lists a null entry while its context cache is empty
        getAvailableContext = jasmine
            .createSpy('getAvailableContext')
            .and.returnValue(of([...CONTEXTS, null]));
        TestBed.configureTestingModule({
            providers: [
                ImprintPrivacyService,
                { provide: ConfigService, useValue: { observeConfig: () => config$ } },
                { provide: ConfigV1Service, useValue: { getAvailableContext } },
                { provide: BridgeService, useValue: {} },
            ],
        });
        service = TestBed.inject(ImprintPrivacyService);
        page = new FakePage();
    });

    it('uses the imprint of the requesting domain without a registered node', () => {
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
        expect(getAvailableContext).not.toHaveBeenCalled();
    });

    it('uses the imprint of the context the node was created in', () => {
        service.setContextNode(nodeInContext('school'), page);
        expect(service.imprintUrl()).toBe('https://school.example/imprint');
        expect(getAvailableContext).toHaveBeenCalledWith({ includeStatic: true });
    });

    it('falls back to the domain for nodes without or in the default context', () => {
        service.setContextNode(nodeInContext(), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
        service.setContextNode(nodeInContext(RestConstants.EDUCONTEXT_DEFAULT), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
        service.setContextNode(null, page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
        expect(getAvailableContext).not.toHaveBeenCalled();
    });

    it('falls back to the domain for unknown contexts and contexts without an imprint', () => {
        service.setContextNode(nodeInContext('unknown'), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
        service.setContextNode(nodeInContext('plain'), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
    });

    it('falls back to the domain when the context list cannot be loaded', () => {
        getAvailableContext.and.returnValue(throwError(() => new Error('unavailable')));
        service.setContextNode(nodeInContext('school'), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
    });

    it('follows changes of the domain config', () => {
        service.setContextNode(nodeInContext('plain'), page);
        config$.next({ imprintUrl: 'https://other-domain.example/imprint' });
        expect(service.imprintUrl()).toBe('https://other-domain.example/imprint');
    });

    it('provides the privacy link of the domain config', () => {
        expect(service.privacyInformationUrl()).toBeUndefined();
        config$.next({ privacyInformationUrl: 'https://domain.example/privacy' });
        expect(service.privacyInformationUrl()).toBe('https://domain.example/privacy');
    });

    it('loads the context list only once', () => {
        service.setContextNode(nodeInContext('school'), page);
        service.setContextNode(nodeInContext('plain'), page);
        service.setContextNode(nodeInContext('school'), page);
        expect(getAvailableContext).toHaveBeenCalledTimes(1);
    });

    it('resets to the domain when the page is destroyed', () => {
        service.setContextNode(nodeInContext('school'), page);
        page.destroy();
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
    });

    it('keeps the node of the next page when the previous page is destroyed later', () => {
        const nextPage = new FakePage();
        service.setContextNode(nodeInContext('plain'), page);
        service.setContextNode(nodeInContext('school'), nextPage);
        page.destroy();
        expect(service.imprintUrl()).toBe('https://school.example/imprint');
        nextPage.destroy();
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
    });

    it('ignores nodes of pages that are already destroyed', () => {
        page.destroy();
        service.setContextNode(nodeInContext('school'), page);
        expect(service.imprintUrl()).toBe(DOMAIN_URL);
    });
});
