import {
    Directive,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { Subject } from 'rxjs';
import { AppContainerService } from '../services/app-container.service';

@Directive({
    selector: '[esInfiniteScroll], [infinite-scroll], [data-infinite-scroll]',
    standalone: false,
})
export class InfiniteScrollDirective implements OnInit, OnDestroy {
    private appContainer = inject(AppContainerService);
    private element = inject(ElementRef);
    private zone = inject(NgZone);

    @Output() scrolled = new EventEmitter<void>();

    @Input() infiniteScrollDistance: number = 1.5;
    @Input() infiniteScrollThrottle: number = 1000;
    @Input() scrollWindow: boolean = true;
    private lastEvent = 0;
    private lastScroll = 0;
    private destroyed$ = new Subject<void>();
    private scrollElement: HTMLElement | null = null;

    ngOnInit(): void {
        this.zone.runOutsideAngular(() => {
            const handleScroll = () => this.handleOnScroll();
            const eventTarget = this.scrollWindow
                ? this.appContainer.getScrollContainer({ fallback: window })
                : this.getScrollElement();
            eventTarget.addEventListener('scroll', handleScroll);
            this.destroyed$.subscribe(() =>
                eventTarget.removeEventListener('scroll', handleScroll),
            );
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
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    handleOnScroll() {
        if (!this.element.nativeElement) return;
        if (this.scrollWindow) {
            const scrollContainer = this.appContainer.getScrollContainer();
            const height = scrollContainer.scrollHeight;
            const scroll = scrollContainer.scrollTop;
            if (
                scroll > this.lastScroll &&
                height - scroll < scrollContainer.clientHeight * this.infiniteScrollDistance
            ) {
                const time = new Date().getTime();
                if (time - this.lastEvent < this.infiniteScrollThrottle) return;
                this.lastEvent = time;
                this.emitScrolled();
            }
            this.lastScroll = scroll;
        } else {
            const element = this.getScrollElement();
            const height = element.scrollHeight;
            const scroll = element.scrollTop;
            if (
                scroll > this.lastScroll &&
                height - scroll <
                    element.getBoundingClientRect().height * this.infiniteScrollDistance
            ) {
                const time = new Date().getTime();
                if (time - this.lastEvent < this.infiniteScrollThrottle) return;
                this.lastEvent = time;
                this.emitScrolled();
            }
            this.lastScroll = scroll;
        }
    }

    private emitScrolled(): void {
        this.zone.run(() => this.scrolled.emit());
    }
}
