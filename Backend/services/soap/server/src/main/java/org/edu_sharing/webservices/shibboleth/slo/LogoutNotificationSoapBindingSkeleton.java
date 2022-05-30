/**
 * LogoutNotificationSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.shibboleth.slo;

public class LogoutNotificationSoapBindingSkeleton implements org.edu_sharing.webservices.shibboleth.slo.LogoutNotification, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.shibboleth.slo.LogoutNotification impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "sessionID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("logoutNotification", _params, new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "logoutNotificationReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "OKType"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://slo.shibboleth.webservices.edu_sharing.org", "logoutNotification"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("logoutNotification") == null) {
            _myOperations.put("logoutNotification", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("logoutNotification")).add(_oper);
    }

    public LogoutNotificationSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.shibboleth.slo.LogoutNotificationSoapBindingImpl();
    }

    public LogoutNotificationSoapBindingSkeleton(org.edu_sharing.webservices.shibboleth.slo.LogoutNotification impl) {
        this.impl = impl;
    }
    public org.edu_sharing.webservices.shibboleth.slo.OKType logoutNotification(java.lang.String[] sessionID) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.shibboleth.slo.OKType ret = impl.logoutNotification(sessionID);
        return ret;
    }

}
