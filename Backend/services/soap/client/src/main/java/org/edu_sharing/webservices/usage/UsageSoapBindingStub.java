/**
 * UsageSoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class UsageSoapBindingStub extends org.apache.axis.client.Stub implements org.edu_sharing.webservices.usage.Usage {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[6];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUsage");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appSessionId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appCurrentUserId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsageReturn"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUsage");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUser"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult"));
        oper.setReturnClass(org.edu_sharing.webservices.usage.UsageResult.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageReturn"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUsages");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appSessionId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appCurrentUserId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsagesReturn"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("usageAllowed");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "nodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "usageAllowedReturn"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("setUsage");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUser"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUserMail"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fromUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "toUsed"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "distinctPersons"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "version"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "xmlParams"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUsageByParentNodeId");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult"));
        oper.setReturnClass(org.edu_sharing.webservices.usage.UsageResult[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageByParentNodeIdReturn"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault"),
                      "org.edu_sharing.webservices.usage.UsageException",
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "fault1"),
                      "org.edu_sharing.webservices.authentication.AuthenticationException",
                      new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"), 
                      true
                     ));
        _operations[5] = oper;

    }

    public UsageSoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public UsageSoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public UsageSoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.authentication.AuthenticationException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsage");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.DeleteUsage.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsageResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.DeleteUsageResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsages");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.DeleteUsages.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsagesResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.DeleteUsagesResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsage");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.GetUsage.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsageByParentNodeId");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.GetUsageByParentNodeId.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsageByParentNodeIdResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.UsageResult[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult");
            qName2 = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageByParentNodeIdReturn");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsageResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.GetUsageResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">setUsage");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.SetUsage.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">setUsageResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.SetUsageResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">usageAllowed");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.UsageAllowed.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">usageAllowedResponse");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.UsageAllowedResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageException");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.UsageException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult");
            cachedSerQNames.add(qName);
            cls = org.edu_sharing.webservices.usage.UsageResult.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public boolean deleteUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsage"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, appSessionId, appCurrentUserId, lmsId, courseId, parentNodeId, resourceId});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.edu_sharing.webservices.usage.UsageResult getUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsage"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, lmsId, courseId, parentNodeId, appUser, resourceId});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.edu_sharing.webservices.usage.UsageResult) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.edu_sharing.webservices.usage.UsageResult) org.apache.axis.utils.JavaUtils.convert(_resp, org.edu_sharing.webservices.usage.UsageResult.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean deleteUsages(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsages"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, appSessionId, appCurrentUserId, lmsId, courseId});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public boolean usageAllowed(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "usageAllowed"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, nodeId, lmsId, courseId});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return ((java.lang.Boolean) _resp).booleanValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, boolean.class)).booleanValue();
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void setUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String appUserMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "setUsage"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, lmsId, courseId, parentNodeId, appUser, appUserMail, fromUsed, toUsed, new java.lang.Integer(distinctPersons), version, resourceId, xmlParams});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.edu_sharing.webservices.usage.UsageResult[] getUsageByParentNodeId(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String parentNodeId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "getUsageByParentNodeId"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {repositoryTicket, repositoryUsername, parentNodeId});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.edu_sharing.webservices.usage.UsageResult[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.edu_sharing.webservices.usage.UsageResult[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.edu_sharing.webservices.usage.UsageResult[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.usage.UsageException) {
              throw (org.edu_sharing.webservices.usage.UsageException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.edu_sharing.webservices.authentication.AuthenticationException) {
              throw (org.edu_sharing.webservices.authentication.AuthenticationException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

}
