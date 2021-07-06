/**
 * GetChildenByPropsResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildenByPropsResponse  implements java.io.Serializable {
    private java.util.HashMap getChildenByPropsReturn;

    public GetChildenByPropsResponse() {
    }

    public GetChildenByPropsResponse(
           java.util.HashMap getChildenByPropsReturn) {
           this.getChildenByPropsReturn = getChildenByPropsReturn;
    }


    /**
     * Gets the getChildenByPropsReturn value for this GetChildenByPropsResponse.
     * 
     * @return getChildenByPropsReturn
     */
    public java.util.HashMap getGetChildenByPropsReturn() {
        return getChildenByPropsReturn;
    }


    /**
     * Sets the getChildenByPropsReturn value for this GetChildenByPropsResponse.
     * 
     * @param getChildenByPropsReturn
     */
    public void setGetChildenByPropsReturn(java.util.HashMap getChildenByPropsReturn) {
        this.getChildenByPropsReturn = getChildenByPropsReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildenByPropsResponse)) return false;
        GetChildenByPropsResponse other = (GetChildenByPropsResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getChildenByPropsReturn==null && other.getGetChildenByPropsReturn()==null) || 
             (this.getChildenByPropsReturn!=null &&
              this.getChildenByPropsReturn.equals(other.getGetChildenByPropsReturn())));
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
        if (getGetChildenByPropsReturn() != null) {
            _hashCode += getGetChildenByPropsReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildenByPropsResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildenByPropsResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getChildenByPropsReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildenByPropsReturn"));
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
