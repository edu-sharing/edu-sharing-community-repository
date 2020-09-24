import {
    AfterContentInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    Directive,
    ElementRef,
    Injectable,
    Input,
    OnInit,
    ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatFormField, MatFormFieldControl } from '@angular/material/form-field';
import { MatRadioChange } from '@angular/material/radio';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { distinctUntilChanged, map, startWith } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { assertUnreachable, BulkMode, InputStatus, RequiredMode } from '../../types';
import { ValueType } from '../mds-editor-widget-base';

// This is a Service-Directive combination to get hold of the `MatFormField` before it initializes
// its `FormFieldControl`.
//
// This is needed so we can pass inputs (representing a `FormFieldControl`) through `<ng-content>`
// into a `MatFormField` (see https://github.com/angular/components/issues/9411).
//
// Initialization of the `FormFieldControl` happens on `ngAfterContentInit` in `MatFormField`, so we
// will be too late if we were to do this on `ngAfterViewInit`. Therefore, we hook into
// `MatFormField` with a Directive and register our `FormFieldControl` in the constructor, which is
// early enough to be picked up.
@Injectable()
export class FormFieldRegistrationService {
    private callback: (formField: MatFormField) => void;

    register(formField: MatFormField): void {
        this.callback(formField);
    }

    onRegister(callback: (formField: MatFormField) => void): void {
        this.callback = callback;
    }
}

@Directive({
    selector: '[appRegisterFormField]',
})
export class RegisterFormFieldDirective {
    constructor(formField: MatFormField, formFieldRegistration: FormFieldRegistrationService) {
        formFieldRegistration.register(formField);
    }
}

@Component({
    selector: 'app-mds-editor-widget-container',
    templateUrl: './mds-editor-widget-container.component.html',
    styleUrls: ['./mds-editor-widget-container.component.scss'],
    providers: [FormFieldRegistrationService],
})
export class MdsEditorWidgetContainerComponent implements OnInit, AfterContentInit {
    readonly RequiredMode = RequiredMode;
    readonly ValueType = ValueType;

    @Input() widget: Widget;
    @Input() valueType: ValueType;
    @Input() label: string | boolean;
    @Input() control: FormControl; // Naming this variable `formControl` causes bugs.
    @Input() wrapInFormField: boolean;

    @ViewChild(MatFormField, { read: ElementRef }) formFieldRef: ElementRef<Element>;
    @ContentChild(MatFormFieldControl) formFieldControl: MatFormFieldControl<any>;

    readonly isBulk: boolean;
    readonly labelId: string;
    bulkMode: BehaviorSubject<BulkMode>;
    missingRequired: RequiredMode | null;

    constructor(
        private mdsEditorInstance: MdsEditorInstanceService,
        private cdr: ChangeDetectorRef,
        private formFieldRegistration: FormFieldRegistrationService,
    ) {
        this.isBulk = this.mdsEditorInstance.isBulk;
        this.labelId = Math.random().toString(36).substr(2);
    }

    ngAfterContentInit() {
        this.formFieldRegistration.onRegister((formField) => {
            formField._control = this.formFieldControl;
        });
        this.cdr.detectChanges();
    }

    ngOnInit(): void {
        if (this.label === true) {
            this.label = this.widget.definition.caption;
        }
        if (this.widget && this.isBulk) {
            this.bulkMode = new BehaviorSubject(this.getInitialBulkMode());
            this.bulkMode.subscribe((bulkMode) => this.widget.setBulkMode(bulkMode));
        }
        if (this.control) {
            this.initFormControl(this.control);
        }
        this.wrapInFormField = this.wrapInFormField ?? !!this.control;
    }

    onBulkModeReplaceToggleChange(event: MatSlideToggleChange): void {
        this.bulkMode.next(event.checked ? 'replace' : 'no-change');
    }

    onBulkModeMultiValueChange(event: MatRadioChange): void {
        this.bulkMode.next(event.value);
    }

    private getInitialBulkMode(): BulkMode {
        switch (this.valueType) {
            case ValueType.String:
            case ValueType.Range:
                return 'no-change';
            case ValueType.MultiValue:
                if (this.widget.hasCommonInitialValue) {
                    return 'replace';
                } else {
                    return 'append';
                }
            default:
                assertUnreachable(this.valueType);
        }
    }

    private initFormControl(formControl: FormControl): void {
        this.widget.observeIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.control.disable();
            } else {
                this.control.enable();
            }
        });
        formControl.statusChanges
            .pipe(startWith(formControl.status), distinctUntilChanged())
            .subscribe((status: InputStatus) => {
                this.handleStatus(status);
            });
        this.widget.onShowMissingRequired((shouldScrollIntoView) => {
            formControl.markAllAsTouched();
            if (formControl.errors?.required && shouldScrollIntoView) {
                this.formFieldRef.nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                });
                return true;
            } else {
                return false;
            }
        });
    }

    private handleStatus(status: InputStatus): void {
        if (this.control.errors?.required) {
            if (
                this.widget.definition.isRequired === RequiredMode.MandatoryForPublish &&
                Object.keys(this.control.errors).length === 1
            ) {
                status = 'VALID'; // downgrade to warning
            }
            this.missingRequired = this.widget.definition.isRequired;
        } else {
            this.missingRequired = null;
        }
        this.widget.setStatus(status);
    }
}
