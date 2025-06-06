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
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnChanges, AfterViewInit, OnInit {
    @ViewChild(PdfComponent) pdfComponent: PdfComponent;
    @Input() encoded_node: string;
    @Input() signature: string;
    @Input() display_mode: 'inline' | 'full' = 'inline';
    @Input() jwt: string;
    @Input() render_url: string;
    @Input() encoded_user: string;
    @Input() service_worker_url: string;
    @Input() activate_service_worker: boolean;
    @Input() assets_url: string = '';
    @Input() resource_url: string = '';
    showInlineMetadata = false;
    node = signal<Node>(null);
    request = signal<RenderDataRequestWithToken>(null);

    serviceWorkerUrl: string;
    activateServiceWorker: boolean;
    assetUrl: string;
    resourceUrl: string;

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
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.encoded_node) {
            const data = await this.renderHelperService.getRenderDataForLms(
                this.encoded_node,
                this.signature,
                this.jwt,
                this.render_url,
                this.encoded_user,
            );
            this.node.set(data.node);
            this.request.set(data.request);
            console.log(data);
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
