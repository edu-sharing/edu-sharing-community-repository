import {
    Directive,
    ElementRef,
    EventEmitter,
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
})
export class InfiniteScrollDirective implements OnInit, OnDestroy {
    @Output() scrolled = new EventEmitter<void>();

    @Input() infiniteScrollDistance: number = 1.5;
    @Input() infiniteScrollThrottle: number = 1000;
    @Input() scrollWindow: boolean = true;
    private lastEvent = 0;
    private lastScroll = 0;
    private destroyed$ = new Subject<void>();

    constructor(
        private appContainer: AppContainerService,
        private element: ElementRef,
        private zone: NgZone,
    ) {}

    ngOnInit(): void {
        this.zone.runOutsideAngular(() => {
            const handleScroll = () => this.handleOnScroll();
            const eventTarget = this.scrollWindow
                ? this.appContainer.getScrollContainer({ fallback: window })
                : this.element.nativeElement;
            eventTarget.addEventListener('scroll', handleScroll);
            this.destroyed$.subscribe(() =>
                eventTarget.removeEventListener('scroll', handleScroll),
            );
        });
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
            const element = this.element.nativeElement;
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
