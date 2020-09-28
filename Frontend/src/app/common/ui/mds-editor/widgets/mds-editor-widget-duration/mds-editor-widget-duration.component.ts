import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MdsWidgetType, MdsWidgetValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { Options } from 'ng5-slider/options';
import {TranslateService} from '@ngx-translate/core';

@Component({
    selector: 'app-mds-editor-widget-duration',
    templateUrl: './mds-editor-widget-duration.component.html',
    styleUrls: ['./mds-editor-widget-duration.component.scss'],
})
export class MdsEditorWidgetDurationComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;

    sliderOptions: Options = {
        floor: 0,
        ceil: 0,
        animate: true,
        step: 1,
        draggableRange: true,
        minRange: 1,
        translate: (value: number): string => this.format(value),
    };

    isRange: boolean;
    values: Promise<MdsWidgetValue[]>;
    formControl = new FormControl();
    currentValue: number;
    hours: string;
    minutes: string;

    ngOnInit() {
        const initialValues = this.initWidget();
        this.sliderOptions.floor = this.widget.definition.min;
        this.sliderOptions.ceil = this.widget.definition.max ?? 599;
        this.currentValue = parseInt(initialValues[0] ?? '0', 10);
        this.updateInput();
        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    update(src: 'slider' | 'input') {
        if(src === 'slider') {
            this.updateInput();
        } else {
            this.currentValue = parseInt(this.hours, 10)*60 +  parseInt(this.minutes, 10);
        }
        this.setValue([this.currentValue.toString()]);
    }

    updateInput() {
        this.hours = Math.floor(this.currentValue / 60) + '';
        this.minutes = this.currentValue % 60 + '';
    }

    private format(value: number): string {
        return value + ' ' + this.translate.instant('INPUT_MINUTES');
    }
}
