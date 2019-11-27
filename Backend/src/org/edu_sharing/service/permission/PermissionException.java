package org.edu_sharing.service.permission;

public class PermissionException extends RuntimeException{
    private final String node;
    private final String permissionName;

    public PermissionException(String node, String permissionName){
        this.node=node;
        this.permissionName=permissionName;
    }
    @Override
    public String toString() {
        return permissionName+" is missing for current user on node "+node;
    }
    @Override
    public String getMessage() {
        return toString();
    }
}
