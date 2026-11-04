/**
 * Query parameter of the page URL naming the collection the `collectionTree` widget starts at,
 * e.g. the collection of the topic page a teaser links from.
 */
export const COLLECTION_TREE_ROOT_QUERY_PARAM = 'collectionRoot';

/**
 * Search property of the default `collectionTree` widget; it matches material of the picked
 * collections and of all their sub-collections.
 */
export const COLLECTION_TREE_PROPERTY = 'virtual:collection_id_tree';

/**
 * Search property the widget sends the picked collections under instead of its own one when
 * sub-collections are left out; it matches material placed directly in those collections.
 */
export const COLLECTION_TREE_PRIMARY_PROPERTY = 'virtual:collection_id_primary';

/** Search properties filtering by collection, with and without sub-collections. */
export const COLLECTION_TREE_PROPERTIES: readonly string[] = [
    COLLECTION_TREE_PROPERTY,
    COLLECTION_TREE_PRIMARY_PROPERTY,
];

/** Search property holding the picked collections of the widget with the given ID. */
export function getCollectionTreeSearchProperty(
    widgetId: string,
    includeSubcollections: boolean,
): string {
    return includeSubcollections ? widgetId : COLLECTION_TREE_PRIMARY_PROPERTY;
}

/**
 * The picked collections stored in the given search values and whether they include
 * sub-collections, which is told by the property holding them.
 */
export function readCollectionTreeValues<T>(
    widgetId: string,
    values: { [property: string]: T } | undefined,
    primaryHasOwnWidget: boolean,
): { value: T | undefined; includeSubcollections: boolean } {
    if (widgetId === COLLECTION_TREE_PRIMARY_PROPERTY) {
        return { value: values?.[widgetId], includeSubcollections: false };
    }
    if (
        !primaryHasOwnWidget &&
        values?.[widgetId] === undefined &&
        values?.[COLLECTION_TREE_PRIMARY_PROPERTY]
    ) {
        return { value: values[COLLECTION_TREE_PRIMARY_PROPERTY], includeSubcollections: false };
    }
    return { value: values?.[widgetId], includeSubcollections: true };
}

export const DEFAULT_COLLECTION_TREE_PAGE_SIZE = 50;
/** Search hits listed at most; a more specific search narrows them down. */
export const COLLECTION_TREE_MAX_SEARCH_HITS = 100;

export type CollectionTreeScope =
    | 'MY'
    | 'EDU_ALL'
    | 'EDU_GROUPS'
    | 'TYPE_EDITORIAL'
    | 'TYPE_MEDIA_CENTER';

/** Categories of the top level in the order of the tabs on the collections page. */
export const DEFAULT_COLLECTION_TREE_SCOPES: readonly CollectionTreeScope[] = [
    'MY',
    'EDU_GROUPS',
    'TYPE_EDITORIAL',
    'TYPE_MEDIA_CENTER',
    'EDU_ALL',
];

/** Categories only signed-in users have collections in. */
export const PERSONAL_COLLECTION_TREE_SCOPES: readonly CollectionTreeScope[] = ['MY', 'EDU_GROUPS'];

/** Options of the `collectionTree` widget, given as JSON in the widget's `configuration`. */
export interface CollectionTreeConfig {
    /** Collection to start at when the hosting page passes none. */
    rootCollectionId?: string;
    /** Categories of the top level that is listed when no start collection is known. */
    scopes?: CollectionTreeScope[];
    /** Number of collections loaded per tree level at once. */
    pageSize?: number;
}

export function parseCollectionTreeConfig(configuration: string | undefined): CollectionTreeConfig {
    if (!configuration?.trim()) {
        return {};
    }
    try {
        const parsed: unknown = JSON.parse(configuration);
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
            return parsed as CollectionTreeConfig;
        }
    } catch (e) {
        console.warn('Invalid collectionTree configuration, using defaults', configuration, e);
        return {};
    }
    console.warn('collectionTree configuration is not an object, using defaults', configuration);
    return {};
}
