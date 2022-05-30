/**
 * DeleteUsagesResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public class DeleteUsagesResponse  implements java.io.Serializable {
    private boolean deleteUsagesReturn;

    public DeleteUsagesResponse() {
    }

    public DeleteUsagesResponse(
           boolean deleteUsagesReturn) {
           this.deleteUsagesReturn = deleteUsagesReturn;
    }


    /**
     * Gets the deleteUsagesReturn value for this DeleteUsagesResponse.
     * 
     * @return deleteUsagesReturn
     */
    public boolean isDeleteUsagesReturn() {
        return deleteUsagesReturn;
    }


    /**
     * Sets the deleteUsagesReturn value for this DeleteUsagesResponse.
     * 
     * @param deleteUsagesReturn
     */
    public void setDeleteUsagesReturn(boolean deleteUsagesReturn) {
        this.deleteUsagesReturn = deleteUsagesReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeleteUsagesResponse)) return false;
        DeleteUsagesResponse other = (DeleteUsagesResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.deleteUsagesReturn == other.isDeleteUsagesReturn();
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
        _hashCode += (isDeleteUsagesReturn() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeleteUsagesResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", ">deleteUsagesResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deleteUsagesReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "deleteUsagesReturn"));
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
