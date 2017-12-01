/**
 * GetGroupFolderIdResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetGroupFolderIdResponse  implements java.io.Serializable {
    private java.lang.String getGroupFolderIdReturn;

    public GetGroupFolderIdResponse() {
    }

    public GetGroupFolderIdResponse(
           java.lang.String getGroupFolderIdReturn) {
           this.getGroupFolderIdReturn = getGroupFolderIdReturn;
    }


    /**
     * Gets the getGroupFolderIdReturn value for this GetGroupFolderIdResponse.
     * 
     * @return getGroupFolderIdReturn
     */
    public java.lang.String getGetGroupFolderIdReturn() {
        return getGroupFolderIdReturn;
    }


    /**
     * Sets the getGroupFolderIdReturn value for this GetGroupFolderIdResponse.
     * 
     * @param getGroupFolderIdReturn
     */
    public void setGetGroupFolderIdReturn(java.lang.String getGroupFolderIdReturn) {
        this.getGroupFolderIdReturn = getGroupFolderIdReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetGroupFolderIdResponse)) return false;
        GetGroupFolderIdResponse other = (GetGroupFolderIdResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getGroupFolderIdReturn==null && other.getGetGroupFolderIdReturn()==null) || 
             (this.getGroupFolderIdReturn!=null &&
              this.getGroupFolderIdReturn.equals(other.getGetGroupFolderIdReturn())));
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
        if (getGetGroupFolderIdReturn() != null) {
            _hashCode += getGetGroupFolderIdReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetGroupFolderIdResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getGroupFolderIdResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getGroupFolderIdReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getGroupFolderIdReturn"));
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
