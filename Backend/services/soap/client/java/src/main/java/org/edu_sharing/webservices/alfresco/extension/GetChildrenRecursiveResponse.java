/**
 * GetChildrenRecursiveResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildrenRecursiveResponse  implements java.io.Serializable {
    private java.util.HashMap getChildrenRecursiveReturn;

    public GetChildrenRecursiveResponse() {
    }

    public GetChildrenRecursiveResponse(
           java.util.HashMap getChildrenRecursiveReturn) {
           this.getChildrenRecursiveReturn = getChildrenRecursiveReturn;
    }


    /**
     * Gets the getChildrenRecursiveReturn value for this GetChildrenRecursiveResponse.
     * 
     * @return getChildrenRecursiveReturn
     */
    public java.util.HashMap getGetChildrenRecursiveReturn() {
        return getChildrenRecursiveReturn;
    }


    /**
     * Sets the getChildrenRecursiveReturn value for this GetChildrenRecursiveResponse.
     * 
     * @param getChildrenRecursiveReturn
     */
    public void setGetChildrenRecursiveReturn(java.util.HashMap getChildrenRecursiveReturn) {
        this.getChildrenRecursiveReturn = getChildrenRecursiveReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildrenRecursiveResponse)) return false;
        GetChildrenRecursiveResponse other = (GetChildrenRecursiveResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getChildrenRecursiveReturn==null && other.getGetChildrenRecursiveReturn()==null) || 
             (this.getChildrenRecursiveReturn!=null &&
              this.getChildrenRecursiveReturn.equals(other.getGetChildrenRecursiveReturn())));
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
        if (getGetChildrenRecursiveReturn() != null) {
            _hashCode += getGetChildrenRecursiveReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildrenRecursiveResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildrenRecursiveResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getChildrenRecursiveReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenRecursiveReturn"));
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
