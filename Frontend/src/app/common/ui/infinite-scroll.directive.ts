import {
  AfterViewInit,
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges
} from '@angular/core';
@Directive({
  selector: '[infiniteScroll], [infinite-scroll], [data-infinite-scroll]'
})
export class InfiniteScrollDirective{
  @Output() scrolled = new EventEmitter<void>();

  @Input() infiniteScrollDistance: number = 1.5;
  @Input() infiniteScrollThrottle: number = 1000;
  @Input() scrollWindow: boolean = true;
  private lastEvent = 0;
  private lastScroll = 0;

  constructor(private element: ElementRef, private zone: NgZone) {
    setTimeout(()=> {
        if (this.scrollWindow) {
            window.addEventListener('scroll', (event: any) => {
                this.handleOnScroll(event);
            });
        } else {
            this.element.nativeElement.addEventListener('scroll', (event: any) => {
                this.handleOnScroll(event);
            });
        }
    });
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
        this.scrolled.emit();
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
          this.scrolled.emit();
      }
      this.lastScroll=scroll;
    }
    //console.log(window.pageYOffset);
  }
}
