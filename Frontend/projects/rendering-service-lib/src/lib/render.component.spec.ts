import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from 'ngx-edu-sharing-ui';
import {
    ModuleInfoControllerService,
    RenderControllerWrapperService,
    RenderDataRequestWithToken,
} from 'ngx-rendering-service-api';
import { firstValueFrom, of, throwError } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { TrackingService } from '../tracking.service';
import { RenderComponent } from './render.component';

describe('RenderComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RenderComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Toast, useValue: { toast: (): void => {}, error: (): void => {} } },
            ],
        }).compileComponents();
    });

    it('should create the app', () => {
        const fixture = TestBed.createComponent(RenderComponent);
        const app = fixture.componentInstance;
        expect(app).toBeTruthy();
    });

    it('should render without a node', () => {
        const fixture = TestBed.createComponent(RenderComponent);
        expect(() => fixture.detectChanges()).not.toThrow();
    });
});

describe('RenderComponent module selection', () => {
    let renderService: jasmine.SpyObj<RenderControllerWrapperService>;
    let trackingService: jasmine.SpyObj<TrackingService>;
    let moduleInfoController: jasmine.SpyObj<ModuleInfoControllerService>;

    beforeEach(async () => {
        renderService = jasmine.createSpyObj('RenderControllerWrapperService', [
            'getRenderDataToken',
            'getRenderDataTokenSessionSafe',
        ]);
        trackingService = jasmine.createSpyObj('TrackingService', ['trackViewedWithToken']);
        moduleInfoController = jasmine.createSpyObj('ModuleInfoControllerService', [
            'getModulesInfo',
        ]);
        await TestBed.configureTestingModule({
            imports: [RenderComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Toast, useValue: { toast: (): void => {}, error: (): void => {} } },
                { provide: RenderControllerWrapperService, useValue: renderService },
                { provide: TrackingService, useValue: trackingService },
                { provide: ModuleInfoControllerService, useValue: moduleInfoController },
            ],
        }).compileComponents();
    });

    function createComponent(node: Node) {
        const fixture = TestBed.createComponent(RenderComponent);
        const app = fixture.componentInstance;
        app.node = node;
        app.request = {
            token: 'test-token',
            nodeId: node.ref.id,
            repoId: node.ref.repo,
        } as RenderDataRequestWithToken;
        fixture.detectChanges();
        return app;
    }

    function awaitRenderData(app: RenderComponent) {
        return firstValueFrom(
            app.renderData$.pipe(
                filter((data) => data !== null),
                take(1),
            ),
        );
    }

    it('uses the backend module when renderdata succeeds', async () => {
        renderService.getRenderDataToken.and.returnValue(
            of({
                deferred: false,
                jobId: null,
                module: 'IMAGE',
                objectLinks: [{ link: 'https://example.org/a.png', width: 1, height: 1 }],
                supportedByBackend: true,
            }),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('IMAGE');
        expect(moduleInfoController.getModulesInfo).not.toHaveBeenCalled();
        expect(trackingService.trackViewedWithToken).not.toHaveBeenCalled();
    });

    // The two 415 tests below cover rendering services that predate `supportedByBackend`.
    it('falls back to the default module on 415', async () => {
        renderService.getRenderDataToken.and.returnValue(
            throwError(() => new HttpErrorResponse({ status: 415 })),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('default');
        expect(data!.frontendModuleConfig?.urlModuleConfig).toBeNull();
        expect(moduleInfoController.getModulesInfo).not.toHaveBeenCalled();
        expect(trackingService.trackViewedWithToken).not.toHaveBeenCalled();
    });

    it('falls back to a matching frontend module on 415', async () => {
        renderService.getRenderDataToken.and.returnValue(
            throwError(() => new HttpErrorResponse({ status: 415 })),
        );
        const app = createComponent({
            ref: { id: 'id', repo: 'repo' },
            properties: { 'ccm:wwwurl': ['https://youtu.be/abc123'] },
        } as unknown as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('url');
        expect(data!.frontendModuleConfig?.urlModuleConfig?.externalId).toBe('abc123');
        expect(trackingService.trackViewedWithToken).not.toHaveBeenCalled();
    });

    it('uses the default frontend module when the service reports supportedByBackend false', async () => {
        renderService.getRenderDataToken.and.returnValue(
            of({ deferred: false, jobId: null, supportedByBackend: false }),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('default');
        expect(data!.frontendModuleConfig?.urlModuleConfig).toBeNull();
        expect(moduleInfoController.getModulesInfo).not.toHaveBeenCalled();
        expect(trackingService.trackViewedWithToken).not.toHaveBeenCalled();
    });

    it('uses a matching frontend module when the service reports supportedByBackend false', async () => {
        renderService.getRenderDataToken.and.returnValue(
            of({ deferred: false, jobId: null, supportedByBackend: false }),
        );
        const app = createComponent({
            ref: { id: 'id', repo: 'repo' },
            properties: { 'ccm:wwwurl': ['https://youtu.be/abc123'] },
        } as unknown as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('url');
        expect(data!.frontendModuleConfig?.urlModuleConfig?.externalId).toBe('abc123');
    });

    it('prefers supportedByBackend false over a returned jobId', async () => {
        // Guards the branch ordering: the flag is checked before deferred/jobId, so a stray jobId
        // must not start a poll for a node the service never renders.
        renderService.getRenderDataToken.and.returnValue(
            of({ deferred: true, jobId: 'job-1', module: 'IMAGE', supportedByBackend: false }),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('default');
    });

    it('falls back to the frontend module when a response has neither links nor a job', async () => {
        renderService.getRenderDataToken.and.returnValue(
            of({ deferred: false, jobId: null, objectLinks: [], supportedByBackend: true }),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('default');
    });

    it('shows the error module on other errors', async () => {
        renderService.getRenderDataToken.and.returnValue(
            throwError(() => new HttpErrorResponse({ status: 500 })),
        );
        const app = createComponent({ ref: { id: 'id', repo: 'repo' }, properties: {} } as Node);
        const data = await awaitRenderData(app);
        expect(data!.module).toBe('ERROR');
        expect(data!.publicErrorMessage).toBe('GENERIC_ERROR_MESSAGE');
        expect(trackingService.trackViewedWithToken).not.toHaveBeenCalled();
    });
});
