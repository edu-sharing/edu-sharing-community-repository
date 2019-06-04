/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ContentChildren, Directive, ElementRef, Input, Type, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";

@Directive({
  selector: '[icon]',
})
export class IconComponent{
    private element: ElementRef;
    private _id: string;
    private _aria: boolean;
    @Input() set aria(aria:boolean){
        this._aria=aria;
        this.updateAria();
    }
    @Input() set iconId(id: string){
        this._id=id;
        let css:string;
        this.updateAria();
        if(id.startsWith("edu-")){
            css="edu-icons";
            id=id.substr(4);
        }
        else{
            css="material-icons";
        }
        this.element.nativeElement.classList.add(css);
        this.element.nativeElement.innerText=id;
    };
    @Input() set id(id:string){

    }
    constructor(element:ElementRef,private translate:TranslateService) {
        // console.log(element);
        this.element=element;
    }

    private updateAria() {
        this.element.nativeElement.removeAttribute('aria-label');
        this.element.nativeElement.removeAttribute('aria-hidden');
        if(this._aria){
            this.translate.get('ICON_LABELS.'+this._id).subscribe((lang)=> {
                this.element.nativeElement.setAttribute('aria-label',lang);
            });
        }
        else{
            this.element.nativeElement.setAttribute('aria-hidden',true);
        }
    }
}
