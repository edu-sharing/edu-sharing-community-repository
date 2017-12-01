/*
 * 
 */

package org.edu_sharing.webservices.permission;

public interface PermissionService extends javax.xml.rpc.Service {
    public java.lang.String getpermissionAddress();

    public org.edu_sharing.webservices.permission.Permission getpermission() throws javax.xml.rpc.ServiceException;

    public org.edu_sharing.webservices.permission.Permission getpermission(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
