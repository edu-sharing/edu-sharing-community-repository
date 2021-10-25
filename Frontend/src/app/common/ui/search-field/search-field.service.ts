import { EventEmitter, Injectable } from '@angular/core';
import {
    FacetsDict,
    LabeledValue,
    LabeledValuesDict,
    MdsIdentifier,
    MdsLabelService,
    RawValuesDict,
    SearchConfig,
    SearchService,
} from 'edu-sharing-api';
import { BehaviorSubject, combineLatest, EMPTY, Observable, of, ReplaySubject, timer } from 'rxjs';
import { debounce, filter, map, switchMap, tap } from 'rxjs/operators';

const NUMBER_OF_FACET_SUGGESTIONS = 5;

@Injectable({
    providedIn: 'root',
})
export class SearchFieldService {
    /** Properties for which to fetch suggestions. */
    readonly categoriesSubject = new BehaviorSubject<string[]>(null);
    /** Active filters for use by search field. */
    readonly filters$: Observable<LabeledValuesDict>;
    /** Active suggestions for use by search field. */
    readonly suggestions$: Observable<FacetsDict>;
    /** Emits when the user added or removed filters through the search field. */
    readonly filterValuesChange = new EventEmitter<RawValuesDict>();

    private readonly searchConfigSubject = new BehaviorSubject<Partial<SearchConfig>>({});
    private readonly enableFiltersAndSuggestionsSubject = new BehaviorSubject(false);
    private readonly filtersSubject = new BehaviorSubject<LabeledValuesDict>({});
    private readonly suggestionsInputStringSubject = new ReplaySubject<string>(1);
    private readonly suggestionsSubject = new BehaviorSubject<FacetsDict>(null);

    readonly mdsInfo$ = this.searchConfigSubject.pipe(
        filter((config): config is SearchConfig => !!config.repository && !!config.metadataSet),
    );
    readonly rawFilters$ = this.filtersSubject.pipe(
        map((filters) => this.mdsLabel.getRawValuesDict(filters)),
    );

    constructor(private search: SearchService, private mdsLabel: MdsLabelService) {
        this.searchConfigSubject.subscribe((searchConfig) => this.search.configure(searchConfig));
        this.filters$ = this.enableFiltersAndSuggestionsSubject.pipe(
            switchMap((enabled) => (enabled ? this.filtersSubject : of(null))),
        );
        this.suggestions$ = this.suggestionsSubject.asObservable();
        this.registerSuggestionsSubject();
    }

    /**
     * Enables or disables the filters and suggestions functions.
     *
     * When disabled, `filters$` and `suggestions$` will not emit values and suggestions will not be
     * fetched.
     *
     * To be called by the search-field component.
     */
    setEnableFiltersAndSuggestions(enabled: boolean): void {
        this.enableFiltersAndSuggestionsSubject.next(enabled);
        if (!enabled) {
            this.suggestionsInputStringSubject.next('');
            this.filtersSubject.next({});
        }
    }

    /**
     * Updates the input string as the user types to fetch matching as-you-type suggestions.
     *
     * To be called by the search-field component.
     */
    updateSuggestions(inputString: string): void {
        this.suggestionsInputStringSubject.next(inputString);
    }

    /**
     * Removes a filter from the active-filters dictionary as the user clicks on a chip's remove
     * button.
     *
     * To be called by the search-field component.
     */
    removeFilter(property: string, filter: LabeledValue): void {
        const filterList = this.filtersSubject.value[property];
        const index = filterList?.findIndex((f) => f.value === filter.value);
        if (index >= 0) {
            const filterCopy = filterList.slice();
            filterCopy.splice(index, 1);
            const newFilters = { ...this.filtersSubject.value, [property]: filterCopy };
            this.filtersSubject.next(newFilters);
            this.filterValuesChange.emit(this.mdsLabel.getRawValuesDict(newFilters));
        }
    }

    /**
     * Sets the active filters to be displayed as chips.
     *
     * To be called  by the search-field component and by the component or service controlling the
     * search logic.
     */
    setFilterValues(values: RawValuesDict, { emitValuesChange = false } = {}): void {
        const mdsId = this.getCurrentMdsIdentifier();
        if (mdsId) {
            this.mdsLabel.labelValuesDict(mdsId, values).subscribe((filterValues) => {
                this.filtersSubject.next(filterValues);
                if (emitValuesChange) {
                    this.filterValuesChange.emit(values);
                }
            });
        } else {
            console.warn('Called setFilterValues when mds was not configured.');
        }
        // Propagate to search service, so the filters will be taken into account for facet
        // suggestions.
        this.search.setParams({ filters: values });
    }

    /**
     * Sets the repository to be used for suggestions and value lookups.
     *
     * To be called by the component or service controlling the search logic.
     */
    setRepository(repository: string): void {
        if (this.searchConfigSubject.value.repository !== repository) {
            this.searchConfigSubject.next({ ...this.searchConfigSubject.value, repository });
        }
    }

    /**
     * Sets the metadata set to be used for suggestions and value lookups.
     *
     * To be called by the component or service controlling the search logic.
     */
    setMetadataSet(metadataSet: string): void {
        if (this.searchConfigSubject.value.metadataSet !== metadataSet) {
            this.searchConfigSubject.next({ ...this.searchConfigSubject.value, metadataSet });
        }
    }

    private registerSuggestionsSubject(): void {
        combineLatest([
            this.enableFiltersAndSuggestionsSubject,
            this.categoriesSubject,
            this.filtersSubject,
        ]).subscribe(() => this.suggestionsSubject.next(null));
        this.suggestionsInputStringSubject
            .pipe(
                map((inputString) =>
                    this.enableFiltersAndSuggestionsSubject.value ? inputString : null,
                ),
                map((inputString) => (inputString?.length >= 3 ? inputString : null)),
                debounce((inputString) => (inputString ? timer(200) : EMPTY)),
                switchMap((inputString) =>
                    inputString
                        ? this.search.getAsYouTypeFacetSuggestions(
                              inputString,
                              this.categoriesSubject.value,
                              NUMBER_OF_FACET_SUGGESTIONS,
                          )
                        : of(null),
                ),
            )
            .subscribe((suggestions) => this.suggestionsSubject.next(suggestions));
    }

    private getCurrentMdsIdentifier(): MdsIdentifier | null {
        const { repository, metadataSet } = this.searchConfigSubject.value;
        if (repository && metadataSet) {
            return { repository, metadataSet };
        } else {
            return null;
        }
    }
}
