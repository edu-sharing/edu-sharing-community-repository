import {
    Component,
    ElementRef,
    Input,
    OnChanges,
    SimpleChanges,
    ViewChild,
    inject,
} from '@angular/core';
import { RenderingModule } from '../../rendering.module';
import { RenderModule } from '../RenderModule';
import { Node } from 'ngx-edu-sharing-api';
import { RenderData } from '../../dto/RenderData';
import {
    RENDERING_SERVICE_LIB_CONFIG,
    RenderingServiceLibConfiguration,
} from '../../../rendering-service-lib-configuration';
import { pdfDefaultOptions } from 'ngx-extended-pdf-viewer';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';
import { PdfComponent } from '../pdf/pdf.component';

@Component({
    selector: 'rs-module-pdf-iframe',
    imports: [RenderingModule],
    templateUrl: './pdf-iframe.component.html',
    styleUrl: './pdf-iframe.component.scss',
})
export class PdfIframeComponent implements RenderModule, OnChanges {
    private nodeHelper = inject(NodeHelperService);
    private elementRef = inject(ElementRef);
    configuration = inject<RenderingServiceLibConfiguration>(RENDERING_SERVICE_LIB_CONFIG, {
        optional: true,
    });

    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;

    private componentBaseUrl: string = '';

    constructor() {
        const configuration = this.configuration;

        if (configuration && configuration.assetsUrl) {
            pdfDefaultOptions.assetsFolder =
                (configuration?.assetsUrl || '') + '/ngx-extended-pdf-viewer';
            this.componentBaseUrl = (configuration?.assetsUrl || '').replace('/assets', '/pdf');
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (
            this.nodeHelper.getNodesRight([this.node!], 'DownloadContent', NodesRightMode.Effective)
        ) {
            setTimeout(() => this.initFrame(this.data?.items?.[0]?.link ?? ''));
        }
    }

    private initFrame(data: string) {
        const iframe = this.iframe.nativeElement;
        const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document;
        if (iframeDoc) {
            // CSS custom properties do not cross the iframe boundary, so forward the
            // optional `--containerHeight` styling hook from the host into the iframe.
            const containerHeight = getComputedStyle(this.elementRef.nativeElement)
                .getPropertyValue('--containerHeight')
                .trim();
            if (containerHeight) {
                iframe.style.height = containerHeight;
            }
            iframe.onload = () => {
                const doc = iframe.contentDocument || iframe.contentWindow?.document;
                const widget = doc!.getElementsByTagName('es-pdf')[0] as unknown as PdfComponent;
                widget.data = this.data;
                widget.node = this.node;
                widget.assetUrl = this.componentBaseUrl + '/assets';
            };
            iframeDoc.open();
            iframeDoc.write(`<!DOCTYPE html><html><head>
        <style>
          html, body { margin: 0; padding: 0; height: 100%; }
          es-pdf { display: block; height: 100%; }
          ${containerHeight ? `:root { --containerHeight: ${containerHeight}; }` : ''}
        </style>
</head><body>
      <script src="${this.componentBaseUrl}/runtime.js" type="module"></script>
      <script src="${this.componentBaseUrl}/polyfills.js" type="module"></script>
      <script src="${this.componentBaseUrl}/vendor.js" type="module"></script>
      <script src="${this.componentBaseUrl}/main.js" type="module"></script>
      <es-pdf></es-pdf>
</body></html>`);
            iframeDoc.close();
        }
    }
}
