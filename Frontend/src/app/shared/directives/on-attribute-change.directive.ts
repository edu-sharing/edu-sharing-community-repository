import {
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject,
} from '@angular/core';

@Directive({
    selector: '[esOnAttributeChange]',
    standalone: false,
})
export class OnAttributeChangeDirective implements OnInit, OnDestroy {
    private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

    @Input('esOnAttributeChange') attribute: string;

    @Output() attributeChange = new EventEmitter<string>();

    private changes: MutationObserver;

    ngOnInit(): void {
        this.changes = new MutationObserver((mutations: MutationRecord[]) => {
            mutations
                .filter((mutation) => mutation.attributeName === this.attribute)
                .forEach((mutation: MutationRecord) =>
                    this.attributeChange.emit(
                        (mutation.target as HTMLElement).getAttribute(this.attribute),
                    ),
                );
        });
        this.changes.observe(this.elementRef.nativeElement, {
            attributes: true,
        });
    }

    ngOnDestroy(): void {
        this.changes.disconnect();
    }
}
