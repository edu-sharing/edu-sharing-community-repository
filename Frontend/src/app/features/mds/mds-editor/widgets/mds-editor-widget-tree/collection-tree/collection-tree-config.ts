import { CollectionScope } from '../../../../../../services/collection-scopes.service';

/**
 * Query parameter of the page URL naming the collection the `collectionTree` widget starts at,
 * e.g. the collection of the topic page a teaser links from.
 */
export const COLLECTION_TREE_ROOT_QUERY_PARAM = 'collectionRoot';

export const DEFAULT_COLLECTION_TREE_PAGE_SIZE = 50;
/** Search hits listed at most; a more specific search narrows them down. */
export const COLLECTION_TREE_MAX_SEARCH_HITS = 100;

/** Options of the `collectionTree` widget, given as JSON in the widget's `configuration`. */
export interface CollectionTreeConfig {
    /** Collection to start at when the hosting page passes none. */
    rootCollectionId?: string;
    /** Categories of the top level that is listed when no start collection is known. */
    scopes?: CollectionScope[];
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
