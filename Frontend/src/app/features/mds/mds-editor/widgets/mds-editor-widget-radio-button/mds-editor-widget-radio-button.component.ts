import { Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, ValidatorFn } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { DisplayValue, DisplayValues } from '../DisplayValues';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { MdsWidgetType, ValueType } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, firstValueFrom, timer } from 'rxjs';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-mds-editor-widget-radio-button',
    templateUrl: './mds-editor-widget-radio-button.component.html',
    styleUrls: ['./mds-editor-widget-radio-button.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetRadioButtonComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    values: DisplayValues;
    formControl: UntypedFormControl;
    mode: 'horizontal' | 'vertical';
    aiSuggestion$ = new BehaviorSubject<SuggestionResponseDto>(null);

    async ngOnInit() {
        this.formControl = new UntypedFormControl(
            null,
            this.getStandardValidators({ requiredValidator }),
        );
        this.mode = this.getMode();
        this.values = DisplayValues.fromMdsValues(this.widget.definition.values);
        const initialValue = (await this.widget.getInitalValuesAsync()).jointValues;
        this.formControl = new UntypedFormControl(
            this.values.get(initialValue[0]),
            this.getStandardValidators({ requiredValidator }),
        );
        this.formControl.valueChanges
            .pipe(filter((value) => value !== null))
            .subscribe((value: DisplayValue) => {
                this.setValue([value.key]);
                if (this.aiSuggestion$.value && value?.key !== this.aiSuggestion$.value.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
                }
            });
        this.registerValueChanges(this.formControl);
        this.widget.getShowAiSuggestions().subscribe(async ([show, suggestions]) => {
            const suggestion = suggestions?.find((s) => s.type === 'AI' && s.status === 'PENDING');
            if (this.aiSuggestion$.value?.status !== 'DECLINED') {
                if (!this.formControl.value && suggestion && show) {
                    const value = this.values.values.find((v) => v.key === suggestion.value);
                    if (value) {
                        this.aiSuggestion$.next(suggestion);
                        this.widget.setSuggestionState(this.aiSuggestion$, 'ACCEPTED');
                        this.setValue([suggestion.value as string], false);
                        this.formControl.setValue(value, { emitEvent: false });
                    } else {
                        console.warn(
                            `Invalid suggestion value ${suggestion.value} for widget`,
                            this.widget.definition.id,
                            this.widget.definition.values,
                        );
                    }
                } else if (!initialValue?.length && !show && this.aiSuggestion$.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'PENDING');
                    this.setValue(null, false);
                    this.formControl.setValue(null, { emitEvent: false });
                }
            }
        });
    }

    private getMode(): 'horizontal' | 'vertical' {
        switch (this.widget.definition.type) {
            case MdsWidgetType.RadioHorizontal:
                return 'horizontal';
            case MdsWidgetType.RadioVertical:
                return 'vertical';
            default:
                throw new Error('Unexpected widget type: ' + this.widget.definition.type);
        }
    }
}

const requiredValidator: ValidatorFn = (
    control: AbstractControl,
): { [key: string]: any } | null => {
    const value: DisplayValue | null = control.value;
    const valid = value?.key.length > 0;
    return valid ? null : { required: true };
};
