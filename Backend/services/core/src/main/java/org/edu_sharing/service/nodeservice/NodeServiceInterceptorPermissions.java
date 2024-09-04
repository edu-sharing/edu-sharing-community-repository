package org.edu_sharing.service.nodeservice;

public interface NodeServiceInterceptorPermissions {
    public boolean accessable(String nodeId, int recursionDepth);
}
