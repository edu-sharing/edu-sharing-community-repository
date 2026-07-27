import {
    Component,
    createEnvironmentInjector,
    DestroyRef,
    EnvironmentInjector,
    Injector,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RenderingModule } from './rendering.module';
import {
    JobInfoControllerService,
    ModuleInfoControllerService,
    RSApiConfiguration,
    JobInfoReply,
    RenderControllerWrapperService,
    RenderDataRequestWithToken,
    RenderDataResponse,
} from 'ngx-rendering-service-api';
import {
    BehaviorSubject,
    debounceTime,
    exhaustMap,
    firstValueFrom,
    interval,
    map,
    Subject,
    takeUntil,
} from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ImageComponent } from './module/image/image.component';
import { RenderingApiModule } from './rendering-api.module';
import { VideoComponent } from './module/video/video.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EduSharingApiModule, Node } from 'ngx-edu-sharing-api';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AssetStateItem, RenderData } from './dto/RenderData';
import { PdfComponent } from './module/pdf/pdf.component';
import { UrlComponent } from './module/url/url.component';
import { EduHtmlComponent } from './module/eduhtml/eduHtml.component';
import { ModuleInfoService } from '../module-info.service';
import { filter } from 'rxjs/operators';
import { PlatformLocation } from '@angular/common';
import { DefaultComponent } from './module/default/default.component';
import { FrontendModuleConfig } from './dto/FrontendModuleConfig';
import { AudioComponent } from './module/audio/audio.component';
import { BinderComponent } from './module/binder/binder.component';
import { DdbComponent } from './module/ddb/ddb.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { EduSharingUiCommonModule, NodeHelperService } from 'ngx-edu-sharing-ui';
import { ErrorComponent } from './module/error/error.component';
import {
    RENDERING_SERVICE_LIB_CONFIG,
    RenderingServiceLibConfiguration,
} from '../rendering-service-lib-configuration';
import { MatButtonModule } from '@angular/material/button';
import { TrackingService } from '../tracking.service';
import { PdfIframeComponent } from './module/pdf-iframe/pdf-iframe.component';
import { MoodleComponent } from './module/moodle/moodle.component';
import { RevokedComponent } from './generic/revoked/revoked.component';
import { H5pComponent } from './module/h5p/h5p.component';
import { CurrentRenderRootUrlService } from './current-render-root-url.service';

@Component({
    selector: 'rs-root',
    imports: [
        RenderingApiModule,
        RenderingModule,
        ImageComponent,
        VideoComponent,
        MatProgressSpinnerModule,
        MatProgressBarModule,
        PdfComponent,
        PdfIframeComponent,
        UrlComponent,
        EduHtmlComponent,
        DefaultComponent,
        MatButtonModule,
        AudioComponent,
        BinderComponent,
        DdbComponent,
        EduSharingApiModule,
        EduSharingUiCommonModule,
        RevokedComponent,
        ErrorComponent,
        TranslateModule,
        MoodleComponent,
        H5pComponent,
    ],
    providers: [TranslateService, CurrentRenderRootUrlService],
    templateUrl: './render.component.html',
    styleUrl: './render.component.scss',
})
export class RenderComponent implements OnChanges, OnInit {
    private platformLocation = inject(PlatformLocation);
    private trackingService = inject(TrackingService);
    private injector = inject(Injector);
    private envInjector = inject(EnvironmentInjector);
    private destroyRef = inject(DestroyRef);
    private currentRootUrl = inject(CurrentRenderRootUrlService);
    nodeHelperService = inject(NodeHelperService);
    configuration = inject<RenderingServiceLibConfiguration>(RENDERING_SERVICE_LIB_CONFIG, {
        optional: true,
    });

    @Input() request: RenderDataRequestWithToken | undefined;
    @Input() node: Node | undefined;
    @Input() serviceWorkerUrl: string | undefined;
    @Input() activateServiceWorker: boolean = true;
    @Input() assetUrl: string = '';
    @Input() resourceUrl: string = '';
    @Input() isWebComponent: boolean = false;
    serviceWorkerReady = false;
    loadData = new Subject<void>();
    renderData$ = new BehaviorSubject<RenderData | null>(null);
    finished = new Subject<void>();
    someButNotAllFinished = new Subject<Boolean>();
    progress$ = new BehaviorSubject<{ module: string; progress?: number } | null>(null);

    constructor() {
        this.loadData
            .pipe(
                filter(() => this.node !== undefined && this.serviceWorkerReady),
                debounceTime(10),
                // load services including the scope so they fetch via a remote render url (if applicable for the current request input)
                map(() => this.resolveServices(this.request?.renderingBaseUrl)),
            )
            .subscribe({
                next: async (services) => {
                    try {
                        this.request!!.eventType = this.isWebComponent
                            ? 'VIEW_MATERIAL_EMBEDDED'
                            : 'VIEW_MATERIAL';
                        const { render, jobInfo: jobInfoService } = services;
                        const renderResponseData = this.isWebComponent
                            ? await firstValueFrom(
                                  render.getRenderDataTokenSessionSafe(this.request!!),
                              )
                            : await firstValueFrom(render.getRenderDataToken(this.request!!));
                        // Case 1: no job returned => no polling needed
                        if (renderResponseData.jobId === null) {
                            this.handleRenderingResponseWithoutJob(renderResponseData);
                        } else {
                            this.progress$.next({ module: renderResponseData.module ?? '' });
                            interval(renderResponseData.module === 'VIDEO' ? 2000 : 500)
                                .pipe(
                                    takeUntil(this.finished),
                                    takeUntilDestroyed(this.destroyRef),
                                    exhaustMap(() =>
                                        jobInfoService
                                            .getJobInfo({ jobId: renderResponseData.jobId!! })
                                            .pipe(),
                                    ),
                                )
                                .subscribe({
                                    next: async (jobInfo) => {
                                        // Some, but not all, jobs are in a final status (this means some are still QUEUED or PROCESSING)
                                        if (
                                            jobInfo.jobs.some((j) => j.status === 'FINISHED') &&
                                            jobInfo.status !== 'FINISHED' &&
                                            jobInfo.status !== 'PARTIALLY_FAILED'
                                        ) {
                                            this.handleJobInfoWithSubJobsInProgress(
                                                renderResponseData,
                                                jobInfo,
                                            );
                                            // main job finished or partially failed
                                        } else if (
                                            jobInfo.status === 'FINISHED' ||
                                            jobInfo.status === 'PARTIALLY_FAILED'
                                        ) {
                                            this.handleFinishedOrPartialJobInfo(
                                                renderResponseData,
                                                jobInfo,
                                            );
                                            // Main job failed => no module but error page
                                        } else if (jobInfo.status === 'FAILED') {
                                            this.handleFailedMainJob(jobInfo);
                                        } else {
                                            // no finished jobs -> show either one progress bar with race leader (progress 0-100)
                                            // or queue position (progress (-inf,-1])
                                            this.handleMainJobWithNoSubJobInFinalStatus(jobInfo);
                                        }
                                    },
                                    error: (error) => {
                                        this.handleApiError(error);
                                    },
                                });
                        }
                    } catch (error) {
                        // 415 = backend has no module for this media type -> frontend module fallback
                        if (error instanceof HttpErrorResponse && error.status === 415) {
                            this.renderFrontendModule(services.moduleInfo);
                        } else {
                            this.handleApiError(error);
                        }
                    }
                },
                error: (error) => {
                    this.handleApiError(error);
                },
            });
    }

    private resolveServices(baseUrl: string | undefined) {
        this.currentRootUrl.rootUrl = baseUrl;
        if (!baseUrl) {
            console.info('Using local rendering base url');
            return {
                render: this.injector.get(RenderControllerWrapperService),
                jobInfo: this.injector.get(JobInfoControllerService),
                moduleInfo: this.injector.get(ModuleInfoService),
            };
        }
        console.info('Using external rendering base url:', baseUrl);
        const child = createEnvironmentInjector(
            [
                { provide: RSApiConfiguration, useValue: { rootUrl: baseUrl } },
                {
                    provide: RenderControllerWrapperService,
                    useClass: RenderControllerWrapperService,
                },
                { provide: JobInfoControllerService, useClass: JobInfoControllerService },
                { provide: ModuleInfoControllerService, useClass: ModuleInfoControllerService },
                { provide: ModuleInfoService, useClass: ModuleInfoService },
            ],
            this.envInjector,
        );
        return {
            render: child.get(RenderControllerWrapperService),
            jobInfo: child.get(JobInfoControllerService),
            moduleInfo: child.get(ModuleInfoService),
        };
    }

    handleApiError(error: any) {
        const publicMessage = error?.error?.userMessage ?? 'GENERIC_ERROR_MESSAGE';
        const data: RenderData = {
            module: 'ERROR',
            publicErrorMessage: publicMessage,
        };
        this.renderData$.next(data);
        this.finished.next();
    }

    async ngOnInit() {
        if (this.assetUrl !== '') {
            this.configuration.assetsUrl = this.assetUrl;
        }
        if (!this.activateServiceWorker) {
            this.serviceWorkerReady = true;
            this.loadData.next();
            return;
        }

        if (!('serviceWorker' in navigator)) {
            console.warn('Service worker API not available');
            this.serviceWorkerReady = true;
            this.loadData.next();
            return;
        }
        // await firstValueFrom(this.translationsService.initialize())

        try {
            const reg = await navigator.serviceWorker.getRegistrations();
            await Promise.all(reg?.map((r: ServiceWorkerRegistration) => r.unregister()));
            const serviceWorkerUrl =
                this.serviceWorkerUrl ??
                this.platformLocation.getBaseHrefFromDOM() + 'edu-service-worker.js';
            const scope = this.serviceWorkerUrl ? '/' : this.platformLocation.getBaseHrefFromDOM();
            const registration = await navigator.serviceWorker.register(serviceWorkerUrl, {
                scope: scope,
            });
            if (registration.waiting) {
                registration.waiting.postMessage({ type: 'SKIP_WAITING' });
            }
            this.serviceWorkerReady = true;
            this.loadData.next();
            registration.onupdatefound = () => {
                const newWorker = registration.installing;
                if (newWorker) {
                    newWorker.onstatechange = () => {
                        if (newWorker.state === 'installed') {
                            if (navigator.serviceWorker.controller) {
                                newWorker.postMessage({ type: 'SKIP_WAITING' });
                            }
                        }
                    };
                }
            };
            navigator.serviceWorker.addEventListener('controllerchange', () => {
                console.info('Service Worker controlling this page.');
            });
        } catch (error) {
            console.warn('Error during service worker registration:', error);
            this.serviceWorkerReady = true;
            this.loadData.next();
        }
    }

    async ngOnChanges(_: SimpleChanges) {
        this.loadData.next();
    }

    private renderFrontendModule(moduleInfo: ModuleInfoService) {
        const frontendModule: FrontendModuleConfig =
            this.node !== undefined
                ? moduleInfo.getFrontendModuleSetting(this.node)
                : { module: 'default', urlModuleConfig: null };
        this.renderData$.next({
            module: frontendModule.module,
            frontendModuleConfig: frontendModule,
        });
        this.finished.next();
    }

    handleRenderingResponseWithoutJob(renderDataResponse: RenderDataResponse) {
        const items = renderDataResponse.objectLinks?.map((item) => {
            const assetItem: AssetStateItem = {
                link: item.link,
                progress: 100,
                height: item.height,
                width: item.width,
                additionalData: undefined,
                status: 'FINISHED',
            };
            return assetItem;
        });
        const data: RenderData = {
            module: renderDataResponse.module ?? '',
            items: items,
        };
        renderDataResponse.objectLinks !== undefined && this.renderData$.next(data);
        this.finished.next();
    }

    handleJobInfoWithSubJobsInProgress(
        renderDataResponse: RenderDataResponse,
        jobInfo: JobInfoReply,
    ) {
        const items = jobInfo.jobs.map((subJob) => {
            const assetItem: AssetStateItem = {
                link: subJob.objectLink?.link ?? '',
                progress: subJob.progress,
                height: subJob.objectLink?.height ?? subJob.quality,
                width: subJob.objectLink?.width ?? 0,
                additionalData: subJob.additionalData,
                status: subJob.status,
            };
            return assetItem;
        });
        const data: RenderData = {
            module: renderDataResponse.module ?? '',
            items: items,
        };
        this.renderData$.next(data);
        this.someButNotAllFinished.next(true);
    }

    handleFinishedOrPartialJobInfo(renderDataResponse: RenderDataResponse, jobInfo: JobInfoReply) {
        const items = jobInfo.jobs.map((subJob) => {
            const assetItem: AssetStateItem = {
                link: subJob.objectLink?.link ?? '',
                progress: subJob.progress,
                height: subJob.objectLink?.height ?? subJob.quality,
                width: subJob.objectLink?.width ?? 0,
                additionalData: subJob.additionalData,
                publicErrorMessage: subJob.publicErrorMessage,
                status: subJob.status,
            };
            return assetItem;
        });
        const data: RenderData = {
            module: renderDataResponse.module ?? '',
            items: items,
        };
        this.renderData$.next(data);
        this.finished.next();
    }

    handleFailedMainJob(jobInfo: JobInfoReply) {
        const data: RenderData = {
            module: 'ERROR',
            publicErrorMessage: jobInfo.userMessage ?? 'GENERIC_ERROR_MESSAGE',
        };
        this.renderData$.next(data);
        this.finished.next();
    }

    handleMainJobWithNoSubJobInFinalStatus(jobInfo: JobInfoReply) {
        let progress = 0;
        if (jobInfo.jobs.some((j) => j.status === 'PROCESSING')) {
            progress = Math.max(...jobInfo.jobs.map((j) => j.progress));
        } else if (jobInfo.jobs.some((j) => j.status === 'QUEUED')) {
            progress = -Math.min(...jobInfo.jobs.map((j) => j.progress + 1));
        }
        this.progress$.next({
            module: jobInfo.module ?? '',
            progress: progress,
        });
    }
}
