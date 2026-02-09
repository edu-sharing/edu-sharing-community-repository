import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl, ValidatorFn, Validators } from '@angular/forms';
import { MAT_FORM_FIELD } from '@angular/material/form-field';
import { TranslateService } from '@ngx-translate/core';
import { SuggestionResponseDto } from 'ngx-edu-sharing-api';
import { DateHelper, UIService, ValueType } from 'ngx-edu-sharing-ui';
import { filter } from 'rxjs/operators';
import { Toast } from '../../../../../services/toast';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase } from '../mds-editor-widget-base';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-mds-editor-widget-text',
    templateUrl: './mds-editor-widget-text.component.html',
    styleUrls: ['./mds-editor-widget-text.component.scss'],
    providers: [
        // Tell the input that it is inside a form field so it will apply relevant classes.
        { provide: MAT_FORM_FIELD, useValue: true },
    ],
    animations: [],
    standalone: false,
})
export class MdsEditorWidgetTextComponent extends MdsEditorWidgetBase implements OnInit {
    @ViewChild('inputElement') inputElement: ElementRef;
    @ViewChild('textAreaElement') textAreaElement: ElementRef;
    readonly valueType: ValueType = ValueType.String;
    formControl: UntypedFormControl;
    fileNameChecker: FileNameChecker;
    aiSuggestion$ = new BehaviorSubject<SuggestionResponseDto>(null);
    constructor(
        toast: Toast,
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private uiService: UIService,
    ) {
        super(toast, mdsEditorInstance, translate);
    }
    async ngOnInit() {
        this.formControl = new UntypedFormControl(null, this.getValidators());
        let initialValue = (await this.widget.getInitalValuesAsync()).jointValues;
        if (this.widget.definition.type === 'date') {
            initialValue = initialValue.map((v) => DateHelper.formatDateByPattern(v, 'y-M-d'));
        }
        this.formControl = new UntypedFormControl(initialValue[0] ?? null, this.getValidators());
        this.formControl.valueChanges
            .pipe(
                filter((value) => value !== null && this.mdsEditorInstance.editorMode !== 'search'),
            )
            .subscribe((value) => {
                if (this.aiSuggestion$.value) {
                    this.widget.setSuggestionState(this.aiSuggestion$, 'DECLINED');
                }
                this.setValue([value]);
            });
        this.widget.observeBulkMode().subscribe(() => {
            if (this.showBulkMixedValues()) {
                this.formControl.disable();
            } else {
                this.formControl.enable();
            }
        });
        if (this.widget.definition.id === 'cm:name') {
            this.fileNameChecker = new FileNameChecker(
                this.formControl,
                this.widget,
                this.toast,
                this.translate,
            );
        }
        this.widget.getShowAiSuggestions().subscribe(([show, suggestions]) => {
            const suggestion = suggestions?.find((s) => s.type === 'AI' && s.status === 'PENDING');
            if (this.aiSuggestion$.value?.status !== 'DECLINED') {
                if (!this.formControl.value?.trim() && suggestion && show) {
                    this.aiSuggestion$.next(suggestion);
                    this.applySuggestion(this.aiSuggestion$);
                } else if (!initialValue[0] && !show && this.aiSuggestion$.value) {
                    this.clearSuggestion(this.aiSuggestion$);
                }
            }
        });
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

    clearSuggestion(suggestion: BehaviorSubject<SuggestionResponseDto>) {
        this.formControl.setValue('', { emitEvent: false });
        this.setValue(['']);
        this.widget.setSuggestionState(suggestion, 'PENDING');
    }
    applySuggestion(suggestion: BehaviorSubject<SuggestionResponseDto>) {
        this.formControl.setValue(suggestion.value.value as string, { emitEvent: false });
        this.setValue([suggestion.value.value as string]);
        this.widget.setSuggestionState(suggestion, 'ACCEPTED');
    }

    fieldGotFocus(element: HTMLInputElement | HTMLTextAreaElement) {
        if (
            this.aiSuggestion$.value?.status === 'ACCEPTED' &&
            !this.uiService.isTouchSubject.value
        ) {
            element?.select();
        }
    }
}

class FileNameChecker {
    readonly initialValue: string;
    previousValue: string;

    constructor(
        private formControl: UntypedFormControl,
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
            void this.warn(
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

    private async warn(
        extensionOld: string,
        extensionNew: string,
        callbacks: { onAccept: () => void; onRevert: () => void; onCancel: () => void },
    ): Promise<void> {
        const dialogRef = await this.toast.openGenericDialog({
            title: 'EXTENSION_NOT_MATCH',
            message: (() => {
                if (!extensionOld) {
                    return 'EXTENSION_NOT_MATCH_INFO_NEW';
                } else if (!extensionNew) {
                    return 'EXTENSION_NOT_MATCH_INFO_OLD';
                } else {
                    return 'EXTENSION_NOT_MATCH_INFO';
                }
            })(),
            messageParameters: {
                extensionOld,
                extensionNew,
                warning: this.translate.instant('EXTENSION_NOT_MATCH_WARNING'),
            },
            buttons: [
                { label: 'CANCEL', config: { color: 'standard' } },
                { label: 'EXTENSION_KEEP', config: { color: 'standard' } },
                { label: 'EXTENSION_CHANGE', config: { color: 'primary' } },
            ],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'EXTENSION_KEEP') {
                callbacks.onRevert();
            } else if (response === 'EXTENSION_CHANGE') {
                callbacks.onAccept();
            } else {
                callbacks.onCancel();
            }
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
