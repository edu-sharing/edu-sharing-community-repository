import { Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Options } from '@angular-slider/ngx-slider';
import { MdsWidget, MdsWidgetValue } from '../../../types/types';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { MdsWidgetType, ValueType } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, firstValueFrom, timer } from 'rxjs';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';

@Component({
    templateUrl: './mds-editor-widget-slider.component.html',
    styleUrls: ['./mds-editor-widget-slider.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetSliderComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    aiSuggestion$ = new BehaviorSubject<SuggestionResponseDto>(null);
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
    formControl = new UntypedFormControl();
    currentValue: number[] = [];

    async ngOnInit() {
        this.sliderOptions.floor = this.widget.definition.min;
        this.sliderOptions.ceil = this.widget.definition.max;
        this.sliderOptions.step = this.widget.definition.step ?? 1;
        this.isRange = this.widget.definition.type === MdsWidgetType.Range;
        this.currentValue = await this.getInitialValue_();
        // Since computation of initial values is a bit different for sliders and ranges, we handle
        // processing of default values here in this component. To reflect default values, we save
        // values once when initializing. This might mark the whole dialog as dirty without the user
        // interacting even if default values have not been provided. However, since the slider
        // always implies a state, to save that state seems to be the natural behavior.
        this.updateValues();
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
        this.widget.getShowAiSuggestions().subscribe(async ([show, suggestions]) => {
            const suggestion = suggestions?.find((s) => s.type === 'AI' && s.status === 'PENDING');
            if (this.aiSuggestion$.value?.status !== 'DECLINED') {
                if (suggestion && show) {
                    this.currentValue = (suggestion.value as string)
                        .split('-')
                        .map((s) => parseInt(s, 10));
                    // delay so 'slider' can trigger its own event first
                    await firstValueFrom(timer(1));
                    this.aiSuggestion$.next(suggestion);
                    this.widget.setSuggestionState(this.aiSuggestion$, 'ACCEPTED');
                } else if (!show && this.aiSuggestion$.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'PENDING');
                    this.currentValue = await this.getInitialValue_();
                }
            }
        });
    }

    updateValues() {
        if (this.currentValue == null) {
            return;
        }
        if (this.widget.definition.type === 'slider') {
            // emit single value
            this.setValue([this.currentValue[0].toString()], false);
        } else {
            this.setValue(
                [this.currentValue[0].toString(), this.currentValue[1].toString()],
                false,
            );
        }
    }

    // TODO: remove trailing underscore when `getInitialValue` is removed from
    // `MdsEditorWidgetBase`.
    private async getInitialValue_() {
        const initialValues = (await this.widget.getInitalValuesAsync())?.jointValues;
        if (this.isRange && initialValues) {
            if (initialValues.length === 2) {
                return [parseInt(initialValues[0], 10), parseInt(initialValues[1], 10)];
            } else {
                return [
                    this.widget.definition.defaultMin ?? this.widget.definition.min,
                    this.widget.definition.defaultMax ?? this.widget.definition.max,
                ];
            }
        } else {
            if (initialValues?.length === 1) {
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

    updateValue(value: number) {
        if (isNaN(value)) {
            return;
        }
        if (this.widget.definition.type === 'slider') {
            // emit single value
            this.setSliderValue([value]);
        } else {
            this.setSliderValue([value, this.currentValue?.[1]]);
        }
        if (this.aiSuggestion$.value?.status === 'ACCEPTED') {
            this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
        }
    }
    setSliderValue(value: number[]) {
        this.currentValue = value;
        this.setValue(
            value.map((v) => v?.toString()),
            true,
        );
    }

    updateHighValue(value: number) {
        if (isNaN(value)) {
            return;
        }
        this.setValue([this.currentValue?.[0]?.toString(), value.toString()], true);
    }
}
@Component({
    templateUrl: './mds-editor-widget-slider.component.html',
    styleUrls: ['./mds-editor-widget-slider.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetSliderRangeComponent extends MdsEditorWidgetSliderComponent {
    public static mapGraphqlId(definition: MdsWidget) {
        // attach the "IntRangeNominal" graphql Attributes
        return MdsEditorWidgetBase.attachGraphqlSelection(definition, ['from', 'to']);
    }
}
