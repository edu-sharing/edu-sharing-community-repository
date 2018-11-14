/**
 * Usage2SoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage2;

public class Usage2SoapBindingSkeleton implements org.edu_sharing.webservices.usage2.Usage2, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.usage2.Usage2 impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "eduRef"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUsagesByEduRef", _params, new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "getUsagesByEduRefReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Result"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "getUsagesByEduRef"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUsagesByEduRef") == null) {
            _myOperations.put("getUsagesByEduRef", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUsagesByEduRef")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("Usage2Exception");
        _fault.setQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage2.Usage2Exception");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Exception"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "eduRef"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteUsage", _params, new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "deleteUsageReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "deleteUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteUsage") == null) {
            _myOperations.put("deleteUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("Usage2Exception");
        _fault.setQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage2.Usage2Exception");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Exception"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "eduRef"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUsage", _params, new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "getUsageReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Result"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "getUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUsage") == null) {
            _myOperations.put("getUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("Usage2Exception");
        _fault.setQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage2.Usage2Exception");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Exception"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "eduRef"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "userMail"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fromUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "toUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "distinctPersons"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "version"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "xmlParams"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setUsage", _params, new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "setUsageReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Result"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "setUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setUsage") == null) {
            _myOperations.put("setUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("Usage2Exception");
        _fault.setQName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage2.Usage2Exception");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Exception"));
        _oper.addFault(_fault);
    }

    public Usage2SoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.usage2.Usage2SoapBindingImpl();
    }

    public Usage2SoapBindingSkeleton(org.edu_sharing.webservices.usage2.Usage2 impl) {
        this.impl = impl;
    }
    public org.edu_sharing.webservices.usage2.Usage2Result[] getUsagesByEduRef(java.lang.String eduRef, java.lang.String user) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception
    {
        org.edu_sharing.webservices.usage2.Usage2Result[] ret = impl.getUsagesByEduRef(eduRef, user);
        return ret;
    }

    public boolean deleteUsage(java.lang.String eduRef, java.lang.String user, java.lang.String lmsId, java.lang.String courseId, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception
    {
        boolean ret = impl.deleteUsage(eduRef, user, lmsId, courseId, resourceId);
        return ret;
    }

    public org.edu_sharing.webservices.usage2.Usage2Result getUsage(java.lang.String eduRef, java.lang.String lmsId, java.lang.String courseId, java.lang.String user, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception
    {
        org.edu_sharing.webservices.usage2.Usage2Result ret = impl.getUsage(eduRef, lmsId, courseId, user, resourceId);
        return ret;
    }

    public org.edu_sharing.webservices.usage2.Usage2Result setUsage(java.lang.String eduRef, java.lang.String user, java.lang.String lmsId, java.lang.String courseId, java.lang.String userMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception
    {
        org.edu_sharing.webservices.usage2.Usage2Result ret = impl.setUsage(eduRef, user, lmsId, courseId, userMail, fromUsed, toUsed, distinctPersons, version, resourceId, xmlParams);
        return ret;
    }

}
