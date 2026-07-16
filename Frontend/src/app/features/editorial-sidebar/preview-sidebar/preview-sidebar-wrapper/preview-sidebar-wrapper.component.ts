import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, Input, OnDestroy, TemplateRef, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { CustomOptions, RestHelper } from 'ngx-edu-sharing-ui';
import { EditorMode } from '../../../mds/types/types';
import { PreviewSidebarService } from '../preview-sidebar.service';
import { DialogsService } from '../../../dialogs/dialogs.service';
import { CardDialogRef } from '../../../dialogs/card-dialog/card-dialog-ref';
import { GenericDialogData } from '../../../dialogs/dialog-modules/generic-dialog/generic-dialog-data';

/**
 * Header and card chrome around es-preview-sidebar (like the editorial sidebar). It owns the
 * resizable column and, below 900px, shows the preview in a full-screen modal it controls.
 */
@Component({
    selector: 'es-preview-sidebar-wrapper',
    templateUrl: './preview-sidebar-wrapper.component.html',
    styleUrls: ['./preview-sidebar-wrapper.component.scss'],
    standalone: false,
})
export class PreviewSidebarWrapperComponent implements OnDestroy {
    previewSidebarService = inject(PreviewSidebarService);
    private breakpointObserver = inject(BreakpointObserver);
    private dialogs = inject(DialogsService);

    /** Custom options for the embedded preview actionbar. */
    @Input() customOptions: CustomOptions;
    /** Editor mode for the preview. Defaults to 'viewer' (read-only). */
    @Input() editorMode: EditorMode = 'viewer';
    /** Group id for the embedded mds-editor-wrapper. */
    @Input() groupId = 'preview_sidebar';

    // Resize config passed to the inner esResizableSidenav; hosts pass their own values.
    /** localStorage key under which the chosen width is persisted. */
    @Input() storageKey?: string;
    /** default width in absolute pixels. */
    @Input() defaultWidthPx: number | null = null;
    /** minimum width as a fraction of the viewport (0 disables the fractional minimum). */
    @Input() minWidth = 0;
    /** minimum width in absolute pixels. */
    @Input() minWidthPx = 400;
    /** maximum width in absolute pixels. */
    @Input() maxWidthPx = 800;

    @ViewChild('modal', { static: true }) private modalTemplate: TemplateRef<unknown>;

    /** Below 900px the preview shows as a full-screen modal instead of the inline card. */
    readonly isMobile$ = this.breakpointObserver
        .observe(['(max-width: 900px)'])
        .pipe(map(({ matches }) => matches));

    private modalRef: CardDialogRef<GenericDialogData<string>, string> | null = null;
    /** guards against opening the dialog twice while the first open is still pending */
    private opening = false;
    /** set while we close the dialog because the screen grew — so we keep the node (inline view) */
    private closingForResize = false;

    constructor() {
        // Open/close the modal when the breakpoint or previewed node changes (follows window resize).
        combineLatest([this.isMobile$, this.previewSidebarService.getCurrentNode()])
            .pipe(takeUntilDestroyed())
            .subscribe(([isMobile, node]) => {
                const showModal = isMobile && !!node;
                if (showModal && !this.modalRef && !this.opening) {
                    void this.openModal();
                } else if (!showModal && this.modalRef) {
                    // keep the node when we close only because the screen grew (still previewing)
                    this.closingForResize = !!node;
                    this.modalRef.close();
                }
            });
    }

    ngOnDestroy(): void {
        this.modalRef?.close();
    }

    close(): void {
        this.previewSidebarService.handleNodeClick(null);
    }

    private async openModal(): Promise<void> {
        const node = this.previewSidebarService.getCurrentNode().value;
        if (!node) {
            return;
        }
        this.opening = true;
        this.modalRef = await this.dialogs.openGenericDialog({
            title: RestHelper.getTitle(node),
            avatar: { kind: 'image', url: node.icon?.url },
            contentTemplate: this.modalTemplate,
            minWidth: '100%',
            minHeight: '100%',
            contentPadding: 0,
        });
        this.opening = false;
        this.modalRef.afterClosed().subscribe(() => {
            const wasResize = this.closingForResize;
            this.closingForResize = false;
            this.modalRef = null;
            // a user-initiated close (X / backdrop) clears the previewed node; a resize-close keeps it
            if (!wasResize) {
                this.close();
            }
        });
    }
}
