import { Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { MdsWidgetValue } from '../../../types/types';
import { DisplayValue } from '../DisplayValues';
import { MdsEditorWidgetChipsSuggestionBase } from '../mds-editor-widget-base';
import { ValueType } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-mds-editor-widget-multivalue-buttons',
    templateUrl: './mds-editor-widget-multivalue-buttons.component.html',
    styleUrls: ['./mds-editor-widget-multivalue-buttons.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetMultivalueButtonsComponent
    extends MdsEditorWidgetChipsSuggestionBase
    implements OnInit
{
    readonly valueType: ValueType = ValueType.MultiValue;
    inputControl = new UntypedFormControl();
    chipsControl: UntypedFormControl;
    indeterminateValues$: BehaviorSubject<string[]>;
    hasFocus = true;

    async ngOnInit() {
        this.chipsControl = new UntypedFormControl(
            [
                ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
                ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
            ].map((value) => this.toDisplayValue(value)),
            this.getStandardValidators(),
        );

        this.widget
            .getInitialDisplayValues()
            .pipe(filter((v) => !!v))
            .subscribe(async (value) => {
                this.chipsControl.setValue(await this.getInitialValues());
            });

        super.initSuggestions();

        this.chipsControl.valueChanges
            .pipe(distinctUntilChanged())
            .subscribe((values: DisplayValue[]) => this.setValue(values.map((value) => value.key)));

        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );

        this.widget.addValue.subscribe((value: MdsWidgetValue) => {
            this.add(this.toDisplayValue(value));
        });

        this.registerValueChanges(this.chipsControl);
    }

    private async getInitialValues() {
        return [
            ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
            ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
        ].map((value) => this.toDisplayValue(value));
    }

    remove(toBeRemoved: DisplayValue): void {
        const values: DisplayValue[] = this.chipsControl.value;
        console.log(values, toBeRemoved, values.includes(toBeRemoved));
        this.chipsControl.setValue(values.filter((value) => value.key !== toBeRemoved.key));
    }

    add(value: DisplayValue): void {
        if (!this.chipsControl.value.some((v: DisplayValue) => v.key === value.key)) {
            this.chipsControl.setValue([...this.chipsControl.value, value]);
        }
    }

    toDisplayValue(value: MdsWidgetValue | string): DisplayValue {
        if (typeof value === 'string') {
            const knownValue = this.widget.definition.values?.find((v) => v.id === value);
            if (!knownValue && this.widget.getInitialDisplayValues().value) {
                const ds = this.widget
                    .getInitialDisplayValues()
                    .value.values?.find((v) => v.key === value)?.displayString;
                return {
                    key: value,
                    label: ds || value,
                };
            }
            if (knownValue) {
                value = knownValue;
            } else {
                return {
                    key: value,
                    label: value,
                };
            }
        }
        return {
            key: value.id,
            label: value.caption,
        };
    }

    trackByValue(index: number, value: any): string {
        return value.id;
    }

    isValueSelected(value: MdsWidgetValue): boolean {
        const displayValue = this.toDisplayValue(value);
        return this.chipsControl.value.some(
            (selectedValue: DisplayValue) => selectedValue.key === displayValue.key,
        );
    }

    toggleValue(value: MdsWidgetValue): void {
        const displayValue = this.toDisplayValue(value);
        const isSelected = this.isValueSelected(value);

        if (isSelected) {
            this.remove(displayValue);
        } else {
            this.add(displayValue);
        }
    }
}
