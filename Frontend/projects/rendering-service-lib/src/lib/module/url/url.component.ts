import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    inject,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RenderData } from '../../dto/RenderData';
import { UrlEmbeddings } from '../../dto/FrontendModuleConfig';
import { AccessibilityService, EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { TrackingService } from '../../../tracking.service';
import { GdprService } from '../../../gdpr.service';
import { GdprConfig } from '../../dto/GdprConfig';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom, Subscription } from 'rxjs';
import { GlobalStateService } from 'ngx-rendering-service-api';

@Component({
    selector: 'rs-module-url',
    imports: [
        RenderingModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        EduSharingUiModule,
    ],
    templateUrl: './url.component.html',
    styleUrl: './url.component.scss',
})
export class UrlComponent implements RenderModule, OnInit, OnChanges, OnDestroy {
    private sanitizer = inject(DomSanitizer);
    private trackingService = inject(TrackingService);
    private gdprService = inject(GdprService);
    private translate = inject(TranslateService);
    private accessibilityService = inject(AccessibilityService);
    private globalStateService = inject(GlobalStateService);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() isWebComponent: boolean = false;
    // Emitted when the user asks for a fresh link (e.g. an expired Sodix playout URL).
    @Output() reload = new EventEmitter<void>();
    // True while a refresh is in flight; disables the reload control to prevent repeat triggers.
    @Input() reloading = false;
    gdpr: GdprConfig | null = null;
    embedding?: UrlEmbeddings;
    externalId?: string;
    sanitizedUrl: SafeResourceUrl | null = null;
    url: string = '';
    previewUrl: string = '';
    gdprInfoText: string = '';
    isContrastMode: boolean = false;
    private hasBeenClicked: boolean = false;
    private contrastModeSubscription?: Subscription;
    // True while a fresh link is being fetched for the Sodix "to original page" button: shows a
    // spinner, disables the button, and marks that the fetched link should be opened once it lands.
    loading = false;
    static readonly LTI_QUERY = '&editMode=false&launchPresentation=iframe';

    async ngOnInit() {
        this.setupContent();
        this.contrastModeSubscription = this.accessibilityService
            .observe('contrastMode')
            .subscribe((value) => {
                this.isContrastMode = value;
            });
        if (this.node) {
            this.gdpr = await this.gdprService.getGdprConfig(this.node);
            await this.getGdprText();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        // A fresh link (e.g. after fetchLinks()) arrives as a new `data` input; recompute the
        // embedding/url so the iframe/link updates. The first change is handled by ngOnInit.
        if (changes['data'] && !changes['data'].firstChange) {
            this.setupContent();
            // A fetch triggered by the "to original page" button just resolved: stop the spinner and
            // open the freshly-fetched link in a new tab.
            if (this.loading) {
                this.loading = false;
                if (this.url) {
                    window.open(this.url, '_blank');
                    this.onLinkClick();
                }
            }
        }
    }

    ngOnDestroy(): void {
        this.contrastModeSubscription?.unsubscribe();
    }

    /** Ask the host RenderComponent to fetch fresh links for the current job. */
    onReload(): void {
        this.reload.emit();
    }

    /**
     * "To original page": fetch a FRESH link on every click (the backend re-fetches from Sodix),
     * show a spinner while it loads, and open the result in a new tab once it arrives (see
     * ngOnChanges). Guarded so a click is ignored while a fetch is already running.
     */
    onFetchAndOpen(): void {
        if (this.loading) {
            return;
        }
        this.loading = true;
        this.reload.emit();
    }

    private setupContent(): void {
        if (this.data?.module === 'SODIX') {
            this.processSodix();
        } else if (this.data?.module === 'OMEGA') {
            this.processOmega();
        } else {
            this.embedding = this.data?.frontendModuleConfig?.urlModuleConfig?.embedding;
            this.url = this.node?.properties?.['ccm:wwwurl']?.[0] || '';
        }
        this.previewUrl = this.node?.preview?.url ?? '';
        this.externalId = this.data?.frontendModuleConfig?.urlModuleConfig?.externalId;
        if (this.embedding === UrlEmbeddings.VIMEO) {
            this.sanitizedUrl = this.getVimeoUri();
        } else if (this.embedding === UrlEmbeddings.PREZI) {
            this.sanitizedUrl = this.getPreziUrl();
        } else if (this.embedding === UrlEmbeddings.LEARNINGAPPS) {
            this.sanitizedUrl = this.getLearningAppsUrl();
        } else if (this.embedding === UrlEmbeddings.LTI13TOOL) {
            this.sanitizedUrl = this.getLtiUrl();
        } else if (this.embedding === UrlEmbeddings.SIMPLECONNECTOR) {
            this.sanitizedUrl = this.getConnectorRenderUrl();
        } else if (this.embedding === UrlEmbeddings.SODIX) {
            this.sanitizedUrl = this.getSodixUrl();
        } else if (this.embedding === UrlEmbeddings.YOUTUBE) {
            this.sanitizedUrl = this.getYoutubeUrl();
        } else if (this.embedding === UrlEmbeddings.SERLO) {
            this.sanitizedUrl = this.getSerloUrl();
        }
    }

    consentToGdprWarning() {
        if (this.isGdprGeneric()) {
            window.open(this.url, '_blank');
        }
        this.gdpr = null;
        if (this.node?.ref.id && this.node?.ref.repo) {
            this.trackingService.trackGdprConsent(this.node.ref.id, this.node.ref.repo);
        }
    }

    getYoutubeUrl(): SafeResourceUrl {
        const id = (this.externalId ?? '').split('?')[0];
        const youtubeUrl = new URL(`https://www.youtube-nocookie.com/embed/${id}`);
        youtubeUrl.searchParams.set('modestbranding', '1');
        return this.sanitizer.bypassSecurityTrustResourceUrl(youtubeUrl.toString());
    }

    getVimeoUri(): SafeResourceUrl {
        const vimeoUrl = new URL(`https://player.vimeo.com/video/${this.externalId ?? ''}`);
        const externalId = this.externalId ?? '';
        const [videoId, queryString] = externalId.split('?', 2);
        vimeoUrl.pathname = `/video/${videoId}`;

        if (queryString) {
            const params = new URLSearchParams(queryString);
            const h = params.get('h');
            if (h !== null) {
                vimeoUrl.searchParams.set('h', h);
            }
        }

        return this.sanitizer.bypassSecurityTrustResourceUrl(vimeoUrl.toString());
    }

    getPreziUrl(): SafeResourceUrl {
        const preziUrl =
            this.url.slice(-1) === '/' ? this.url.substring(0, this.url.length - 1) : this.url;
        return this.sanitizer.bypassSecurityTrustResourceUrl(preziUrl);
    }

    getLearningAppsUrl(): SafeResourceUrl {
        const learningAppsUrl = this.url.replace(
            'https://learningapps.org/',
            'https://learningapps.org/view',
        );
        return this.sanitizer.bypassSecurityTrustResourceUrl(learningAppsUrl);
    }

    getLtiUrl(): SafeResourceUrl | null {
        // the backend-provided lti resource link (correct repo base + short-lived jwt for
        // single-use node ids) is delivered as the virtual node property `virtual:ltiurl`
        const url = this.node?.properties?.['virtual:ltiurl']?.[0];
        return url
            ? this.sanitizer.bypassSecurityTrustResourceUrl(url + UrlComponent.LTI_QUERY)
            : null;
    }

    getConnectorRenderUrl(): SafeResourceUrl | null {
        // the backend-provided render url for simple connector nodes is delivered as the virtual
        // node property `virtual:connectorrenderurl` and goes into the iframe as-is
        const url = this.node?.properties?.['virtual:connectorrenderurl']?.[0];
        return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
    }

    getSodixUrl(): SafeResourceUrl {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.data?.items?.[0].link || '');
    }

    getSerloUrl(): SafeResourceUrl {
        const serloUrl = `https://de.serlo.org/${
            this.externalId ?? ''
        }?contentOnly&hideBreadcrumbs`;
        return this.sanitizer.bypassSecurityTrustResourceUrl(serloUrl);
    }

    onLinkClick() {
        if (this.node?.ref.id && this.node?.ref.repo && !this.hasBeenClicked) {
            this.trackingService.trackClicked(this.node.ref.id, this.node.ref.repo);
        }
        this.hasBeenClicked = true;
    }

    isGdprGeneric(): boolean {
        return this.embedding === UrlEmbeddings.LINK;
    }

    async getGdprText() {
        if (this.gdpr !== null) {
            if (this.isGdprGeneric()) {
                this.gdprInfoText = await firstValueFrom(
                    this.translate.get('RENDERING.GDPR.EXTERNAL_SITE_INFO'),
                );
            } else {
                const name =
                    this.gdpr.name === 'EXTERNAL_SOURCES'
                        ? await firstValueFrom(this.translate.get('RENDERING.GDPR.EXTERNAL_SOURCE'))
                        : this.gdpr.name;
                const template = await firstValueFrom(
                    this.translate.get('RENDERING.GDPR.EXTERNAL_CONTENT_INFO'),
                );
                this.gdprInfoText = template.replace('{{EXTERNAL_RESOURCE}}', name);
            }
        }
    }

    private processSodix(): void {
        const jobData = this.data?.items?.[0];
        const isPaidMedia =
            (this.node?.properties?.['ccm:editorial_state']?.[0] || '').toLowerCase() ===
            'restricted_mz';
        const additionalData = jobData?.additionalData;
        const downloadUrl = additionalData?.['downloadUrl'] ?? undefined;
        this.globalStateService.setDownloadUrl(downloadUrl);
        const checkIfIframeAllowed = (): boolean => {
            if (isPaidMedia) {
                return false;
            }
            const playoutMimetypes = additionalData?.['playoutMimetypes'] ?? '';
            if (
                playoutMimetypes !== '' &&
                !new RegExp(playoutMimetypes).test(this.node?.mimetype || '')
            ) {
                return false;
            }
            const allowExternalFrameSrc =
                additionalData?.['allowExternalFrameSrc']?.toLowerCase() === 'true';
            if (!allowExternalFrameSrc) {
                return new RegExp('playout\\.sodix\\.de').test(jobData?.link || '');
            }
            return true;
        };
        if (!checkIfIframeAllowed()) {
            this.embedding = UrlEmbeddings.LINK;
            this.url = jobData?.link || '';
            return;
        }

        this.embedding = UrlEmbeddings.SODIX;
    }

    private processOmega(): void {
        const jobData = this.data?.items?.[0];
        const additionalData = jobData?.additionalData;
        const downloadUrl = additionalData?.['downloadUrl'] ?? undefined;
        this.globalStateService.setDownloadUrl(downloadUrl);
        this.url = jobData.link;
        // mimetype of the node can sometimes be wrong (wrong import data vs playout data)
        // if we detect that the url is a html file, we always offer link
        if (
            this.url?.toLowerCase()?.endsWith('.htm') ||
            this.url?.toLowerCase()?.endsWith('.html')
        ) {
            this.embedding = UrlEmbeddings.LINK;
        } else {
            const mimetype = this.node?.mimetype || '';
            if (mimetype.startsWith('image')) {
                this.embedding = UrlEmbeddings.IMAGE;
            } else if (mimetype.startsWith('video')) {
                this.embedding = UrlEmbeddings.VIDEO;
            } else if (mimetype.startsWith('audio')) {
                this.embedding = UrlEmbeddings.AUDIO;
            } else {
                this.embedding = UrlEmbeddings.LINK;
            }
        }
    }

    protected readonly UrlEmbeddings = UrlEmbeddings;
}
