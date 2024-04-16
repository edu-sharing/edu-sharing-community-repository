/**
 * HasPermissionsSimpleResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class HasPermissionsSimpleResponse  implements java.io.Serializable {
    private boolean hasPermissionsSimpleReturn;

    public HasPermissionsSimpleResponse() {
    }

    public HasPermissionsSimpleResponse(
           boolean hasPermissionsSimpleReturn) {
           this.hasPermissionsSimpleReturn = hasPermissionsSimpleReturn;
    }


    /**
     * Gets the hasPermissionsSimpleReturn value for this HasPermissionsSimpleResponse.
     * 
     * @return hasPermissionsSimpleReturn
     */
    public boolean isHasPermissionsSimpleReturn() {
        return hasPermissionsSimpleReturn;
    }


    /**
     * Sets the hasPermissionsSimpleReturn value for this HasPermissionsSimpleResponse.
     * 
     * @param hasPermissionsSimpleReturn
     */
    public void setHasPermissionsSimpleReturn(boolean hasPermissionsSimpleReturn) {
        this.hasPermissionsSimpleReturn = hasPermissionsSimpleReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof HasPermissionsSimpleResponse)) return false;
        HasPermissionsSimpleResponse other = (HasPermissionsSimpleResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.hasPermissionsSimpleReturn == other.isHasPermissionsSimpleReturn();
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
        _hashCode += (isHasPermissionsSimpleReturn() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(HasPermissionsSimpleResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">hasPermissionsSimpleResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("hasPermissionsSimpleReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissionsSimpleReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
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
