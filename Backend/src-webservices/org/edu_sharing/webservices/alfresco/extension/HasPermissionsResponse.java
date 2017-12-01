/**
 * HasPermissionsResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class HasPermissionsResponse  implements java.io.Serializable {
    private java.util.HashMap hasPermissionsReturn;

    public HasPermissionsResponse() {
    }

    public HasPermissionsResponse(
           java.util.HashMap hasPermissionsReturn) {
           this.hasPermissionsReturn = hasPermissionsReturn;
    }


    /**
     * Gets the hasPermissionsReturn value for this HasPermissionsResponse.
     * 
     * @return hasPermissionsReturn
     */
    public java.util.HashMap getHasPermissionsReturn() {
        return hasPermissionsReturn;
    }


    /**
     * Sets the hasPermissionsReturn value for this HasPermissionsResponse.
     * 
     * @param hasPermissionsReturn
     */
    public void setHasPermissionsReturn(java.util.HashMap hasPermissionsReturn) {
        this.hasPermissionsReturn = hasPermissionsReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof HasPermissionsResponse)) return false;
        HasPermissionsResponse other = (HasPermissionsResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.hasPermissionsReturn==null && other.getHasPermissionsReturn()==null) || 
             (this.hasPermissionsReturn!=null &&
              this.hasPermissionsReturn.equals(other.getHasPermissionsReturn())));
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
        if (getHasPermissionsReturn() != null) {
            _hashCode += getHasPermissionsReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(HasPermissionsResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">hasPermissionsResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("hasPermissionsReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "hasPermissionsReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
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
