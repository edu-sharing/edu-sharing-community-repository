import { computed, Injectable, signal } from '@angular/core';
import { NodeEntriesDisplayType } from 'ngx-edu-sharing-ui';

/** The ways the content of a collection can be presented; `topic` shows its topic page. */
export type CollectionViewType = 'grid' | 'list' | 'tree' | 'topicPage';

/**
 * View settings of the collection that is currently shown. Provided by the collections page so
 * that the controls that change them and the lists that apply them do not need to know each other.
 */
@Injectable()
export class CollectionViewStateService {
    /** How the content (sub-collections and materials) of the collection is presented. */
    readonly viewType = signal<CollectionViewType>('grid');

    /** Display type of the sub-collections; the tree view has no list of its own. */
    readonly collectionsDisplayType = computed(() =>
        this.viewType() === 'list'
            ? NodeEntriesDisplayType.Table
            : NodeEntriesDisplayType.SmallGrid,
    );

    /** Display type of the materials; the tree view has no list of its own. */
    readonly referencesDisplayType = computed(() =>
        this.viewType() === 'list' ? NodeEntriesDisplayType.Table : NodeEntriesDisplayType.Grid,
    );
}
