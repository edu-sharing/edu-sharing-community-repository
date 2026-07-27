import { PlatformLocation } from '@angular/common';
import {
    ChangeDetectorRef,
    Component,
    ContentChild,
    DestroyRef,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    OnInit,
    Output,
    TemplateRef,
    ViewChild,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Node, NodeServiceUnwrapped } from 'ngx-edu-sharing-api';
import { OptionItem, RepoUrlService, RestHelper } from 'ngx-edu-sharing-ui';
import {
    BehaviorSubject,
    Observable,
    Subject,
    combineLatest,
    forkJoin,
    fromEvent,
    interval,
    of,
} from 'rxjs';
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
export class MdsEditorWidgetPreviewComponent implements NativeWidgetComponent, OnInit {
    private changeDetectorRef = inject(ChangeDetectorRef);
    private destroyRef = inject(DestroyRef);
    private events = inject(FrameEventsService);
    // optional: when used standalone (host-controlled, see `standalone`) there is no MDS editor
    mdsEditorInstance = inject(MdsEditorInstanceService, { optional: true });
    private nodeService = inject(RestNodeService);
    private nodeServiceUnwrapped = inject(NodeServiceUnwrapped);
    private platformLocation = inject(PlatformLocation);
    private repoUrlService = inject(RepoUrlService);
    private router = inject(Router);
    private sanitizer = inject(DomSanitizer);
    private toast = inject(Toast);

    @ViewChild('fileInput') fileInput: ElementRef<HTMLInputElement>;
    @ViewChild('overlayRef') overlayRef: ElementRef<HTMLElement>;
    @ViewChild('aiPreviewImagesOverlay') aiPreviewImagesOverlay: AiPreviewImagesOverlayComponent;

    /**
     * Host-controlled mode: operate without an {@link MdsEditorInstanceService} and without saving
     * to a node. The chosen image (or a delete request) is emitted via {@link previewChange} and
     * the host persists it. Enables reusing this widget outside the MDS editor (e.g. the
     * collection create/edit dialog).
     */
    @Input() standalone = false;
    /** in standalone mode: the node whose current preview should be shown (may be undefined) */
    @Input() standaloneNode: Node;
    /** in standalone mode: emitted whenever the pending preview changes (new file or delete) */
    @Output() previewChange = new EventEmitter<{ file: File | null; delete: boolean }>();
    /** in standalone mode: optional fallback shown when there is no user-defined preview image */
    @ContentChild('previewPlaceholder') placeholderTemplate: TemplateRef<unknown>;
    /**
     * render the delete action as an entry in the change-preview menu (with a divider) instead of
     * a separate button. Useful where there is no room for a second button (e.g. the collection
     * create/edit dialog). The change-preview button then shows a "more options" icon.
     */
    @Input() deleteAsOption = false;

    /** standalone layout: the widget fills the host's box (full-width image + blurred backdrop) */
    @HostBinding('class.standalone') get standaloneClass(): boolean {
        return this.standalone;
    }

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
    /**
     * whether AI drawing styles are available. Requires an MDS editor instance, so it stays
     * `false` in standalone mode. Assigned once in {@link ngOnInit}.
     */
    hasAi$: Observable<boolean> = of(false);
    /** options for the collapsed menu to change the preview image */
    previewOptions: OptionItem[] = [];
    /** last known AI availability, kept so previewOptions can be rebuilt on demand */
    private hasAiValue = false;
    /** popup window for selecting an image from search */
    private imageWindow: Window;

    /** whether the preview can be changed (all edit modes and standalone; not the viewer) */
    get editable(): boolean {
        return this.standalone || this.mdsEditorInstance?.editorMode !== 'viewer';
    }
    /** the compact inline layout (label + explicit save button) — never in standalone mode */
    get inline(): boolean {
        return !this.standalone && this.mdsEditorInstance?.editorMode === 'inline';
    }
    /**
     * whether to render the host-provided placeholder instead of an image (standalone only).
     * Also shown while a delete is pending, so the "default" placeholder replaces the removed
     * image rather than a "will be deleted" overlay.
     */
    get showPlaceholder(): boolean {
        return (
            this.standalone &&
            !!this.placeholderTemplate &&
            !this.file &&
            (this.delete || !(this.node?.preview && !this.node.preview.isIcon))
        );
    }
    /** whether there is a preview that can currently be removed */
    get canDeletePreview(): boolean {
        return (
            !this.delete &&
            (!!this.file ||
                this.getType() === 'TYPE_USERDEFINED' ||
                // an existing (non auto-generated) image, e.g. a collection icon
                (this.standalone && !!this.node?.preview && !this.node.preview.isIcon))
        );
    }

    ngOnInit(): void {
        // AI drawing styles need an MDS editor instance -> off in standalone mode
        this.hasAi$ =
            this.standalone || !this.mdsEditorInstance ? of(false) : this.mdsEditorInstance.hasAi;
        // 1) Wire up the input sources for setting a new preview (edit modes only):
        //    clipboard-availability polling, global image paste, and the image-search popup.
        if (this.editable) {
            void this.checkClipboardForImage();
            fromEvent(window, 'focus')
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe(() => void this.checkClipboardForImage());
            // capture phase, so we run before the global `PasteService` document listener
            // and can claim the event via stopPropagation (otherwise `create-menu` toasts
            // CLIPBOARD_DATA_UNSUPPORTED for image pastes)
            fromEvent<ClipboardEvent>(document, 'paste', { capture: true })
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe((event) => this.onPaste(event));
            // receives the node picked in the image-search popup (EVENT_APPLY_NODE);
            // a dedicated teardown subject unregisters the listener on destroy
            const destroyed$ = new Subject<void>();
            this.destroyRef.onDestroy(() => {
                destroyed$.next();
                destroyed$.complete();
            });
            this.events.addListener(this, destroyed$);
        }
        // 2) Reactively (re)build the collapsed "change preview" menu whenever the set of
        //    available sources changes (clipboard image present / AI enabled).
        combineLatest([this.clipboardImageAvailable$, this.hasAi$])
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(([clipboardAvailable, hasAi]) => {
                this.hasAiValue = hasAi;
                this.rebuildPreviewOptions();
            });
        // 3) Resolve the node whose preview is shown. In standalone mode it comes from an input;
        //    otherwise from the MDS editor's current node (with reload polling for processing media).
        if (this.standalone) {
            this.node = this.standaloneNode;
            if (this.node?.preview && !this.node.preview.isIcon) {
                this.nodeSrc = this.buildNodeSrc(this.node);
                void this.updateSrc();
            }
            return;
        }
        this.mdsEditorInstance.nodes$
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe((nodes) => {
                if (nodes?.length === 1 && nodes[0]?.preview) {
                    this.node = nodes[0];
                    this.nodeSrc = this.buildNodeSrc(this.node);
                    void this.updateSrc();
                    // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                    interval(5000)
                        .pipe(
                            takeUntilDestroyed(this.destroyRef),
                            takeWhile(() => !this.file && !this.overlayVisible$.getValue()),
                        )
                        .subscribe(() => this.updateSrc());
                }
            });
    }

    /** Build the (cache-busting) preview source URL/data-URI for a node. */
    private buildNodeSrc(node: Node): string {
        return node.preview.url
            ? node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache'
            : 'data:' + node.preview.mimetype + ';base64,' + node.preview.data;
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
        } else if (this.nodeSrc) {
            const src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
            if (this.node) {
                this.src$.next(await this.repoUrlService.getRepoUrl(src, this.node));
            } else {
                this.src$.next(src);
            }
        } else {
            // no image and no source (e.g. a brand-new collection) -> fall back to the placeholder
            this.src$.next(null);
        }
        this.changeDetectorRef.detectChanges();
        this.hasChanges.next(this.file != null || this.delete);
        // the in-menu delete entry depends on the current file/preview state
        if (this.deleteAsOption) {
            this.rebuildPreviewOptions();
        }
        // host-controlled mode: let the host persist the pending change itself
        if (this.standalone) {
            this.previewChange.emit({ file: this.file, delete: this.delete });
        }
    }

    /** Remove the current preview: discard a pending file, otherwise flag the saved one for deletion. */
    deletePreview() {
        if (this.file) {
            this.file = null;
        } else {
            this.delete = true;
        }
        void this.updateSrc();
    }

    /** Rebuild the change-preview menu from the current state. */
    private rebuildPreviewOptions() {
        this.previewOptions = this.createPreviewOptions(
            this.clipboardImageAvailable$.value,
            this.hasAiValue,
        );
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
     * Re-check the clipboard each time the change-preview menu opens, so an image copied after the
     * menu was first built is offered as a "paste from clipboard" option without needing a window
     * focus change. Opening the menu is a user gesture, so the clipboard read is permitted.
     */
    onPreviewMenuOpened(): void {
        void this.checkClipboardForImage();
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
     * All ways to set a new image live here; delete is included too when {@link deleteAsOption}
     * is set, otherwise it stays a separate button.
     */
    private createPreviewOptions(clipboardAvailable: boolean, hasAi: boolean): OptionItem[] {
        const editable = this.editable;
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
        if (editable && this.deleteAsOption && this.canDeletePreview) {
            const deleteOption = new OptionItem('WORKSPACE.EDITOR.PREVIEW_DELETE', 'delete', () =>
                this.deletePreview(),
            );
            // renders a divider above it in the menu
            deleteOption.isSeparate = true;
            options.push(deleteOption);
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
                this.nodeServiceUnwrapped
                    .changePreview({
                        repository: n.ref.repo,
                        node: n.ref.id,
                        mimetype: RestHelper.guessMimeType(this.file),
                        createVersion: false,
                        body: { image: this.file },
                    })
                    .pipe(map((nodeEntry) => nodeEntry.node)),
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
