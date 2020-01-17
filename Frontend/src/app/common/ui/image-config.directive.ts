/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ContentChildren, Directive, ElementRef, Input, Type, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../core-module/core.module";

@Directive({
    selector: '[image-config]',
})
export class ImageConfigDirective{
    private element: ElementRef;
    constructor(element:ElementRef,private config:ConfigurationService) {
        this.element=element;
        this.replace();
    }
    replace(){
        if(this.element.nativeElement && this.element.nativeElement.src){
            this.config.get("images",[]).subscribe((images:any)=>{
                for(let img of images){
                    // src contains the absolute url, so we need to verify via endsWith
                    if(this.element.nativeElement.src.endsWith(img.src)){
                        console.log("replace image "+img.src);
                        this.element.nativeElement.src=img.replace;
                    }
                }
            });
        }
        else{
            setTimeout(()=>this.replace(),16);
        }
    }
}
