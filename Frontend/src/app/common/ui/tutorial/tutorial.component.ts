import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
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
  private static activeTutorial:ElementRef=null;
  @ViewChild('tutoral') tutorial : ElementRef;
  private static PADDING_TOLERANCE=50;
  private background: SafeStyle;
  private show = false;
  private alignEnd:boolean;
  private interval: any;
  private padding:any={};
  @Input() rgbColor = [0,0,0];
  //@Input() rgbColor = [72,112,142];
  @Input() showSkip = true;
  @Input() heading = "TEST";
  @Input() description = "lorem ipsum dolor sit amet";
  @Input() set element(element : ElementRef){
    console.log(element);
    this.setElement(element);
  }
  @Output() onNext = new EventEmitter();
  @Output() onSkip = new EventEmitter();
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
            this.padding={};
            let spaceX=(window.innerWidth-x+size)*window.innerHeight;
            let spaceY=(window.innerHeight-y+size)*window.innerWidth;
            if(spaceX>spaceY) {
                let diffX=TutorialComponent.PADDING_TOLERANCE + x+size-pos.left;
                if (diffX > 0) {
                    this.padding.left = diffX + "px";
                }
            }
            else{
                let diffY=TutorialComponent.PADDING_TOLERANCE + y+size-pos.top;
                if (diffY > 0) {
                    this.padding.top = diffY + "px";
                }
            }
        });
    },1000/60);
  }
}
