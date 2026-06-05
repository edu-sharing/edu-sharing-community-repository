import { Directive, ElementRef, inject } from '@angular/core';

// From https://stackoverflow.com/a/61065054
/**
 * Export the ElementRef of the selected element for use with template references.
 *
 * @example
 * <button mat-button #button="esElementRef" esElementRef></button>
 */
@Directive({
    selector: '[esElementRef]',
    exportAs: 'esElementRef',
    standalone: false,
})
export class ElementRefDirective<T> extends ElementRef<T> {
    constructor() {
        const elementRef = inject<ElementRef<T>>(ElementRef);

        super(elementRef.nativeElement);
    }
}
