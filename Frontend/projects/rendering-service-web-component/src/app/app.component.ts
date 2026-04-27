import { Node } from 'ngx-edu-sharing-api';
import {
    AfterViewInit,
    Component,
    Input,
    OnChanges,
    OnInit,
    signal,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { RenderHelperService, TranslationsService } from 'ngx-edu-sharing-ui';
import { RenderDataRequestWithToken } from 'ngx-rendering-service-api';
import { PdfComponent } from 'ngx-rendering-service-lib';

@Component({
    selector: 'edu-sharing-render',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false,
})
export class AppComponent implements OnChanges, AfterViewInit, OnInit {
    @ViewChild(PdfComponent) pdfComponent: PdfComponent;
    @Input() encoded_node: string;
    @Input() signature: string;
    @Input() display_mode: 'inline' | 'full' = 'inline';
    @Input() jwt: string;
    @Input() render_url: string;
    @Input() service_worker_url: string;
    @Input() activate_service_worker: boolean;
    @Input() assets_url: string = '';
    @Input() resource_url: string = '';
    @Input() preview_url: string = '';
    @Input() signature_algorithm: string | null = null;
    showInlineMetadata = false;
    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);

    serviceWorkerUrl: string;
    activateServiceWorker: boolean;
    assetUrl: string;
    resourceUrl: string;
    previewUrl: string;
    signatureAlgorithm: string | null;

    constructor(
        private renderHelperService: RenderHelperService,
        private translations: TranslationsService,
    ) {
        this.translations.initialize().subscribe(() => {});
    }

    ngAfterViewInit(): void {
        if (this.pdfComponent) {
            this.pdfComponent.fileData = 'test';
        }
    }

    ngOnInit() {
        this.serviceWorkerUrl = this.service_worker_url;
        this.activateServiceWorker = this.activate_service_worker;
        this.assetUrl = this.assets_url;
        this.resourceUrl = this.resource_url;
        this.previewUrl = this.preview_url;
        this.signatureAlgorithm = this.signature_algorithm ?? 'SHA1withRSA';
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.encoded_node) {
            const data = await this.renderHelperService.getRenderDataForLms(
                this.encoded_node,
                this.signature,
                this.jwt,
                this.render_url,
                this.signatureAlgorithm,
            );
            data.node.preview.url = this.previewUrl;
            this.node.set(data.node);
            this.request.set(data.request);
        }
    }

    getAuthors(): string {
        if (this.node()?.properties !== undefined) {
            const freeText = (this.node().properties['ccm:author_freetext'] ?? [])
                .join(', ')
                .trim();
            const authors = (this.node().properties['ccm:lifecyclecontributer_authorFN'] ?? [])
                .join(', ')
                .trim();
            const orgs = (this.node().properties['ccm:lifecyclecontributer_authorVCARD_ORG'] ?? [])
                .join(', ')
                .trim();
            return authors + (orgs ? ', ' + orgs : '') + (freeText ? ', ' + freeText : '');
        }
        return '';
    }
}
