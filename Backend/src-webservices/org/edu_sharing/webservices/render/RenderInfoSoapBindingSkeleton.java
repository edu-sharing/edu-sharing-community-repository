/**
 * RenderInfoSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.render;

public class RenderInfoSoapBindingSkeleton implements org.edu_sharing.webservices.render.RenderInfo, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.render.RenderInfo impl;
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
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "userName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "version"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
        };
        _oper = new org.apache.axis.description.OperationDesc("getRenderInfoRepo", _params, new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "getRenderInfoRepoReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "RenderInfoResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "getRenderInfoRepo"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getRenderInfoRepo") == null) {
            _myOperations.put("getRenderInfoRepo", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getRenderInfoRepo")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "userName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "version"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false),
        };
        _oper = new org.apache.axis.description.OperationDesc("getRenderInfoLMS", _params, new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "getRenderInfoLMSReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "RenderInfoResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "getRenderInfoLMS"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getRenderInfoLMS") == null) {
            _myOperations.put("getRenderInfoLMS", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getRenderInfoLMS")).add(_oper);
    }

    public RenderInfoSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.render.RenderInfoSoapBindingImpl();
    }

    public RenderInfoSoapBindingSkeleton(org.edu_sharing.webservices.render.RenderInfo impl) {
        this.impl = impl;
    }
    public org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoRepo(java.lang.String userName, java.lang.String nodeId, java.lang.String version) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.render.RenderInfoResult ret = impl.getRenderInfoRepo(userName, nodeId, version);
        return ret;
    }

    public org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoLMS(java.lang.String userName, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId, java.lang.String resourceId, java.lang.String version) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.render.RenderInfoResult ret = impl.getRenderInfoLMS(userName, nodeId, lmsId, courseId, resourceId, version);
        return ret;
    }

}
