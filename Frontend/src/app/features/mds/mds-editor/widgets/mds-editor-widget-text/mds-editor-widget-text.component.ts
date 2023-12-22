import { filter } from 'rxjs/operators';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl, ValidatorFn, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { DialogButton } from '../../../../../core-module/core.module';
import { DateHelper } from '../../../../../core-ui-module/DateHelper';
import { Toast } from '../../../../../core-ui-module/toast';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-text',
    templateUrl: './mds-editor-widget-text.component.html',
    styleUrls: ['./mds-editor-widget-text.component.scss'],
})
export class MdsEditorWidgetTextComponent extends MdsEditorWidgetBase implements OnInit {
    @ViewChild('inputElement') inputElement: ElementRef;
    @ViewChild('textAreaElement') textAreaElement: ElementRef;
    readonly valueType: ValueType = ValueType.String;
    formControl: FormControl;
    fileNameChecker: FileNameChecker;

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private toast: Toast,
    ) {
        super(mdsEditorInstance, translate);
    }

    async ngOnInit() {
        this.formControl = new FormControl(null, this.getValidators());
        let initialValue = (await this.widget.getInitalValuesAsync()).jointValues;
        if (this.widget.definition.type === 'date') {
            initialValue = initialValue.map((v) => DateHelper.formatDateByPattern(v, 'y-M-d'));
        }
        this.formControl = new FormControl(initialValue[0] ?? null, this.getValidators());
        this.formControl.valueChanges
            .pipe(
                filter((value) => value !== null && this.mdsEditorInstance.editorMode !== 'search'),
            )
            .subscribe((value) => {
                this.setValue([value]);
            });
        if (this.widget.definition.id === 'cm:name') {
            this.fileNameChecker = new FileNameChecker(
                this.formControl,
                this.widget,
                this.toast,
                this.translate,
            );
        }
        this.registerValueChanges(this.formControl);
    }

    focus(): void {
        this.inputElement?.nativeElement?.focus();
        this.textAreaElement?.nativeElement?.focus();
    }

    blur(): void {
        this.fileNameChecker?.check();
        this.onBlur.emit();
        this.submit();
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

    showBulkMixedValues() {
        return (
            this.widget.getInitialValues()?.individualValues &&
            this.mdsEditorInstance.editorBulkMode?.isBulk &&
            this.widget.getBulkMode() === 'no-change'
        );
    }

    submit() {
        if (this.mdsEditorInstance.editorMode === 'search') {
            this.setValue([this.formControl.value]);
        }
    }
}

class FileNameChecker {
    readonly initialValue: string;
    previousValue: string;

    constructor(
        private formControl: FormControl,
        widget: Widget,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.initialValue = widget.getInitialValues().jointValues[0];
        this.previousValue = this.initialValue;
    }

    check(): void {
        if (!this.initialValue) {
            return;
        }
        const currentValue = this.formControl.value;
        if (this.shouldWarn(this.previousValue, currentValue)) {
            this.warn(
                [...this.previousValue.split('.').slice(1)].join('.'),
                [...currentValue.split('.').slice(1)].join('.'),
                {
                    onAccept: () => this.onAccept(),
                    onRevert: () => this.onRevert(),
                    onCancel: () => this.onCancel(),
                },
            );
        } else {
            this.previousValue = currentValue;
            // In the `true` branch, `previousValue` will be updated by the chosen callback.
        }
    }

    private warn(
        extensionOld: string,
        extensionNew: string,
        callbacks: { onAccept: () => void; onRevert: () => void; onCancel: () => void },
    ): void {
        const message = (() => {
            if (!extensionOld) {
                return 'EXTENSION_NOT_MATCH_INFO_NEW';
            } else if (!extensionNew) {
                return 'EXTENSION_NOT_MATCH_INFO_OLD';
            } else {
                return 'EXTENSION_NOT_MATCH_INFO';
            }
        })();
        this.toast.showConfigurableDialog({
            title: 'EXTENSION_NOT_MATCH',
            message,
            messageParameters: {
                extensionOld,
                extensionNew,
                warning: this.translate.instant('EXTENSION_NOT_MATCH_WARNING'),
            },
            priority: 1000,
            isCancelable: true,
            onCancel: () => {
                callbacks.onCancel();
                this.toast.closeModalDialog();
            },
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () => {
                    callbacks.onCancel();
                    this.toast.closeModalDialog();
                }),
                new DialogButton('EXTENSION_KEEP', { color: 'standard' }, () => {
                    callbacks.onRevert();
                    this.toast.closeModalDialog();
                }),
                new DialogButton('EXTENSION_CHANGE', { color: 'primary' }, () => {
                    callbacks.onAccept();
                    this.toast.closeModalDialog();
                }),
            ],
        });
    }

    private shouldWarn(oldValue: string, newValue: string): boolean {
        if (!oldValue) {
            return false;
        }
        const oldComponents = oldValue.split('.');
        const newComponents = newValue.split('.');
        if (
            (oldComponents.length === 1 && newComponents.length !== 1) ||
            (oldComponents.length !== 1 && newComponents.length === 1)
        ) {
            return true;
        } else if (oldComponents.length === 1 && newComponents.length === 1) {
            return false;
        } else {
            // Whether the extension has changed
            return (
                oldComponents[oldComponents.length - 1]?.toLowerCase() !==
                newComponents[newComponents.length - 1]?.toLowerCase()
            );
        }
    }

    private onAccept(): void {
        this.previousValue = this.formControl.value;
    }

    private onRevert(): void {
        if (this.formControl.value) {
            const newValue = [
                this.formControl.value.split('.')[0],
                ...this.previousValue.split('.').slice(1),
            ].join('.');
            this.previousValue = newValue;
            this.formControl.setValue(newValue);
        } else {
            this.formControl.setValue(this.previousValue);
        }
    }

    private onCancel(): void {
        this.formControl.setValue(this.previousValue);
    }
}
