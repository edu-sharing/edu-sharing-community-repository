import { trigger } from '@angular/animations';
import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { UIAnimation } from 'ngx-edu-sharing-ui';

/**
 * Shows a small button on the bottom of the screen that scrolls to the page top.
 *
 * Place after all visible content for correct keyboard focus order.
 */
@Component({
    selector: 'es-scroll-to-top-button',
    templateUrl: './scroll-to-top-button.component.html',
    styleUrls: ['./scroll-to-top-button.component.scss'],
    animations: [trigger('fade', UIAnimation.fade())],
})
export class ScrollToTopButtonComponent implements OnInit, OnDestroy {
    showScrollToTop = false;

    private readonly destroyed$ = new Subject<void>();

    constructor(private ngZone: NgZone) {}

    ngOnInit() {
        this.ngZone.runOutsideAngular(() => {
            const handleScroll = () => this.handleScroll();
            window.addEventListener('scroll', handleScroll);
            this.destroyed$.subscribe(() => window.removeEventListener('scroll', handleScroll));
        });
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private handleScroll() {
        const showScrollTop = (window.pageYOffset || document.documentElement.scrollTop) > 400;
        if (showScrollTop !== this.showScrollToTop) {
            this.ngZone.run(() => (this.showScrollToTop = showScrollTop));
        }
    }

    scrollToTop() {
        window.scroll({
            top: 0,
            behavior: 'smooth',
        });
    }
}
