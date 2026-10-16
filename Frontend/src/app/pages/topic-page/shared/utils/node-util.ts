import { Node } from 'ngx-edu-sharing-api';

/**
 * Checks, whether a given node has write access.
 */
export const checkUserAccess = (node: Node): boolean => {
    return node.access.includes('Write');
};

/**
 * Returns either a nodeId or defaultNodeId based on whether the nodeId is defined.
 */
export const getNodeOrDefaultNodeId = (nodeId: string, defaultNodeId: string): string => {
    return nodeId && nodeId !== '' ? nodeId : defaultNodeId;
};
