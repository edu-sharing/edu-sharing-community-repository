import { Overlay, OverlayRef, PositionStrategy } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    signal,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
    WritableSignal,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthenticationService, MdsWidget, Node } from 'ngx-edu-sharing-api';
import { EduSharingLlmService, Image, ImagesResponse, MdsConfig } from 'ngx-edu-sharing-b-api';
import { firstValueFrom } from 'rxjs';
import { Toast } from '../../../../../../services/toast';
import { MdsEditorInstanceService } from '../../../mds-editor-instance.service';

@Component({
    selector: 'es-ai-preview-images-overlay',
    templateUrl: './ai-preview-images-overlay.component.html',
    styleUrls: ['./ai-preview-images-overlay.component.scss'],
    standalone: false,
})
export class AiPreviewImagesOverlayComponent implements OnInit {
    protected readonly IMAGES_PREFIX: string = 'assets/images/ai/previews/';
    protected readonly IMAGES_SUFFIX: string = '.jpg';
    static readonly WIDGET_ID_DRAWING_STYLE = 'preview.drawingStyle';

    @Input() file: File;
    @Input() node: Node;
    @Output() updateShowOverlay: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() triggerUpdateSrc: EventEmitter<void> = new EventEmitter<void>();
    @Output() updateFile: EventEmitter<File> = new EventEmitter<File>();
    @Output() updateSrcAttribute: EventEmitter<SafeResourceUrl> =
        new EventEmitter<SafeResourceUrl>();
    @ViewChild('overlayTemplate') overlayTemplate!: TemplateRef<any>;

    private overlayRef!: OverlayRef;
    aiDrawingStyles: MdsWidget;
    aiLoading: WritableSignal<boolean> = signal(false);
    previewImages: SafeResourceUrl[] = [];
    selectedImageIndex: number = -1;
    selectedStyleId: string | null = null;
    styleIdToPreviewImagesMap: Map<string, Image[]> = new Map<string, Image[]>();
    private readonly BASE_64_PREFIX: string = 'data:image/jpg;base64,';

    constructor(
        private auth: AuthenticationService,
        private eduSharingLlmService: EduSharingLlmService,
        public mdsEditorInstance: MdsEditorInstanceService,
        private overlay: Overlay,
        private sanitizer: DomSanitizer,
        private toast: Toast,
        private viewContainerRef: ViewContainerRef,
    ) {}

    /**
     * Initializes the component by retrieving the AI drawing styles from the mds definition.
     */
    ngOnInit(): void {
        this.aiDrawingStyles = this.mdsEditorInstance.mdsDefinition$.value.widgets.find(
            (w) => w.id === AiPreviewImagesOverlayComponent.WIDGET_ID_DRAWING_STYLE,
        );
    }

    /**
     * Opens the overlay next to a given trigger HTML element.
     *
     * @param trigger
     */
    open(trigger: HTMLElement): void {
        const positionStrategy: PositionStrategy = this.overlay
            .position()
            .flexibleConnectedTo(trigger)
            .withPositions([
                {
                    originX: 'end',
                    originY: 'center',
                    overlayX: 'start',
                    overlayY: 'center',
                    offsetX: 10,
                },
            ])
            .withPush(false);

        this.overlayRef = this.overlay.create({
            positionStrategy,
            scrollStrategy: this.overlay.scrollStrategies.reposition(),
            hasBackdrop: true,
            backdropClass: 'cdk-overlay-transparent-backdrop',
            panelClass: 'ai-preview-images-overlay',
        });

        this.overlayRef.backdropClick().subscribe(() => this.close());

        const portal = new TemplatePortal(this.overlayTemplate, this.viewContainerRef);
        this.overlayRef.attach(portal);
        this.updateShowOverlay.emit(true);
    }

    /**
     * Closes the overlay by detaching its reference and emitting an event.
     *
     * @param resetData
     */
    close(resetData: boolean = true) {
        if (this.overlayRef) {
            // reset the selected image
            if (resetData) {
                this.resetSelectedImage();
            }
            this.overlayRef.detach();
            this.updateShowOverlay.emit(false);
        }
    }

    /**
     * Generates AI preview images for a given style.
     *
     * @param styleId
     * @param regenerateRequested
     */
    async generatePreviews(styleId: string, regenerateRequested: boolean = false): Promise<void> {
        this.aiLoading.set(true);
        // reset the selected image due to changes taking place on the displayed images
        this.resetSelectedImage();
        // check, whether a map value exist or the regeneration is requested
        if (!this.styleIdToPreviewImagesMap.has(styleId) || regenerateRequested) {
            try {
                const result: ImagesResponse = await this.createAiImages(styleId);
                this.styleIdToPreviewImagesMap.set(styleId, result.data);
            } catch (e) {
                this.toast.error(e);
            }
        }
        // reset the current preview images
        this.previewImages = [];
        // fill the preview images based on the values in the image map
        this.styleIdToPreviewImagesMap.get(styleId)?.forEach((imageData: Image, index: number) => {
            this.previewImages[index] = this.sanitizer.bypassSecurityTrustResourceUrl(
                this.BASE_64_PREFIX + imageData.b64_json,
            );
        });
        this.aiLoading.set(false);
    }

    /**
     * Selects or deselects the image at a given index.
     *
     * @param index
     */
    async selectImage(index: number): Promise<void> {
        if (this.selectedImageIndex !== index) {
            this.selectedImageIndex = index;
            this.updateSrcAttribute.emit(this.previewImages[this.selectedImageIndex]);
        } else {
            // reset the selected image
            this.resetSelectedImage();
        }
    }

    /**
     * Confirms the selected image by emitting a created file and
     */
    async confirmSelection(): Promise<void> {
        if (
            this.selectedImageIndex !== -1 &&
            this.styleIdToPreviewImagesMap.get(this.selectedStyleId)?.[this.selectedImageIndex]
        ) {
            // create a unique filename such as: "ai_image_2025-05-21_15-30-45.jpg"
            // .replace(/[:.]/g, '-') replaces colons and dots with dashes
            // .replace('T', '_') makes it more readable
            // .slice(0,19) removes milliseconds and Z
            const filename = `ai_image_${new Date()
                .toISOString()
                .replace(/[:.]/g, '-')
                .replace('T', '_')
                .slice(0, 19)}.jpg`;
            // retrieve the base64 string from the map, as it still contains the base64 string
            const file = base64ToFile(
                this.styleIdToPreviewImagesMap.get(this.selectedStyleId)[this.selectedImageIndex]
                    .b64_json,
                filename,
                'image/jpg',
            );
            this.updateFile.emit(file);
            this.triggerUpdateSrc.emit();
            this.close(false);
        }
    }

    /**
     * Helper function to create AI images with a given style.
     *
     * @param styleId
     */
    private async createAiImages(styleId: string): Promise<ImagesResponse> {
        const user: string =
            (await firstValueFrom(this.auth.observeLoginInfo()))?.authorityName ?? 'guest';
        const values = await this.mdsEditorInstance.getValues(null, false);
        const mdsConfig: MdsConfig = {
            type: 'mds',
            id: 'image_ai',
        };
        return firstValueFrom(
            this.eduSharingLlmService.imageGeneration({
                body: {
                    configIds: [mdsConfig],
                    metadataSet: this.mdsEditorInstance.mdsId,
                    user,
                    contextNodeId: this.node.ref.id,
                    variables: {
                        ...values,
                        [AiPreviewImagesOverlayComponent.WIDGET_ID_DRAWING_STYLE]: [styleId],
                    },
                },
            }),
        );
    }

    /**
     * Helper function to reset the selected image together with its preview.
     */
    private resetSelectedImage(): void {
        this.selectedImageIndex = -1;
        this.triggerUpdateSrc.emit();
    }
}

/**
 * Util function to convert a given base64 string into a File with a given file name and mime type.
 *
 * @param base64
 * @param filename
 * @param mimeType
 */
function base64ToFile(base64: string, filename: string, mimeType: string): File {
    // split the base64 string in case it has the data URL prefix
    const arr = base64.split(',');
    // decode base64
    const bstr = atob(arr[arr.length - 1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);

    while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
    }

    // create a File from the Uint8Array
    return new File([u8arr], filename, { type: mimeType });
}
