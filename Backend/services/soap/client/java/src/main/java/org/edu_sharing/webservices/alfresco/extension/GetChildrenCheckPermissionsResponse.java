/**
 * GetChildrenCheckPermissionsResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildrenCheckPermissionsResponse  implements java.io.Serializable {
    private java.util.HashMap getChildrenCheckPermissionsReturn;

    public GetChildrenCheckPermissionsResponse() {
    }

    public GetChildrenCheckPermissionsResponse(
           java.util.HashMap getChildrenCheckPermissionsReturn) {
           this.getChildrenCheckPermissionsReturn = getChildrenCheckPermissionsReturn;
    }


    /**
     * Gets the getChildrenCheckPermissionsReturn value for this GetChildrenCheckPermissionsResponse.
     * 
     * @return getChildrenCheckPermissionsReturn
     */
    public java.util.HashMap getGetChildrenCheckPermissionsReturn() {
        return getChildrenCheckPermissionsReturn;
    }


    /**
     * Sets the getChildrenCheckPermissionsReturn value for this GetChildrenCheckPermissionsResponse.
     * 
     * @param getChildrenCheckPermissionsReturn
     */
    public void setGetChildrenCheckPermissionsReturn(java.util.HashMap getChildrenCheckPermissionsReturn) {
        this.getChildrenCheckPermissionsReturn = getChildrenCheckPermissionsReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildrenCheckPermissionsResponse)) return false;
        GetChildrenCheckPermissionsResponse other = (GetChildrenCheckPermissionsResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getChildrenCheckPermissionsReturn==null && other.getGetChildrenCheckPermissionsReturn()==null) || 
             (this.getChildrenCheckPermissionsReturn!=null &&
              this.getChildrenCheckPermissionsReturn.equals(other.getGetChildrenCheckPermissionsReturn())));
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
        if (getGetChildrenCheckPermissionsReturn() != null) {
            _hashCode += getGetChildrenCheckPermissionsReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetChildrenCheckPermissionsResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildrenCheckPermissionsResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getChildrenCheckPermissionsReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getChildrenCheckPermissionsReturn"));
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
