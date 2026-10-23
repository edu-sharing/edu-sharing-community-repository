import { computed, Injectable, signal } from '@angular/core';

/**
 * Search settings of the collections page. Provided by the page so that the search field that
 * changes them and the lists that apply them do not need to know each other.
 */
@Injectable()
export class CollectionSearchStateService {
    /** The term the collections are searched for, `null` while no search is active. */
    readonly query = signal<string | null>(null);

    /** Whether the lists show search hits instead of the content of the collection. */
    readonly active = computed(() => !!this.query()?.trim());
}
