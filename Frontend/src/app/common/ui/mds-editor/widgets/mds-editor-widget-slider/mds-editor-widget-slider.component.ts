import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MdsWidgetType, MdsWidgetValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { Options } from 'ng5-slider/options';

@Component({
    selector: 'app-mds-editor-widget-slider',
    templateUrl: './mds-editor-widget-slider.component.html',
    styleUrls: ['./mds-editor-widget-slider.component.scss'],
})
export class MdsEditorWidgetSliderComponent extends MdsEditorWidgetBase implements OnInit {
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
    currentValue: number[] = [];

    ngOnInit() {
        const initialValues = this.getInitialValue();
        this.sliderOptions.floor = this.widget.definition.min;
        this.sliderOptions.ceil = this.widget.definition.max;
        this.sliderOptions.step = this.widget.definition.step ?? 1;
        this.currentValue[0] = parseInt(initialValues[0] ?? '0', 10);
        this.currentValue[1] = parseInt(initialValues[1] ?? '0', 10);
        this.isRange = this.widget.definition.type === MdsWidgetType.Range;

        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    updateValues() {
        if (this.widget.definition.type === 'slider') {
            // emit single value
            this.setValue([this.currentValue[0].toString()]);
        } else {
            this.setValue([this.currentValue[0].toString(), this.currentValue[1].toString()]);
        }
    }

    private format(value: number): string {
        return (value + ' ' + (this.widget.definition.unit ?? '')).trim();
    }
}
