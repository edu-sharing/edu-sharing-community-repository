import { Input } from '@angular/core';
import { ValidatorFn, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {MdsEditorInstanceService, MdsEditorWidgetCore, Widget} from '../mds-editor-instance.service';
import { assertUnreachable, InputStatus, EditorMode, RequiredMode } from '../types';

export enum ValueType {
    String,
    MultiValue,
    Range,
}

export abstract class MdsEditorWidgetBase extends MdsEditorWidgetCore {
    abstract readonly valueType: ValueType;

    /**
     * @deprecated use `widget.initialValues` directly
     */
    protected getInitialValue(): readonly string[] {
        if (!this.widget.getInitialValues().individualValues) {
            return this.widget.getInitialValues().jointValues;
        } else {
            switch (this.valueType) {
                case ValueType.String:
                    return [this.translate.instant('MDS.DIFFERENT_VALUES')];
                case ValueType.MultiValue:
                case ValueType.Range:
                    return [];
                default:
                    assertUnreachable(this.valueType);
            }
        }
    }

    /**
     * this method should set focus on the primary input of the widget
     */
    focus(): void {
        // default implementation will do nothing
    }

    protected setValue(value: string[]): void {
        this.widget.setValue(value);
    }

    protected setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
    }

    protected getStandardValidators(
        overrides: { requiredValidator?: ValidatorFn } = {},
    ): ValidatorFn[] {
        const validators: ValidatorFn[] = [];
        const widgetDefinition = this.widget.definition;
        if (
            widgetDefinition.isRequired === RequiredMode.Mandatory ||
            widgetDefinition.isRequired === RequiredMode.MandatoryForPublish
        ) {
            validators.push(overrides.requiredValidator ?? Validators.required);
        }
        return validators;
    }
}
