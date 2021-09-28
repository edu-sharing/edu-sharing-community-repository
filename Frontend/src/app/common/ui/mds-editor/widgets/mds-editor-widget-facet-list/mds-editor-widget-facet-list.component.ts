import { Component, OnInit } from '@angular/core';
import { FormArray, FormControl } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { MdsWidgetFacetValue } from '../../types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'app-mds-editor-widget-facet-list',
    templateUrl: './mds-editor-widget-facet-list.component.html',
    styleUrls: ['./mds-editor-widget-facet-list.component.scss'],
})
export class MdsEditorWidgetFacetListComponent extends MdsEditorWidgetBase implements OnInit {
    readonly valueType: ValueType = ValueType.MultiValue;
    /** Available facet values being updated from `mdsEditorInstance.suggestions$`. */
    readonly facetValuesSubject = new BehaviorSubject<MdsWidgetFacetValue[]>(null);
    /** Form array representing checkbox states. */
    formArray: FormArray;
    /** IDs of selected values. Updated through user interaction. */
    private values: string[];

    ngOnInit(): void {
        this.values = this.widget.getInitialValues().jointValues;
        this.registerFacetValuesSubject();
        this.registerFormControls();
    }

    private registerFacetValuesSubject(): void {
        this.mdsEditorInstance.facets$
            .pipe(map((facets) => facets[this.widget.definition.id]))
            .subscribe((values) => this.facetValuesSubject.next(values));
    }

    private registerFormControls(): void {
        // (Re-)create `formArray` on changed facet values.
        this.facetValuesSubject.subscribe((facetValues) => {
            if (facetValues) {
                this.formArray = this.generateFormArray(facetValues);
            } else {
                this.formArray = null;
            }
        });
    }

    private generateFormArray(facetValues: MdsWidgetFacetValue[]): FormArray {
        const formArray = new FormArray(
            facetValues.map((value) => new FormControl(this.values.includes(value.id))),
        );
        // Propagate user interaction to instance service.
        formArray.valueChanges
            .pipe(filter((value) => value !== null))
            .subscribe((checkboxStates: boolean[]) => {
                this.values = this.facetValuesSubject.value
                    .filter((_, index) => checkboxStates[index] === true)
                    .map((value) => value.id);
                this.setValue(this.values);
            });
        return formArray;
    }
}
