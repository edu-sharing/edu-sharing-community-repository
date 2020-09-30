import { animate, group, keyframes, state, style, transition, trigger } from '@angular/animations';
import {
    AfterContentInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    Directive,
    ElementRef,
    HostBinding,
    Injectable,
    Input,
    OnInit,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { MatFormField, MatFormFieldControl } from '@angular/material/form-field';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, startWith } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { BulkMode, InputStatus, RequiredMode } from '../../types';
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
    animations: [
        trigger('showHideExtended', [
            state(
                'hidden',
                style({
                    height: '0px',
                    margin: '0px',
                    opacity: '0',
                    visibility: 'hidden',
                }),
            ),
            transition('hidden => shown', [
                style({ overflow: 'hidden' }),
                group([
                    animate('.2s'), // animate hight and margin to original size
                    animate('.2s', style({ overflow: 'hidden' })),
                    animate(
                        '2s',
                        keyframes([
                            style({ backgroundColor: '#c90' }),
                            style({ backgroundColor: '*' }),
                        ]),
                    ),
                ]),
            ]),
            transition('shown => hidden', [
                style({ overflow: 'hidden' }),
                animate(
                    '.2s',
                    style({
                        height: '0px',
                        margin: '0px',
                        opacity: '0',
                        overflow: 'hidden',
                    }),
                ),
            ]),
        ]),
    ],
})
export class MdsEditorWidgetContainerComponent implements OnInit, AfterContentInit {
    readonly RequiredMode = RequiredMode;
    readonly ValueType = ValueType;

    @Input() widget: Widget;
    @Input() valueType: ValueType;
    @Input() label: string | boolean;
    @Input() control: AbstractControl;
    @Input() wrapInFormField: boolean;

    @ContentChild(MatFormFieldControl) formFieldControl: MatFormFieldControl<any>;

    @HostBinding('class.disabled') isDisabled = false;
    @HostBinding('@showHideExtended') get showHideExtendedState(): string {
        return this.isHidden ? 'hidden' : 'shown';
    }

    readonly isBulk: boolean;
    readonly labelId: string;
    readonly descriptionId: string;
    bulkMode: BehaviorSubject<BulkMode>;
    missingRequired: RequiredMode | null;
    isHidden: boolean;

    constructor(
        private elementRef: ElementRef,
        private mdsEditorInstance: MdsEditorInstanceService,
        private cdr: ChangeDetectorRef,
        private formFieldRegistration: FormFieldRegistrationService,
    ) {
        this.isBulk = this.mdsEditorInstance.isBulk;
        const id = Math.random().toString(36).substr(2);
        this.labelId = id + '_label';
        this.descriptionId = id + '_description';
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
            this.bulkMode = new BehaviorSubject('no-change');
            this.bulkMode.subscribe((bulkMode) => this.widget.setBulkMode(bulkMode));
        }
        if (this.control) {
            this.initFormControl(this.control);
        }
        this.wrapInFormField = this.wrapInFormField ?? !!this.control;
        this.mdsEditorInstance.shouldShowExtendedWidgets$.subscribe(
            (shouldShowExtendedWidgets) =>
                (this.isHidden =
                    !!this.widget?.definition.isExtended && !shouldShowExtendedWidgets),
        );
    }

    onBulkEditToggleChange(event: MatSlideToggleChange): void {
        this.bulkMode.next(event.checked ? 'replace' : 'no-change');
    }

    shouldShowError(): boolean {
        return !!this.control?.invalid && (this.control.touched || this.control.dirty);
    }

    private initFormControl(formControl: AbstractControl): void {
        this.widget.observeIsDisabled().subscribe((isDisabled) => this.setDisabled(isDisabled));
        formControl.statusChanges
            .pipe(startWith(formControl.status), distinctUntilChanged())
            .subscribe((status: InputStatus) => {
                this.handleStatus(status);
            });
        this.widget.onShowMissingRequired((shouldScrollIntoView) => {
            formControl.markAllAsTouched();
            if (formControl.errors?.required && shouldScrollIntoView) {
                this.elementRef.nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                });
                return true;
            } else {
                return false;
            }
        });
    }

    private setDisabled(isDisabled: boolean): void {
        this.isDisabled = isDisabled;
        if (isDisabled) {
            this.control.disable();
        } else {
            this.control.enable();
        }
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
