/**
 * UsageSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class UsageSoapBindingSkeleton implements org.edu_sharing.webservices.usage.Usage, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.usage.Usage impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appSessionId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appCurrentUserId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteUsage", _params, new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsageReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteUsage") == null) {
            _myOperations.put("deleteUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUser"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUsage", _params, new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUsage") == null) {
            _myOperations.put("getUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appSessionId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appCurrentUserId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteUsages", _params, new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsagesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsages"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteUsages") == null) {
            _myOperations.put("deleteUsages", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteUsages")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("usageAllowed", _params, new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "usageAllowedReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "usageAllowed"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("usageAllowed") == null) {
            _myOperations.put("usageAllowed", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("usageAllowed")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUser"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUserMail"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fromUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "toUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "distinctPersons"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "version"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "xmlParams"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setUsage", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "setUsage"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setUsage") == null) {
            _myOperations.put("setUsage", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setUsage")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUsageByParentNodeId", _params, new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageByParentNodeIdReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageByParentNodeId"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUsageByParentNodeId") == null) {
            _myOperations.put("getUsageByParentNodeId", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUsageByParentNodeId")).add(_oper);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("UsageException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"));
        _fault.setClassName("org.edu_sharing.webservices.usage.UsageException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"));
        _oper.addFault(_fault);
        _fault = new org.apache.axis.description.FaultDesc();
        _fault.setName("AuthenticationException");
        _fault.setQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"));
        _fault.setClassName("org.edu_sharing.webservices.authentication.AuthenticationException");
        _fault.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        _oper.addFault(_fault);
    }

    public UsageSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.usage.UsageSoapBindingImpl();
    }

    public UsageSoapBindingSkeleton(org.edu_sharing.webservices.usage.Usage impl) {
        this.impl = impl;
    }
    public boolean deleteUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        boolean ret = impl.deleteUsage(repositoryTicket, repositoryUsername, appSessionId, appCurrentUserId, lmsId, courseId, parentNodeId, resourceId);
        return ret;
    }

    public org.edu_sharing.webservices.usage.UsageResult getUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.usage.UsageResult ret = impl.getUsage(repositoryTicket, repositoryUsername, lmsId, courseId, parentNodeId, appUser, resourceId);
        return ret;
    }

    public boolean deleteUsages(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        boolean ret = impl.deleteUsages(repositoryTicket, repositoryUsername, appSessionId, appCurrentUserId, lmsId, courseId);
        return ret;
    }

    public boolean usageAllowed(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        boolean ret = impl.usageAllowed(repositoryTicket, repositoryUsername, nodeId, lmsId, courseId);
        return ret;
    }

    public void setUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String appUserMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        impl.setUsage(repositoryTicket, repositoryUsername, lmsId, courseId, parentNodeId, appUser, appUserMail, fromUsed, toUsed, distinctPersons, version, resourceId, xmlParams);
    }

    public org.edu_sharing.webservices.usage.UsageResult[] getUsageByParentNodeId(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String parentNodeId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException
    {
        org.edu_sharing.webservices.usage.UsageResult[] ret = impl.getUsageByParentNodeId(repositoryTicket, repositoryUsername, parentNodeId);
        return ret;
    }

}
