/**
 * GetChildRecursiveResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildRecursiveResponse  implements java.io.Serializable {
    private java.util.HashMap getChildRecursiveReturn;

    public GetChildRecursiveResponse() {
    }

    public GetChildRecursiveResponse(
           java.util.HashMap getChildRecursiveReturn) {
           this.getChildRecursiveReturn = getChildRecursiveReturn;
    }


    /**
     * Gets the getChildRecursiveReturn value for this GetChildRecursiveResponse.
     * 
     * @return getChildRecursiveReturn
     */
    public java.util.HashMap getGetChildRecursiveReturn() {
        return getChildRecursiveReturn;
    }


    /**
     * Sets the getChildRecursiveReturn value for this GetChildRecursiveResponse.
     * 
     * @param getChildRecursiveReturn
     */
    public void setGetChildRecursiveReturn(java.util.HashMap getChildRecursiveReturn) {
        this.getChildRecursiveReturn = getChildRecursiveReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildRecursiveResponse)) return false;
        GetChildRecursiveResponse other = (GetChildRecursiveResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getChildRecursiveReturn==null && other.getGetChildRecursiveReturn()==null) || 
             (this.getChildRecursiveReturn!=null &&
              this.getChildRecursiveReturn.equals(other.getGetChildRecursiveReturn())));
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
        if (getGetChildRecursiveReturn() != null) {
            _hashCode += getGetChildRecursiveReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildRecursiveResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildRecursiveResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getChildRecursiveReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildRecursiveReturn"));
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
