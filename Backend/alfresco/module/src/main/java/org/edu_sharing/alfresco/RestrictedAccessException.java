package org.edu_sharing.alfresco;

public class RestrictedAccessException extends RuntimeException{

    private final String node;

    public RestrictedAccessException(String node) {
        this.node = node;
    }
    @Override
    public String toString() {
        return  "Target node " + node +" has ccm:restricted_access, so no access is allowed";
    }
    @Override
    public String getMessage() {
        return toString();
    }
}
