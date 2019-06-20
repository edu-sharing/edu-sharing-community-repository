import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../../../core-ui-module/DateHelper";
import {trigger} from "@angular/animations";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {DateAdapter} from "@angular/material";
import {Translation} from "../../../core-ui-module/translation";

@Component({
  selector: 'calendar',
  templateUrl: 'calendar.component.html',
  styleUrls: ['calendar.component.scss'],
    animations: [
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
    ]
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CalendarComponent{
    showDatepicker = false;
    @Input() date : Date;
    @Input() label : string;
    @Input() isResettable = false;
    @Output() dateChange = new EventEmitter();
    @Input() minDate : Date;
    @Input() maxDate : Date;

    setDate(date:Date){
      console.log(date);
      this.date=date;
      this.dateChange.emit(date);
      this.showDatepicker=false;
    }
    constructor(
      private translate : TranslateService,
      private _adapter: DateAdapter<any>
    ){
        this._adapter.setLocale(Translation.getLanguage());
    }
    getFormatted() {
        if(this.date){
          return DateHelper.formatDate(this.translate,this.date.getTime(),{useRelativeLabels:false,showAlwaysTime:false});
        }
        return null;
    }
}
