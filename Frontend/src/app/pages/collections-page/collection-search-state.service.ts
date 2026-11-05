import { computed, Injectable, signal } from '@angular/core';
import { Values } from 'ngx-edu-sharing-ui';

/** Query param holding the metadata filters of the collection search as JSON. */
export const COLLECTION_SEARCH_FILTERS_PARAM = 'searchFilters';

/**
 * Search settings of the collections page. Provided by the page so that the search field that
 * changes them and the lists that apply them do not need to know each other.
 */
@Injectable()
export class CollectionSearchStateService {
    /** The term the collections are searched for, `null` while no search is active. */
    readonly query = signal<string | null>(null);

    /**
     * Whether collections below the shown level are searched as well; otherwise only the
     * collections placed directly in the shown level are.
     */
    readonly includeSubLevels = signal(true);

    /** Metadata filters the collections have to match, keyed by property. */
    readonly filters = signal<Values>({});

    /**
     * Whether the settings were read from the URL. Until then they are not the user's, so a
     * metadata editor that starts earlier would report its empty values as a change.
     */
    readonly paramsApplied = signal(false);

    /** Number of collections the active search found, `null` until the hits are loaded. */
    readonly hitCount = signal<number | null>(null);

    /** Whether the lists show search hits instead of the content of the collection. */
    readonly active = computed(
        () => !!this.query()?.trim() || Object.keys(this.filters()).length > 0,
    );
}
