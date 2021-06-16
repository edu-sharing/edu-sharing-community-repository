/**
 * GetUsageByParentNodeId.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class GetUsageByParentNodeId  implements java.io.Serializable {
    private java.lang.String repositoryTicket;

    private java.lang.String repositoryUsername;

    private java.lang.String parentNodeId;

    public GetUsageByParentNodeId() {
    }

    public GetUsageByParentNodeId(
           java.lang.String repositoryTicket,
           java.lang.String repositoryUsername,
           java.lang.String parentNodeId) {
           this.repositoryTicket = repositoryTicket;
           this.repositoryUsername = repositoryUsername;
           this.parentNodeId = parentNodeId;
    }


    /**
     * Gets the repositoryTicket value for this GetUsageByParentNodeId.
     * 
     * @return repositoryTicket
     */
    public java.lang.String getRepositoryTicket() {
        return repositoryTicket;
    }


    /**
     * Sets the repositoryTicket value for this GetUsageByParentNodeId.
     * 
     * @param repositoryTicket
     */
    public void setRepositoryTicket(java.lang.String repositoryTicket) {
        this.repositoryTicket = repositoryTicket;
    }


    /**
     * Gets the repositoryUsername value for this GetUsageByParentNodeId.
     * 
     * @return repositoryUsername
     */
    public java.lang.String getRepositoryUsername() {
        return repositoryUsername;
    }


    /**
     * Sets the repositoryUsername value for this GetUsageByParentNodeId.
     * 
     * @param repositoryUsername
     */
    public void setRepositoryUsername(java.lang.String repositoryUsername) {
        this.repositoryUsername = repositoryUsername;
    }


    /**
     * Gets the parentNodeId value for this GetUsageByParentNodeId.
     * 
     * @return parentNodeId
     */
    public java.lang.String getParentNodeId() {
        return parentNodeId;
    }


    /**
     * Sets the parentNodeId value for this GetUsageByParentNodeId.
     * 
     * @param parentNodeId
     */
    public void setParentNodeId(java.lang.String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetUsageByParentNodeId)) return false;
        GetUsageByParentNodeId other = (GetUsageByParentNodeId) obj;
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
            ((this.parentNodeId==null && other.getParentNodeId()==null) || 
             (this.parentNodeId!=null &&
              this.parentNodeId.equals(other.getParentNodeId())));
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
        if (getParentNodeId() != null) {
            _hashCode += getParentNodeId().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetUsageByParentNodeId.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">getUsageByParentNodeId"));
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
        elemField.setFieldName("parentNodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "parentNodeId"));
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
