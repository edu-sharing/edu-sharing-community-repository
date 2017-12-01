/**
 * GetUsage.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class GetUsage  implements java.io.Serializable {
    private java.lang.String repositoryTicket;

    private java.lang.String repositoryUsername;

    private java.lang.String lmsId;

    private java.lang.String courseId;

    private java.lang.String parentNodeId;

    private java.lang.String appUser;

    private java.lang.String resourceId;

    public GetUsage() {
    }

    public GetUsage(
           java.lang.String repositoryTicket,
           java.lang.String repositoryUsername,
           java.lang.String lmsId,
           java.lang.String courseId,
           java.lang.String parentNodeId,
           java.lang.String appUser,
           java.lang.String resourceId) {
           this.repositoryTicket = repositoryTicket;
           this.repositoryUsername = repositoryUsername;
           this.lmsId = lmsId;
           this.courseId = courseId;
           this.parentNodeId = parentNodeId;
           this.appUser = appUser;
           this.resourceId = resourceId;
    }


    /**
     * Gets the repositoryTicket value for this GetUsage.
     * 
     * @return repositoryTicket
     */
    public java.lang.String getRepositoryTicket() {
        return repositoryTicket;
    }


    /**
     * Sets the repositoryTicket value for this GetUsage.
     * 
     * @param repositoryTicket
     */
    public void setRepositoryTicket(java.lang.String repositoryTicket) {
        this.repositoryTicket = repositoryTicket;
    }


    /**
     * Gets the repositoryUsername value for this GetUsage.
     * 
     * @return repositoryUsername
     */
    public java.lang.String getRepositoryUsername() {
        return repositoryUsername;
    }


    /**
     * Sets the repositoryUsername value for this GetUsage.
     * 
     * @param repositoryUsername
     */
    public void setRepositoryUsername(java.lang.String repositoryUsername) {
        this.repositoryUsername = repositoryUsername;
    }


    /**
     * Gets the lmsId value for this GetUsage.
     * 
     * @return lmsId
     */
    public java.lang.String getLmsId() {
        return lmsId;
    }


    /**
     * Sets the lmsId value for this GetUsage.
     * 
     * @param lmsId
     */
    public void setLmsId(java.lang.String lmsId) {
        this.lmsId = lmsId;
    }


    /**
     * Gets the courseId value for this GetUsage.
     * 
     * @return courseId
     */
    public java.lang.String getCourseId() {
        return courseId;
    }


    /**
     * Sets the courseId value for this GetUsage.
     * 
     * @param courseId
     */
    public void setCourseId(java.lang.String courseId) {
        this.courseId = courseId;
    }


    /**
     * Gets the parentNodeId value for this GetUsage.
     * 
     * @return parentNodeId
     */
    public java.lang.String getParentNodeId() {
        return parentNodeId;
    }


    /**
     * Sets the parentNodeId value for this GetUsage.
     * 
     * @param parentNodeId
     */
    public void setParentNodeId(java.lang.String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }


    /**
     * Gets the appUser value for this GetUsage.
     * 
     * @return appUser
     */
    public java.lang.String getAppUser() {
        return appUser;
    }


    /**
     * Sets the appUser value for this GetUsage.
     * 
     * @param appUser
     */
    public void setAppUser(java.lang.String appUser) {
        this.appUser = appUser;
    }


    /**
     * Gets the resourceId value for this GetUsage.
     * 
     * @return resourceId
     */
    public java.lang.String getResourceId() {
        return resourceId;
    }


    /**
     * Sets the resourceId value for this GetUsage.
     * 
     * @param resourceId
     */
    public void setResourceId(java.lang.String resourceId) {
        this.resourceId = resourceId;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetUsage)) return false;
        GetUsage other = (GetUsage) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.repositoryTicket==null && other.getRepositoryTicket()==null) || 
             (this.repositoryTicket!=null &&
              this.repositoryTicket.equals(other.getRepositoryTicket()))) &&
            ((this.repositoryUsername==null && other.getRepositoryUsername()==null) || 
             (this.repositoryUsername!=null &&
              this.repositoryUsername.equals(other.getRepositoryUsername()))) &&
            ((this.lmsId==null && other.getLmsId()==null) || 
             (this.lmsId!=null &&
              this.lmsId.equals(other.getLmsId()))) &&
            ((this.courseId==null && other.getCourseId()==null) || 
             (this.courseId!=null &&
              this.courseId.equals(other.getCourseId()))) &&
            ((this.parentNodeId==null && other.getParentNodeId()==null) || 
             (this.parentNodeId!=null &&
              this.parentNodeId.equals(other.getParentNodeId()))) &&
            ((this.appUser==null && other.getAppUser()==null) || 
             (this.appUser!=null &&
              this.appUser.equals(other.getAppUser()))) &&
            ((this.resourceId==null && other.getResourceId()==null) || 
             (this.resourceId!=null &&
              this.resourceId.equals(other.getResourceId())));
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
        if (getRepositoryTicket() != null) {
            _hashCode += getRepositoryTicket().hashCode();
        }
        if (getRepositoryUsername() != null) {
            _hashCode += getRepositoryUsername().hashCode();
        }
        if (getLmsId() != null) {
            _hashCode += getLmsId().hashCode();
        }
        if (getCourseId() != null) {
            _hashCode += getCourseId().hashCode();
        }
        if (getParentNodeId() != null) {
            _hashCode += getParentNodeId().hashCode();
        }
        if (getAppUser() != null) {
            _hashCode += getAppUser().hashCode();
        }
        if (getResourceId() != null) {
            _hashCode += getResourceId().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetUsage.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsage"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryTicket");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryTicket"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryUsername");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "repositoryUsername"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lmsId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "lmsId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("courseId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "courseId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parentNodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("appUser");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "appUser"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resourceId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "resourceId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
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
