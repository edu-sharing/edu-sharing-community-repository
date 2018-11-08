/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ElementRef, Input, ViewChild} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";

@Component({
  selector: 'icon',
  templateUrl: 'icon.component.html',
})
export class IconComponent implements AfterViewInit{
    @ViewChild('icon') icon: ElementRef;
    class = 'material-icons';
    constructor() {
    }
    ngAfterViewInit() {
        const id=this.icon.nativeElement.textContent;
        if(id.startsWith("edu-")){
            this.class="edu-icons";
            this.icon.nativeElement.innerText=id.substr(4);
        }
    }
}
