import { Directive, ElementRef, Input, OnInit } from '@angular/core';

/**
 * Checks whether text of the annotated element or a descendent (give a selector) was cut of, e.g.,
 * with an ellipsis.
 *
 * Call `hasTextOverflow()` for the result.
 */
@Directive({
    selector: '[appCheckTextOverflow]',
    exportAs: 'appCheckTextOverflow',
})
export class CheckTextOverflowDirective implements OnInit {
    @Input('appCheckTextOverflow') selector?: string;

    private textElement: HTMLElement;

    constructor(private readonly elementRef: ElementRef<HTMLElement>) {}

    ngOnInit(): void {
        this.textElement = this.getTextElement();
    }

    hasTextOverflow(): boolean {
        const element = this.textElement;
        if (element) {
            return element.offsetWidth < element.scrollWidth;
        } else {
            return false;
        }
    }

    private getTextElement(): HTMLElement {
        if (this.selector) {
            return this.elementRef.nativeElement.querySelector(this.selector);
        } else {
            return this.elementRef.nativeElement;
        }
    }
}
