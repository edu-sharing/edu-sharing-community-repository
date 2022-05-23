/**
 * AuthbyappSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authbyapp;

public class AuthbyappSoapBindingSkeleton implements org.edu_sharing.webservices.authbyapp.AuthByApp, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.authbyapp.AuthByApp impl;
    private static java.util.Map _myOperations = new java.util.Hashtable();
    private static java.util.Collection _myOperationsList = new java.util.ArrayList();

    /**
    * Returns List of OperationDesc objects with this name
    */
    public static java.util.List getOperationDescByName(java.lang.String methodName) {
        return (java.util.List)_myOperations.get(methodName);
    }

    /**
    * Returns Collection of OperationDescs
    */
    public static java.util.Collection getOperationDescs() {
        return _myOperationsList;
    }

    static {
        org.apache.axis.description.OperationDesc _oper;
        org.apache.axis.description.FaultDesc _fault;
        org.apache.axis.description.ParameterDesc [] _params;
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkTicket", _params, new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "checkTicketReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "checkTicket"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkTicket") == null) {
            _myOperations.put("checkTicket", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkTicket")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "applicationId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "ssoData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"), org.edu_sharing.webservices.types.KeyValue[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("authenticateByTrustedApp", _params, new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "authenticateByTrustedAppReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "authenticateByTrustedApp"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("authenticateByTrustedApp") == null) {
            _myOperations.put("authenticateByTrustedApp", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("authenticateByTrustedApp")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
    }

    public AuthbyappSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.authbyapp.AuthbyappSoapBindingImpl();
    }

    public AuthbyappSoapBindingSkeleton(org.edu_sharing.webservices.authbyapp.AuthByApp impl) {
        this.impl = impl;
    }
    public boolean checkTicket(java.lang.String ticket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        boolean ret = impl.checkTicket(ticket);
        return ret;
    }

    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, org.edu_sharing.webservices.types.KeyValue[] ssoData) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.authentication.AuthenticationResult ret = impl.authenticateByTrustedApp(applicationId, ssoData);
        return ret;
    }

}
