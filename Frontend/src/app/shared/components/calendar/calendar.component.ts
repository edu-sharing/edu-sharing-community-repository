import { Component, Input, Output, EventEmitter, OnInit, HostListener } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DateHelper } from '../../../core-ui-module/DateHelper';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';
import { DateAdapter } from '@angular/material/core';

@Component({
    selector: 'es-calendar',
    templateUrl: 'calendar.component.html',
    styleUrls: ['calendar.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CalendarComponent {
    showDatepicker = false;
    @Input() date: Date;
    @Input() label: string;
    @Input() isResettable = false;
    @Output() dateChange = new EventEmitter();
    @Input() minDate: Date;
    @Input() maxDate: Date;
    @Input() disabled = false;

    setDate(date: Date) {
        this.date = date;
        this.dateChange.emit(date);
        this.showDatepicker = false;
    }
    constructor(private translate: TranslateService, private _adapter: DateAdapter<any>) {
        this.translate.currentLang;
        this._adapter.setLocale(this.translate.currentLang);
    }
    getFormatted() {
        if (this.date) {
            return DateHelper.formatDate(this.translate, this.date.getTime(), {
                useRelativeLabels: false,
                showAlwaysTime: false,
            });
        }
        return null;
    }
}
