import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { DEFAULT_MDS_WIDGET_PREFIX, DEFAULT_OER_LICENSES } from '../types/custom-definitions';

/**
 * Checks, whether a given node has write access.
 *
 * @param node
 */
export const checkUserAccess = (node: Node): boolean => {
    return node.access.includes('Write');
};

/**
 * Returns either a nodeId or defaultNodeId based on whether the nodeId is defined.
 *
 * @param nodeId
 * @param defaultNodeId
 */
export const getNodeOrDefaultNodeId = (nodeId: string, defaultNodeId: string): string => {
    return nodeId && nodeId !== '' ? nodeId : defaultNodeId;
};

/**
 * Returns, whether a given node is proven editorial or not.
 *
 * @param node
 */
export const isEditorial = (node: Node): boolean => {
    return node.usedInCollections.some(
        (collection: Node): boolean =>
            collection.collection?.type === RestConstants.COLLECTIONTYPE_EDITORIAL,
    );
};

/**
 * Returns, whether a given node contains OER content or not.
 *
 * @param node
 */
export const isOer = (node: Node): boolean => {
    const nodeLicense: string = node.properties?.[RestConstants.CCM_PROP_LICENSE]?.[0] ?? '';
    return DEFAULT_OER_LICENSES.includes(nodeLicense);
};

/**
 * Returns, whether the metadataset of a given node has a specific given type.
 *
 * @param node
 * @param type
 */
export const checkMetadataset = (node: Node, type: string): boolean => {
    return node.properties?.[RestConstants.CM_PROP_METADATASET_EDU_METADATASET]?.[0] === type;
};

/**
 * Adds a prefix to a given widget ID string.
 */
export const prefixWidgetId = (widgetId: string): string => DEFAULT_MDS_WIDGET_PREFIX + widgetId;
