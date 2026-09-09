import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
    TestRequest,
} from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ApiConfiguration } from './api/api-configuration';
import { RenderDataResponse } from './api/models/render-data-response';
import {
    RenderControllerWrapperService,
    RenderDataRequestWithToken,
} from './render-controller-wrapper.service';

const RENDER_DATA_URL = '/public/renderdata';
const RESPONSE: RenderDataResponse = { module: 'IMAGE', jobId: null, deferred: false };

describe('RenderControllerWrapperService session gate', () => {
    let service: RenderControllerWrapperService;
    let httpMock: HttpTestingController;

    const request = (nodeId: string) =>
        ({ nodeId, repoId: 'local', token: 'jwt' } as RenderDataRequestWithToken);

    /**
     * The render-data requests that are still in flight. `match()` consumes what it returns and
     * also reports requests whose subscriber went away, so cancelled ones are dropped here.
     */
    const pending = (): TestRequest[] =>
        httpMock.match((r) => r.url.endsWith(RENDER_DATA_URL)).filter((r) => !r.cancelled);

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: ApiConfiguration, useValue: { rootUrl: '' } },
                RenderControllerWrapperService,
            ],
        });
        service = TestBed.inject(RenderControllerWrapperService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify({ ignoreCancelled: true }));

    it('holds back later requests until the first one has run', fakeAsync(() => {
        service.getRenderDataTokenSessionSafe(request('first')).subscribe();
        service.getRenderDataTokenSessionSafe(request('second')).subscribe();

        // the gate is closed, so only the first component may talk to the backend
        const first = pending();
        expect(first.length).toBe(1);
        expect(first[0].request.body.nodeId).toBe('first');

        first[0].flush(RESPONSE);
        tick(50);

        const second = pending();
        expect(second.length).toBe(1);
        expect(second[0].request.body.nodeId).toBe('second');
        second[0].flush(RESPONSE);
        tick(50);
    }));

    // regression: a 415 on the first component (backend has no module for the node, e.g. a
    // DE.FWU replication-source node, which the caller recovers from with a frontend module)
    // used to leave the gate closed forever, so no other render component on the page ever
    // issued its request.
    it('releases later requests when the first one fails', fakeAsync(() => {
        let firstError: HttpErrorResponse | null = null;
        service.getRenderDataTokenSessionSafe(request('first')).subscribe({
            error: (error) => (firstError = error),
        });
        let secondResponse: unknown = null;
        service
            .getRenderDataTokenSessionSafe(request('second'))
            .subscribe((response) => (secondResponse = response));

        pending()[0].flush(null, { status: 415, statusText: 'Unsupported Media Type' });
        tick(50);

        expect(firstError!.status).toBe(415);

        const second = pending();
        expect(second.length).toBe(1);
        expect(second[0].request.body.nodeId).toBe('second');
        second[0].flush(RESPONSE);
        tick(50);

        expect(secondResponse).toEqual(RESPONSE);
    }));

    it('releases later requests when the first one is unsubscribed before it responds', fakeAsync(() => {
        const subscription = service.getRenderDataTokenSessionSafe(request('first')).subscribe();
        let secondResponse: unknown = null;
        service
            .getRenderDataTokenSessionSafe(request('second'))
            .subscribe((response) => (secondResponse = response));

        // the first component is destroyed while its request is still in flight
        subscription.unsubscribe();
        tick(50);

        const second = pending();
        expect(second.length).toBe(1);
        expect(second[0].request.body.nodeId).toBe('second');
        second[0].flush(RESPONSE);
        tick(50);

        expect(secondResponse).toEqual(RESPONSE);
    }));
});
