/**
 * FolderTemplateServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.foldertemplate;

public class FolderTemplateServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.foldertemplate.FolderTemplateService {

    public FolderTemplateServiceLocator() {
    }


    public FolderTemplateServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public FolderTemplateServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for FolderTemplate
    private java.lang.String FolderTemplate_address = "http://localhost:8080/edu-sharing/services/FolderTemplate";

    public java.lang.String getFolderTemplateAddress() {
        return FolderTemplate_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String FolderTemplateWSDDServiceName = "FolderTemplate";

    public java.lang.String getFolderTemplateWSDDServiceName() {
        return FolderTemplateWSDDServiceName;
    }

    public void setFolderTemplateWSDDServiceName(java.lang.String name) {
        FolderTemplateWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.foldertemplate.FolderTemplate getFolderTemplate() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FolderTemplate_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getFolderTemplate(endpoint);
    }

    public org.edu_sharing.webservices.foldertemplate.FolderTemplate getFolderTemplate(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.foldertemplate.FolderTemplateSoapBindingStub _stub = new org.edu_sharing.webservices.foldertemplate.FolderTemplateSoapBindingStub(portAddress, this);
            _stub.setPortName(getFolderTemplateWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setFolderTemplateEndpointAddress(java.lang.String address) {
        FolderTemplate_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.foldertemplate.FolderTemplate.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.foldertemplate.FolderTemplateSoapBindingStub _stub = new org.edu_sharing.webservices.foldertemplate.FolderTemplateSoapBindingStub(new java.net.URL(FolderTemplate_address), this);
                _stub.setPortName(getFolderTemplateWSDDServiceName());
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
        if ("FolderTemplate".equals(inputPortName)) {
            return getFolderTemplate();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "FolderTemplateService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "FolderTemplate"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("FolderTemplate".equals(portName)) {
            setFolderTemplateEndpointAddress(address);
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
