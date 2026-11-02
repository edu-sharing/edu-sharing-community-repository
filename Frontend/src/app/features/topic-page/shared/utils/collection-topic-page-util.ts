import { Node, RestConstants } from 'ngx-edu-sharing-api';

/** Query param of the collections page naming its view type; only the topic view is stored. */
export const VIEW_TYPE_QUERY_PARAM = 'viewType';
/** Value of {@link VIEW_TYPE_QUERY_PARAM} that opens the topic page of a collection. */
export const TOPIC_PAGE_VIEW_TYPE = 'topicPage';
/** Query param naming a menu the topic page opens once it has loaded, and then drops. */
export const OPEN_MENU_QUERY_PARAM = 'openMenu';
/** Value of {@link OPEN_MENU_QUERY_PARAM} that opens the list of the collection's pages. */
export const OPEN_MANAGE_SIDEBAR_PARAM = 'managePages';

/** Query params that open the topic page of the collection with the given id. */
export function getTopicPageQueryParams(collectionId: string): Record<string, string> {
    return { id: collectionId, [VIEW_TYPE_QUERY_PARAM]: TOPIC_PAGE_VIEW_TYPE };
}

/**
 * Whether a topic page is configured for the collection: by its own page config or by one a
 * parent propagates to its sub-collections.
 *
 * @param parents ancestors of the collection; the collection itself may be included
 */
export function hasTopicPage(collection: Node, parents: Node[]): boolean {
    if (collection.properties?.[RestConstants.CCM_PROP_PAGE_CONFIG_REF]?.[0]) {
        return true;
    }
    return parents.some(
        (parent) =>
            parent.ref.id !== collection.ref.id &&
            !!parent.properties?.[RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_REF]?.[0],
    );
}
