import { Component, effect, EventEmitter, Input, input, InputSignal, Output } from '@angular/core';
import { FormArray, FormControl, FormGroup, UntypedFormControl } from '@angular/forms';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { MdsExtendedValue } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../../shared/shared.module';
import { Widget } from '../../../mds-editor-instance.service';
import { DisplayValue } from '../../DisplayValues';

@Component({
    selector: 'es-mds-editor-widget-tree-value-selection',
    templateUrl: './mds-editor-widget-tree-value-selection.component.html',
    styleUrls: ['./mds-editor-widget-tree-value-selection.component.scss'],
    imports: [SharedModule],
})
export class MdsEditorWidgetTreeValueSelectionComponent {
    @Input() control: UntypedFormControl;
    selectedValues: InputSignal<DisplayValue[]> = input<DisplayValue[]>([]);
    widget: InputSignal<Widget> = input<Widget>(null);
    @Output() removed: EventEmitter<DisplayValue> = new EventEmitter<DisplayValue>();

    form: FormGroup = new FormGroup({
        selections: new FormArray<FormControl<boolean>>([]),
    });

    get selectionsArray(): FormArray {
        return this.form.get('selections') as FormArray;
    }

    constructor() {
        effect(() => {
            if (this.selectedValues() && this.widget()) {
                this.updateExtendedValuesAndForm();
            }
        });
    }

    /**
     * Track the loop by its current index.
     *
     * @param index
     */
    trackByIndex(index: number) {
        return index;
    }

    /**
     * Handles the change of a checkbox by updating the extended value of the selected value.
     *
     * @param changeEvent
     * @param index
     */
    onCheckboxChange(changeEvent: MatCheckboxChange, index: number): void {
        const value: DisplayValue = this.selectedValues()[index];
        if (!value) {
            return;
        }
        this.widget().patchExtendedValue(value.key, { enabled: changeEvent.checked });
    }

    /**
     * Handles the removal of a value by emitting the removed value.
     *
     * @param index
     */
    onRemove(index: number): void {
        const value: DisplayValue = this.selectedValues()[index];
        if (!value) {
            return;
        }
        this.removed.emit(value);
    }

    /**
     * Helper function to retrieve the extended values of the selected values
     * and update the form array accordingly.
     */
    private updateExtendedValuesAndForm(): void {
        const extendedValues: MdsExtendedValue = this.widget().getExtendedValue();
        // make sure to clear the form array before pushing new values on the current extended values
        const selections = this.form.get('selections') as FormArray;
        selections.clear();
        this.selectedValues().forEach((value) => {
            selections.push(new FormControl<boolean>(extendedValues[value.key]?.enabled ?? false));
        });
    }
}
