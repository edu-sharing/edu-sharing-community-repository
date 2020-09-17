import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { BehaviorSubject, combineLatest, from, Observable, Subject } from 'rxjs';
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
    values: DisplayValue[];
    formControl = new FormControl();
    filteredValues: Observable<DisplayValue[]>;

    private values$: Subject<DisplayValue[]>;

    ngOnInit(): void {
        const initialValues = this.initWidget();
        this.values = initialValues.map((value) => this.toDisplayValues(value));
        this.values$ = new BehaviorSubject(this.values);
        if (
            this.widget.definition.type === MdsWidgetType.MultiValueSuggestBadges ||
            this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges
        ) {
            this.filteredValues = this.subscribeForSuggestionUpdates();
        }
        this.getIsDisabled().subscribe((isDisabled) => {
            if (isDisabled) {
                this.setStatus('DISABLED');
            } else {
                this.setStatus('VALID');
            }
        });
    }

    add(event: MatChipInputEvent): void {
        if (this.widget.definition.type === MdsWidgetType.MultiValueFixedBadges) {
            return;
        }
        const input = event.input;
        const value = (event.value || '').trim();
        if (value && !this.values.some((v) => v.displayString === value)) {
            this.values.push(this.toDisplayValues(value));
        }
        if (input) {
            input.value = '';
        }
        this.updateValues();
    }

    remove(value: DisplayValue): void {
        const index = this.values.indexOf(value);
        if (index >= 0) {
            this.values.splice(index, 1);
        }
        this.updateValues();
    }

    selected(event: MatAutocompleteSelectedEvent): void {
        this.values.push(event.option.value);
        this.updateValues();
        this.input.nativeElement.value = '';
        this.formControl.setValue(null);
    }

    private subscribeForSuggestionUpdates(): Observable<DisplayValue[]> {
        // Combine observables to update suggestions when either the input field or currently
        // selected values change.
        return combineLatest([
            this.formControl.valueChanges.pipe(
                startWith(null as string),
                filter(
                    (value: string | null | DisplayValue) =>
                        typeof value === 'string' || value === null,
                ),
                debounceTime(200),
                distinctUntilChanged(),
            ) as Observable<string | null>,
            this.values$,
        ]).pipe(
            switchMap(([filterString, selectedValues]) =>
                this.filter(filterString, selectedValues),
            ),
        );
    }

    private updateValues(): void {
        this.values$.next(this.values);
        this.setValue(this.values.map((value) => value.key));
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
