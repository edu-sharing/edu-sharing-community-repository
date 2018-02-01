/**
 * FolderTemplateSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.foldertemplate;

public class FolderTemplateSoapBindingSkeleton implements org.edu_sharing.webservices.foldertemplate.FolderTemplate, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.foldertemplate.FolderTemplate impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "template"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "group"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "folderid"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("process", _params, new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "processReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://foldertemplate.webservices.edu_sharing.org", "process"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("process") == null) {
            _myOperations.put("process", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("process")).add(_oper);
    }

    public FolderTemplateSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.foldertemplate.FolderTemplateSoapBindingImpl();
    }

    public FolderTemplateSoapBindingSkeleton(org.edu_sharing.webservices.foldertemplate.FolderTemplate impl) {
        this.impl = impl;
    }
    public java.lang.String process(java.lang.String template, java.lang.String group, java.lang.String folderid) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.process(template, group, folderid);
        return ret;
    }

}
