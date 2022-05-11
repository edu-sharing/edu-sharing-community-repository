/**
 * GetChildrenByAssociation.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildrenByAssociation  implements java.io.Serializable {
    private java.lang.String storeString;

    private java.lang.String nodeId;

    private java.lang.String association;

    public GetChildrenByAssociation() {
    }

    public GetChildrenByAssociation(
           java.lang.String storeString,
           java.lang.String nodeId,
           java.lang.String association) {
           this.storeString = storeString;
           this.nodeId = nodeId;
           this.association = association;
    }


    /**
     * Gets the storeString value for this GetChildrenByAssociation.
     * 
     * @return storeString
     */
    public java.lang.String getStoreString() {
        return storeString;
    }


    /**
     * Sets the storeString value for this GetChildrenByAssociation.
     * 
     * @param storeString
     */
    public void setStoreString(java.lang.String storeString) {
        this.storeString = storeString;
    }


    /**
     * Gets the nodeId value for this GetChildrenByAssociation.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this GetChildrenByAssociation.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the association value for this GetChildrenByAssociation.
     * 
     * @return association
     */
    public java.lang.String getAssociation() {
        return association;
    }


    /**
     * Sets the association value for this GetChildrenByAssociation.
     * 
     * @param association
     */
    public void setAssociation(java.lang.String association) {
        this.association = association;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildrenByAssociation)) return false;
        GetChildrenByAssociation other = (GetChildrenByAssociation) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.storeString==null && other.getStoreString()==null) || 
             (this.storeString!=null &&
              this.storeString.equals(other.getStoreString()))) &&
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId()))) &&
            ((this.association==null && other.getAssociation()==null) || 
             (this.association!=null &&
              this.association.equals(other.getAssociation())));
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
        if (getStoreString() != null) {
            _hashCode += getStoreString().hashCode();
        }
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        if (getAssociation() != null) {
            _hashCode += getAssociation().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildrenByAssociation.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildrenByAssociation"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("storeString");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeString"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("association");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "association"));
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
