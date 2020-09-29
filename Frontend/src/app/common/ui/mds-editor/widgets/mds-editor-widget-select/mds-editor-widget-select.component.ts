import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { MdsWidgetValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-select',
    templateUrl: './mds-editor-widget-select.component.html',
    styleUrls: ['./mds-editor-widget-select.component.scss'],
})
export class MdsEditorWidgetSelectComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;

    values: Promise<MdsWidgetValue[]>;
    formControl: FormControl;

    ngOnInit() {
        this.formControl = new FormControl(null, this.getStandardValidators());
        const initialValue = this.widget.initialValues.jointValues[0];
        this.values = this.widget.getSuggestedValues();
        if (initialValue) {
            this.values.then((values) => {
                const value = values.find((v) => v.id === initialValue);
                if (value) {
                    this.formControl.setValue(value);
                } else {
                    throw new Error(
                        `Invalid node value "${initialValue}" for ${this.widget.definition.id}`,
                    );
                }
            });
        }
        this.formControl.valueChanges.pipe(filter((value) => value !== null)).subscribe((value) => {
            this.setValue([value.id]);
        });
    }
}
