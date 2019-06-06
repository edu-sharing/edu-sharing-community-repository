import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {DateHelper} from "../../../core-ui-module/DateHelper";

@Component({
  selector: 'calendar',
  templateUrl: 'calendar.component.html',
  styleUrls: ['calendar.component.scss'],
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CalendarComponent{
    showDatepicker = false;
    @Input() date : Date;
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
      private translate : TranslateService
    ){}
    getFormatted() {
        if(this.date){
          return DateHelper.formatDate(this.translate,this.date.getTime(),{useRelativeLabels:false,showAlwaysTime:false});
        }
        return null;
    }
}
