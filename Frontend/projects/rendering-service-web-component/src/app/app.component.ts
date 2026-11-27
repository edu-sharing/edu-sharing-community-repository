import { MdsService, Node, RestConstants } from 'ngx-edu-sharing-api';
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
import { firstValueFrom } from 'rxjs';
import { RenderTokenService } from './render-token.interceptor';

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

    assetUrl: string;
    resourceUrl: string;
    previewUrl: string;
    targetBlank: boolean;

    constructor(
        private renderHelperService: RenderHelperService,
        private translations: TranslationsService,
        private renderTokenService: RenderTokenService,
        private mdsService: MdsService,
    ) {
        this.translations.initialize().subscribe(() => {});
    }

    ngAfterViewInit(): void {
        if (this.pdfComponent) {
            this.pdfComponent.fileData = 'test';
        }
    }

    ngOnInit() {
        this.assetUrl = this.assets_url;
        this.resourceUrl = this.resource_url;
        this.previewUrl = this.preview_url;
        this.targetBlank = this.target_blank;
        if (this.component_height !== null && this.component_height > 0) {
            const containerHeight = this.component_height - this.footer_height;
            document.documentElement.style.setProperty('--containerHeight', `${containerHeight}px`);
            this.overlayHeight = `${Math.min(300, containerHeight)}px`;
        }
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.jwt) {
            // authenticates the metadata requests of this embedding, see render-token.interceptor
            this.renderTokenService.setToken(this.jwt);
        }
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
            if (this.display_mode === 'inline') {
                this.preloadMds(data.node);
            }
        }
    }

    /**
     * Loads the metadataset of the inline metadata viewer right away, while the rendering token is
     * still valid (it expires after one hour, the page may stay open much longer).
     *
     * `MdsService.getMetadataSet` caches its result per arguments, so they must be exactly the ones
     * `es-mds-viewer` uses (see `MdsViewerComponent.ngOnChanges`) to be reused when the viewer opens.
     * A failed request is not cached, the viewer just tries again.
     */
    private preloadMds(node: Node): void {
        const metadataSet =
            node.properties?.[RestConstants.CM_PROP_METADATASET_EDU_METADATASET]?.[0];
        firstValueFrom(
            this.mdsService.getMetadataSet(metadataSet != null ? { metadataSet } : {}),
        ).catch((e) => console.warn('could not preload the metadataset', e));
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
