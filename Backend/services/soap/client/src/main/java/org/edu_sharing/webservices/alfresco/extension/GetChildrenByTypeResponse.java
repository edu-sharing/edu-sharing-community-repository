/**
 * GetChildrenByTypeResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildrenByTypeResponse  implements java.io.Serializable {
    private java.util.HashMap getChildrenByTypeReturn;

    public GetChildrenByTypeResponse() {
    }

    public GetChildrenByTypeResponse(
           java.util.HashMap getChildrenByTypeReturn) {
           this.getChildrenByTypeReturn = getChildrenByTypeReturn;
    }


    /**
     * Gets the getChildrenByTypeReturn value for this GetChildrenByTypeResponse.
     * 
     * @return getChildrenByTypeReturn
     */
    public java.util.HashMap getGetChildrenByTypeReturn() {
        return getChildrenByTypeReturn;
    }


    /**
     * Sets the getChildrenByTypeReturn value for this GetChildrenByTypeResponse.
     * 
     * @param getChildrenByTypeReturn
     */
    public void setGetChildrenByTypeReturn(java.util.HashMap getChildrenByTypeReturn) {
        this.getChildrenByTypeReturn = getChildrenByTypeReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildrenByTypeResponse)) return false;
        GetChildrenByTypeResponse other = (GetChildrenByTypeResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getChildrenByTypeReturn==null && other.getGetChildrenByTypeReturn()==null) || 
             (this.getChildrenByTypeReturn!=null &&
              this.getChildrenByTypeReturn.equals(other.getGetChildrenByTypeReturn())));
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
        if (getGetChildrenByTypeReturn() != null) {
            _hashCode += getGetChildrenByTypeReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildrenByTypeResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildrenByTypeResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getChildrenByTypeReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenByTypeReturn"));
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
