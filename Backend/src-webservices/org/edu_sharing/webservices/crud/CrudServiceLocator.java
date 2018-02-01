/**
 * CrudServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.crud;

public class CrudServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.crud.CrudService {

    public CrudServiceLocator() {
    }


    public CrudServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public CrudServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for crud
    private java.lang.String crud_address = "http://localhost:8080/edu-sharing/services/crud";

    public java.lang.String getcrudAddress() {
        return crud_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String crudWSDDServiceName = "crud";

    public java.lang.String getcrudWSDDServiceName() {
        return crudWSDDServiceName;
    }

    public void setcrudWSDDServiceName(java.lang.String name) {
        crudWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.crud.Crud getcrud() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(crud_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getcrud(endpoint);
    }

    public org.edu_sharing.webservices.crud.Crud getcrud(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.crud.CrudSoapBindingStub _stub = new org.edu_sharing.webservices.crud.CrudSoapBindingStub(portAddress, this);
            _stub.setPortName(getcrudWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setcrudEndpointAddress(java.lang.String address) {
        crud_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.crud.Crud.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.crud.CrudSoapBindingStub _stub = new org.edu_sharing.webservices.crud.CrudSoapBindingStub(new java.net.URL(crud_address), this);
                _stub.setPortName(getcrudWSDDServiceName());
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
        if ("crud".equals(inputPortName)) {
            return getcrud();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "CrudService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "crud"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("crud".equals(portName)) {
            setcrudEndpointAddress(address);
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
