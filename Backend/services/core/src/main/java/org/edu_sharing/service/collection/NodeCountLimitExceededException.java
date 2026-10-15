package org.edu_sharing.service.collection;

import lombok.Getter;

/**
 * Thrown if a node (recursively) contains more elements than allowed for an operation, e.g. copying a collection or
 * creating a collection from a folder
 */
@Getter
public class NodeCountLimitExceededException extends RuntimeException {
    private final int maxNodes;

    public NodeCountLimitExceededException(String nodeId, int maxNodes) {
        super("The node " + nodeId + " contains more than " + maxNodes + " elements, which is the maximum allowed for this operation");
        this.maxNodes = maxNodes;
    }
}
