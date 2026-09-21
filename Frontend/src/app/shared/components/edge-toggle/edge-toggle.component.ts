import {
    AfterViewInit,
    Component,
    computed,
    effect,
    ElementRef,
    HostBinding,
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
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

/**
 * Always-visible tab ("Lasche") docked to a viewport edge that opens/closes a drawer. By default
 * renders through a body-level CDK overlay so it stays visible while the drawer is closed, and
 * tracks the drawer's width so it slides with the panel.
 *
 * Set {@link inline} to render at the tab's own template position instead (no overlay) — only
 * safe when that position is outside any transformed ancestor.
 *
 * Provide {@link drawer} to measure a specific element; otherwise falls back to the nearest
 * `.mat-drawer` ancestor.
 */
@Component({
    selector: 'es-edge-toggle',
    standalone: true,
    imports: [CommonModule, OverlayModule, TranslateModule, EduSharingUiCommonModule],
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
    /** id of the element the tab operates, exposed as `aria-controls` */
    readonly ariaControls = input<string | null>(null);
    /** see class doc */
    readonly inline = input(false);
    @HostBinding('class.inline') get isInline(): boolean {
        return this.inline();
    }
    /** mirrors `animatePane` for `inline`, since there's no overlay `panelClass` here */
    @HostBinding('class.static') get isStatic(): boolean {
        return this.inline() && !this.animatePane();
    }

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
        if (this.inline()) {
            // rendered directly by the template — no overlay/portal to set up
            this.updatePosition();
            return;
        }
        this.position = this.overlay
            .position()
            .global()
            .top(`calc(var(--mainnavCurrentHeight) + ${this.topOffset()})`);
        this.overlayRef = this.overlay.create({
            positionStrategy: this.position,
            // '--static' skips the offset transition when a ResizeObserver already tracks live resizing
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

    /** The tab's button element (queried from the overlay's DOM, or a normal descendant if inline). */
    tabElement(): HTMLElement | null {
        if (this.inline()) {
            return (this.elementRef.nativeElement as HTMLElement).querySelector('button');
        }
        return this.overlayRef?.overlayElement.querySelector('button') ?? null;
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

    /** Offset from the drawer edge, shared by the overlay and inline position strategies. */
    private computeOffset(): number {
        let offset = 0;
        const rect = this.resolveDrawer()?.getBoundingClientRect();
        if (this.measure() === 'edge') {
            // drawer's actual inner edge — works for an in-flow panel and animating widths
            if (rect) {
                offset = this.side() === 'end' ? window.innerWidth - rect.left : rect.right;
            }
        } else if (this.open() && rect) {
            // drawer's full width; gated on open() since mat-sidenav keeps its width while closed
            offset = rect.width;
        }
        return Math.round(Math.max(0, offset));
    }

    private updatePosition(): void {
        const value = `${this.computeOffset()}px`;
        if (this.inline()) {
            this.updateInlinePosition(value);
            return;
        }
        if (!this.overlayRef || !this.position) {
            return;
        }
        if (this.side() === 'end') {
            this.position.right(value);
        } else {
            this.position.left(value);
        }
        this.overlayRef.updatePosition();
    }

    /** Positions the host via `position: fixed` (see `:host(.inline)` in the stylesheet). */
    private updateInlinePosition(value: string): void {
        const host = this.elementRef.nativeElement as HTMLElement;
        host.style.top = `calc(var(--mainnavCurrentHeight) + ${this.topOffset()})`;
        if (this.side() === 'end') {
            host.style.right = value;
            host.style.left = '';
        } else {
            host.style.left = value;
            host.style.right = '';
        }
    }
}
