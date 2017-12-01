/**
 * GetProperty.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetProperty  implements java.io.Serializable {
    private java.lang.String storeProtocol;

    private java.lang.String storeIdentifier;

    private java.lang.String nodeId;

    private java.lang.String property;

    public GetProperty() {
    }

    public GetProperty(
           java.lang.String storeProtocol,
           java.lang.String storeIdentifier,
           java.lang.String nodeId,
           java.lang.String property) {
           this.storeProtocol = storeProtocol;
           this.storeIdentifier = storeIdentifier;
           this.nodeId = nodeId;
           this.property = property;
    }


    /**
     * Gets the storeProtocol value for this GetProperty.
     * 
     * @return storeProtocol
     */
    public java.lang.String getStoreProtocol() {
        return storeProtocol;
    }


    /**
     * Sets the storeProtocol value for this GetProperty.
     * 
     * @param storeProtocol
     */
    public void setStoreProtocol(java.lang.String storeProtocol) {
        this.storeProtocol = storeProtocol;
    }


    /**
     * Gets the storeIdentifier value for this GetProperty.
     * 
     * @return storeIdentifier
     */
    public java.lang.String getStoreIdentifier() {
        return storeIdentifier;
    }


    /**
     * Sets the storeIdentifier value for this GetProperty.
     * 
     * @param storeIdentifier
     */
    public void setStoreIdentifier(java.lang.String storeIdentifier) {
        this.storeIdentifier = storeIdentifier;
    }


    /**
     * Gets the nodeId value for this GetProperty.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this GetProperty.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the property value for this GetProperty.
     * 
     * @return property
     */
    public java.lang.String getProperty() {
        return property;
    }


    /**
     * Sets the property value for this GetProperty.
     * 
     * @param property
     */
    public void setProperty(java.lang.String property) {
        this.property = property;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetProperty)) return false;
        GetProperty other = (GetProperty) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.storeProtocol==null && other.getStoreProtocol()==null) || 
             (this.storeProtocol!=null &&
              this.storeProtocol.equals(other.getStoreProtocol()))) &&
            ((this.storeIdentifier==null && other.getStoreIdentifier()==null) || 
             (this.storeIdentifier!=null &&
              this.storeIdentifier.equals(other.getStoreIdentifier()))) &&
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId()))) &&
            ((this.property==null && other.getProperty()==null) || 
             (this.property!=null &&
              this.property.equals(other.getProperty())));
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
        if (getStoreProtocol() != null) {
            _hashCode += getStoreProtocol().hashCode();
        }
        if (getStoreIdentifier() != null) {
            _hashCode += getStoreIdentifier().hashCode();
        }
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        if (getProperty() != null) {
            _hashCode += getProperty().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetProperty.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getProperty"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("storeProtocol");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeProtocol"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("storeIdentifier");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "storeIdentifier"));
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
        elemField.setFieldName("property");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"));
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
