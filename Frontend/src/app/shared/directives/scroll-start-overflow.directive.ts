import { afterNextRender, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

/**
 * Sets the class `scroll-start-overflow` on its host while the first scroll child of the host
 * has content hidden beyond its left edge, i.e. while a right-aligned (`direction: rtl`) scroll
 * container is clipping its beginning.
 */
@Directive({
    selector: '[esScrollStartOverflow]',
})
export class ScrollStartOverflowDirective {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

    constructor() {
        const destroyRef = inject(DestroyRef);
        afterNextRender(() => {
            const scroller = this.host.firstElementChild as HTMLElement | null;
            if (!scroller) {
                return;
            }
            const update = () => {
                // `scrollLeft` is 0 at the right end and negative once scrolled towards the left
                const hidden = scroller.scrollWidth - scroller.clientWidth + scroller.scrollLeft;
                this.host.classList.toggle('scroll-start-overflow', hidden > 1);
            };
            const observer = new ResizeObserver(update);
            observer.observe(scroller);
            const content = scroller.firstElementChild;
            if (content) {
                observer.observe(content);
            }
            scroller.addEventListener('scroll', update, { passive: true });
            update();
            destroyRef.onDestroy(() => {
                observer.disconnect();
                scroller.removeEventListener('scroll', update);
            });
        });
    }
}
