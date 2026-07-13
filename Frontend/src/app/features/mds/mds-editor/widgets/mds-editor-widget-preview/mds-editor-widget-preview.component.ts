import { PlatformLocation } from '@angular/common';
import {
    ChangeDetectorRef,
    Component,
    DestroyRef,
    ElementRef,
    ViewChild,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Node } from 'ngx-edu-sharing-api';
import { OptionItem, RepoUrlService } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Subject, combineLatest, forkJoin, fromEvent, interval, of } from 'rxjs';
import { catchError, map, take, takeWhile } from 'rxjs/operators';
import { FrameEventsService } from '../../../../../core-module/rest/services/frame-events.service';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { Toast } from '../../../../../services/toast';
import { Constraints, NativeWidgetComponent } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { AiPreviewImagesOverlayComponent } from './ai-preview-images-overlay/ai-preview-images-overlay.component';

@Component({
    selector: 'es-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetPreviewComponent implements NativeWidgetComponent {
    private changeDetectorRef = inject(ChangeDetectorRef);
    private events = inject(FrameEventsService);
    mdsEditorInstance = inject(MdsEditorInstanceService);
    private nodeService = inject(RestNodeService);
    private platformLocation = inject(PlatformLocation);
    private repoUrlService = inject(RepoUrlService);
    private router = inject(Router);
    private sanitizer = inject(DomSanitizer);
    private toast = inject(Toast);

    @ViewChild('fileInput') fileInput: ElementRef<HTMLInputElement>;
    @ViewChild('overlayRef') overlayRef: ElementRef<HTMLElement>;
    @ViewChild('aiPreviewImagesOverlay') aiPreviewImagesOverlay: AiPreviewImagesOverlayComponent;

    static readonly constraints: Constraints = {
        requiresNode: true,
        supportsBulk: false,
        supportsInlineEditing: true,
        onConstraintFailed: 'hide',
    };

    hasChanges = new BehaviorSubject<boolean>(false);
    src$ = new BehaviorSubject<SafeResourceUrl | string>(null);
    nodeSrc: string;
    file: File;
    node: Node;
    delete = false;
    loading$ = new BehaviorSubject(false);
    overlayVisible$ = new BehaviorSubject(false);
    clipboardImageAvailable$ = new BehaviorSubject(false);
    /** options for the collapsed menu to change the preview image */
    previewOptions: OptionItem[] = [];
    /** popup window for selecting an image from search */
    private imageWindow: Window;

    constructor() {
        // 1) Wire up the input sources for setting a new preview (edit modes only):
        //    clipboard-availability polling, global image paste, and the image-search popup.
        if (this.mdsEditorInstance.editorMode !== 'viewer') {
            void this.checkClipboardForImage();
            fromEvent(window, 'focus')
                .pipe(takeUntilDestroyed())
                .subscribe(() => void this.checkClipboardForImage());
            // capture phase, so we run before the global `PasteService` document listener
            // and can claim the event via stopPropagation (otherwise `create-menu` toasts
            // CLIPBOARD_DATA_UNSUPPORTED for image pastes)
            fromEvent<ClipboardEvent>(document, 'paste', { capture: true })
                .pipe(takeUntilDestroyed())
                .subscribe((event) => this.onPaste(event));
            // receives the node picked in the image-search popup (EVENT_APPLY_NODE);
            // a dedicated teardown subject unregisters the listener on destroy
            const destroyed$ = new Subject<void>();
            inject(DestroyRef).onDestroy(() => {
                destroyed$.next();
                destroyed$.complete();
            });
            this.events.addListener(this, destroyed$);
        }
        // 2) Reactively (re)build the collapsed "change preview" menu whenever the set of
        //    available sources changes (clipboard image present / AI enabled).
        combineLatest([this.clipboardImageAvailable$, this.mdsEditorInstance.hasAi])
            .pipe(takeUntilDestroyed())
            .subscribe(
                ([clipboardAvailable, hasAi]) =>
                    (this.previewOptions = this.createPreviewOptions(clipboardAvailable, hasAi)),
            );
        forkJoin([this.mdsEditorInstance.nodes$.pipe(take(1))])
            .pipe(takeUntilDestroyed())
            .subscribe(([nodes]) => {
                if (nodes?.length === 1) {
                    this.node = nodes[0];
                    this.nodeSrc = this.node.preview.url
                        ? this.node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache'
                        : 'data:' +
                          this.node.preview.mimetype +
                          ';base64,' +
                          this.node.preview.data;
                    void this.updateSrc();
                    // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                    interval(5000)
                        .pipe(
                            takeUntilDestroyed(),
                            takeWhile(() => !this.file && !this.overlayVisible$.getValue()),
                        )
                        .subscribe(() => this.updateSrc());
                }
            });
    }

    /** Handle the hidden file input's change event: use the chosen file as the new preview. */
    async setPreview(event: Event) {
        this.file = (event.target as HTMLInputElement).files[0];
        this.delete = false;
        void this.updateSrc();
    }

    /**
     * Refresh the displayed image: from the pending local file if set, otherwise from the
     * node's preview URL. Also updates the `hasChanges` flag.
     */
    async updateSrc() {
        if (this.file) {
            this.src$.next(
                this.sanitizer.bypassSecurityTrustResourceUrl(
                    window.URL.createObjectURL(this.file),
                ),
            );
        } else {
            const src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
            if (this.node) {
                this.src$.next(await this.repoUrlService.getRepoUrl(src, this.node));
            } else {
                this.src$.next(src);
            }
        }
        this.changeDetectorRef.detectChanges();
        this.hasChanges.next(this.file != null || this.delete);
    }

    /** Persist the pending preview change (upload or delete) to the node. */
    async foreSave() {
        this.loading$.next(true);

        try {
            this.node = (await this.onSaveNode(this.mdsEditorInstance.nodes$.value))[0];
            this.file = null;
            this.delete = false;
            void this.updateSrc();
            // loading false is triggered via img load event
            this.toast.toast('DIALOG.SAVED');
        } catch (e) {
            this.toast.error(e);
        }
    }

    /** apply an image (clipboard paste or search pick) as the new, unsaved preview */
    private applyImageFile(file: File) {
        this.file = file;
        this.delete = false;
        void this.updateSrc();
    }

    /** wrap an image blob in a File, deriving the extension from its mime type */
    private imageBlobToFile(blob: Blob, baseName: string): File {
        const extension = blob.type?.split('/')[1]?.split('+')[0] || 'png';
        return new File([blob], `${baseName}.${extension}`, { type: blob.type });
    }

    /**
     * Menu action: read the clipboard on demand and apply the first image it holds,
     * toasting if there is none or the clipboard cannot be read.
     */
    private async pasteFromClipboard() {
        try {
            const items = await navigator.clipboard.read();
            for (const item of items) {
                const imageType = item.types.find((type) => type.startsWith('image/'));
                if (imageType) {
                    this.applyImageFile(
                        this.imageBlobToFile(await item.getType(imageType), 'clipboard-image'),
                    );
                    return;
                }
            }
            this.toast.toast('WORKSPACE.EDITOR.PREVIEW_CLIPBOARD_NO_IMAGE');
            void this.checkClipboardForImage();
        } catch (e) {
            this.toast.error(null, 'WORKSPACE.EDITOR.PREVIEW_CLIPBOARD_ERROR');
        }
    }

    /**
     * Global (capture-phase) paste handler: adopt a pasted image as the preview, unless the
     * paste targets an editable field that also carries text (then keep native behavior).
     */
    private onPaste(event: ClipboardEvent) {
        const image = Array.from(event.clipboardData?.files ?? []).find((file) =>
            file.type.startsWith('image/'),
        );
        if (!image) {
            return;
        }
        // when the user pastes into a text field and the clipboard also carries text, keep the
        // native paste behavior and don't hijack the image
        if (
            this.isEditableTarget(event.target) &&
            event.clipboardData.types.includes('text/plain')
        ) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        this.applyImageFile(image);
    }

    /** Whether the event target is an editable field (input / textarea / contenteditable). */
    private isEditableTarget(target: EventTarget): boolean {
        if (!(target instanceof HTMLElement)) {
            return false;
        }
        return target.isContentEditable || !!target.closest('input, textarea, [contenteditable]');
    }

    /**
     * Probe whether the paste-from-clipboard option should be offered: read the clipboard when
     * permission is granted, and optimistically offer the option where the permission API is
     * unavailable (Firefox / Safari), since reading is only allowed on user gesture there.
     */
    private async checkClipboardForImage() {
        if (!navigator.clipboard?.read) {
            this.clipboardImageAvailable$.next(false);
            return;
        }
        let canReadSilently = false;
        try {
            const permission = await navigator.permissions.query({
                name: 'clipboard-read' as PermissionName,
            });
            if (permission.state === 'denied') {
                this.clipboardImageAvailable$.next(false);
                return;
            }
            canReadSilently = permission.state === 'granted';
        } catch (e) {
            // 'clipboard-read' is unknown to the permissions API (Firefox, Safari); reading is
            // only possible on user gesture there, so offer the button and inspect on click
        }
        if (!canReadSilently) {
            this.clipboardImageAvailable$.next(true);
            return;
        }
        try {
            const items = await navigator.clipboard.read();
            this.clipboardImageAvailable$.next(
                items.some((item) => item.types.some((type) => type.startsWith('image/'))),
            );
        } catch (e) {
            // e.g. document lost focus between the permission check and the read
            this.clipboardImageAvailable$.next(false);
        }
    }

    /**
     * The options shown in the collapsed menu to change the preview image.
     * All ways to set a new image live here; only delete stays a separate button.
     */
    private createPreviewOptions(clipboardAvailable: boolean, hasAi: boolean): OptionItem[] {
        const editable = this.mdsEditorInstance.editorMode !== 'viewer';
        const options: OptionItem[] = [];
        if (editable) {
            options.push(
                new OptionItem('WORKSPACE.EDITOR.PREVIEW_UPLOAD', 'upload', () =>
                    this.fileInput.nativeElement.click(),
                ),
            );
        }
        if (editable && clipboardAvailable) {
            options.push(
                new OptionItem('WORKSPACE.EDITOR.PREVIEW_PASTE_CLIPBOARD', 'content_paste', () => {
                    void this.pasteFromClipboard();
                }),
            );
        }
        if (editable) {
            options.push(
                new OptionItem('WORKSPACE.EDITOR.PREVIEW_FROM_SEARCH', 'search', () =>
                    this.chooseFromSearch(),
                ),
            );
        }
        if (hasAi) {
            options.push(
                new OptionItem('MDS.AI.DRAWING_STYLES.HEADING', 'brush', () => this.showOverlay()),
            );
        }
        return options;
    }

    /**
     * Pick an image node from the edu-sharing search:
     * opens the search page as a reurl popup window;
     * the picked node comes back via EVENT_APPLY_NODE
     * with its preview embedded as a data URL.
     */
    private chooseFromSearch() {
        this.imageWindow = UIHelper.openSearchWithReurl(
            this.platformLocation,
            this.router,
            'WINDOW',
            {
                queryParams: { reurlCreate: false, reurlTypes: ['image'] },
            },
        ) as Window;
    }

    // noinspection JSUnusedGlobalSymbols
    /** FrameEventsService listener (registered in the constructor) */
    onEvent(event: string, data: Node) {
        // only react while our own image-search picker is open
        if (event !== FrameEventsService.EVENT_APPLY_NODE || !this.imageWindow) {
            return;
        }
        const imageData = data?.preview?.data;
        if (imageData) {
            void fetch(imageData)
                .then((res) => res.blob())
                .then((blob) => this.applyImageFile(this.imageBlobToFile(blob, 'search-image')));
        } else {
            this.toast.error(null, 'COLLECTIONS.TOAST.ERROR_IMAGE_APPLY');
        }
        this.imageWindow.close();
        this.imageWindow = null;
    }

    /** Open the AI preview-images overlay, anchored to the change-preview button. */
    showOverlay() {
        if (this.aiPreviewImagesOverlay && this.overlayRef) {
            this.aiPreviewImagesOverlay.open(this.overlayRef.nativeElement);
        }
    }

    /**
     * NativeWidgetComponent save hook: delete the preview when flagged, otherwise upload the
     * pending file. Returns the updated nodes, or null when there is nothing to save.
     */
    onSaveNode(nodes: Node[]) {
        if (this.delete) {
            return forkJoin(nodes.map((n) => this.nodeService.deleteNodePreview(n.ref.id)))
                .pipe(map(() => nodes))
                .toPromise();
        }
        if (this.file == null) {
            return null;
        }
        return forkJoin(
            nodes.map((n) =>
                this.nodeService
                    .uploadNodePreview(n.ref.id, this.file, false)
                    .pipe(map((n) => n.node)),
            ),
        )
            .pipe(
                catchError((e) => {
                    this.toast.error(null, 'MDS.ERROR_PREVIEW');
                    return of(nodes);
                }),
            )
            .toPromise();
    }

    /** The node's preview type (e.g. generated vs. user-defined). */
    getType() {
        return this.node?.preview?.type;
    }
}
