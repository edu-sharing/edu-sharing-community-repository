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
    delay,
    distinctUntilChanged,
    filter,
    first,
    map,
    switchMap,
    timeout,
} from 'rxjs/operators';
import { EventListener, FrameEventsService } from '../../../core-module/core.module';
import { isTrue, notNull } from '../../../util/functions';
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
    searchFieldComponent = new BehaviorSubject<SearchFieldComponent>(null);
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
        map((filters) => (filters ? this.mdsLabel.getRawValuesDict(filters) : null)),
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
        this.registerAutoFocus();
    }

    private registerAutoFocus(): void {
        const autoFocus = this.config.pipe(
            map((config) => config?.autoFocus ?? false),
            // `config` will be set to `null` between page changes, so we will get events on page
            // changes if `autoFocus` is set to `true`.
            distinctUntilChanged(),
        );
        const inputElement = this.searchFieldComponent.pipe(
            switchMap((component) => component?.inputSubject ?? rxjs.of(null)),
            map((input) => input?.nativeElement),
        );
        // Each time the autoFocus option is set to true (i.e. we changed to a page that set the
        // option), wait for the input element to become available _once_ and focus it.
        autoFocus
            .pipe(
                filter(isTrue),
                switchMap(() =>
                    inputElement.pipe(
                        first(notNull),
                        timeout(5_000),
                        catchError(
                            () => (console.warn('Could not focus search field'), rxjs.EMPTY),
                        ),
                    ),
                ),
                // Don't loose focus to the main-menu button being refocused after the menu closes
                // on page navigation.
                delay(0),
            )
            .subscribe((input) => input.focus());
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
        const { [property]: propertyFilters, ...otherFilters } = this.filtersSubject.value;
        const index = propertyFilters?.findIndex((f) => f.value === filter.value);
        if (index >= 0) {
            let newFilters: { [x: string]: LabeledValue[] };
            if (propertyFilters.length > 1) {
                const filterCopy = propertyFilters.slice();
                filterCopy.splice(index, 1);
                newFilters = { ...otherFilters, [property]: filterCopy };
            } else {
                // The filter to be removed was the last one of this property. Remove the entire
                // property list.
                newFilters = otherFilters;
            }
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
                first(notNull),
                // Wait for the mds information to be set, but give up after 5 seconds.
                timeout(5_000),
                switchMap((mdsId) =>
                    mdsId ? this.mdsLabel.labelValuesDict(mdsId, values ?? {}) : rxjs.of(null),
                ),
                catchError(
                    () => (
                        console.warn(
                            'Tried to set filter values for search field, ' +
                                'but did not set mds information',
                        ),
                        rxjs.EMPTY
                    ),
                ),
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
                filter(() => this.categoriesSubject.value?.length > 0),
                debounce((inputString) => (inputString ? timer(200) : EMPTY)),
                switchMap((inputString) =>
                    inputString
                        ? this.search
                              .getAsYouTypeFacetSuggestions(
                                  inputString,
                                  this.categoriesSubject.value,
                                  NUMBER_OF_FACET_SUGGESTIONS,
                              )
                              // TODO: Figure out if the MDS supports suggestion facet queries
                              // beforehand.
                              .pipe(
                                  catchError((error) => {
                                      console.warn(
                                          'Failed to fetch as-you-type facet suggestions. ' +
                                              'In case your MDS does not support facet ' +
                                              'suggestions, please override the group ' +
                                              '"search_input" to not contain any views.',
                                      );
                                      error.preventDefault();
                                      return rxjs.of(null);
                                  }),
                              )
                        : of(null),
                ),
            )
            .subscribe((suggestions) => this.suggestionsSubject.next(suggestions));
    }

    private getMdsIdentifier(): Observable<MdsIdentifier | null> {
        return this.mdsInfoSubject.pipe(
            map((mdsInfo) =>
                notNull(mdsInfo?.repository) && notNull(mdsInfo?.metadataSet)
                    ? { repository: mdsInfo.repository, metadataSet: mdsInfo.metadataSet }
                    : null,
            ),
        );
    }
}
