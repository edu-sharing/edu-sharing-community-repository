import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import {
    MatAutocomplete,
    MatAutocompleteSelectedEvent,
    MatAutocompleteTrigger,
} from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, combineLatest, from, Observable } from 'rxjs';
import {
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    startWith,
    switchMap,
} from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsWidgetType, MdsWidgetValue } from '../../types';
import {DisplayValue, DisplayValues} from '../DisplayValues';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import {AuthoritySearchInputComponent} from '../../../authority-search-input/authority-search-input.component';
import {Authority, AuthorityProfile, Group} from '../../../../../core-module/rest/data-object';
import {AuthorityNamePipe} from '../../../../../shared/pipes/authority-name.pipe';
import {AuthorityAffiliationPipe} from '../../../../../core-ui-module/pipes/authority-affiliation.pipe';
import { waitForAsync } from '@angular/core/testing';
import {RestConnectorService} from '../../../../../core-module/rest/services/rest-connector.service';
import {RestConstants} from '../../../../../core-module/rest/rest-constants';

@Component({
    selector: 'es-mds-editor-widget-authority',
    templateUrl: './mds-editor-widget-authority.component.html',
    styleUrls: ['./mds-editor-widget-authority.component.scss'],
})
export class MdsEditorWidgetAuthorityComponent extends MdsEditorWidgetBase implements OnInit {
    @ViewChild('authoritySearchInputComponent') authoritySearchInputComponent: AuthoritySearchInputComponent;
    @ViewChild(MatAutocompleteTrigger, { read: MatAutocompleteTrigger })
    trigger: MatAutocompleteTrigger;
    @ViewChild('auto') matAutocomplete: MatAutocomplete;

    readonly valueType: ValueType = ValueType.MultiValue;
    readonly separatorKeysCodes: number[] = [ENTER, COMMA];
    inputControl = new FormControl();
    chipsControl: FormControl;
    autocompleteValues: Observable<DisplayValue[]>;
    indeterminateValues$: BehaviorSubject<string[]>;
    showDropdownArrow: boolean;

    private autocompleteIsInhibited = new BehaviorSubject(false);

    readonly showTooltip = (() => {
        let previousTooltip: MatTooltip;
        return (tooltip?: MatTooltip) => {
            previousTooltip?.hide();
            tooltip?.show();
            previousTooltip = tooltip;
        };
    })();
    globalSearchAllowed: boolean;

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private conntector: RestConnectorService,
        private changeDetectorRef: ChangeDetectorRef,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnInit(): void {
        this.chipsControl = new FormControl(
            [
                ...this.widget.getInitialValues().jointValues,
                ...(this.widget.getInitialValues().individualValues ?? []),
            ].filter((value) => !!value).map((value: string) => ({
                    key: value,
                    label: value,
                }) as DisplayValue),
            this.getStandardValidators()
        );
        this.indeterminateValues$ = new BehaviorSubject(
            this.widget.getInitialValues().individualValues,
        );
        this.conntector.hasToolPermission(RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH).subscribe((tp) =>
            this.globalSearchAllowed = tp
        )
        this.chipsControl.valueChanges
            .pipe(distinctUntilChanged())
            .subscribe((values: DisplayValue[]) => this.setValue(values.map((value) => value.key)));

        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );
        this.widget.addValue.subscribe((value: MdsWidgetValue) =>
            this.chipsControl.setValue([...this.chipsControl.value, value])
        );
    }

    toggleAutoCompletePanel(): void {
        // use set timeout because otherwise multiple panels stay open cause of stopPropagation
        // see https://stackoverflow.com/questions/50491195/open-matautocomplete-with-open-openpanel-method
        if (this.trigger.panelOpen) {
            setTimeout(() => this.trigger.closePanel());
        } else {
            // this.input.nativeElement.focus();
            setTimeout(() => this.trigger.openPanel());
        }
    }

    remove(toBeRemoved: DisplayValue): void {
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
        this.inhibitAutocomplete();
    }

    focus() {
        this.authoritySearchInputComponent?.inputElement?.nativeElement?.focus();
    }

    add(value: Authority|string): void {
        if(value instanceof String) {
            console.warn('Authority widget does currently not support state handling');
        } else {
            const displayValue: DisplayValue = {
                label: new AuthorityNamePipe(this.translate).transform(value),
                hint: new AuthorityAffiliationPipe(this.translate).transform(value),
                key: (value as Authority).authorityName
            };
            if (!this.chipsControl.value.some((v: DisplayValue) => v.key === displayValue.key)) {
                this.chipsControl.setValue([...this.chipsControl.value, displayValue]);
            }
        }
    }

    getTooltip(value: DisplayValue, hasTextOverflow: boolean): string | null {
        const shouldShowIndeterminateNotice =
            this.widget.getStatus() !== 'DISABLED' &&
            this.widget.getIndeterminateValues()?.includes(value.key);
        if (shouldShowIndeterminateNotice) {
            return this.translate.instant('MDS.INDETERMINATE_NOTICE', { value: value.label });
        } else if (hasTextOverflow) {
            return value.label;
        } else {
            return null;
        }
    }

    private toDisplayValues(value: MdsWidgetValue | string): DisplayValue {
        if (typeof value === 'string') {
            const knownValue = this.widget.definition.values?.find((v) => v.id === value);
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

    private inhibitAutocomplete() {
        this.autocompleteIsInhibited.next(true);
        setTimeout(() => {
            this.trigger.closePanel();
            this.autocompleteIsInhibited.next(false);
        });
    }
}
