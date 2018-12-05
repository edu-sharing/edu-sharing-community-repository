/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ContentChildren, Directive, ElementRef, Input, Type, ViewChild} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";

@Directive({
  selector: '[icon]',
})
export class IconComponent{
    private element: ElementRef;
    @Input() set iconId(id: string){
        let css:string;
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
    constructor(element:ElementRef) {
        console.log(element);
        this.element=element;
    }
}
