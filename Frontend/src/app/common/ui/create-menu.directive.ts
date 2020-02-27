/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ContentChildren, Directive, ElementRef, Input, Type, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../core-module/core.module";
import {MainNavComponent} from './main-nav/main-nav.component';

@Directive({
    selector: '[create-menu]',
})
export class CreateMenuDirective {
    @Input() mainNav: MainNavComponent;
    constructor(private element:ElementRef,private config:ConfigurationService) {
        console.log('directive',this.mainNav)
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
