import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Options } from '@angular-slider/ngx-slider';
import { MdsWidgetType, MdsWidgetValue } from '../../../types/types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-slider',
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
        this.sliderOptions.floor = this.widget.definition.min;
        this.sliderOptions.ceil = this.widget.definition.max;
        this.sliderOptions.step = this.widget.definition.step ?? 1;
        this.isRange = this.widget.definition.type === MdsWidgetType.Range;
        this.currentValue = this.getInitialValue_();
        // Since computation of initial values is a bit different for sliders and ranges, we handle
        // processing of default values here in this component. To reflect default values, we save
        // values once when initializing. This might mark the whole dialog as dirty without the user
        // interacting even if default values have not been provided. However, since the slider
        // always implies a state, to save that state seems to be the natural behavior.
        this.updateValues(false);
        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            this.sliderOptions = {
                ...this.sliderOptions,
                disabled: isDisabled,
            };
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    updateValues(dirty: boolean = true) {
        if (this.widget.definition.type === 'slider') {
            // emit single value
            this.setValue([this.currentValue[0].toString()], dirty);
        } else {
            this.setValue(
                [this.currentValue[0].toString(), this.currentValue[1].toString()],
                dirty,
            );
        }
    }

    // TODO: remove trailing underscore when `getInitialValue` is removed from
    // `MdsEditorWidgetBase`.
    private getInitialValue_(): number[] {
        const initialValues = this.widget.getInitialValues().jointValues;
        if (this.isRange) {
            if (initialValues.length === 2) {
                return [parseInt(initialValues[0], 10), parseInt(initialValues[1], 10)];
            } else {
                return [
                    this.widget.definition.defaultMin ?? this.widget.definition.min,
                    this.widget.definition.defaultMax ?? this.widget.definition.max,
                ];
            }
        } else {
            if (initialValues.length === 1) {
                return [parseInt(initialValues[0], 10)];
            } else {
                return [
                    this.widget.definition.defaultvalue
                        ? parseInt(this.widget.definition.defaultvalue, 10)
                        : this.widget.definition.min,
                ];
            }
        }
    }

    private format(value: number): string {
        return (value + ' ' + (this.widget.definition.unit ?? '')).trim();
    }
}
