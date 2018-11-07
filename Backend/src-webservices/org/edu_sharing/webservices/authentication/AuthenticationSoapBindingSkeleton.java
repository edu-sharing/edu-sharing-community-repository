/**
 * AuthenticationSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticationSoapBindingSkeleton implements org.edu_sharing.webservices.authentication.Authentication, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.authentication.Authentication impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkTicket", _params, new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "checkTicketReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "checkTicket"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkTicket") == null) {
            _myOperations.put("checkTicket", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkTicket")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "applicationId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "email"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "createUser"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("authenticateByApp", _params, new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByAppReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByApp"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("authenticateByApp") == null) {
            _myOperations.put("authenticateByApp", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("authenticateByApp")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "applicationId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "ssoData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"), org.edu_sharing.webservices.types.KeyValue[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("authenticateByTrustedApp", _params, new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByTrustedAppReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByTrustedApp"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("authenticateByTrustedApp") == null) {
            _myOperations.put("authenticateByTrustedApp", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("authenticateByTrustedApp")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "proxyTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("authenticateByCAS", _params, new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByCASReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByCAS"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("authenticateByCAS") == null) {
            _myOperations.put("authenticateByCAS", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("authenticateByCAS")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "password"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("authenticate", _params, new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticate"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("authenticate") == null) {
            _myOperations.put("authenticate", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("authenticate")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
    }

    public AuthenticationSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.authentication.AuthenticationSoapBindingImpl();
    }

    public AuthenticationSoapBindingSkeleton(org.edu_sharing.webservices.authentication.Authentication impl) {
        this.impl = impl;
    }
    public boolean checkTicket(java.lang.String username, java.lang.String ticket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        boolean ret = impl.checkTicket(username, ticket);
        return ret;
    }

    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByApp(java.lang.String applicationId, java.lang.String username, java.lang.String email, java.lang.String ticket, boolean createUser) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.authentication.AuthenticationResult ret = impl.authenticateByApp(applicationId, username, email, ticket, createUser);
        return ret;
    }

    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, java.lang.String ticket, org.edu_sharing.webservices.types.KeyValue[] ssoData) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.authentication.AuthenticationResult ret = impl.authenticateByTrustedApp(applicationId, ticket, ssoData);
        return ret;
    }

    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByCAS(java.lang.String username, java.lang.String proxyTicket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.authentication.AuthenticationResult ret = impl.authenticateByCAS(username, proxyTicket);
        return ret;
    }

    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticate(java.lang.String username, java.lang.String password) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.authentication.AuthenticationResult ret = impl.authenticate(username, password);
        return ret;
    }

}
