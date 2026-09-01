import {
    Directive,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    NgZone,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { AppContainerService } from '../services/app-container.service';

@Directive({
    selector: '[esInfiniteScroll], [infinite-scroll], [data-infinite-scroll]',
    standalone: false,
})
export class InfiniteScrollDirective implements OnInit, OnChanges, OnDestroy {
    private appContainer = inject(AppContainerService);
    private element = inject(ElementRef);
    private zone = inject(NgZone);

    @Output() scrolled = new EventEmitter<void>();

    @Input() infiniteScrollDistance: number = 1.5;
    @Input() infiniteScrollThrottle: number = 1000;
    @Input() scrollWindow: boolean = true;
    /**
     * Axis to observe. Use `horizontal` for containers that scroll sideways
     * (e.g. the card grid in `scroll` layout), which never change `scrollTop`.
     */
    @Input() infiniteScrollAxis: 'vertical' | 'horizontal' = 'vertical';
    private lastEvent = 0;
    private lastScroll = 0;
    /** Emits to remove the current scroll listener (on re-attach and on destroy). */
    private detach$ = new Subject<void>();
    private scrollElement: HTMLElement | null = null;

    ngOnInit(): void {
        this.attach();
    }

    ngOnChanges(changes: SimpleChanges): void {
        // `scrollWindow` decides which element we listen on. It is commonly bound to an
        // expression that only settles after init (e.g. the card grid's `layout`), so the
        // listener has to follow instead of being wired once.
        if (changes['scrollWindow'] && !changes['scrollWindow'].firstChange) {
            this.attach();
        } else if (changes['infiniteScrollAxis'] && !changes['infiniteScrollAxis'].firstChange) {
            // `lastScroll` holds a position of the previous axis and would otherwise be
            // compared against the new one.
            this.lastScroll = 0;
        }
    }

    private attach(): void {
        this.detach$.next();
        this.scrollElement = null;
        this.lastScroll = 0;
        this.zone.runOutsideAngular(() => {
            const handleScroll = () => this.handleOnScroll();
            const eventTarget = this.scrollWindow
                ? this.appContainer.getScrollContainer({ fallback: window })
                : this.getScrollElement();
            eventTarget.addEventListener('scroll', handleScroll);
            this.detach$
                .pipe(take(1))
                .subscribe(() => eventTarget.removeEventListener('scroll', handleScroll));
        });
    }

    /**
     * Resolves the scroll container for the non-window case: the host element itself
     * if it scrolls, otherwise the next closest ancestor with an `overflow`
     * (or `overflow-y`) of `auto`/`scroll`. Falls back to the host element.
     */
    private getScrollElement(): HTMLElement {
        if (this.scrollElement) {
            return this.scrollElement;
        }
        this.scrollElement =
            this.findScrollableParent(this.element.nativeElement) ?? this.element.nativeElement;
        return this.scrollElement;
    }

    private findScrollableParent(element: HTMLElement | null): HTMLElement | null {
        if (!element) {
            return null;
        }
        const style = getComputedStyle(element);
        const overflow = style.overflowY + style.overflow;
        if (/(auto|scroll|overlay)/.test(overflow)) {
            return element;
        }
        return this.findScrollableParent(element.parentElement);
    }

    ngOnDestroy(): void {
        this.detach$.next();
        this.detach$.complete();
    }

    handleOnScroll() {
        if (!this.element.nativeElement) return;
        const element = this.scrollWindow
            ? this.appContainer.getScrollContainer()
            : this.getScrollElement();
        const { scroll, size, viewport } = this.getScrollMetrics(element);
        if (scroll > this.lastScroll && size - scroll < viewport * this.infiniteScrollDistance) {
            const time = new Date().getTime();
            if (time - this.lastEvent >= this.infiniteScrollThrottle) {
                this.lastEvent = time;
                this.emitScrolled();
            }
        }
        this.lastScroll = scroll;
    }

    /**
     * Position, total extent and visible extent of `element` along the observed axis.
     *
     * For the window container we keep using `clientHeight` as the viewport, while an
     * element container is measured via its bounding rect (as before).
     */
    private getScrollMetrics(element: HTMLElement): {
        scroll: number;
        size: number;
        viewport: number;
    } {
        if (this.infiniteScrollAxis === 'horizontal') {
            return {
                scroll: element.scrollLeft,
                size: element.scrollWidth,
                viewport: this.scrollWindow
                    ? element.clientWidth
                    : element.getBoundingClientRect().width,
            };
        }
        return {
            scroll: element.scrollTop,
            size: element.scrollHeight,
            viewport: this.scrollWindow
                ? element.clientHeight
                : element.getBoundingClientRect().height,
        };
    }

    private emitScrolled(): void {
        this.zone.run(() => this.scrolled.emit());
    }
}
