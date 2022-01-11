/**
 * CrudSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.crud;

public class CrudSoapBindingSkeleton implements org.edu_sharing.webservices.crud.Crud, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.crud.Crud impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "nodeType"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "repositoryId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "properties"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "content"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "icon"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("update", _params, new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "updateReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "update"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("update") == null) {
            _myOperations.put("update", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("update")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "nodeType"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "repositoryId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "properties"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "content"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "icon"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("create", _params, new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "createReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://crud.webservices.edu_sharing.org", "create"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("create") == null) {
            _myOperations.put("create", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("create")).add(_oper);
    }

    public CrudSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.crud.CrudSoapBindingImpl();
    }

    public CrudSoapBindingSkeleton(org.edu_sharing.webservices.crud.Crud impl) {
        this.impl = impl;
    }
    public java.lang.String update(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.lang.String nodeId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.update(username, ticket, nodeType, repositoryId, nodeId, properties, content, icon);
        return ret;
    }

    public java.lang.String create(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.create(username, ticket, nodeType, repositoryId, properties, content, icon);
        return ret;
    }

}
