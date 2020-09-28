import { Component, OnInit } from '@angular/core';
import { FormControl, ValidatorFn, Validators } from '@angular/forms';
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
        const initialValue = this.getInitialValue();
        this.formControl = new FormControl(initialValue[0], this.getValidators());
        this.formControl.valueChanges.subscribe((value) => {
            this.setValue([value]);
        });
    }

    private getValidators(): ValidatorFn[] {
        const validators: ValidatorFn[] = [...this.getStandardValidators()];
        const widgetDefinition = this.widget.definition;
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
