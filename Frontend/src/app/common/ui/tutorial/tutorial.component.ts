import {Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, ViewChild} from '@angular/core';
import {DomSanitizer, SafeStyle} from "@angular/platform-browser";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../ui-animation";
import {KeyEvents} from '../key-events';

@Component({
  selector: 'tutorial',
  templateUrl: 'tutorial.component.html',
  styleUrls: ['tutorial.component.scss'],
  animations: [
      trigger('fade', UIAnimation.fade()),
  ]
})
export class TutorialComponent implements OnInit {
  private static activeTutorial:ElementRef=null;
  private static PADDING_TOLERANCE=50;
  @ViewChild('tutoral') tutorial : ElementRef;
  background: SafeStyle;
  show = false;
  private interval: any;
  pos:any={};
  @Input() rgbColor = [0,0,0];
  //@Input() rgbColor = [72,112,142];
  @Input() showSkip = true;
  @Input() heading : string;
  @Input() description : string
  @Input() set element(element : ElementRef){
    console.log(element);
    this.setElement(element);
  }
  @Output() onNext = new EventEmitter();
  @Output() onSkip = new EventEmitter();

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key == "Escape" && this.show) {
            this.next();
            event.preventDefault();
            event.stopPropagation();
        }
    }

  constructor(private sanitizer:DomSanitizer) { }

  ngOnInit() {
  }
  wasTutorialShown(heading:string){
    let item=localStorage.getItem('TUTORIAL.'+heading);
    return item;
    //return false;
  }
  setTutorialShown(heading:string){
    localStorage.setItem('TUTORIAL.'+heading,"true");
  }
  finish(){
    this.setTutorialShown(this.heading);
    clearInterval(this.interval);
    this.show=false;
    TutorialComponent.activeTutorial=null;
  }
  skip(){
    this.finish();
    this.onSkip.emit();
  }
  next(){
    this.finish();
    this.onNext.emit();
  }

  setElement(element: ElementRef) {
    if(TutorialComponent.activeTutorial && element){
        setTimeout(()=>this.setElement(element),500);
        return;
    }
    TutorialComponent.activeTutorial=element;
    this.interval=setInterval(()=>{
        if(this.wasTutorialShown(this.heading)) {
            this.finish();
            return;
        }
        if(!element || !element.nativeElement)
            return;
        this.show=true;
        let rect=element.nativeElement.getBoundingClientRect();
        let size=Math.min(Math.max(window.innerWidth,window.innerHeight)/4,Math.max(rect.width,rect.height))/2;
        let x=rect.left + rect.width/2;
        let y=rect.top + rect.height/2;
        //this.alignEnd=y<window.innerHeight/2;
        this.background=this.sanitizer.bypassSecurityTrustStyle(
            "radial-gradient(circle at "+x+"px "+y+"px, transparent 0," +
            "transparent "+size+"px," +
            "rgba("+this.rgbColor[0]+","+this.rgbColor[1]+","+this.rgbColor[2]+",0.3) "+(size+TutorialComponent.PADDING_TOLERANCE/2)+"px," +
            "rgba("+this.rgbColor[0]+","+this.rgbColor[1]+","+this.rgbColor[2]+",0.9) 70%)");
        setTimeout(()=>{
            if(!this.tutorial || !this.tutorial.nativeElement)
                return;
            let pos=this.tutorial.nativeElement.getBoundingClientRect();
            this.pos={};
            let space:number[]=[]
            space.push((window.innerWidth-x-size)*window.innerHeight);
            space.push((window.innerHeight-y-size)*window.innerWidth);
            space.push((x-size)*window.innerHeight);
            space.push((y-size)*window.innerWidth);

            let maxIndex=space.indexOf(Math.max(...space));
            console.log(space);
            // we prefer a centered region if we are on a big screen
            if(space[1]*2>space[0] && y<window.innerHeight/3)
                maxIndex=1;
            let diffX=0,diffY=0;
            if(maxIndex==0) {
                diffX=x+size;
            }
            if(maxIndex==1){
                diffY=y+size;
            }
            if(maxIndex==2) {
                diffX=1;
            }
            if(maxIndex==3) {
                diffY=1;
            }
            if (diffX > 0) {
                this.pos.left = Math.min(window.innerWidth-pos.width,diffX) + "px";
                this.pos.top = Math.max(0,(window.innerHeight/2-pos.height/2)) + "px";
                this.pos.width = (window.innerWidth-y-size) + "px";
            }
            if (diffY > 0) {
                this.pos.top = Math.min(window.innerHeight-pos.height,diffY) + "px";
                this.pos.left = Math.max(0,(window.innerWidth/2-pos.width/2)) + "px";
            }
        });
    },1000/30);
  }
}
