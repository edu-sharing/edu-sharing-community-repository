import { Component, Input, OnDestroy, OnInit, inject } from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgOptimizedImage } from '@angular/common';
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
        NgOptimizedImage,
        EduSharingUiModule,
    ],
    templateUrl: './url.component.html',
    styleUrl: './url.component.scss',
})
export class UrlComponent implements RenderModule, OnInit, OnDestroy {
    private sanitizer = inject(DomSanitizer);
    private trackingService = inject(TrackingService);
    private gdprService = inject(GdprService);
    private translate = inject(TranslateService);
    private accessibilityService = inject(AccessibilityService);
    private globalStateService = inject(GlobalStateService);

    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() isWebComponent: boolean = false;
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
    static readonly LTI_QUERY = '&editMode=false&launchPresentation=iframe';

    async ngOnInit() {
        if (this.data?.module === 'SODIX') {
            this.processSodix();
        } else if (this.data?.module === 'OMEGA') {
            this.processOmega();
        } else {
            this.embedding = this.data?.frontendModuleConfig?.urlModuleConfig?.embedding;
            this.url = this.node?.properties?.['ccm:wwwurl']?.[0] || '';
        }
        this.contrastModeSubscription = this.accessibilityService
            .observe('contrastMode')
            .subscribe((value) => {
                this.isContrastMode = value;
            });
        if (this.node) {
            this.gdpr = await this.gdprService.getGdprConfig(this.node);
            await this.getGdprText();
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
        } else if (this.embedding === UrlEmbeddings.SODIX) {
            this.sanitizedUrl = this.getSodixUrl();
        } else if (this.embedding === UrlEmbeddings.YOUTUBE) {
            this.sanitizedUrl = this.getYoutubeUrl();
        } else if (this.embedding === UrlEmbeddings.SERLO) {
            this.sanitizedUrl = this.getSerloUrl();
        }
    }

    ngOnDestroy(): void {
        this.contrastModeSubscription?.unsubscribe();
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

    protected readonly UrlEmbeddings = UrlEmbeddings;
}
