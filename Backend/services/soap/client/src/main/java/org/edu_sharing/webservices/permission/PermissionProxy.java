/*
 * 
 */
package org.edu_sharing.webservices.permission;

public class PermissionProxy implements org.edu_sharing.webservices.permission.Permission {
  private String _endpoint = null;
  private org.edu_sharing.webservices.permission.Permission permission = null;
  
  public PermissionProxy() {
    _initPermissionProxy();
  }
  
  public PermissionProxy(String endpoint) {
    _endpoint = endpoint;
    _initPermissionProxy();
  }
  
  private void _initPermissionProxy() {
    try {
      permission = (new org.edu_sharing.webservices.permission.PermissionServiceLocator()).getpermission();
      if (permission != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)permission)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)permission)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (permission != null)
      ((javax.xml.rpc.Stub)permission)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public org.edu_sharing.webservices.permission.Permission getPermission() {
    if (permission == null)
      _initPermissionProxy();
    return permission;
  }
  
  public java.lang.String checkCourse(java.lang.String in0, int in1) throws java.rmi.RemoteException{
    if (permission == null)
      _initPermissionProxy();
    return permission.checkCourse(in0, in1);
  }
  
  public boolean getPermission(java.lang.String session, int courseid, java.lang.String action, java.lang.String resourceid) throws java.rmi.RemoteException{
    if (permission == null)
      _initPermissionProxy();
    return permission.getPermission(session, courseid, action, resourceid);
  }
  
  
}