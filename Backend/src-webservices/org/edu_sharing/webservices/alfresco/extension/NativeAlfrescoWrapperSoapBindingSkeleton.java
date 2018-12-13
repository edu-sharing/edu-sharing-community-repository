/**
 * NativeAlfrescoWrapperSoapBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class NativeAlfrescoWrapperSoapBindingSkeleton implements org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper, org.apache.axis.wsdl.Skeleton {
    private org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "value"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setProperty", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setProperty"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setProperty") == null) {
            _myOperations.put("setProperty", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setProperty")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeIdentifier"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getProperty", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertyReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getProperty"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getProperty") == null) {
            _myOperations.put("getProperty", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getProperty")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getProperties", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getProperties"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getProperties") == null) {
            _myOperations.put("getProperties", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getProperties")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getPermissions", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPermissionsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACL"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getPermissions") == null) {
            _myOperations.put("getPermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getPermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getType", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getTypeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getType"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getType") == null) {
            _myOperations.put("getType", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getType")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getPath", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPathReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPath"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getPath") == null) {
            _myOperations.put("getPath", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getPath")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUserInfo", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserInfoReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserInfo"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUserInfo") == null) {
            _myOperations.put("getUserInfo", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUserInfo")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchCriterias"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchCriteria"), org.edu_sharing.webservices.alfresco.extension.SearchCriteria[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "metadatasetId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "start"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfResults"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("search", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "search"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("search") == null) {
            _myOperations.put("search", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("search")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "fromId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeNode", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeNode") == null) {
            _myOperations.put("removeNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "toNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "copyChildren"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("copyNode", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "copyNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("copyNode") == null) {
            _myOperations.put("copyNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("copyNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildren", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildren"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildren") == null) {
            _myOperations.put("getChildren", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildren")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "value"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChild", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChild"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChild") == null) {
            _myOperations.put("getChild", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChild")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getPropertiesExt", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesExtReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesExt"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getPropertiesExt") == null) {
            _myOperations.put("getPropertiesExt", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getPropertiesExt")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "query"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "startIdx"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfresults"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettesMinCount"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettesLimit"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchSolr", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchSolrReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchSolr"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchSolr") == null) {
            _myOperations.put("searchSolr", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchSolr")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissionsOnChild"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildrenCheckPermissions", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenCheckPermissionsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenCheckPermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildrenCheckPermissions") == null) {
            _myOperations.put("getChildrenCheckPermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildrenCheckPermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeTypeString"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childAssociation"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "props"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createNode", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createNodeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createNode") == null) {
            _myOperations.put("createNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeTypeString"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childAssociation"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "props"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createNodeAtomicValues", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createNodeAtomicValuesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createNodeAtomicValues"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createNodeAtomicValues") == null) {
            _myOperations.put("createNodeAtomicValues", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createNodeAtomicValues")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "properties"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("updateNodeAtomicValues", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "updateNodeAtomicValues"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("updateNodeAtomicValues") == null) {
            _myOperations.put("updateNodeAtomicValues", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("updateNodeAtomicValues")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("isAdmin", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isAdminReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isAdmin"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("isAdmin") == null) {
            _myOperations.put("isAdmin", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("isAdmin")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasPermissions", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissionsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasPermissions") == null) {
            _myOperations.put("hasPermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasPermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasPermissionsSimple", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissionsSimpleReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissionsSimple"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasPermissionsSimple") == null) {
            _myOperations.put("hasPermissionsSimple", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasPermissionsSimple")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "properties"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("updateNode", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "updateNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("updateNode") == null) {
            _myOperations.put("updateNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("updateNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getCompanyHomeNodeId", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getCompanyHomeNodeIdReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getCompanyHomeNodeId"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getCompanyHomeNodeId") == null) {
            _myOperations.put("getCompanyHomeNodeId", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getCompanyHomeNodeId")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getPropertiesSimple", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesSimpleReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesSimple"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getPropertiesSimple") == null) {
            _myOperations.put("getPropertiesSimple", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getPropertiesSimple")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "store"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "luceneQuery"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permission"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchNodeIds", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodeIdsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodeIds"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchNodeIds") == null) {
            _myOperations.put("searchNodeIds", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchNodeIds")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "store"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "luceneQuery"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permission"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "propertiesToReturn"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchNodes", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "RepositoryNode"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodes"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchNodes") == null) {
            _myOperations.put("searchNodes", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchNodes")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("validateTicket", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "validateTicketReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "validateTicket"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("validateTicket") == null) {
            _myOperations.put("validateTicket", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("validateTicket")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getVersionHistory", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getVersionHistoryReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "RepositoryNode"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getVersionHistory"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getVersionHistory") == null) {
            _myOperations.put("getVersionHistory", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getVersionHistory")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeIdentifier"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getPreviewUrl", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPreviewUrlReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "GetPreviewResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPreviewUrl"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getPreviewUrl") == null) {
            _myOperations.put("getPreviewUrl", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getPreviewUrl")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "emails"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "expiryDate"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createShare", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createShare"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createShare") == null) {
            _myOperations.put("createShare", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createShare")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getShares", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getSharesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "Share"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getShares"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getShares") == null) {
            _myOperations.put("getShares", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getShares")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("isOwner", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isOwnerReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isOwner"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("isOwner") == null) {
            _myOperations.put("isOwner", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("isOwner")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getMetadataSets", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getMetadataSetsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getMetadataSets"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getMetadataSets") == null) {
            _myOperations.put("getMetadataSets", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getMetadataSets")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchWord"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "eduGroupNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "from"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfResults"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("findGroups", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findGroupsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findGroups"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("findGroups") == null) {
            _myOperations.put("findGroups", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("findGroups")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchProps"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "KeyValue"), org.edu_sharing.webservices.alfresco.extension.KeyValue[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "eduGroupNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "from"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfResults"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("findUsers", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findUsersReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchResult"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findUsers"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("findUsers") == null) {
            _myOperations.put("findUsers", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("findUsers")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getEduGroupContextOfNode", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getEduGroupContextOfNodeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "KeyValue"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getEduGroupContextOfNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getEduGroupContextOfNode") == null) {
            _myOperations.put("getEduGroupContextOfNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getEduGroupContextOfNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "toolPermission"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasToolPermission", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasToolPermissionReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasToolPermission"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasToolPermission") == null) {
            _myOperations.put("hasToolPermission", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasToolPermission")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "content"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "fileName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setUserDefinedPreview", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setUserDefinedPreview"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setUserDefinedPreview") == null) {
            _myOperations.put("setUserDefinedPreview", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setUserDefinedPreview")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeUserDefinedPreview", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeUserDefinedPreview"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeUserDefinedPreview") == null) {
            _myOperations.put("removeUserDefinedPreview", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeUserDefinedPreview")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "filename"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("guessMimetype", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "guessMimetypeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "guessMimetype"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("guessMimetype") == null) {
            _myOperations.put("guessMimetype", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("guessMimetype")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "luceneString"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "limit"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchNodeIdsLimit", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodeIdsLimitReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchNodeIdsLimit"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchNodeIdsLimit") == null) {
            _myOperations.put("searchNodeIdsLimit", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchNodeIdsLimit")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aspect"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeAspect", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeAspect"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeAspect") == null) {
            _myOperations.put("removeAspect", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeAspect")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeGlobalAspectFromGroup", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeGlobalAspectFromGroup"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeGlobalAspectFromGroup") == null) {
            _myOperations.put("removeGlobalAspectFromGroup", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeGlobalAspectFromGroup")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getNotifyList", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getNotifyListReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "Notify"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getNotifyList"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getNotifyList") == null) {
            _myOperations.put("getNotifyList", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getNotifyList")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "verLbl"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("revertVersion", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "revertVersion"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("revertVersion") == null) {
            _myOperations.put("revertVersion", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("revertVersion")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "properties"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createVersion", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createVersion"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createVersion") == null) {
            _myOperations.put("createVersion", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createVersion")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasAllPermissions", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasAllPermissionsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasAllPermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasAllPermissions") == null) {
            _myOperations.put("hasAllPermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasAllPermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasAllPermissionsExt", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasAllPermissionsExtReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasAllPermissionsExt"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasAllPermissionsExt") == null) {
            _myOperations.put("hasAllPermissionsExt", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasAllPermissionsExt")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getHomeFolderID", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getHomeFolderIDReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getHomeFolderID"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getHomeFolderID") == null) {
            _myOperations.put("getHomeFolderID", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getHomeFolderID")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aces"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACE"), org.edu_sharing.repository.client.rpc.ACE[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("addPermissionACEs", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "addPermissionACEs"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("addPermissionACEs") == null) {
            _myOperations.put("addPermissionACEs", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("addPermissionACEs")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aces"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACE"), org.edu_sharing.repository.client.rpc.ACE[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removePermissionACEs", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removePermissionACEs"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removePermissionACEs") == null) {
            _myOperations.put("removePermissionACEs", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removePermissionACEs")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "_authority"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "changeInherit"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "inheritPermission"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setPermissionsBasic", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setPermissionsBasic"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setPermissionsBasic") == null) {
            _myOperations.put("setPermissionsBasic", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setPermissionsBasic")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "_authority"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "_permissions"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removePermissions", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removePermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removePermissions") == null) {
            _myOperations.put("removePermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removePermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "actionName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "actionId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parameters"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "async"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("executeAction", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "executeAction"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("executeAction") == null) {
            _myOperations.put("executeAction", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("executeAction")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "fromID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "toID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createAssociation", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createAssociation"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createAssociation") == null) {
            _myOperations.put("createAssociation", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createAssociation")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "from"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "to"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "assocType"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "assocName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("createChildAssociation", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "createChildAssociation"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("createChildAssociation") == null) {
            _myOperations.put("createChildAssociation", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("createChildAssociation")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "newParentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childAssocType"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("moveNode", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "moveNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("moveNode") == null) {
            _myOperations.put("moveNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("moveNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "fromID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "toID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeAssociation", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeAssociation"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeAssociation") == null) {
            _myOperations.put("removeAssociation", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeAssociation")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeParentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeRelationsForNode", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeRelationsForNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeRelationsForNode") == null) {
            _myOperations.put("removeRelationsForNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeRelationsForNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeRelations", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeRelations"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeRelations") == null) {
            _myOperations.put("removeRelations", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeRelations")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aspect"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("addAspect", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "addAspect"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("addAspect") == null) {
            _myOperations.put("addAspect", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("addAspect")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getGroupFolderId", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupFolderIdReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupFolderId"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getGroupFolderId") == null) {
            _myOperations.put("getGroupFolderId", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getGroupFolderId")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getRepositoryRoot", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getRepositoryRootReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getRepositoryRoot"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getRepositoryRoot") == null) {
            _myOperations.put("getRepositoryRoot", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getRepositoryRoot")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "props"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildenByProps", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildenByPropsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildenByProps"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildenByProps") == null) {
            _myOperations.put("getChildenByProps", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildenByProps")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildrenByType", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenByTypeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenByType"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildrenByType") == null) {
            _myOperations.put("getChildrenByType", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildrenByType")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeString"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildrenByAssociation", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenByAssociationReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenByAssociation"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildrenByAssociation") == null) {
            _myOperations.put("getChildrenByAssociation", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildrenByAssociation")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "primary"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"), boolean.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getParents", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getParentsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getParents"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getParents") == null) {
            _myOperations.put("getParents", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getParents")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAssocNode", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAssocNodeReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAssocNode"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAssocNode") == null) {
            _myOperations.put("getAssocNode", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAssocNode")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "props"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"), java.util.HashMap.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildRecursive", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildRecursiveReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildRecursive"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildRecursive") == null) {
            _myOperations.put("getChildRecursive", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildRecursive")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getChildrenRecursive", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenRecursiveReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenRecursive"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getChildrenRecursive") == null) {
            _myOperations.put("getChildrenRecursive", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getChildrenRecursive")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAssociationNodeIds", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAssociationNodeIdsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAssociationNodeIds"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAssociationNodeIds") == null) {
            _myOperations.put("getAssociationNodeIds", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAssociationNodeIds")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getUserNames", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserNamesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserNames"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUserNames") == null) {
            _myOperations.put("getUserNames", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUserNames")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userNames"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getUserDetails", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserDetailsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "UserDetails"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getUserDetails"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getUserDetails") == null) {
            _myOperations.put("getUserDetails", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getUserDetails")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userDetails"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "UserDetails"), org.edu_sharing.webservices.alfresco.extension.UserDetails[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setUserDetails", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setUserDetails"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setUserDetails") == null) {
            _myOperations.put("setUserDetails", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setUserDetails")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userNames"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteUser", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "deleteUser"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteUser") == null) {
            _myOperations.put("deleteUser", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteUser")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
        };
        _oper = new org.apache.axis.description.OperationDesc("getGroupNames", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupNamesReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupNames"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getGroupNames") == null) {
            _myOperations.put("getGroupNames", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getGroupNames")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupNames"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getGroupDetails", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupDetailsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "GroupDetails"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupDetails"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getGroupDetails") == null) {
            _myOperations.put("getGroupDetails", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getGroupDetails")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupDetails"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "GroupDetails"), org.edu_sharing.webservices.alfresco.extension.GroupDetails.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setGroupDetails", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setGroupDetailsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "GroupDetails"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setGroupDetails"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setGroupDetails") == null) {
            _myOperations.put("setGroupDetails", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setGroupDetails")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupNames"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteGroup", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "deleteGroup"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteGroup") == null) {
            _myOperations.put("deleteGroup", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteGroup")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getMemberships", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getMembershipsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getMemberships"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getMemberships") == null) {
            _myOperations.put("getMemberships", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getMemberships")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "members"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("addMemberships", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "addMemberships"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("addMemberships") == null) {
            _myOperations.put("addMemberships", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("addMemberships")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "members"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeMemberships", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeMemberships"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeMemberships") == null) {
            _myOperations.put("removeMemberships", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeMemberships")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupNames"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeAllMemberships", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeAllMemberships"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeAllMemberships") == null) {
            _myOperations.put("removeAllMemberships", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeAllMemberships")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "type"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentType"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("isSubOf", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isSubOfReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "isSubOf"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("isSubOf") == null) {
            _myOperations.put("isSubOf", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("isSubOf")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "contentProp"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("hasContent", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasContentReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasContent"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("hasContent") == null) {
            _myOperations.put("hasContent", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("hasContent")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "content"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"), byte[].class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "mimetype"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "encoding"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("writeContent", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "writeContent"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("writeContent") == null) {
            _myOperations.put("writeContent", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("writeContent")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "userName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "password"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setUserPassword", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setUserPassword"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setUserPassword") == null) {
            _myOperations.put("setUserPassword", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setUserPassword")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "ticket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("invalideTicket", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "invalideTicket"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("invalideTicket") == null) {
            _myOperations.put("invalideTicket", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("invalideTicket")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "groupName"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "folderId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("bindEduGroupFolder", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "bindEduGroupFolder"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("bindEduGroupFolder") == null) {
            _myOperations.put("bindEduGroupFolder", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("bindEduGroupFolder")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "path"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("findNodeByPath", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findNodeByPathReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "findNodeByPath"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("findNodeByPath") == null) {
            _myOperations.put("findNodeByPath", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("findNodeByPath")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAspects", _params, new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAspectsReturn"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAspects"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAspects") == null) {
            _myOperations.put("getAspects", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAspects")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "username"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setOwner", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setOwner"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setOwner") == null) {
            _myOperations.put("setOwner", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setOwner")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aces"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACE"), org.edu_sharing.repository.client.rpc.ACE[].class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setPermissions", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "setPermissions"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setPermissions") == null) {
            _myOperations.put("setPermissions", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setPermissions")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("removeChild", _params, null);
        _oper.setElementQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "removeChild"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("removeChild") == null) {
            _myOperations.put("removeChild", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("removeChild")).add(_oper);
    }

    public NativeAlfrescoWrapperSoapBindingSkeleton() {
        this.impl = new org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperSoapBindingImpl();
    }

    public NativeAlfrescoWrapperSoapBindingSkeleton(org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper impl) {
        this.impl = impl;
    }
    public void setProperty(java.lang.String nodeId, java.lang.String property, java.lang.String value) throws java.rmi.RemoteException
    {
        impl.setProperty(nodeId, property, value);
    }

    public java.lang.String getProperty(java.lang.String storeProtocol, java.lang.String storeIdentifier, java.lang.String nodeId, java.lang.String property) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getProperty(storeProtocol, storeIdentifier, nodeId, property);
        return ret;
    }

    public java.util.HashMap getProperties(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getProperties(nodeId);
        return ret;
    }

    public org.edu_sharing.repository.client.rpc.ACL getPermissions(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        org.edu_sharing.repository.client.rpc.ACL ret = impl.getPermissions(nodeId);
        return ret;
    }

    public java.lang.String getType(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getType(nodeId);
        return ret;
    }

    public java.lang.String getPath(java.lang.String nodeID) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getPath(nodeID);
        return ret;
    }

    public java.util.HashMap getUserInfo(java.lang.String userName) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getUserInfo(userName);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.SearchResult search(org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias, java.lang.String metadatasetId, int start, int nrOfResults, java.lang.String[] facettes) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.SearchResult ret = impl.search(searchCriterias, metadatasetId, start, nrOfResults, facettes);
        return ret;
    }

    public void removeNode(java.lang.String nodeId, java.lang.String fromId) throws java.rmi.RemoteException
    {
        impl.removeNode(nodeId, fromId);
    }

    public void copyNode(java.lang.String nodeId, java.lang.String toNodeId, boolean copyChildren) throws java.rmi.RemoteException
    {
        impl.copyNode(nodeId, toNodeId, copyChildren);
    }

    public java.util.HashMap getChildren(java.lang.String parentID, java.lang.String type) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildren(parentID, type);
        return ret;
    }

    public java.util.HashMap getChild(java.lang.String parentId, java.lang.String type, java.lang.String property, java.lang.String value) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChild(parentId, type, property, value);
        return ret;
    }

    public java.util.HashMap getPropertiesExt(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getPropertiesExt(storeProtocol, storeId, nodeId);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.SearchResult searchSolr(java.lang.String query, int startIdx, int nrOfresults, java.lang.String[] facettes, int facettesMinCount, int facettesLimit) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.SearchResult ret = impl.searchSolr(query, startIdx, nrOfresults, facettes, facettesMinCount, facettesLimit);
        return ret;
    }

    public java.util.HashMap getChildrenCheckPermissions(java.lang.String parentID, java.lang.String[] permissionsOnChild) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildrenCheckPermissions(parentID, permissionsOnChild);
        return ret;
    }

    public java.lang.String createNode(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.createNode(parentID, nodeTypeString, childAssociation, props);
        return ret;
    }

    public java.lang.String createNodeAtomicValues(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.createNodeAtomicValues(parentID, nodeTypeString, childAssociation, props);
        return ret;
    }

    public void updateNodeAtomicValues(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException
    {
        impl.updateNodeAtomicValues(nodeId, properties);
    }

    public boolean isAdmin(java.lang.String username) throws java.rmi.RemoteException
    {
        boolean ret = impl.isAdmin(username);
        return ret;
    }

    public java.util.HashMap hasPermissions(java.lang.String userId, java.lang.String[] permissions, java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.hasPermissions(userId, permissions, nodeId);
        return ret;
    }

    public boolean hasPermissionsSimple(java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException
    {
        boolean ret = impl.hasPermissionsSimple(nodeId, permissions);
        return ret;
    }

    public void updateNode(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException
    {
        impl.updateNode(nodeId, properties);
    }

    public java.lang.String getCompanyHomeNodeId() throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getCompanyHomeNodeId();
        return ret;
    }

    public java.util.HashMap getPropertiesSimple(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getPropertiesSimple(nodeId);
        return ret;
    }

    public java.lang.String[] searchNodeIds(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.searchNodeIds(store, luceneQuery, permission);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] searchNodes(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission, java.lang.String[] propertiesToReturn) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] ret = impl.searchNodes(store, luceneQuery, permission, propertiesToReturn);
        return ret;
    }

    public java.lang.String validateTicket(java.lang.String ticket) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.validateTicket(ticket);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] getVersionHistory(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] ret = impl.getVersionHistory(nodeId);
        return ret;
    }

    public org.edu_sharing.service.nodeservice.model.GetPreviewResult getPreviewUrl(java.lang.String storeProtocol, java.lang.String storeIdentifier, java.lang.String nodeId) throws java.rmi.RemoteException
    {
    	org.edu_sharing.service.nodeservice.model.GetPreviewResult ret = impl.getPreviewUrl(storeProtocol, storeIdentifier, nodeId);
        return ret;
    }

    public void createShare(java.lang.String nodeId, java.lang.String[] emails, long expiryDate) throws java.rmi.RemoteException
    {
        impl.createShare(nodeId, emails, expiryDate);
    }

    public org.edu_sharing.repository.client.rpc.Share[] getShares(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        org.edu_sharing.repository.client.rpc.Share[] ret = impl.getShares(nodeId);
        return ret;
    }

    public boolean isOwner(java.lang.String nodeId, java.lang.String user) throws java.rmi.RemoteException
    {
        boolean ret = impl.isOwner(nodeId, user);
        return ret;
    }

    public java.lang.String[] getMetadataSets() throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getMetadataSets();
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.SearchResult findGroups(java.lang.String searchWord, java.lang.String eduGroupNodeId, int from, int nrOfResults) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.SearchResult ret = impl.findGroups(searchWord, eduGroupNodeId, from, nrOfResults);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.SearchResult findUsers(org.edu_sharing.webservices.alfresco.extension.KeyValue[] searchProps, java.lang.String eduGroupNodeId, int from, int nrOfResults) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.SearchResult ret = impl.findUsers(searchProps, eduGroupNodeId, from, nrOfResults);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.KeyValue[] getEduGroupContextOfNode(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.KeyValue[] ret = impl.getEduGroupContextOfNode(nodeId);
        return ret;
    }

    public boolean hasToolPermission(java.lang.String toolPermission) throws java.rmi.RemoteException
    {
        boolean ret = impl.hasToolPermission(toolPermission);
        return ret;
    }

    public void setUserDefinedPreview(java.lang.String nodeId, byte[] content, java.lang.String fileName) throws java.rmi.RemoteException
    {
        impl.setUserDefinedPreview(nodeId, content, fileName);
    }

    public void removeUserDefinedPreview(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        impl.removeUserDefinedPreview(nodeId);
    }

    public java.lang.String guessMimetype(java.lang.String filename) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.guessMimetype(filename);
        return ret;
    }

    public java.lang.String[] searchNodeIdsLimit(java.lang.String luceneString, int limit) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.searchNodeIdsLimit(luceneString, limit);
        return ret;
    }

    public void removeAspect(java.lang.String nodeId, java.lang.String aspect) throws java.rmi.RemoteException
    {
        impl.removeAspect(nodeId, aspect);
    }

    public void removeGlobalAspectFromGroup(java.lang.String groupNodeId) throws java.rmi.RemoteException
    {
        impl.removeGlobalAspectFromGroup(groupNodeId);
    }

    public org.edu_sharing.repository.client.rpc.Notify[] getNotifyList(java.lang.String nodeId) throws java.rmi.RemoteException
    {
        org.edu_sharing.repository.client.rpc.Notify[] ret = impl.getNotifyList(nodeId);
        return ret;
    }

    public void revertVersion(java.lang.String nodeId, java.lang.String verLbl) throws java.rmi.RemoteException
    {
        impl.revertVersion(nodeId, verLbl);
    }

    public void createVersion(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException
    {
        impl.createVersion(nodeId, properties);
    }

    public java.util.HashMap hasAllPermissions(java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.hasAllPermissions(nodeId, permissions);
        return ret;
    }

    public java.util.HashMap hasAllPermissionsExt(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.hasAllPermissionsExt(storeProtocol, storeId, nodeId, permissions);
        return ret;
    }

    public java.lang.String getHomeFolderID(java.lang.String username) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getHomeFolderID(username);
        return ret;
    }

    public void addPermissionACEs(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException
    {
        impl.addPermissionACEs(nodeId, aces);
    }

    public void removePermissionACEs(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException
    {
        impl.removePermissionACEs(nodeId, aces);
    }

    public void setPermissionsBasic(java.lang.String nodeId, java.lang.String _authority, java.lang.String[] permissions, boolean changeInherit, boolean inheritPermission) throws java.rmi.RemoteException
    {
        impl.setPermissionsBasic(nodeId, _authority, permissions, changeInherit, inheritPermission);
    }

    public void removePermissions(java.lang.String nodeId, java.lang.String _authority, java.lang.String[] _permissions) throws java.rmi.RemoteException
    {
        impl.removePermissions(nodeId, _authority, _permissions);
    }

    public void executeAction(java.lang.String nodeId, java.lang.String actionName, java.lang.String actionId, java.util.HashMap parameters, boolean async) throws java.rmi.RemoteException
    {
        impl.executeAction(nodeId, actionName, actionId, parameters, async);
    }

    public void createAssociation(java.lang.String fromID, java.lang.String toID, java.lang.String association) throws java.rmi.RemoteException
    {
        impl.createAssociation(fromID, toID, association);
    }

    public void createChildAssociation(java.lang.String from, java.lang.String to, java.lang.String assocType, java.lang.String assocName) throws java.rmi.RemoteException
    {
        impl.createChildAssociation(from, to, assocType, assocName);
    }

    public void moveNode(java.lang.String newParentId, java.lang.String childAssocType, java.lang.String nodeId) throws java.rmi.RemoteException
    {
        impl.moveNode(newParentId, childAssocType, nodeId);
    }

    public void removeAssociation(java.lang.String fromID, java.lang.String toID, java.lang.String association) throws java.rmi.RemoteException
    {
        impl.removeAssociation(fromID, toID, association);
    }

    public void removeRelationsForNode(java.lang.String nodeId, java.lang.String nodeParentId) throws java.rmi.RemoteException
    {
        impl.removeRelationsForNode(nodeId, nodeParentId);
    }

    public void removeRelations(java.lang.String parentID) throws java.rmi.RemoteException
    {
        impl.removeRelations(parentID);
    }

    public void addAspect(java.lang.String nodeId, java.lang.String aspect) throws java.rmi.RemoteException
    {
        impl.addAspect(nodeId, aspect);
    }

    public java.lang.String getGroupFolderId() throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getGroupFolderId();
        return ret;
    }

    public java.lang.String getRepositoryRoot() throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.getRepositoryRoot();
        return ret;
    }

    public java.util.HashMap getChildenByProps(java.lang.String parentId, java.lang.String type, java.util.HashMap props) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildenByProps(parentId, type, props);
        return ret;
    }

    public java.util.HashMap getChildrenByType(java.lang.String nodeId, java.lang.String type) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildrenByType(nodeId, type);
        return ret;
    }

    public java.util.HashMap getChildrenByAssociation(java.lang.String storeString, java.lang.String nodeId, java.lang.String association) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildrenByAssociation(storeString, nodeId, association);
        return ret;
    }

    public java.util.HashMap getParents(java.lang.String nodeID, boolean primary) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getParents(nodeID, primary);
        return ret;
    }

    public java.util.HashMap getAssocNode(java.lang.String nodeID, java.lang.String association) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getAssocNode(nodeID, association);
        return ret;
    }

    public java.util.HashMap getChildRecursive(java.lang.String parentId, java.lang.String type, java.util.HashMap props) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildRecursive(parentId, type, props);
        return ret;
    }

    public java.util.HashMap getChildrenRecursive(java.lang.String parentId, java.lang.String type) throws java.rmi.RemoteException
    {
        java.util.HashMap ret = impl.getChildrenRecursive(parentId, type);
        return ret;
    }

    public java.lang.String[] getAssociationNodeIds(java.lang.String nodeID, java.lang.String association) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getAssociationNodeIds(nodeID, association);
        return ret;
    }

    public java.lang.String[] getUserNames() throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getUserNames();
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.UserDetails[] getUserDetails(java.lang.String[] userNames) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.UserDetails[] ret = impl.getUserDetails(userNames);
        return ret;
    }

    public void setUserDetails(org.edu_sharing.webservices.alfresco.extension.UserDetails[] userDetails) throws java.rmi.RemoteException
    {
        impl.setUserDetails(userDetails);
    }

    public void deleteUser(java.lang.String[] userNames) throws java.rmi.RemoteException
    {
        impl.deleteUser(userNames);
    }

    public java.lang.String[] getGroupNames() throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getGroupNames();
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.GroupDetails[] getGroupDetails(java.lang.String[] groupNames) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.GroupDetails[] ret = impl.getGroupDetails(groupNames);
        return ret;
    }

    public org.edu_sharing.webservices.alfresco.extension.GroupDetails setGroupDetails(org.edu_sharing.webservices.alfresco.extension.GroupDetails groupDetails) throws java.rmi.RemoteException
    {
        org.edu_sharing.webservices.alfresco.extension.GroupDetails ret = impl.setGroupDetails(groupDetails);
        return ret;
    }

    public void deleteGroup(java.lang.String[] groupNames) throws java.rmi.RemoteException
    {
        impl.deleteGroup(groupNames);
    }

    public java.lang.String[] getMemberships(java.lang.String groupName) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getMemberships(groupName);
        return ret;
    }

    public void addMemberships(java.lang.String groupName, java.lang.String[] members) throws java.rmi.RemoteException
    {
        impl.addMemberships(groupName, members);
    }

    public void removeMemberships(java.lang.String groupName, java.lang.String[] members) throws java.rmi.RemoteException
    {
        impl.removeMemberships(groupName, members);
    }

    public void removeAllMemberships(java.lang.String[] groupNames) throws java.rmi.RemoteException
    {
        impl.removeAllMemberships(groupNames);
    }

    public boolean isSubOf(java.lang.String type, java.lang.String parentType) throws java.rmi.RemoteException
    {
        boolean ret = impl.isSubOf(type, parentType);
        return ret;
    }

    public boolean hasContent(java.lang.String nodeId, java.lang.String contentProp) throws java.rmi.RemoteException
    {
        boolean ret = impl.hasContent(nodeId, contentProp);
        return ret;
    }

    public void writeContent(java.lang.String nodeID, byte[] content, java.lang.String mimetype, java.lang.String encoding, java.lang.String property) throws java.rmi.RemoteException
    {
        impl.writeContent(nodeID, content, mimetype, encoding, property);
    }

    public void setUserPassword(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException
    {
        impl.setUserPassword(userName, password);
    }

    public void invalideTicket(java.lang.String ticket) throws java.rmi.RemoteException
    {
        impl.invalideTicket(ticket);
    }

    public void bindEduGroupFolder(java.lang.String groupName, java.lang.String folderId) throws java.rmi.RemoteException
    {
        impl.bindEduGroupFolder(groupName, folderId);
    }

    public java.lang.String findNodeByPath(java.lang.String path) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.findNodeByPath(path);
        return ret;
    }

    public java.lang.String[] getAspects(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId) throws java.rmi.RemoteException
    {
        java.lang.String[] ret = impl.getAspects(storeProtocol, storeId, nodeId);
        return ret;
    }

    public void setOwner(java.lang.String nodeId, java.lang.String username) throws java.rmi.RemoteException
    {
        impl.setOwner(nodeId, username);
    }

    public void setPermissions(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException
    {
        impl.setPermissions(nodeId, aces);
    }

    public void removeChild(java.lang.String parentID, java.lang.String childID, java.lang.String association) throws java.rmi.RemoteException
    {
        impl.removeChild(parentID, childID, association);
    }

}
