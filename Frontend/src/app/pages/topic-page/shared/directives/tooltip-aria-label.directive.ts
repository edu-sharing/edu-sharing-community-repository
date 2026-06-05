import {
    Directive,
    ElementRef,
    Input,
    OnChanges,
    OnInit,
    Renderer2,
    SimpleChanges,
    inject,
} from '@angular/core';

/**
 * Exposes the matTooltip message as aria-label on the host element
 * instead of the default aria-describedby. Useful for elements without
 * visible text (e.g. icon buttons).
 */
@Directive({
    selector: '[matTooltip][tooltipAriaLabel]',
    standalone: true,
})
export class TooltipAriaLabelDirective implements OnInit, OnChanges {
    private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly renderer = inject(Renderer2);

    @Input() matTooltip: string = '';

    ngOnInit(): void {
        this.applyAriaLabel(this.matTooltip);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['matTooltip']) {
            this.applyAriaLabel(changes['matTooltip'].currentValue);
        }
    }

    private applyAriaLabel(message: string): void {
        const host = this.elementRef.nativeElement;
        if (message) {
            this.renderer.setAttribute(host, 'aria-label', message);
        } else {
            this.renderer.removeAttribute(host, 'aria-label');
        }
    }
}
