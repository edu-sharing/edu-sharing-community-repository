import { Directive, ElementRef } from '@angular/core';

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
})
export class ElementRefDirective<T> extends ElementRef<T> {
    constructor(elementRef: ElementRef<T>) {
        super(elementRef.nativeElement);
    }
}
