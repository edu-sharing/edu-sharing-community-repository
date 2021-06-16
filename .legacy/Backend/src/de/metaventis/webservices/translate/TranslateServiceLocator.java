/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */

package de.metaventis.webservices.translate;

import org.apache.log4j.Logger;

public class TranslateServiceLocator extends org.apache.axis.client.Service implements de.metaventis.webservices.translate.TranslateService {

	Logger logger = Logger.getLogger(TranslateServiceLocator.class);
	
    public TranslateServiceLocator() {
    }


    public TranslateServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public TranslateServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for Translate
    private java.lang.String Translate_address = "http://localhost:8080/WebServiceProject/services/Translate";

    public java.lang.String getTranslateAddress() {
        return Translate_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String TranslateWSDDServiceName = "Translate";

    public java.lang.String getTranslateWSDDServiceName() {
        return TranslateWSDDServiceName;
    }

    public void setTranslateWSDDServiceName(java.lang.String name) {
        TranslateWSDDServiceName = name;
    }

    public de.metaventis.webservices.translate.Translate getTranslate() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(Translate_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getTranslate(endpoint);
    }

    public de.metaventis.webservices.translate.Translate getTranslate(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            de.metaventis.webservices.translate.TranslateSoapBindingStub _stub = new de.metaventis.webservices.translate.TranslateSoapBindingStub(portAddress, this);
            _stub.setPortName(getTranslateWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setTranslateEndpointAddress(java.lang.String address) {
        Translate_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (de.metaventis.webservices.translate.Translate.class.isAssignableFrom(serviceEndpointInterface)) {
                de.metaventis.webservices.translate.TranslateSoapBindingStub _stub = new de.metaventis.webservices.translate.TranslateSoapBindingStub(new java.net.URL(Translate_address), this);
                _stub.setPortName(getTranslateWSDDServiceName());
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
        if ("Translate".equals(inputPortName)) {
            return getTranslate();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    
    
    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://translate.webservices.metaventis.de", "TranslateService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://translate.webservices.metaventis.de", "Translate"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("Translate".equals(portName)) {
            setTranslateEndpointAddress(address);
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
