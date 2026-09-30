import { Injectable, OnDestroy, Renderer2, RendererFactory2, inject } from '@angular/core';
import { getBodyHeight } from '../utils/dom-util';

@Injectable({
    providedIn: 'root',
})
export class ScrollHelperService implements OnDestroy {
    private rendererFactory = inject(RendererFactory2);

    private _changeLayoutPending: boolean = false;
    get changeLayoutPending(): boolean {
        return this._changeLayoutPending;
    }
    set changeLayoutPending(val: boolean) {
        this._changeLayoutPending = val;
        // avoid resetting variable, when another change is triggered
        clearTimeout(this.layoutTimeout);
        this.layoutTimeout = setTimeout((): void => {
            this._changeLayoutPending = false;
        }, 500);
    }
    private layoutTimeout: ReturnType<typeof setTimeout>;
    private relativeScrollYPosition: number = -1;
    private renderer: Renderer2;
    // element the page scrolls in; the window while none is registered
    private scrollContainer: HTMLElement | null = null;
    // https://medium.com/claritydesignsystem/four-ways-of-listening-to-dom-events-in-angular-part-3-renderer2-listen-14c6fe052b59
    private unlistener: () => void;

    constructor() {
        // get an instance of Renderer2 inside the service (https://stackoverflow.com/a/47924814)
        this.renderer = this.rendererFactory.createRenderer(null, null);
        this.setScrollContainer(null);
    }

    /**
     * Tracks the scroll position of the given element, or of the window when null is passed.
     */
    setScrollContainer(element: HTMLElement | null): void {
        this.unlistener?.();
        this.scrollContainer = element;
        this.relativeScrollYPosition = -1;
        this.unlistener = this.renderer.listen(element ?? 'window', 'scroll', (): void => {
            if (!this.changeLayoutPending) {
                this.relativeScrollYPosition = Math.round(
                    this.getScrollHeight() - this.getScrollTop(),
                );
            }
        });
    }

    /**
     * Unlistens on service destroy.
     */
    ngOnDestroy(): void {
        this.unlistener();
    }

    /**
     * Restores the scroll position to the latest stored relative position.
     */
    restoreScrollPosition(): void {
        const updatedScrollHeight: number = this.getScrollHeight();
        // check for valid data being provided
        if (
            this.relativeScrollYPosition > -1 &&
            updatedScrollHeight - this.relativeScrollYPosition > 0
        ) {
            (this.scrollContainer ?? window).scrollTo({
                top: updatedScrollHeight - this.relativeScrollYPosition,
                left: 0,
                behavior: 'smooth',
            });
            // reset helper variables
            clearTimeout(this.layoutTimeout);
            this.changeLayoutPending = false;
        }
    }

    private getScrollHeight(): number {
        return this.scrollContainer?.scrollHeight ?? getBodyHeight();
    }

    private getScrollTop(): number {
        return this.scrollContainer?.scrollTop ?? window.scrollY;
    }
}
