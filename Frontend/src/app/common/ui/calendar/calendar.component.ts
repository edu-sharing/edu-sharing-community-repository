import {Component, Input, Output, EventEmitter, OnInit, HostListener} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {RestIamService} from "../../rest/services/rest-iam.service";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestConstants} from "../../rest/rest-constants";
import {RestCollectionService} from "../../rest/services/rest-collection.service";
import {Toast} from "../toast";
import {ListItem} from "../list-item";
import {AddElement} from "../list-table/list-table.component";
import {Router} from "@angular/router";
import {UIConstants} from "../ui-constants";
import {DateHelper} from "../DateHelper";
import {DateAdapter} from "@angular/material";
import {Translation} from "../../translation";

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
