import { AfterViewInit, Directive, ElementRef, EventEmitter, Output, inject } from '@angular/core';

/**
 * Provide event emitters for Angular lifecycle events.
 */
@Directive({
    selector: '[esLifecycle]',
    standalone: true,
})
export class LifecycleDirective<T> implements AfterViewInit {
    private elementRef = inject<ElementRef<T>>(ElementRef);

    @Output() afterViewInit: EventEmitter<ElementRef<T>> = new EventEmitter();

    ngAfterViewInit(): void {
        this.afterViewInit.emit(this.elementRef);
    }
}
