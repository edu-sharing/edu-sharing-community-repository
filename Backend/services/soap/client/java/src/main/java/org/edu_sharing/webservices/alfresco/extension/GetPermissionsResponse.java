/**
 * GetPermissionsResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetPermissionsResponse  implements java.io.Serializable {
    private org.edu_sharing.repository.client.rpc.ACL getPermissionsReturn;

    public GetPermissionsResponse() {
    }

    public GetPermissionsResponse(
           org.edu_sharing.repository.client.rpc.ACL getPermissionsReturn) {
           this.getPermissionsReturn = getPermissionsReturn;
    }


    /**
     * Gets the getPermissionsReturn value for this GetPermissionsResponse.
     * 
     * @return getPermissionsReturn
     */
    public org.edu_sharing.repository.client.rpc.ACL getGetPermissionsReturn() {
        return getPermissionsReturn;
    }


    /**
     * Sets the getPermissionsReturn value for this GetPermissionsResponse.
     * 
     * @param getPermissionsReturn
     */
    public void setGetPermissionsReturn(org.edu_sharing.repository.client.rpc.ACL getPermissionsReturn) {
        this.getPermissionsReturn = getPermissionsReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetPermissionsResponse)) return false;
        GetPermissionsResponse other = (GetPermissionsResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getPermissionsReturn==null && other.getGetPermissionsReturn()==null) || 
             (this.getPermissionsReturn!=null &&
              this.getPermissionsReturn.equals(other.getGetPermissionsReturn())));
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
        if (getGetPermissionsReturn() != null) {
            _hashCode += getGetPermissionsReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetPermissionsResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getPermissionsResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getPermissionsReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getPermissionsReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACL"));
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
