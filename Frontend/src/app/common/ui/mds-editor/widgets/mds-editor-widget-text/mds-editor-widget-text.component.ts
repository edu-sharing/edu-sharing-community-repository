import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import { FormControl, ValidatorFn, Validators } from '@angular/forms';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-text',
    templateUrl: './mds-editor-widget-text.component.html',
    styleUrls: ['./mds-editor-widget-text.component.scss'],
})
export class MdsEditorWidgetTextComponent extends MdsEditorWidgetBase implements OnInit {
    @ViewChild('inputElement') inputElement: ElementRef;
    @ViewChild('textAreaElement') textAreaElement: ElementRef;
    readonly valueType: ValueType = ValueType.String;
    formControl: FormControl;

    ngOnInit(): void {
        const initialValue = this.getInitialValue();
        this.formControl = new FormControl(initialValue[0] ?? null, this.getValidators());
        this.formControl.valueChanges
            .filter((value) => value !== null)
            .subscribe((value) => {
                this.setValue([value]);
            });
    }
    focus(): void {
        this.inputElement?.nativeElement?.focus();
        this.textAreaElement?.nativeElement?.focus();
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
