import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DateHelper, UIAnimation } from 'ngx-edu-sharing-ui';
import { trigger } from '@angular/animations';
import { DateAdapter } from '@angular/material/core';

@Component({
    selector: 'es-calendar',
    templateUrl: 'calendar.component.html',
    styleUrls: ['calendar.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
    standalone: false,
})
/**
 * An edu-sharing sidebar dialog for adding data to a collection
 */
export class CalendarComponent {
    private translate = inject(TranslateService);
    private _adapter = inject<DateAdapter<any>>(DateAdapter);

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
    constructor() {
        this.translate.currentLang;
        this._adapter.setLocale(this.translate.currentLang.split('-')[0]);
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
