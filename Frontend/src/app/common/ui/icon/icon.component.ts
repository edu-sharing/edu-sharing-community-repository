/**
 * Created by Torsten on 13.01.2017.
 */

import {AfterViewInit, Component, ContentChildren, ElementRef, Input, Type, ViewChild} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";

@Component({
  selector: 'icon',
  templateUrl: 'icon.component.html',
})
export class IconComponent{
    _class = '';
    _id= '';
    @Input() set id(id:string){
        if(id.startsWith("edu-")){
            this._class="edu-icons";
            id=id.substr(4);
        }
        else{
            this._class="material-icons";
        }
        this._id=id;
    }
    constructor() {
    }
}
