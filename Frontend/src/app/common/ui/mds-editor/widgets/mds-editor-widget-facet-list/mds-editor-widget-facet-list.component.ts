import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { FacetAggregation, FacetValue, SearchService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Subject } from 'rxjs';
import { filter, finalize, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

@Component({
    selector: 'es-mds-editor-widget-facet-list',
    templateUrl: './mds-editor-widget-facet-list.component.html',
    styleUrls: ['./mds-editor-widget-facet-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MdsEditorWidgetFacetListComponent
    extends MdsEditorWidgetBase
    implements OnInit, OnDestroy
{
    readonly valueType: ValueType = ValueType.MultiValue;
    /** Available facet values being updated from `mdsEditorInstance.suggestions$`. */
    readonly facetAggregationSubject = new BehaviorSubject<FacetAggregation>(null);
    /** Form array representing checkbox states. */
    formArray: FormArray;
    /** Whether we are currently loading more facets. */
    isLoading = false;
    /** IDs of selected values. Updated through user interaction. */
    private values: string[];
    private readonly destroyed$ = new Subject<void>();

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private search: SearchService,
    ) {
        super(mdsEditorInstance, translate);
    }

    ngOnInit(): void {
        this.values = this.widget.getInitialValues().jointValues;
        this.registerFacetValuesSubject();
        this.registerFormControls();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    getFacet(index: number): FacetValue {
        return this.facetAggregationSubject.value.values[index];
    }

    onLoadMore(): void {
        this.isLoading = true;
        this.search
            .loadMoreFacets(this.widget.definition.id, 10)
            .pipe(finalize(() => (this.isLoading = false)))
            .subscribe();
    }

    private registerFacetValuesSubject(): void {
        this.search
            .observeFacet(this.widget.definition.id, { includeActiveFilters: true })
            .pipe(takeUntil(this.destroyed$))
            .subscribe((facetAggregation) => this.facetAggregationSubject.next(facetAggregation));
    }

    private registerFormControls(): void {
        // (Re-)create `formArray` on changed facet values.
        this.facetAggregationSubject.subscribe((facetValues) => {
            if (facetValues) {
                this.formArray = this.generateFormArray(facetValues.values);
            } else {
                this.formArray = null;
            }
        });
    }

    private generateFormArray(facetValues: FacetValue[]): FormArray {
        const formArray = new FormArray(
            facetValues.map((value) => new FormControl(this.values.includes(value.value))),
        );
        // Propagate user interaction to instance service.
        formArray.valueChanges
            .pipe(filter((value) => value !== null))
            .subscribe((checkboxStates: boolean[]) => {
                this.values = this.facetAggregationSubject.value.values
                    .filter((_, index) => checkboxStates[index] === true)
                    .map(({ value }) => value);
                this.setValue(this.values);
            });
        return formArray;
    }
}
