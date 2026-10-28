import { Values } from 'ngx-edu-sharing-ui';
import { COLLECTION_TREE_PROPERTIES } from '../../../../features/mds/mds-editor/widgets/mds-editor-widget-tree/collection-tree/collection-tree-config';

/** Whether the property filters restrict the results to a collection. */
export const hasCollectionFilter = (propertyFilters: Values | null | undefined): boolean =>
    COLLECTION_TREE_PROPERTIES.some((prop) => !!propertyFilters?.[prop]);

/**
 * Points every collection filter in the property filters at the given collection, keeping
 * whether each one includes sub-collections.
 */
export const rebindCollectionFilters = (
    propertyFilters: Values | null | undefined,
    collectionId: string,
): void => {
    COLLECTION_TREE_PROPERTIES.filter((prop) => !!propertyFilters?.[prop]).forEach((prop) => {
        propertyFilters[prop] = [collectionId];
    });
};
