import { Component, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatTooltip } from '@angular/material/tooltip';
import { MdsWidgetValue } from '../../../types/types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { MatSelect } from '@angular/material/select';

@Component({
    selector: 'es-mds-editor-widget-select',
    templateUrl: './mds-editor-widget-select.component.html',
    styleUrls: ['./mds-editor-widget-select.component.scss'],
})
export class MdsEditorWidgetSelectComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;
    @ViewChild(MatSelect) matSelect: MatSelect;

    values: Promise<MdsWidgetValue[]>;
    formControl: FormControl;

    readonly showTooltip = (() => {
        let previousTooltip: MatTooltip;
        return (tooltip?: MatTooltip) => {
            previousTooltip?.hide();
            tooltip?.show();
            previousTooltip = tooltip;
        };
    })();

    focus() {
        this.matSelect.open();
    }

    async ngOnInit() {
        this.formControl = new FormControl(null, this.getStandardValidators());
        const initialValue = (await this.widget.getInitalValuesAsync()).jointValues[0];
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
        this.formControl.valueChanges.subscribe((value) => {
            this.setValue(value ? [value.id] : [null]);
        });
    }

    onActiveDescendantChanges(elementId: string) {
        const element = document.getElementById(elementId);
        this.showTooltip((element as any)?.tooltip);
    }
}
