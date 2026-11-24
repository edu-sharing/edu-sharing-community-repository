import {
    Component,
    DestroyRef,
    ElementRef,
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
import { TranslateModule } from '@ngx-translate/core';
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
    NgxKeyboardManagerService,
    pdfDefaultOptions,
} from 'ngx-extended-pdf-viewer';
import { NodeHelperService, NodesRightMode } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'rs-module-pdf',
    imports: [
        RenderingModule,
        MatButtonModule,
        MatIconModule,
        NgxExtendedPdfViewerModule,
        TranslateModule,
    ],
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
        // Never execute JavaScript embedded in a pdf (OpenAction, document / page / field actions).
        // Set explicitly instead of relying on the library defaults, since the peer dependency range
        // allows older ngx-extended-pdf-viewer versions.
        pdfDefaultOptions.enableScripting = false;
        pdfDefaultOptions.isEvalSupported = false;
        pdfDefaultOptions.enableOpenActionJavaScript = false;
        pdfDefaultOptions.enableCatalogAAJavaScript = false;

        // Keep the viewer theme in sync with live dark-mode toggles.
        const observer = new MutationObserver(() =>
            this.isDarkTheme.set(document.body.classList.contains('isDarkTheme')),
        );
        observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
        inject(DestroyRef).onDestroy(() => observer.disconnect());

        this.restrictShortcutsToActiveViewer();
    }

    /**
     * pdf.js listens for its single-key shortcuts (e.g. `r` rotates, `j`/`k` change the page) on
     * the whole window, so they would fire wherever the user is working — also for speech-input
     * users whose dictation contains these letters. Keys are only passed to the viewer while it is
     * the active area: after a click into it or while the keyboard focus is inside it (the page
     * area itself cannot take focus, so a click there does not move the focus). A click or focus
     * anywhere else deactivates it. Ctrl + mouse wheel zoom (reported as `'WHEEL'`) is not
     * affected.
     */
    private restrictShortcutsToActiveViewer(): void {
        const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
        const keyboardManager = inject(NgxKeyboardManagerService);
        let active = false;
        // composedPath, so a target inside the shadow root of the pdf web component is found too
        const track = (event: Event) => (active = event.composedPath().includes(host));
        document.addEventListener('pointerdown', track, true);
        document.addEventListener('focusin', track, true);
        const isKeyIgnored = keyboardManager.isKeyIgnored;
        keyboardManager.isKeyIgnored = (cmd: number, keycode: number | 'WHEEL') =>
            (keycode !== 'WHEEL' && !active) || isKeyIgnored.call(keyboardManager, cmd, keycode);
        inject(DestroyRef).onDestroy(() => {
            document.removeEventListener('pointerdown', track, true);
            document.removeEventListener('focusin', track, true);
            keyboardManager.isKeyIgnored = isKeyIgnored;
        });
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
