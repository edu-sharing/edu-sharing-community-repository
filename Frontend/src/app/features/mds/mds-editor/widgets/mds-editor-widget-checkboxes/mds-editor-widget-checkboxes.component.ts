import { Component, OnInit, ViewChild } from '@angular/core';
import { FormArray, FormControl, ValidatorFn } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { MdsWidgetType } from '../../../types/types';
import { DisplayValues } from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
@Component({
    selector: 'es-mds-editor-widget-checkboxes',
    templateUrl: './mds-editor-widget-checkboxes.component.html',
    styleUrls: ['./mds-editor-widget-checkboxes.component.scss'],
})
export class MdsEditorWidgetCheckboxesComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    @ViewChild(MdsEditorWidgetContainerComponent) containerRef: MdsEditorWidgetContainerComponent;
    values: DisplayValues;
    formArray: FormArray;
    mode: 'horizontal' | 'vertical';
    indeterminateValues: boolean[];

    ngOnInit(): void {
        this.mode = this.getMode();
        this.values = DisplayValues.fromMdsValues(this.widget.definition.values);
        const initialValue = this.widget.getInitialValues()?.jointValues;
        this.indeterminateValues = this.values.values.map(
            (value) => !!this.widget.getInitialValues()?.individualValues?.includes(value.key),
        );
        this.formArray = new FormArray(
            this.values.values.map((value) => new FormControl(initialValue.includes(value.key))),
            this.getStandardValidators({ requiredValidator }),
        );
        this.formArray.valueChanges
            .pipe(filter((value) => value !== null))
            .subscribe((checkboxStates: boolean[]) => {
                const newValues = this.values.values
                    .filter((_, index) => checkboxStates[index] === true)
                    .map((value) => value.key);
                this.setValue(newValues);
                setTimeout(() => {
                    // expand collapsed field if a value is active/selected
                    if (
                        this.containerRef?.expandedState$.value === 'collapsed' &&
                        newValues?.length
                    ) {
                        this.containerRef.expandedState$.next('expanded');
                    }
                });
            });
        this.widget.setValueExternal.subscribe((values) => {
            this.formArray.setValue(
                this.values.values.map((value, index) => !!values?.includes(value.key)),
            );
            this.setValue(values, true);
        });
    }

    onIndeterminateChange(isIndeterminate: boolean, index: number): void {
        this.indeterminateValues[index] = isIndeterminate;
        this.widget.setIndeterminateValues(
            this.values.values
                .filter((_, i) => this.indeterminateValues[i])
                .map((displayValue) => displayValue.key),
        );
    }

    private getMode(): 'horizontal' | 'vertical' {
        switch (this.widget.definition.type) {
            case MdsWidgetType.CheckboxHorizontal:
                return 'horizontal';
            case MdsWidgetType.CheckboxVertical:
                return 'vertical';
            default:
                throw new Error('Unexpected widget type: ' + this.widget.definition.type);
        }
    }
}

const requiredValidator: ValidatorFn = (control: FormArray): { [key: string]: any } | null => {
    const value: boolean[] = control.value;
    const valid = value.some((checked) => checked);
    return valid ? null : { required: true };
};
