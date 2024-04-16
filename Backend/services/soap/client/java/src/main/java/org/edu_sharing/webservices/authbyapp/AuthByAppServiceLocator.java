/**
 * AuthByAppServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authbyapp;

public class AuthByAppServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.authbyapp.AuthByAppService {

    public AuthByAppServiceLocator() {
    }


    public AuthByAppServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public AuthByAppServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for authbyapp
    private java.lang.String authbyapp_address = "http://localhost:8080/edu-sharing/services/authbyapp";

    public java.lang.String getauthbyappAddress() {
        return authbyapp_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String authbyappWSDDServiceName = "authbyapp";

    public java.lang.String getauthbyappWSDDServiceName() {
        return authbyappWSDDServiceName;
    }

    public void setauthbyappWSDDServiceName(java.lang.String name) {
        authbyappWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.authbyapp.AuthByApp getauthbyapp() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(authbyapp_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getauthbyapp(endpoint);
    }

    public org.edu_sharing.webservices.authbyapp.AuthByApp getauthbyapp(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.authbyapp.AuthbyappSoapBindingStub _stub = new org.edu_sharing.webservices.authbyapp.AuthbyappSoapBindingStub(portAddress, this);
            _stub.setPortName(getauthbyappWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setauthbyappEndpointAddress(java.lang.String address) {
        authbyapp_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.authbyapp.AuthByApp.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.authbyapp.AuthbyappSoapBindingStub _stub = new org.edu_sharing.webservices.authbyapp.AuthbyappSoapBindingStub(new java.net.URL(authbyapp_address), this);
                _stub.setPortName(getauthbyappWSDDServiceName());
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
        if ("authbyapp".equals(inputPortName)) {
            return getauthbyapp();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "AuthByAppService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "authbyapp"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("authbyapp".equals(portName)) {
            setauthbyappEndpointAddress(address);
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
