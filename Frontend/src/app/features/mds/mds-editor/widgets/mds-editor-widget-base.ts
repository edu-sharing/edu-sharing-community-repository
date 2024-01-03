import { FormControl, ValidatorFn, Validators } from '@angular/forms';
import { MdsEditorWidgetCore, UnauthoritzedException } from '../mds-editor-instance.service';
import { InputStatus, RequiredMode } from '../../types/types';
import { Directive, EventEmitter } from '@angular/core';

export enum ValueType {
    String,
    MultiValue,
    Range,
}

@Directive()
export abstract class MdsEditorWidgetBase extends MdsEditorWidgetCore {
    abstract readonly valueType: ValueType;

    /**
     * triggered when the input focus is lost
     */
    onBlur = new EventEmitter<void>();

    /**
     * this method should set focus on the primary input of the widget
     */
    focus(): void {
        // default implementation will do nothing
    }

    protected setValue(value: string[], dirty?: boolean): void {
        this.widget.setValue(value, dirty);
    }

    protected setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
    }

    /**
     * register the form control that should be updated when external value changes received
     */
    protected registerValueChanges(formControl: FormControl) {
        this.widget.setValueExternal.subscribe((value) => {
            formControl.setValue(value);
            this.setValue(value, true);
        });
    }

    protected getStandardValidators(
        overrides: { requiredValidator?: ValidatorFn } = {},
    ): ValidatorFn[] {
        const validators: ValidatorFn[] = [];
        const widgetDefinition = this.widget.definition;
        // Marking both `Mandatory` and `MandatoryForPublish` fields as required is needed for the
        // hint texts to be shown when revealing unfilled fields or leaving the field without
        // entering a value.
        if (
            this.mdsEditorInstance.editorMode !== 'search' &&
            (widgetDefinition.isRequired === RequiredMode.Mandatory ||
                widgetDefinition.isRequired === RequiredMode.MandatoryForPublish)
        ) {
            validators.push(overrides.requiredValidator ?? Validators.required);
        }
        return validators;
    }
}
