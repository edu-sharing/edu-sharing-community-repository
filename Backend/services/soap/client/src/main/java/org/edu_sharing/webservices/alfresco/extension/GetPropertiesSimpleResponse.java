/**
 * GetPropertiesSimpleResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetPropertiesSimpleResponse  implements java.io.Serializable {
    private java.util.HashMap getPropertiesSimpleReturn;

    public GetPropertiesSimpleResponse() {
    }

    public GetPropertiesSimpleResponse(
           java.util.HashMap getPropertiesSimpleReturn) {
           this.getPropertiesSimpleReturn = getPropertiesSimpleReturn;
    }


    /**
     * Gets the getPropertiesSimpleReturn value for this GetPropertiesSimpleResponse.
     * 
     * @return getPropertiesSimpleReturn
     */
    public java.util.HashMap getGetPropertiesSimpleReturn() {
        return getPropertiesSimpleReturn;
    }


    /**
     * Sets the getPropertiesSimpleReturn value for this GetPropertiesSimpleResponse.
     * 
     * @param getPropertiesSimpleReturn
     */
    public void setGetPropertiesSimpleReturn(java.util.HashMap getPropertiesSimpleReturn) {
        this.getPropertiesSimpleReturn = getPropertiesSimpleReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetPropertiesSimpleResponse)) return false;
        GetPropertiesSimpleResponse other = (GetPropertiesSimpleResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getPropertiesSimpleReturn==null && other.getGetPropertiesSimpleReturn()==null) || 
             (this.getPropertiesSimpleReturn!=null &&
              this.getPropertiesSimpleReturn.equals(other.getGetPropertiesSimpleReturn())));
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
        if (getGetPropertiesSimpleReturn() != null) {
            _hashCode += getGetPropertiesSimpleReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetPropertiesSimpleResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getPropertiesSimpleResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getPropertiesSimpleReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPropertiesSimpleReturn"));
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
