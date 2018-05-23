import {Component, ElementRef, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {DomSanitizer, SafeStyle} from "@angular/platform-browser";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../ui-animation";

@Component({
  selector: 'tutorial',
  templateUrl: 'tutorial.component.html',
  styleUrls: ['tutorial.component.scss'],
  animations: [
      trigger('fade', UIAnimation.fade()),
  ]
})
export class TutorialComponent implements OnInit {
  private background: SafeStyle;
  private show = false;
  private alignEnd:boolean;
  private interval: any;
  @Input() heading = "TEST";
  @Input() description = "lorem ipsum dolor sit amet";
  @Input() set element(element : ElementRef){
    this.interval=setInterval(()=>{
      if(this.wasTutorialShown(this.heading)) {
          clearInterval(this.interval);
          return;
      }
      if(!element.nativeElement)
        return;
      this.show=true;
      let rect=element.nativeElement.getBoundingClientRect();
      console.log(rect);
      let size=Math.max(rect.width,rect.height)/2;
      let x=rect.left + rect.width/2;
      let y=rect.top + rect.height/2;
      this.alignEnd=y<window.innerHeight/2;
      this.background=this.sanitizer.bypassSecurityTrustStyle(
        "radial-gradient(circle at "+x+"px "+y+"px, transparent 0," +
        "transparent "+size+"px," +
        "rgba(0,0,0,0.3) "+(size+40)+"px," +
        "rgb(0,0,0) 80%)");
    },1000);
  }
  @Output() onNext = new EventEmitter();
  @Output() onSkip = new EventEmitter();
  constructor(private sanitizer:DomSanitizer) { }

  ngOnInit() {
  }
  wasTutorialShown(heading:string){
    let item=localStorage.getItem('TUTORIAL.'+heading);
    return item;
  }
  setTutorialShown(heading:string){
    localStorage.setItem('TUTORIAL.'+heading,"true");
  }
  skip(){
    this.setTutorialShown(this.heading);
    clearInterval(this.interval);
    this.show=false;
    this.onSkip.emit();
  }
  next(){
    this.setTutorialShown(this.heading);
    clearInterval(this.interval);
    this.show=false;
    this.onNext.emit();
  }

}
