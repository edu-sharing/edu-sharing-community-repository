/*
 * 
 */

package org.edu_sharing.webservices.permission;

public class PermissionServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.permission.PermissionService {

    public PermissionServiceLocator() {
    }


    public PermissionServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public PermissionServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for permission
    private java.lang.String permission_address = "http://127.0.0.1/moodle193/edu_sharing/permissionservice.php";

    public java.lang.String getpermissionAddress() {
        return permission_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String permissionWSDDServiceName = "permission";

    public java.lang.String getpermissionWSDDServiceName() {
        return permissionWSDDServiceName;
    }

    public void setpermissionWSDDServiceName(java.lang.String name) {
        permissionWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.permission.Permission getpermission() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(permission_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getpermission(endpoint);
    }

    public org.edu_sharing.webservices.permission.Permission getpermission(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.permission.PermissionSoapBindingStub _stub = new org.edu_sharing.webservices.permission.PermissionSoapBindingStub(portAddress, this);
            _stub.setPortName(getpermissionWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setpermissionEndpointAddress(java.lang.String address) {
        permission_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.permission.Permission.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.permission.PermissionSoapBindingStub _stub = new org.edu_sharing.webservices.permission.PermissionSoapBindingStub(new java.net.URL(permission_address), this);
                _stub.setPortName(getpermissionWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("permission".equals(inputPortName)) {
            return getpermission();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    // CLEANUP? die URL OK? Wenn nicht, dannnochmal gesondert danach suchen
    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://permission.webservices.edu_sharing.org", "PermissionService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://permission.webservices.edu_sharing.org", "permission"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("permission".equals(portName)) {
            setpermissionEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
