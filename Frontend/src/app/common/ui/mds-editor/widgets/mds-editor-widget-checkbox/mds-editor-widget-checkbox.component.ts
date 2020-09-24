import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormControl, ValidatorFn } from '@angular/forms';
import { first } from 'rxjs/operators';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-checkbox',
    templateUrl: './mds-editor-widget-checkbox.component.html',
    styleUrls: ['./mds-editor-widget-checkbox.component.scss'],
})
export class MdsEditorWidgetCheckboxComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    formControl: FormControl;
    indeterminate: boolean;

    ngOnInit(): void {
        const initialValue = this.initWidget();
        this.indeterminate =
            typeof initialValue[0] === 'string' &&
            initialValue[0] !== 'false' &&
            initialValue[0] !== 'true';
        this.formControl = new FormControl(
            initialValue[0] === 'true',
            this.getStandardValidators({ requiredValidator }),
        );
        // TODO: show visual hint for unchecked required checkbox.
        this.formControl.valueChanges.subscribe((value: boolean) => {
            if (!this.indeterminate) {
                this.setValue([value.toString()]);
            }
        });
        // Set a non-indeterminate value when the user selects 'replace' in bulk mode, so we don't
        // overwrite the property with an empty array on save.
        if (this.indeterminate) {
            this.widget
                .observeIsDisabled()
                .pipe(first((isDisabled) => !isDisabled))
                .subscribe(() => (this.indeterminate = false));
        }
    }

    onIndeterminateChange(indeterminate: boolean): void {
        if (indeterminate === false) {
            this.setValue([this.formControl.value.toString()]);
        }
    }
}

const requiredValidator: ValidatorFn = (
    control: AbstractControl,
): { [key: string]: any } | null => {
    const valid = control.value === true;
    return valid ? null : { required: true };
};
