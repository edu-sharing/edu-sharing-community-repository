import { AfterViewInit, Directive, ElementRef, EventEmitter, Output } from '@angular/core';

/**
 * Provide event emitters for Angular lifecycle events.
 */
@Directive({
    selector: '[wloLifecycle]',
    standalone: true,
})
export class LifecycleDirective<T> implements AfterViewInit {
    @Output() afterViewInit: EventEmitter<ElementRef<T>> = new EventEmitter();

    constructor(private elementRef: ElementRef<T>) {}

    ngAfterViewInit(): void {
        this.afterViewInit.emit(this.elementRef);
    }
}
