/**
 * Usage2ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage2;

public class Usage2ServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.usage2.Usage2Service {

    public Usage2ServiceLocator() {
    }


    public Usage2ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public Usage2ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for usage2
    private java.lang.String usage2_address = "http://localhost:8080/edu-sharing/services/usage2";

    public java.lang.String getusage2Address() {
        return usage2_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String usage2WSDDServiceName = "usage2";

    public java.lang.String getusage2WSDDServiceName() {
        return usage2WSDDServiceName;
    }

    public void setusage2WSDDServiceName(java.lang.String name) {
        usage2WSDDServiceName = name;
    }

    public org.edu_sharing.webservices.usage2.Usage2 getusage2() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(usage2_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getusage2(endpoint);
    }

    public org.edu_sharing.webservices.usage2.Usage2 getusage2(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.usage2.Usage2SoapBindingStub _stub = new org.edu_sharing.webservices.usage2.Usage2SoapBindingStub(portAddress, this);
            _stub.setPortName(getusage2WSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setusage2EndpointAddress(java.lang.String address) {
        usage2_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.usage2.Usage2.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.usage2.Usage2SoapBindingStub _stub = new org.edu_sharing.webservices.usage2.Usage2SoapBindingStub(new java.net.URL(usage2_address), this);
                _stub.setPortName(getusage2WSDDServiceName());
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
        if ("usage2".equals(inputPortName)) {
            return getusage2();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Service");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "usage2"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("usage2".equals(portName)) {
            setusage2EndpointAddress(address);
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
