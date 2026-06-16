import {
    Component,
    HostListener,
    Inject,
    Input,
    OnChanges,
    Optional,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RenderData } from '../../dto/RenderData';
import { AssetControllerService, GetAsset$Params } from 'ngx-rendering-service-api';
import {
    RENDERING_SERVICE_LIB_CONFIG,
    RenderingServiceLibConfiguration,
} from '../../../rendering-service-lib-configuration';
import {
    NgxExtendedPdfViewerComponent,
    NgxExtendedPdfViewerModule,
    pdfDefaultOptions,
} from 'ngx-extended-pdf-viewer';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'rs-module-pdf',
    imports: [RenderingModule, MatButtonModule, MatIconModule, NgxExtendedPdfViewerModule],
    templateUrl: './pdf.component.html',
    styleUrl: './pdf.component.scss',
})
export class PdfComponent implements RenderModule, OnChanges {
    @ViewChild(NgxExtendedPdfViewerComponent) pdfViewer!: NgxExtendedPdfViewerComponent;
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() assetUrl: string | undefined;
    restrictedView: boolean = false;
    fileData: Uint8Array | string | undefined;

    constructor(
        private assetControllerService: AssetControllerService,
        private nodeHelper: NodeHelperService,
        @Optional()
        @Inject(RENDERING_SERVICE_LIB_CONFIG)
        public configuration: RenderingServiceLibConfiguration,
    ) {}

    ngOnChanges(changes: SimpleChanges) {
        if (this.configuration && this.configuration.assetsUrl) {
            pdfDefaultOptions.assetsFolder =
                (this.configuration?.assetsUrl || '') + '/ngx-extended-pdf-viewer';
        } else if (this.assetUrl !== undefined) {
            pdfDefaultOptions.assetsFolder = this.assetUrl;
        }
        if (this.node) {
            if (
                this.nodeHelper.getNodesRight(
                    [this.node!],
                    'DownloadContent',
                    NodesRightMode.Effective,
                )
            ) {
                this.fileData = this.data?.items?.[0]?.link ?? '';
            } else {
                this.restrictedView = true;
                this.downloadFile();
            }
        }
    }

    private downloadFile() {
        if (this.data?.items === undefined || this.data.items[0].link === '') {
            return;
        }
        const url = new URL(this.data.items[0].link);
        const assetParams = url.searchParams.get('assetParams');
        if (assetParams === null) {
            return;
        }
        const params: GetAsset$Params = {
            assetParams: assetParams,
        };
        this.assetControllerService.getAsset$Response(params).subscribe(async (response) => {
            if (response.headers.get('content-type') !== 'application/octet-stream') {
                return;
            }
            const arrayBuffer = await response.body.arrayBuffer();
            this.fileData = new Uint8Array(arrayBuffer);
        });
    }

    @HostListener('window:keydown.control.s', ['$event'])
    @HostListener('window:keydown.F12', ['$event'])
    handleShortCutEvents(event: Event) {
        if (this.restrictedView) {
            event.preventDefault();
            if (event.stopImmediatePropagation) {
                event.stopImmediatePropagation();
            } else {
                event.stopPropagation();
            }
        }
    }

    @HostListener('window:contextmenu', ['$event'])
    handleContextMenu(event: MouseEvent) {
        if (this.restrictedView) {
            event.preventDefault();
            if (event.stopImmediatePropagation) {
                event.stopImmediatePropagation();
            } else {
                event.stopPropagation();
            }
        }
    }
}
