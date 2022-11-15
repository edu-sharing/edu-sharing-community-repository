import { ElementRef, Injectable } from '@angular/core';
import { RawValuesDict, SearchConfig } from 'ngx-edu-sharing-api';
import { Observable } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SearchFieldInternalService } from './search-field-internal.service';

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

/**
 * Provides an interface between search implementations and the search-field component and its
 * internal logic.
 */
@Injectable({
    providedIn: 'root',
})
export class SearchFieldService {
    readonly filterValuesChanged = this.internal.filterValuesChanged.asObservable();

    constructor(private internal: SearchFieldInternalService) {}

    /** Emits when the user triggered a search using the search field. */
    onSearchTriggered(until: Observable<void>): Observable<SearchEvent> {
        return this.internal.searchTriggered.pipe(takeUntil(until));
    }

    /** Emits when the user changed the search string by typing into the search field. */
    onSearchStringChanged(until: Observable<void>): Observable<string> {
        return this.internal.searchStringChanged.pipe(takeUntil(until));
    }

    /** Emits when the user added or removed filters through the search field. */
    onFilterValuesChanged(until: Observable<void>): Observable<RawValuesDict> {
        return this.internal.filterValuesChanged.pipe(takeUntil(until));
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
