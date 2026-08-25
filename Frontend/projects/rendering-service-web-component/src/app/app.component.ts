import { Node } from 'ngx-edu-sharing-api';
import {
    AfterViewInit,
    Component,
    ElementRef,
    inject,
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
    private renderHelperService = inject(RenderHelperService);
    private translations = inject(TranslationsService);
    private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

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
    @Input() component_height: number | null = null;
    @Input() footer_height: number = 100;
    @Input() target_blank: boolean = false;
    showInlineMetadata = false;
    overlayHeight = '300px';
    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);

    serviceWorkerUrl: string;
    activateServiceWorker: boolean;
    assetUrl: string;
    resourceUrl: string;
    previewUrl: string;
    targetBlank: boolean;

    constructor() {
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
        this.targetBlank = this.target_blank;
        if (this.component_height !== null && this.component_height > 0) {
            const containerHeight = this.component_height - this.footer_height;
            // on the host, not on <html>: the modules read this through inheritance, and several
            // elements on one page each need their own height instead of the last one's
            this.elementRef.nativeElement.style.setProperty(
                '--containerHeight',
                `${containerHeight}px`,
            );
            this.overlayHeight = `${Math.min(300, containerHeight)}px`;
        }
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.encoded_node) {
            const data = await this.renderHelperService.getRenderDataForLms(
                this.encoded_node,
                this.signature,
                this.jwt,
                this.render_url,
                this.signature_algorithm ?? 'SHA512withRSA',
            );
            if (this.previewUrl !== '') {
                data.node.preview.url = this.previewUrl;
            }
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
