import { Directive, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({ selector: '[esTouchEvent]' })
export class TouchEventDirective {
    @Output() ngSwipeLeft = new EventEmitter();
    @Output() ngSwipeRight = new EventEmitter();

    private touchStart: any;

    @HostListener('touchstart', ['$event']) onTouchStart(event: any) {
        this.touchStart = event;
    }

    @HostListener('touchend', ['$event']) onTouchEnd(event: any) {
        let horizontal =
            event.changedTouches[0].clientX - this.touchStart.changedTouches[0].clientX;
        let vertical = event.changedTouches[0].clientY - this.touchStart.changedTouches[0].clientY;
        let horizontalRelative = horizontal / window.innerWidth;
        if (Math.abs(horizontalRelative) < 0.1 || Math.abs(horizontal) < 50) return;
        // Vertical touches currently not supported
        if (Math.abs(horizontal) / Math.abs(vertical) < 5) return;
        if (horizontal < 0) this.ngSwipeLeft.emit(horizontalRelative);
        else this.ngSwipeRight.emit(horizontalRelative);
    }
}
