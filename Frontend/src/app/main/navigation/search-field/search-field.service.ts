import { ElementRef, Injectable } from '@angular/core';
import { RawValuesDict, SearchConfig } from 'ngx-edu-sharing-api';
import { Observable, Subject } from 'rxjs';
import { map, skip, takeUntil } from 'rxjs/operators';
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
    constructor(private until: Observable<void>, private internal: SearchFieldInternalService) {}

    /** Emits when the user clicked the filters button inside the search field. */
    onFiltersButtonClicked(): Observable<void> {
        return this.internal.filtersButtonClicked.pipe(takeUntil(this.until));
    }

    /** Emits when the user triggered a search using the search field. */
    onSearchTriggered(): Observable<SearchEvent> {
        return this.internal.searchTriggered.pipe(takeUntil(this.until));
    }

    /** Emits when the user changed the search string by typing into the search field. */
    onSearchStringChanged(): Observable<string> {
        return this.internal.searchStringChanged.pipe(takeUntil(this.until));
    }

    /** Emits when the user added or removed filters through the search field. */
    onFilterValuesChanged(): Observable<RawValuesDict> {
        return this.internal.filterValuesChanged.pipe(takeUntil(this.until));
    }

    setSearchString(value: string): void {
        this.internal.searchString.next(value);
    }

    /**
     * Sets the repository and metadata set to be used for suggestions and value lookups.
     */
    setMdsInfo(mdsInfo: MdsInfo): void {
        this.internal.mdsInfoSubject.next(mdsInfo);
    }

    /**
     * Sets the active filters to be displayed as chips.
     */
    setFilterValues(values: RawValuesDict): void {
        this.internal.setFilterValues(values);
    }

    /**
     * Returns a reference to the search field's input element.
     *
     * Use only for positioning, not for data.
     */
    getInputElement(): ElementRef {
        return this.internal.searchFieldComponent.input;
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
    _currentInstance: SearchFieldInstance | null = null;

    constructor(private internal: SearchFieldInternalService) {}

    observeEnabled(): Observable<boolean> {
        return this.internal.config.pipe(map(notNull));
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
        const configWithDefaults = { ...new SearchFieldConfig(), ...config };
        this.internal.config.next(configWithDefaults);
        until.subscribe(() => {
            if (this.internal.config.value === configWithDefaults) {
                this.internal.config.next(null);
            }
        });
        return this._createInstance();
    }

    disable() {
        this.internal.config.next(null);
    }

    /**
     * Returns the current search field instance or null if the search field is disabled.
     *
     * The returned instance's lifetime is determined by the `until` subject given when `enable` was
     * called. Use this method if you want to use a search field instance, that has been enabled
     * someplace else and you are sure that it will stay enabled for the time you intend to use it.
     * A good example would be a sub-component of the component that called `enable`.
     */
    getCurrentInstance(): SearchFieldInstance | null {
        return this._currentInstance;
    }

    private _createInstance(): SearchFieldInstance {
        const until = this.internal.config.pipe(
            skip(1),
            map(() => void 0),
        );
        const instance = new SearchFieldInstance(until, this.internal);
        this._currentInstance = instance;
        until.subscribe(() => {
            if (this._currentInstance === instance) {
                this._currentInstance = null;
            }
        });
        return instance;
    }
}
