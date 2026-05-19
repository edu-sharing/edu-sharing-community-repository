package org.edu_sharing.service.nodeservice;

import java.util.List;

public interface NodeServiceInterceptorPermissions {
    public boolean accessable(String nodeId, int recursionDepth);

    public default boolean hasPermission(String nodeId, String permission){
        return false;
    }

    /**
     * intercept behaviour of indirect access via collection
     * permissionsResult includes the already resolved, theoretical permissions determined by edu-sharing
     */
    default List<String> hasCollectionPermissions(String nodeId, List<String> permissionsToValidate, List<String> permissionsResult) {
        return permissionsResult;
    }
}
