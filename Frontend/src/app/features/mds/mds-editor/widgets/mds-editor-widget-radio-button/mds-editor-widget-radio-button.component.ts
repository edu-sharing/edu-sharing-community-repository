import { Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, ValidatorFn } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { MdsWidgetType } from '../../../types/types';
import { DisplayValue, DisplayValues } from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-radio-button',
    templateUrl: './mds-editor-widget-radio-button.component.html',
    styleUrls: ['./mds-editor-widget-radio-button.component.scss'],
})
export class MdsEditorWidgetRadioButtonComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    values: DisplayValues;
    formControl: UntypedFormControl;
    mode: 'horizontal' | 'vertical';

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
