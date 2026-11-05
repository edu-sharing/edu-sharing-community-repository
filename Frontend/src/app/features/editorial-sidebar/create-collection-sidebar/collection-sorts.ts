import { RestConstants } from 'ngx-edu-sharing-api';
import type { NewCollectionSort } from './create-collection.service';

export type SortId = 'TITLE' | 'MODIFIED' | 'CREATED' | 'MANUAL';

/** the ways children can be sorted by default, with the property each one stands for */
export const SORTS: {
    [kind in 'subcollections' | 'references']: { [id in SortId]?: NewCollectionSort };
} = {
    subcollections: {
        TITLE: { property: RestConstants.CM_PROP_TITLE, ascending: true },
        MODIFIED: { property: RestConstants.CM_MODIFIED_DATE, ascending: false },
        CREATED: { property: RestConstants.CM_PROP_C_CREATED, ascending: false },
        MANUAL: {
            property: RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
            ascending: true,
        },
    },
    references: {
        TITLE: { property: RestConstants.LOM_PROP_TITLE, ascending: true },
        MODIFIED: { property: RestConstants.CM_MODIFIED_DATE, ascending: false },
        CREATED: { property: RestConstants.CM_PROP_C_CREATED, ascending: false },
        MANUAL: {
            property: RestConstants.CCM_PROP_COLLECTION_ORDERED_POSITION,
            ascending: true,
        },
    },
};

/** the sorting a collection stores as property; a collection that has none is sorted by change */
export function collectionSortIdOf(
    kind: 'subcollections' | 'references',
    stored?: string[],
): SortId | null {
    if (!stored?.length) {
        return 'MODIFIED';
    }
    const ids = Object.keys(SORTS[kind]) as SortId[];
    return ids.find((id) => SORTS[kind][id].property === stored[0]) ?? null;
}
