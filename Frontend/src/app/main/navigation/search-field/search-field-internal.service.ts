import { Injectable } from '@angular/core';
import {
    FacetsDict,
    LabeledValue,
    LabeledValuesDict,
    MdsIdentifier,
    MdsLabelService,
    RawValuesDict,
    SearchService,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import {
    BehaviorSubject,
    combineLatest,
    EMPTY,
    Observable,
    of,
    ReplaySubject,
    Subject,
    timer,
} from 'rxjs';
import {
    catchError,
    debounce,
    distinctUntilChanged,
    filter,
    first,
    map,
    switchMap,
    timeout,
} from 'rxjs/operators';
import { EventListener, FrameEventsService } from '../../../core-module/core.module';
import { notNull } from '../../../util/functions';
import { SearchFieldComponent } from './search-field.component';
import { MdsInfo, SearchEvent, SearchFieldConfig } from './search-field.service';

const NUMBER_OF_FACET_SUGGESTIONS = 5;

/**
 * Provides business logic for the search field as well as an interface between the public
 * search-field service and the search-field component.
 */
@Injectable({
    providedIn: 'root',
})
export class SearchFieldInternalService implements EventListener {
    readonly config = new BehaviorSubject<SearchFieldConfig | null>(null);
    /** Reference to the search-field component, if currently visible. */
    searchFieldComponent: SearchFieldComponent;
    /** The user clicked the filters button inside the search field. */
    readonly filtersButtonClicked = new Subject<void>();
    /** The user triggered a search using the search field. */
    readonly searchTriggered = new Subject<SearchEvent>();
    /** The user changed the search string by typing into the search field. */
    readonly searchStringChanged = new Subject<string>();
    /** The current content of the search field. */
    readonly searchString = new BehaviorSubject<string>('');

    /** Properties for which to fetch suggestions. */
    readonly categoriesSubject = new BehaviorSubject<string[]>(null);
    /** Active filters for use by search field. */
    readonly filters$: Observable<LabeledValuesDict | null>;
    /** Active suggestions for use by search field. */
    readonly suggestions$: Observable<FacetsDict>;
    /** Emits when the user added or removed filters through the search field. */
    readonly filterValuesChanged = new Subject<RawValuesDict>();
    /** The repository and metadata set to be used for suggestions and value lookups. */
    readonly mdsInfoSubject = new BehaviorSubject<MdsInfo>(null);

    private readonly enableFiltersAndSuggestionsSubject = new BehaviorSubject(false);
    private readonly filtersSubject = new BehaviorSubject<LabeledValuesDict>({});
    private readonly suggestionsInputStringSubject = new ReplaySubject<string>(1);
    private readonly suggestionsSubject = new BehaviorSubject<FacetsDict>(null);

    readonly mdsInfo$ = this.mdsInfoSubject.pipe(filter((config) => config !== null));
    readonly rawFilters$ = this.filtersSubject.pipe(
        map((filters) => this.mdsLabel.getRawValuesDict(filters)),
    );

    constructor(
        private search: SearchService,
        private mdsLabel: MdsLabelService,
        private event: FrameEventsService,
    ) {
        this.filters$ = this.enableFiltersAndSuggestionsSubject.pipe(
            switchMap((enabled) => (enabled ? this.filtersSubject : of(null))),
        );
        this.suggestions$ = this.suggestionsSubject.asObservable();
        this.registerSuggestionsSubject();
        this.event.addListener(this, rxjs.NEVER);
        this.config
            .pipe(
                map((config) => config?.enableFiltersAndSuggestions || false),
                distinctUntilChanged(),
            )
            .subscribe((enabled) => this.setEnableFiltersAndSuggestions(enabled));
    }

    onEvent(event: string, data: any) {
        if (event === FrameEventsService.EVENT_PARENT_SEARCH) {
            this.searchTriggered.next(data);
        }
    }

    /** The user triggered a search via the search field. */
    triggerSearch(event: SearchEvent): void {
        this.event.broadcastEvent(FrameEventsService.EVENT_GLOBAL_SEARCH, event.searchString);
        this.searchTriggered.next(event);
    }

    /**
     * Enables or disables the filters and suggestions functions.
     *
     * When disabled, `filters$` and `suggestions$` will not emit values and suggestions will not be
     * fetched.
     *
     * To be called by the search-field component.
     */
    private setEnableFiltersAndSuggestions(enabled: boolean): void {
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
            this.filterValuesChanged.next(this.mdsLabel.getRawValuesDict(newFilters));
        }
    }

    /**
     * Sets the active filters to be displayed as chips.
     *
     * To be called by the search-field component and by the component or service controlling the
     * search logic.
     */
    setFilterValues(values: RawValuesDict, { emitValuesChange = false } = {}): void {
        this.getMdsIdentifier()
            .pipe(
                // Wait for five seconds for the mds info to become available.
                timeout(5_000),
                catchError(() => {
                    console.warn('Called setFilterValues when mds was not configured.');
                    return rxjs.EMPTY;
                }),
                switchMap((mdsId) => this.mdsLabel.labelValuesDict(mdsId, values ?? {})),
            )
            .subscribe((filterValues) => {
                this.filtersSubject.next(filterValues);
                if (emitValuesChange) {
                    this.filterValuesChanged.next(values);
                }
            });
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
                filter(() => !!this.categoriesSubject.value),
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

    private getMdsIdentifier(): Observable<MdsIdentifier> {
        return this.mdsInfoSubject.pipe(
            first((mdsInfo) => notNull(mdsInfo?.repository) && notNull(mdsInfo?.metadataSet)),
            map(({ repository, metadataSet }) => ({ repository, metadataSet })),
        );
    }
}
