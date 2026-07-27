import {
    AfterViewInit,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    OnDestroy,
    output,
    TemplateRef,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { GlobalPositionStrategy, Overlay, OverlayModule, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

/**
 * Always-visible tab ("Lasche") docked to a viewport edge that opens/closes an associated
 * drawer (e.g. a `mat-sidenav`). It is rendered through a body-level CDK overlay so it stays
 * visible even while the drawer is closed (a closed `mat-sidenav` hides its own content), and
 * its offset from the edge tracks the drawer width so the tab slides together with the panel.
 *
 * Provide the drawer element via {@link drawer} (its width is measured); when omitted the tab
 * falls back to the nearest `.mat-drawer` ancestor of its host.
 */
@Component({
    selector: 'es-edge-toggle',
    standalone: true,
    imports: [OverlayModule, TranslateModule, EduSharingUiCommonModule],
    templateUrl: './edge-toggle.component.html',
    styleUrls: ['./edge-toggle.component.scss'],
})
export class EdgeToggleComponent implements AfterViewInit, OnDestroy {
    private overlay = inject(Overlay);
    private viewContainerRef = inject(ViewContainerRef);
    private elementRef = inject(ElementRef);

    /** which viewport edge the drawer/tab sits on ('end' = right, 'start' = left) */
    readonly side = input<'start' | 'end'>('end');
    /** whether the associated drawer is currently open */
    readonly open = input.required<boolean>();
    /** whether the tab should be rendered at all */
    readonly visible = input(true);
    /** the drawer element whose width the tab tracks; falls back to the nearest `.mat-drawer` */
    readonly drawer = input<ElementRef | HTMLElement | null>(null);
    /** distance below the top nav at which the tab is pinned */
    readonly topOffset = input('45px');
    /**
     * how to derive the tab's offset from the drawer:
     * - 'width': use the drawer's width (correct for a `mat-sidenav`, which keeps its full width
     *   while closed and slides via transform)
     * - 'edge': use the drawer's actual inner-edge x-coordinate (correct for an in-flow panel that
     *   sits at an x-offset > 0, so width ≠ edge position)
     */
    readonly measure = input<'width' | 'edge'>('width');
    /**
     * whether the pane animates its offset via CSS transition. Set false when the drawer itself
     * animates its size (so a ResizeObserver tracks the edge live) to avoid the transition lagging.
     */
    readonly animatePane = input(true);
    /** i18n keys for the aria-label in the closed / open state */
    readonly labelOpen = input('EDITORIAL.SIDEBAR.OPEN');
    readonly labelClose = input('EDITORIAL.SIDEBAR.CLOSE');

    /** emitted when the tab is clicked */
    readonly toggled = output<void>();

    @ViewChild('tab', { static: true }) tabTpl: TemplateRef<unknown>;
    private overlayRef: OverlayRef;
    private position: GlobalPositionStrategy;
    private resizeObserver: ResizeObserver;

    readonly icon = computed(() => {
        // chevron points toward the drawer's edge: outward to open, back to close
        const openIcon = this.side() === 'end' ? 'keyboard_arrow_right' : 'keyboard_arrow_left';
        const closedIcon = this.side() === 'end' ? 'keyboard_arrow_left' : 'keyboard_arrow_right';
        return this.open() ? openIcon : closedIcon;
    });

    constructor() {
        // slide the tab to/from the drawer edge whenever the open state changes
        effect(() => {
            this.open();
            this.updatePosition();
        });
        // (re)observe the drawer element for width (resize) changes
        effect(() => {
            const drawer = this.resolveDrawer();
            this.resizeObserver?.disconnect();
            if (typeof ResizeObserver !== 'undefined' && drawer) {
                // Track the edge on every size change (e.g. drag-resize, or a panel animating its
                // own width) — updatePosition resolves the correct offset for the current state.
                this.resizeObserver = new ResizeObserver(() => this.updatePosition());
                this.resizeObserver.observe(drawer);
            }
        });
    }

    ngAfterViewInit(): void {
        this.position = this.overlay
            .position()
            .global()
            .top(`calc(var(--mainnavCurrentHeight) + ${this.topOffset()})`);
        this.overlayRef = this.overlay.create({
            positionStrategy: this.position,
            // '--static' disables the pane's CSS offset transition (used when the drawer animates
            // its own size and a ResizeObserver already tracks the edge live, so a transition here
            // would only lag behind).
            panelClass: this.animatePane()
                ? 'es-edge-toggle-pane'
                : ['es-edge-toggle-pane', 'es-edge-toggle-pane--static'],
            hasBackdrop: false,
            scrollStrategy: this.overlay.scrollStrategies.noop(),
        });
        this.overlayRef.attach(new TemplatePortal(this.tabTpl, this.viewContainerRef));
        this.updatePosition();
    }

    ngOnDestroy(): void {
        this.resizeObserver?.disconnect();
        this.overlayRef?.dispose();
    }

    private resolveDrawer(): HTMLElement | null {
        const drawer = this.drawer();
        if (drawer instanceof ElementRef) {
            return drawer.nativeElement as HTMLElement;
        }
        if (drawer instanceof HTMLElement) {
            return drawer;
        }
        return (this.elementRef.nativeElement as HTMLElement).closest('.mat-drawer');
    }

    private updatePosition(): void {
        if (!this.overlayRef || !this.position) {
            return;
        }
        let offset = 0;
        const rect = this.resolveDrawer()?.getBoundingClientRect();
        if (this.measure() === 'edge') {
            // Follow the drawer's actual inner-edge x-coordinate. Correct for an in-flow panel that
            // sits at an x-offset > 0 and for a panel that animates its own width (a collapsed or
            // hidden drawer reports a ~zero edge → offset 0). Not gated on open() so the tab keeps
            // tracking the edge while the panel animates closed.
            if (rect) {
                offset = this.side() === 'end' ? window.innerWidth - rect.left : rect.right;
            }
        } else if (this.open() && rect) {
            // 'width': the drawer keeps its full width while closed (mat-sidenav slides via
            // transform), so gate on open() and use the width; the pane CSS transition eases it.
            offset = rect.width;
        }
        const value = `${Math.round(Math.max(0, offset))}px`;
        if (this.side() === 'end') {
            this.position.right(value);
        } else {
            this.position.left(value);
        }
        this.overlayRef.updatePosition();
    }
}
