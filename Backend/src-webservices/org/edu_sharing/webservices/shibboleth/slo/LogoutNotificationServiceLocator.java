/**
 * LogoutNotificationServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.shibboleth.slo;

public class LogoutNotificationServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationService {

    public LogoutNotificationServiceLocator() {
    }


    public LogoutNotificationServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public LogoutNotificationServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for LogoutNotification
    private java.lang.String LogoutNotification_address = "http://localhost:8080/edu-sharing/services/LogoutNotification";

    public java.lang.String getLogoutNotificationAddress() {
        return LogoutNotification_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String LogoutNotificationWSDDServiceName = "LogoutNotification";

    public java.lang.String getLogoutNotificationWSDDServiceName() {
        return LogoutNotificationWSDDServiceName;
    }

    public void setLogoutNotificationWSDDServiceName(java.lang.String name) {
        LogoutNotificationWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.shibboleth.slo.LogoutNotification getLogoutNotification() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(LogoutNotification_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getLogoutNotification(endpoint);
    }

    public org.edu_sharing.webservices.shibboleth.slo.LogoutNotification getLogoutNotification(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationSoapBindingStub _stub = new org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationSoapBindingStub(portAddress, this);
            _stub.setPortName(getLogoutNotificationWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setLogoutNotificationEndpointAddress(java.lang.String address) {
        LogoutNotification_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.shibboleth.slo.LogoutNotification.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationSoapBindingStub _stub = new org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationSoapBindingStub(new java.net.URL(LogoutNotification_address), this);
                _stub.setPortName(getLogoutNotificationWSDDServiceName());
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
        if ("LogoutNotification".equals(inputPortName)) {
            return getLogoutNotification();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "LogoutNotificationService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "LogoutNotification"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("LogoutNotification".equals(portName)) {
            setLogoutNotificationEndpointAddress(address);
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
