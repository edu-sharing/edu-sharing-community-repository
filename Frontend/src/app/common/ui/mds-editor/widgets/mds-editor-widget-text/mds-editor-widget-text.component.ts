import { Component, OnInit } from '@angular/core';
import { FormControl, ValidatorFn, Validators } from '@angular/forms';
import { RequiredMode } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-text',
    templateUrl: './mds-editor-widget-text.component.html',
    styleUrls: ['./mds-editor-widget-text.component.scss'],
})
export class MdsEditorWidgetTextComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    formControl: FormControl;

    ngOnInit(): void {
        const initialValue = this.initWidget();
        this.formControl = new FormControl(initialValue[0], this.getValidators());
        this.formControl.valueChanges.subscribe((value) => {
            this.setValue([value]);
        });
        this.formControl.statusChanges.subscribe((status: 'VALID' | 'INVALID' | 'DISABLED') => {
            this.setStatus(status);
        });
        this.getIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.formControl.disable();
            } else {
                this.formControl.enable();
            }
        });
    }

    private getValidators(): ValidatorFn[] {
        const validators: ValidatorFn[] = [];
        const widgetDefinition = this.widget.definition;
        if (widgetDefinition.isRequired === RequiredMode.Mandatory) {
            validators.push(Validators.required);
        }
        if (widgetDefinition.type === 'email') {
            validators.push(Validators.email);
        } else if (widgetDefinition.type === 'number') {
            if (widgetDefinition.min) {
                validators.push(Validators.min(widgetDefinition.min));
            }
            if (widgetDefinition.max) {
                validators.push(Validators.max(widgetDefinition.max));
            }
        }
        if (widgetDefinition.maxlength) {
            validators.push(Validators.maxLength(widgetDefinition.maxlength));
        }
        return validators;
    }
}
