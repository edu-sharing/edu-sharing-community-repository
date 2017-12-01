/**
 * UsageAllowed.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class UsageAllowed  implements java.io.Serializable {
    private java.lang.String repositoryTicket;

    private java.lang.String repositoryUsername;

    private java.lang.String nodeId;

    private java.lang.String lmsId;

    private java.lang.String courseId;

    public UsageAllowed() {
    }

    public UsageAllowed(
           java.lang.String repositoryTicket,
           java.lang.String repositoryUsername,
           java.lang.String nodeId,
           java.lang.String lmsId,
           java.lang.String courseId) {
           this.repositoryTicket = repositoryTicket;
           this.repositoryUsername = repositoryUsername;
           this.nodeId = nodeId;
           this.lmsId = lmsId;
           this.courseId = courseId;
    }


    /**
     * Gets the repositoryTicket value for this UsageAllowed.
     * 
     * @return repositoryTicket
     */
    public java.lang.String getRepositoryTicket() {
        return repositoryTicket;
    }


    /**
     * Sets the repositoryTicket value for this UsageAllowed.
     * 
     * @param repositoryTicket
     */
    public void setRepositoryTicket(java.lang.String repositoryTicket) {
        this.repositoryTicket = repositoryTicket;
    }


    /**
     * Gets the repositoryUsername value for this UsageAllowed.
     * 
     * @return repositoryUsername
     */
    public java.lang.String getRepositoryUsername() {
        return repositoryUsername;
    }


    /**
     * Sets the repositoryUsername value for this UsageAllowed.
     * 
     * @param repositoryUsername
     */
    public void setRepositoryUsername(java.lang.String repositoryUsername) {
        this.repositoryUsername = repositoryUsername;
    }


    /**
     * Gets the nodeId value for this UsageAllowed.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this UsageAllowed.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the lmsId value for this UsageAllowed.
     * 
     * @return lmsId
     */
    public java.lang.String getLmsId() {
        return lmsId;
    }


    /**
     * Sets the lmsId value for this UsageAllowed.
     * 
     * @param lmsId
     */
    public void setLmsId(java.lang.String lmsId) {
        this.lmsId = lmsId;
    }


    /**
     * Gets the courseId value for this UsageAllowed.
     * 
     * @return courseId
     */
    public java.lang.String getCourseId() {
        return courseId;
    }


    /**
     * Sets the courseId value for this UsageAllowed.
     * 
     * @param courseId
     */
    public void setCourseId(java.lang.String courseId) {
        this.courseId = courseId;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UsageAllowed)) return false;
        UsageAllowed other = (UsageAllowed) obj;
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
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId()))) &&
            ((this.lmsId==null && other.getLmsId()==null) || 
             (this.lmsId!=null &&
              this.lmsId.equals(other.getLmsId()))) &&
            ((this.courseId==null && other.getCourseId()==null) || 
             (this.courseId!=null &&
              this.courseId.equals(other.getCourseId())));
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
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        if (getLmsId() != null) {
            _hashCode += getLmsId().hashCode();
        }
        if (getCourseId() != null) {
            _hashCode += getCourseId().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(UsageAllowed.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">usageAllowed"));
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
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "nodeId"));
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
