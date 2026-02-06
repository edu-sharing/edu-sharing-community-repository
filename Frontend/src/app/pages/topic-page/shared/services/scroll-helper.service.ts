import { Injectable, OnDestroy, Renderer2, RendererFactory2 } from '@angular/core';
import { getBodyHeight } from '../utils/dom-util';

@Injectable({
    providedIn: 'root',
})
export class ScrollHelperService implements OnDestroy {
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
    // https://medium.com/claritydesignsystem/four-ways-of-listening-to-dom-events-in-angular-part-3-renderer2-listen-14c6fe052b59
    private unlistener: () => void;

    constructor(private rendererFactory: RendererFactory2) {
        // get an instance of Renderer2 inside the service (https://stackoverflow.com/a/47924814)
        this.renderer = this.rendererFactory.createRenderer(null, null);

        // start listening and store the "unlistener" function
        this.unlistener = this.renderer.listen('window', 'scroll', (): void => {
            if (!this.changeLayoutPending) {
                this.relativeScrollYPosition = Math.round(getBodyHeight() - window.scrollY);
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
        const updatedBodyHeight: number = getBodyHeight();
        // check for valid data being provided
        if (
            this.relativeScrollYPosition > -1 &&
            updatedBodyHeight - this.relativeScrollYPosition > 0
        ) {
            window.scrollTo({
                top: updatedBodyHeight - this.relativeScrollYPosition,
                left: 0,
                behavior: 'smooth',
            });
            // reset helper variables
            clearTimeout(this.layoutTimeout);
            this.changeLayoutPending = false;
        }
    }
}
