/**
 * Usage2Result.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage2;

public class Usage2Result  implements java.io.Serializable {
    private java.lang.String appUser;

    private java.lang.String appUserMail;

    private java.lang.String courseId;

    private java.lang.Integer distinctPersons;

    private java.util.Calendar fromUsed;

    private java.lang.String lmsId;

    private java.lang.String nodeId;

    private java.lang.String parentNodeId;

    private java.lang.String resourceId;

    private java.util.Calendar toUsed;

    private java.lang.Integer usageCounter;

    private java.lang.String usageVersion;

    private java.lang.String usageXmlParams;

    public Usage2Result() {
    }

    public Usage2Result(
           java.lang.String appUser,
           java.lang.String appUserMail,
           java.lang.String courseId,
           java.lang.Integer distinctPersons,
           java.util.Calendar fromUsed,
           java.lang.String lmsId,
           java.lang.String nodeId,
           java.lang.String parentNodeId,
           java.lang.String resourceId,
           java.util.Calendar toUsed,
           java.lang.Integer usageCounter,
           java.lang.String usageVersion,
           java.lang.String usageXmlParams) {
           this.appUser = appUser;
           this.appUserMail = appUserMail;
           this.courseId = courseId;
           this.distinctPersons = distinctPersons;
           this.fromUsed = fromUsed;
           this.lmsId = lmsId;
           this.nodeId = nodeId;
           this.parentNodeId = parentNodeId;
           this.resourceId = resourceId;
           this.toUsed = toUsed;
           this.usageCounter = usageCounter;
           this.usageVersion = usageVersion;
           this.usageXmlParams = usageXmlParams;
    }


    /**
     * Gets the appUser value for this Usage2Result.
     * 
     * @return appUser
     */
    public java.lang.String getAppUser() {
        return appUser;
    }


    /**
     * Sets the appUser value for this Usage2Result.
     * 
     * @param appUser
     */
    public void setAppUser(java.lang.String appUser) {
        this.appUser = appUser;
    }


    /**
     * Gets the appUserMail value for this Usage2Result.
     * 
     * @return appUserMail
     */
    public java.lang.String getAppUserMail() {
        return appUserMail;
    }


    /**
     * Sets the appUserMail value for this Usage2Result.
     * 
     * @param appUserMail
     */
    public void setAppUserMail(java.lang.String appUserMail) {
        this.appUserMail = appUserMail;
    }


    /**
     * Gets the courseId value for this Usage2Result.
     * 
     * @return courseId
     */
    public java.lang.String getCourseId() {
        return courseId;
    }


    /**
     * Sets the courseId value for this Usage2Result.
     * 
     * @param courseId
     */
    public void setCourseId(java.lang.String courseId) {
        this.courseId = courseId;
    }


    /**
     * Gets the distinctPersons value for this Usage2Result.
     * 
     * @return distinctPersons
     */
    public java.lang.Integer getDistinctPersons() {
        return distinctPersons;
    }


    /**
     * Sets the distinctPersons value for this Usage2Result.
     * 
     * @param distinctPersons
     */
    public void setDistinctPersons(java.lang.Integer distinctPersons) {
        this.distinctPersons = distinctPersons;
    }


    /**
     * Gets the fromUsed value for this Usage2Result.
     * 
     * @return fromUsed
     */
    public java.util.Calendar getFromUsed() {
        return fromUsed;
    }


    /**
     * Sets the fromUsed value for this Usage2Result.
     * 
     * @param fromUsed
     */
    public void setFromUsed(java.util.Calendar fromUsed) {
        this.fromUsed = fromUsed;
    }


    /**
     * Gets the lmsId value for this Usage2Result.
     * 
     * @return lmsId
     */
    public java.lang.String getLmsId() {
        return lmsId;
    }


    /**
     * Sets the lmsId value for this Usage2Result.
     * 
     * @param lmsId
     */
    public void setLmsId(java.lang.String lmsId) {
        this.lmsId = lmsId;
    }


    /**
     * Gets the nodeId value for this Usage2Result.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this Usage2Result.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the parentNodeId value for this Usage2Result.
     * 
     * @return parentNodeId
     */
    public java.lang.String getParentNodeId() {
        return parentNodeId;
    }


    /**
     * Sets the parentNodeId value for this Usage2Result.
     * 
     * @param parentNodeId
     */
    public void setParentNodeId(java.lang.String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }


    /**
     * Gets the resourceId value for this Usage2Result.
     * 
     * @return resourceId
     */
    public java.lang.String getResourceId() {
        return resourceId;
    }


    /**
     * Sets the resourceId value for this Usage2Result.
     * 
     * @param resourceId
     */
    public void setResourceId(java.lang.String resourceId) {
        this.resourceId = resourceId;
    }


    /**
     * Gets the toUsed value for this Usage2Result.
     * 
     * @return toUsed
     */
    public java.util.Calendar getToUsed() {
        return toUsed;
    }


    /**
     * Sets the toUsed value for this Usage2Result.
     * 
     * @param toUsed
     */
    public void setToUsed(java.util.Calendar toUsed) {
        this.toUsed = toUsed;
    }


    /**
     * Gets the usageCounter value for this Usage2Result.
     * 
     * @return usageCounter
     */
    public java.lang.Integer getUsageCounter() {
        return usageCounter;
    }


    /**
     * Sets the usageCounter value for this Usage2Result.
     * 
     * @param usageCounter
     */
    public void setUsageCounter(java.lang.Integer usageCounter) {
        this.usageCounter = usageCounter;
    }


    /**
     * Gets the usageVersion value for this Usage2Result.
     * 
     * @return usageVersion
     */
    public java.lang.String getUsageVersion() {
        return usageVersion;
    }


    /**
     * Sets the usageVersion value for this Usage2Result.
     * 
     * @param usageVersion
     */
    public void setUsageVersion(java.lang.String usageVersion) {
        this.usageVersion = usageVersion;
    }


    /**
     * Gets the usageXmlParams value for this Usage2Result.
     * 
     * @return usageXmlParams
     */
    public java.lang.String getUsageXmlParams() {
        return usageXmlParams;
    }


    /**
     * Sets the usageXmlParams value for this Usage2Result.
     * 
     * @param usageXmlParams
     */
    public void setUsageXmlParams(java.lang.String usageXmlParams) {
        this.usageXmlParams = usageXmlParams;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Usage2Result)) return false;
        Usage2Result other = (Usage2Result) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.appUser==null && other.getAppUser()==null) || 
             (this.appUser!=null &&
              this.appUser.equals(other.getAppUser()))) &&
            ((this.appUserMail==null && other.getAppUserMail()==null) || 
             (this.appUserMail!=null &&
              this.appUserMail.equals(other.getAppUserMail()))) &&
            ((this.courseId==null && other.getCourseId()==null) || 
             (this.courseId!=null &&
              this.courseId.equals(other.getCourseId()))) &&
            ((this.distinctPersons==null && other.getDistinctPersons()==null) || 
             (this.distinctPersons!=null &&
              this.distinctPersons.equals(other.getDistinctPersons()))) &&
            ((this.fromUsed==null && other.getFromUsed()==null) || 
             (this.fromUsed!=null &&
              this.fromUsed.equals(other.getFromUsed()))) &&
            ((this.lmsId==null && other.getLmsId()==null) || 
             (this.lmsId!=null &&
              this.lmsId.equals(other.getLmsId()))) &&
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId()))) &&
            ((this.parentNodeId==null && other.getParentNodeId()==null) || 
             (this.parentNodeId!=null &&
              this.parentNodeId.equals(other.getParentNodeId()))) &&
            ((this.resourceId==null && other.getResourceId()==null) || 
             (this.resourceId!=null &&
              this.resourceId.equals(other.getResourceId()))) &&
            ((this.toUsed==null && other.getToUsed()==null) || 
             (this.toUsed!=null &&
              this.toUsed.equals(other.getToUsed()))) &&
            ((this.usageCounter==null && other.getUsageCounter()==null) || 
             (this.usageCounter!=null &&
              this.usageCounter.equals(other.getUsageCounter()))) &&
            ((this.usageVersion==null && other.getUsageVersion()==null) || 
             (this.usageVersion!=null &&
              this.usageVersion.equals(other.getUsageVersion()))) &&
            ((this.usageXmlParams==null && other.getUsageXmlParams()==null) || 
             (this.usageXmlParams!=null &&
              this.usageXmlParams.equals(other.getUsageXmlParams())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getAppUser() != null) {
            _hashCode += getAppUser().hashCode();
        }
        if (getAppUserMail() != null) {
            _hashCode += getAppUserMail().hashCode();
        }
        if (getCourseId() != null) {
            _hashCode += getCourseId().hashCode();
        }
        if (getDistinctPersons() != null) {
            _hashCode += getDistinctPersons().hashCode();
        }
        if (getFromUsed() != null) {
            _hashCode += getFromUsed().hashCode();
        }
        if (getLmsId() != null) {
            _hashCode += getLmsId().hashCode();
        }
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        if (getParentNodeId() != null) {
            _hashCode += getParentNodeId().hashCode();
        }
        if (getResourceId() != null) {
            _hashCode += getResourceId().hashCode();
        }
        if (getToUsed() != null) {
            _hashCode += getToUsed().hashCode();
        }
        if (getUsageCounter() != null) {
            _hashCode += getUsageCounter().hashCode();
        }
        if (getUsageVersion() != null) {
            _hashCode += getUsageVersion().hashCode();
        }
        if (getUsageXmlParams() != null) {
            _hashCode += getUsageXmlParams().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Usage2Result.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "Usage2Result"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("appUser");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "appUser"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("appUserMail");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "appUserMail"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("courseId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "courseId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("distinctPersons");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "distinctPersons"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fromUsed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "fromUsed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lmsId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "lmsId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "nodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parentNodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "parentNodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resourceId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "resourceId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("toUsed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "toUsed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("usageCounter");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "usageCounter"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("usageVersion");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "usageVersion"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("usageXmlParams");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage2.webservices.edu_sharing.org", "usageXmlParams"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
