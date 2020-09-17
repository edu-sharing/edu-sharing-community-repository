import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { combineLatest, from, Observable, Subject } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    startWith,
    switchMap,
} from 'rxjs/operators';
import { MdsWidgetType, MdsWidgetValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import {MatSelectChange} from '@angular/material/select';

interface DisplayValue {
    key: string;
    displayString: string;
}

@Component({
    selector: 'app-mds-editor-widget-select',
    templateUrl: './mds-editor-widget-select.component.html',
    styleUrls: ['./mds-editor-widget-select.component.scss'],
})
export class MdsEditorWidgetSelectComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.String;

    values: Promise<MdsWidgetValue[]>;
    formControl = new FormControl();
    currentValue: MdsWidgetValue;

    async ngOnInit() {
        const initialValues = this.initWidget();
        this.values = this.widget.getSuggestedValues();
        this.currentValue = (await this.values).find((v) => v.id === initialValues[0]);
        this.getIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    selected(event: MatSelectChange): void {
        this.currentValue = event.value;
        this.setValue([this.currentValue.id]);
    }
}
