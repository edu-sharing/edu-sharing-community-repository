import { ElementRef, Injectable } from '@angular/core';
import { RawValuesDict, SearchConfig } from 'ngx-edu-sharing-api';
import { Observable, Subject } from 'rxjs';
import { delay, map, take, takeUntil } from 'rxjs/operators';
import { notNull } from '../../../util/functions';
import { SearchFieldInternalService } from './search-field-internal.service';

export class SearchFieldConfig {
    /** The placeholder text for the search field, will be translated. */
    placeholder: string;
    /**
     * Shows a filter button inside the search field. Handle `onFiltersButtonClicked` when enabling.
     */
    showFiltersButton = false;
    /**
     * If enabled, shows filters as chips inside the search field and suggests additional filters in
     * an overlay as the user types into the search field.
     *
     * Relies on active filters values being provided via `setFilterValues` and `filterValuesChange`
     * being handled.
     */
    enableFiltersAndSuggestions = false;
    /** Focus the search field input when it initially becomes available. */
    autoFocus = false;
}

export type MdsInfo = Pick<SearchConfig, 'repository' | 'metadataSet'>;
export type SearchEvent = {
    /** The content of the search field at the time the search was triggered. */
    searchString: string;
    /**
     * Whether the search was triggered because the user cleared the search field using the 'X'
     * button.
     */
    cleared: boolean;
};

export class SearchFieldInstance {
    constructor(private _until: Observable<void>, private _internal: SearchFieldInternalService) {}

    patchConfig(config: Partial<SearchFieldConfig>) {
        const newConfig = { ...this._internal.config.value, ...config };
        this._internal.config.next(newConfig);
    }

    /** Emits when the user clicked the filters button inside the search field. */
    onFiltersButtonClicked(): Observable<void> {
        return this._internal.filtersButtonClicked.pipe(takeUntil(this._until));
    }

    /** Emits when the user triggered a search using the search field. */
    onSearchTriggered(): Observable<SearchEvent> {
        return this._internal.searchTriggered.pipe(takeUntil(this._until));
    }

    /** Emits when the user changed the search string by typing into the search field. */
    onSearchStringChanged(): Observable<string> {
        return this._internal.searchStringChanged.pipe(takeUntil(this._until));
    }

    /** Emits when the user added or removed filters through the search field. */
    onFilterValuesChanged(): Observable<RawValuesDict> {
        return this._internal.filterValuesChanged.pipe(takeUntil(this._until));
    }

    setSearchString(value: string): void {
        this._internal.searchString.next(value);
    }

    getSearchString(): string {
        return this._internal.searchString.value;
    }

    /**
     * Sets the repository and metadata set to be used for suggestions and value lookups.
     */
    setMdsInfo(mdsInfo: MdsInfo): void {
        this._internal.mdsInfoSubject.next(mdsInfo);
    }

    /**
     * Sets the active filters to be displayed as chips.
     */
    setFilterValues(values: RawValuesDict): void {
        this._internal.setFilterValues(values);
    }

    /**
     * Returns a reference to the search field's input element.
     *
     * Use only for positioning, not for data.
     */
    getInputElement(): ElementRef {
        return this._internal.searchFieldComponent.value.input;
    }
}

/**
 * Provides an interface between search implementations and the search-field component and its
 * internal logic.
 */
@Injectable({
    providedIn: 'root',
})
export class SearchFieldService {
    private _currentInstance: SearchFieldInstance | null = null;
    private _resetInstance = new Subject<void>();

    constructor(private _internal: SearchFieldInternalService) {
        this._resetInstance.subscribe(() => {
            this._currentInstance = null;
            this._internal.config.next(null);
        });
    }

    observeEnabled(): Observable<boolean> {
        return this._internal.config.pipe(map(notNull));
    }

    /**
     * Enables the search field.
     *
     * The search field and the returned search field instance will stay active until (whatever
     * happens first):
     * - the given `until` subject fires
     * - `enable` is called again
     * - `disable` is called.
     */
    enable(config: Partial<SearchFieldConfig>, until: Subject<void>): SearchFieldInstance {
        this._resetInstance.next();
        const configWithDefaults = { ...new SearchFieldConfig(), ...config };
        this._internal.config.next(configWithDefaults);
        until.subscribe(() => this._resetInstance.next());
        return this._createInstance();
    }

    disable() {
        this._resetInstance.next();
    }

    /**
     * Returns the current search-field instance or null if the search field is disabled.
     *
     * The returned instance's lifetime is determined by the `until` subject given when `enable` was
     * called. Use this method if you want to use a search field instance, that has been enabled
     * someplace else and you are sure that it will stay enabled for the time you intend to use it.
     * A good example would be a sub-component of the component that called `enable`.
     */
    getCurrentInstance(): SearchFieldInstance | null {
        return this._currentInstance;
    }

    /**
     * Returns an updated observable of the current search-field instance or null if the search
     * field is disabled.
     *
     * Use this to monitor how other components interact with the search field.
     */
    observeCurrentInstance(): Observable<SearchFieldInstance | null> {
        return this.observeEnabled().pipe(
            delay(0),
            map(() => this._currentInstance),
        );
    }

    private _createInstance(): SearchFieldInstance {
        const until = this._resetInstance.pipe(take(1));
        this._currentInstance = new SearchFieldInstance(until, this._internal);
        return this._currentInstance;
    }
}
