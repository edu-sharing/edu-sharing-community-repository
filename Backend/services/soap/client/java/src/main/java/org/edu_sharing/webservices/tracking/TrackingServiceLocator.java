/**
 * TrackingServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.tracking;

public class TrackingServiceLocator extends org.apache.axis.client.Service implements org.edu_sharing.webservices.tracking.TrackingService {

    public TrackingServiceLocator() {
    }


    public TrackingServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public TrackingServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for Tracking
    private java.lang.String Tracking_address = "http://localhost:8080/edu-sharing-analytics/services/Tracking";

    public java.lang.String getTrackingAddress() {
        return Tracking_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String TrackingWSDDServiceName = "Tracking";

    public java.lang.String getTrackingWSDDServiceName() {
        return TrackingWSDDServiceName;
    }

    public void setTrackingWSDDServiceName(java.lang.String name) {
        TrackingWSDDServiceName = name;
    }

    public org.edu_sharing.webservices.tracking.Tracking getTracking() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(Tracking_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getTracking(endpoint);
    }

    public org.edu_sharing.webservices.tracking.Tracking getTracking(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.edu_sharing.webservices.tracking.TrackingSoapBindingStub _stub = new org.edu_sharing.webservices.tracking.TrackingSoapBindingStub(portAddress, this);
            _stub.setPortName(getTrackingWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setTrackingEndpointAddress(java.lang.String address) {
        Tracking_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.edu_sharing.webservices.tracking.Tracking.class.isAssignableFrom(serviceEndpointInterface)) {
                org.edu_sharing.webservices.tracking.TrackingSoapBindingStub _stub = new org.edu_sharing.webservices.tracking.TrackingSoapBindingStub(new java.net.URL(Tracking_address), this);
                _stub.setPortName(getTrackingWSDDServiceName());
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
        if ("Tracking".equals(inputPortName)) {
            return getTracking();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "TrackingService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "Tracking"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("Tracking".equals(portName)) {
            setTrackingEndpointAddress(address);
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
