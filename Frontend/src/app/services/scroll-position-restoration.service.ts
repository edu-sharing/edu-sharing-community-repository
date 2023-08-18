import { ViewportScroller } from '@angular/common';
import { Injectable } from '@angular/core';
import { Router, Scroll } from '@angular/router';
import { filter } from 'rxjs/operators';

/**
 * Emulates the router option `scrollPositionRestoration: 'enabled'`.
 *
 * Provides additional control features.
 */
@Injectable({
    providedIn: 'root',
})
export class ScrollPositionRestorationService {
    private shouldSkipNextScrollEvent = false;

    constructor(private router: Router, private viewportScroller: ViewportScroller) {}

    /**
     * Sets up scroll position restoration.
     *
     * Call once from the app component.
     */
    setup(): void {
        // Recreate scroll restoration behavior of the scrollPositionRestoration option (see
        // https://angular.io/api/router/ExtraOptions#scrollPositionRestoration).
        //
        // To consistently get the `enabled` behavior across browsers and independently of loading
        // times, we could fetch the content via a [resolver](https://angular.io/api/router/Resolve)
        // on navigation. In case this doesn't work everywhere, uncomment the `delay` operator
        // below.
        this.router.events
            .pipe(
                filter((e): e is Scroll => e instanceof Scroll),
                // delay(0), // Wait for data to be rendered.
            )
            .subscribe((e) => {
                if (this.shouldSkipNextScrollEvent) {
                    this.shouldSkipNextScrollEvent = false;
                    return;
                }
                if (e.position) {
                    this.viewportScroller.scrollToPosition(e.position);
                } else if (e.anchor) {
                    this.viewportScroller.scrollToAnchor(e.anchor);
                } else {
                    this.viewportScroller.scrollToPosition([0, 0]);
                }
            });
    }

    /**
     * Makes this service skip the next scroll event.
     *
     * Useful for performing navigation without scrolling to the page top.
     */
    skipNextScrollEvent(): void {
        this.shouldSkipNextScrollEvent = true;
    }
}
