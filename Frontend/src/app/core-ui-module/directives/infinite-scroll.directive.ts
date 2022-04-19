import {
  AfterViewInit,
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges
} from '@angular/core';
import { Subject } from 'rxjs';

@Directive({
  selector: '[esInfiniteScroll], [infinite-scroll], [data-infinite-scroll]'
})
export class InfiniteScrollDirective implements OnInit, OnDestroy {
  @Output() scrolled = new EventEmitter<void>();

  @Input() infiniteScrollDistance: number = 1.5;
  @Input() infiniteScrollThrottle: number = 1000;
  @Input() scrollWindow: boolean = true;
  private lastEvent = 0;
  private lastScroll = 0;
  private destroyed$ = new Subject<void>();

  constructor(private element: ElementRef, private zone: NgZone) {}

  ngOnInit(): void {
    this.zone.runOutsideAngular(() => {
      const handleScroll = (event: any) => this.handleOnScroll(event);
      const eventTarget = this.scrollWindow ? window : this.element.nativeElement;
      eventTarget.addEventListener('scroll', handleScroll);
      this.destroyed$.subscribe(() => eventTarget.removeEventListener('scroll', handleScroll));
    });
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }

  handleOnScroll(event:any) {
    if(!this.element.nativeElement)
      return;
    if(this.scrollWindow){
      let body = document.body,
          html = document.documentElement;
      let height = Math.max( body.scrollHeight, body.offsetHeight,
          html.clientHeight, html.scrollHeight, html.offsetHeight );
        let scroll= window.pageYOffset;

        if(scroll>this.lastScroll && height - scroll < window.innerHeight*this.infiniteScrollDistance){
        let time=new Date().getTime();
        if(time - this.lastEvent < this.infiniteScrollThrottle)
          return;
        this.lastEvent=time;
        this.emitScrolled();
      }
      this.lastScroll=scroll;
    }
    else{
      let element=this.element.nativeElement;
      let height=element.scrollHeight;
      let scroll=element.scrollTop;
      if(scroll>this.lastScroll && height - scroll < element.getBoundingClientRect().height*this.infiniteScrollDistance){
          let time=new Date().getTime();
          if(time - this.lastEvent < this.infiniteScrollThrottle)
              return;
          this.lastEvent=time;
          this.emitScrolled();
        }
      this.lastScroll=scroll;
    }
    //console.log(window.pageYOffset);
  }

  private emitScrolled(): void {
    this.zone.run(() => this.scrolled.emit());
  }
}
