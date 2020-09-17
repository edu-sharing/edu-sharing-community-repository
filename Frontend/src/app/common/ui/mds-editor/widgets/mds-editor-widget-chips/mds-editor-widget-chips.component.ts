import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { combineLatest, from, Observable } from 'rxjs';
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

interface DisplayValue {
    key: string;
    displayString: string;
}

@Component({
    selector: 'app-mds-editor-widget-chips',
    templateUrl: './mds-editor-widget-chips.component.html',
    styleUrls: ['./mds-editor-widget-chips.component.scss'],
})
export class MdsEditorWidgetChipsComponent extends MdsEditorWidgetBase implements OnInit {
    @ViewChild('input') input: ElementRef<HTMLInputElement>;
    @ViewChild('auto') matAutocomplete: MatAutocomplete;

    readonly valueType: ValueType = ValueType.MultiValue;
    readonly separatorKeysCodes: number[] = [ENTER, COMMA];
    inputControl = new FormControl();
    chipsControl: FormControl;
    filteredValues: Observable<DisplayValue[]>;

    ngOnInit(): void {
        // this.widget.definition.isRequired = RequiredMode.MandatoryForPublish;
        const initialValues = this.initWidget();
        this.chipsControl = new FormControl(
            initialValues.map((value) => this.toDisplayValues(value)),
            this.getStandardValidators(),
        );
        this.chipsControl.valueChanges
            .pipe(distinctUntilChanged())
            .subscribe((values: DisplayValue[]) => this.setValue(values.map((value) => value.key)));
        if (
            this.widget.definition.type === MdsWidgetType.MultiValueSuggestBadges ||
            this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges
        ) {
            this.filteredValues = this.subscribeForSuggestionUpdates();
        }
    }

    add(event: MatChipInputEvent): void {
        if (this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges) {
            return;
        }
        const input = event.input;
        const value = (event.value || '').trim();
        if (
            value &&
            !this.chipsControl.value.some((v: DisplayValue) => v.displayString === value)
        ) {
            this.chipsControl.setValue([...this.chipsControl.value, this.toDisplayValues(value)]);
        }
        if (input) {
            input.value = '';
        }
    }

    remove(toBeRemoved: DisplayValue): void {
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
    }

    selected(event: MatAutocompleteSelectedEvent): void {
        this.chipsControl.setValue([...this.chipsControl.value, event.option.value]);
        this.input.nativeElement.value = '';
        this.inputControl.setValue(null);
    }

    private subscribeForSuggestionUpdates(): Observable<DisplayValue[]> {
        // Combine observables to update suggestions when either the input field or currently
        // selected values change.
        return combineLatest([
            this.inputControl.valueChanges.pipe(
                startWith(null as string),
                filter(
                    (value: string | null | DisplayValue) =>
                        typeof value === 'string' || value === null,
                ),
                debounceTime(200),
                distinctUntilChanged(),
            ) as Observable<string | null>,
            this.chipsControl.valueChanges.pipe(
                startWith(this.chipsControl.value),
                distinctUntilChanged(),
            ),
        ]).pipe(
            switchMap(([filterString, selectedValues]) =>
                this.filter(filterString, selectedValues),
            ),
        );
    }

    private toDisplayValues(value: MdsWidgetValue | string): DisplayValue {
        if (typeof value === 'string') {
            const knownValue = this.widget.definition.values?.find((v) => v.id === value);
            if (knownValue) {
                value = knownValue;
            } else {
                return {
                    key: value,
                    displayString: value,
                };
            }
        }
        return {
            key: value.id,
            displayString: value.caption,
        };
    }

    private filter(
        filterString: string | null,
        selectedValues: DisplayValue[],
    ): Observable<DisplayValue[]> {
        return from(this.widget.getSuggestedValues(filterString)).pipe(
            map((suggestedValues) =>
                suggestedValues
                    .map((suggestedValue) => this.toDisplayValues(suggestedValue))
                    .filter((suggestedValue) =>
                        selectedValues.every(
                            (selectedValue) => suggestedValue.key !== selectedValue.key,
                        ),
                    ),
            ),
        );
    }
}
