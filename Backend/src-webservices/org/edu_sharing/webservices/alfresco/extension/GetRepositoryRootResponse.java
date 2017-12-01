/**
 * GetRepositoryRootResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetRepositoryRootResponse  implements java.io.Serializable {
    private java.lang.String getRepositoryRootReturn;

    public GetRepositoryRootResponse() {
    }

    public GetRepositoryRootResponse(
           java.lang.String getRepositoryRootReturn) {
           this.getRepositoryRootReturn = getRepositoryRootReturn;
    }


    /**
     * Gets the getRepositoryRootReturn value for this GetRepositoryRootResponse.
     * 
     * @return getRepositoryRootReturn
     */
    public java.lang.String getGetRepositoryRootReturn() {
        return getRepositoryRootReturn;
    }


    /**
     * Sets the getRepositoryRootReturn value for this GetRepositoryRootResponse.
     * 
     * @param getRepositoryRootReturn
     */
    public void setGetRepositoryRootReturn(java.lang.String getRepositoryRootReturn) {
        this.getRepositoryRootReturn = getRepositoryRootReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetRepositoryRootResponse)) return false;
        GetRepositoryRootResponse other = (GetRepositoryRootResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getRepositoryRootReturn==null && other.getGetRepositoryRootReturn()==null) || 
             (this.getRepositoryRootReturn!=null &&
              this.getRepositoryRootReturn.equals(other.getGetRepositoryRootReturn())));
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
        if (getGetRepositoryRootReturn() != null) {
            _hashCode += getGetRepositoryRootReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetRepositoryRootResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getRepositoryRootResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getRepositoryRootReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getRepositoryRootReturn"));
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
