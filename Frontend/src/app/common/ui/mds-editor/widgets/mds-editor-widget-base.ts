import { Input } from '@angular/core';
import { ValidatorFn, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MdsEditorInstanceService, Widget } from '../mds-editor-instance.service';
import { assertUnreachable, InputStatus, EditorMode, RequiredMode } from '../types';

export enum ValueType {
    String,
    MultiValue,
    Range,
}

export abstract class MdsEditorWidgetBase {
    @Input() widget: Widget;

    abstract readonly valueType: ValueType;
    readonly isBulk: boolean;
    readonly editorMode: EditorMode;

    constructor(
        private mdsEditorInstance: MdsEditorInstanceService,
        protected translate: TranslateService,
    ) {
        this.isBulk = this.mdsEditorInstance.isBulk;
        this.editorMode = this.mdsEditorInstance.editorMode;
    }

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
