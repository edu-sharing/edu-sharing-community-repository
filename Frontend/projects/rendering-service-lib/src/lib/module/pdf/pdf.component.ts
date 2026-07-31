import {
    Component,
    DestroyRef,
    HostListener,
    inject,
    Input,
    OnChanges,
    signal,
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
    AnnotationEditorType,
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
    private assetControllerService = inject(AssetControllerService);
    private nodeHelper = inject(NodeHelperService);
    configuration = inject<RenderingServiceLibConfiguration>(RENDERING_SERVICE_LIB_CONFIG, {
        optional: true,
    });

    @ViewChild(NgxExtendedPdfViewerComponent) pdfViewer!: NgxExtendedPdfViewerComponent;
    @Input() data: RenderData | undefined;
    @Input() node: Node | undefined;
    @Input() assetUrl: string | undefined;
    restrictedView: boolean = false;
    fileData: Uint8Array | string | undefined;

    // Mirrors the app's applied dark-mode state. The library must not depend on the app's `ThemeService
    protected isDarkTheme = signal(document.body.classList.contains('isDarkTheme'));

    constructor() {
        // This is a view-only PDF widget — we never enable pdf.js' annotation
        // editor. Disabling it (vs. the default NONE mode) stops pdf.js from
        // constructing its AnnotationEditorUIManager, which otherwise registers
        // document-level `dragover`/`drop` listeners (to paste dropped images
        // into the stamp editor)
        pdfDefaultOptions.annotationEditorMode = AnnotationEditorType.DISABLE;

        // Keep the viewer theme in sync with live dark-mode toggles.
        const observer = new MutationObserver(() =>
            this.isDarkTheme.set(document.body.classList.contains('isDarkTheme')),
        );
        observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
        inject(DestroyRef).onDestroy(() => observer.disconnect());
    }

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
