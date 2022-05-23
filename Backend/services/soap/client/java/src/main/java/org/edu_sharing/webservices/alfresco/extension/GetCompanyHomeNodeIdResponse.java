/**
 * GetCompanyHomeNodeIdResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetCompanyHomeNodeIdResponse  implements java.io.Serializable {
    private java.lang.String getCompanyHomeNodeIdReturn;

    public GetCompanyHomeNodeIdResponse() {
    }

    public GetCompanyHomeNodeIdResponse(
           java.lang.String getCompanyHomeNodeIdReturn) {
           this.getCompanyHomeNodeIdReturn = getCompanyHomeNodeIdReturn;
    }


    /**
     * Gets the getCompanyHomeNodeIdReturn value for this GetCompanyHomeNodeIdResponse.
     * 
     * @return getCompanyHomeNodeIdReturn
     */
    public java.lang.String getGetCompanyHomeNodeIdReturn() {
        return getCompanyHomeNodeIdReturn;
    }


    /**
     * Sets the getCompanyHomeNodeIdReturn value for this GetCompanyHomeNodeIdResponse.
     * 
     * @param getCompanyHomeNodeIdReturn
     */
    public void setGetCompanyHomeNodeIdReturn(java.lang.String getCompanyHomeNodeIdReturn) {
        this.getCompanyHomeNodeIdReturn = getCompanyHomeNodeIdReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetCompanyHomeNodeIdResponse)) return false;
        GetCompanyHomeNodeIdResponse other = (GetCompanyHomeNodeIdResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getCompanyHomeNodeIdReturn==null && other.getGetCompanyHomeNodeIdReturn()==null) || 
             (this.getCompanyHomeNodeIdReturn!=null &&
              this.getCompanyHomeNodeIdReturn.equals(other.getGetCompanyHomeNodeIdReturn())));
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
        if (getGetCompanyHomeNodeIdReturn() != null) {
            _hashCode += getGetCompanyHomeNodeIdReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetCompanyHomeNodeIdResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getCompanyHomeNodeIdResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getCompanyHomeNodeIdReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getCompanyHomeNodeIdReturn"));
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
