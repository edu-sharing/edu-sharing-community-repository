import {ValidatorFn, Validators} from '@angular/forms';
import {assertUnreachable, InputStatus, MdsDefinition, MdsWidget, RequiredMode} from '../../types/types';
import {Directive, EventEmitter} from '@angular/core';
import {MdsEditorWidgetCore} from '../mds-editor-widget-core.directive';

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

    protected setValue(value: string[], dirty?: boolean): void {
        this.widget.setValue(value, dirty);
    }

    protected setStatus(value: InputStatus): void {
        this.widget.setStatus(value);
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

    public static mapGraphqlId(definition: MdsWidget) {
        // @TODO: make types!
        const id = (definition as any).ids?.graphql;
        if(id) {
            return [(definition as any).ids?.graphql];
        }
        return null;
    }
}
