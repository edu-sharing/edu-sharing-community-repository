/**
 * NativeAlfrescoWrapperServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class NativeAlfrescoWrapperServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperService {

    public NativeAlfrescoWrapperServiceLocator() {
    }


    public NativeAlfrescoWrapperServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public NativeAlfrescoWrapperServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for NativeAlfrescoWrapper
    private java.lang.String NativeAlfrescoWrapper_address = "http://localhost:8080/edu-sharing/services/NativeAlfrescoWrapper";

    public java.lang.String getNativeAlfrescoWrapperAddress() {
        return NativeAlfrescoWrapper_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String NativeAlfrescoWrapperWSDDServiceName = "NativeAlfrescoWrapper";

    public java.lang.String getNativeAlfrescoWrapperWSDDServiceName() {
        return NativeAlfrescoWrapperWSDDServiceName;
    }

    public void setNativeAlfrescoWrapperWSDDServiceName(java.lang.String name) {
        NativeAlfrescoWrapperWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper getNativeAlfrescoWrapper() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NativeAlfrescoWrapper_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNativeAlfrescoWrapper(endpoint);
    }

    public org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper getNativeAlfrescoWrapper(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperSoapBindingStub _stub = new org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperSoapBindingStub(portAddress, this);
            _stub.setPortName(getNativeAlfrescoWrapperWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNativeAlfrescoWrapperEndpointAddress(java.lang.String address) {
        NativeAlfrescoWrapper_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperSoapBindingStub _stub = new org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperSoapBindingStub(new java.net.URL(NativeAlfrescoWrapper_address), this);
                _stub.setPortName(getNativeAlfrescoWrapperWSDDServiceName());
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
        if ("NativeAlfrescoWrapper".equals(inputPortName)) {
            return getNativeAlfrescoWrapper();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "NativeAlfrescoWrapperService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "NativeAlfrescoWrapper"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("NativeAlfrescoWrapper".equals(portName)) {
            setNativeAlfrescoWrapperEndpointAddress(address);
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
