import {Directive, ElementRef, EventEmitter, HostListener, Input, Output} from '@angular/core';

@Directive({ selector: '[ng-touchevent]' })
export class ToucheventDirective {
  private element: ElementRef;
  @Output() ngSwipeLeft=new EventEmitter();
  @Output() ngSwipeRight=new EventEmitter();
  private touchStart: any;
  constructor(e: ElementRef) {
    this.element = e;
  }
  @HostListener('touchstart',['$event']) onTouchStart(event:any) {
    console.log(event);
    this.touchStart=event;
  }
  @HostListener('touchend',['$event']) onTouchEnd(event:any) {
    console.log(event);
    let horizontal=event.changedTouches[0].clientX-this.touchStart.changedTouches[0].clientX;
    let vertical=event.changedTouches[0].clientY-this.touchStart.changedTouches[0].clientY;
    let horizontalRelative=horizontal/window.innerWidth;
    if(Math.abs(horizontalRelative)<0.1 || Math.abs(horizontal)<50)
      return;
    // Vertical touches currently not supported
    if(Math.abs(horizontal)/Math.abs(vertical)<5)
      return;
    console.log(horizontalRelative);
    if(horizontal<0)
      this.ngSwipeLeft.emit(horizontalRelative);
    else
      this.ngSwipeRight.emit(horizontalRelative);
  }

}
