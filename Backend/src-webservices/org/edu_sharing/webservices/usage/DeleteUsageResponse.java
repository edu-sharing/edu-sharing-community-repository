/**
 * DeleteUsageResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class DeleteUsageResponse  implements java.io.Serializable {
    private boolean deleteUsageReturn;

    public DeleteUsageResponse() {
    }

    public DeleteUsageResponse(
           boolean deleteUsageReturn) {
           this.deleteUsageReturn = deleteUsageReturn;
    }


    /**
     * Gets the deleteUsageReturn value for this DeleteUsageResponse.
     * 
     * @return deleteUsageReturn
     */
    public boolean isDeleteUsageReturn() {
        return deleteUsageReturn;
    }


    /**
     * Sets the deleteUsageReturn value for this DeleteUsageResponse.
     * 
     * @param deleteUsageReturn
     */
    public void setDeleteUsageReturn(boolean deleteUsageReturn) {
        this.deleteUsageReturn = deleteUsageReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeleteUsageResponse)) return false;
        DeleteUsageResponse other = (DeleteUsageResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.deleteUsageReturn == other.isDeleteUsageReturn();
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
        _hashCode += (isDeleteUsageReturn() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeleteUsageResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsageResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deleteUsageReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsageReturn"));
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
