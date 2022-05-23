/**
 * GetAssociationNodeIdsResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetAssociationNodeIdsResponse  implements java.io.Serializable {
    private java.lang.Object[] getAssociationNodeIdsReturn;

    public GetAssociationNodeIdsResponse() {
    }

    public GetAssociationNodeIdsResponse(
           java.lang.Object[] getAssociationNodeIdsReturn) {
           this.getAssociationNodeIdsReturn = getAssociationNodeIdsReturn;
    }


    /**
     * Gets the getAssociationNodeIdsReturn value for this GetAssociationNodeIdsResponse.
     * 
     * @return getAssociationNodeIdsReturn
     */
    public java.lang.Object[] getGetAssociationNodeIdsReturn() {
        return getAssociationNodeIdsReturn;
    }


    /**
     * Sets the getAssociationNodeIdsReturn value for this GetAssociationNodeIdsResponse.
     * 
     * @param getAssociationNodeIdsReturn
     */
    public void setGetAssociationNodeIdsReturn(java.lang.Object[] getAssociationNodeIdsReturn) {
        this.getAssociationNodeIdsReturn = getAssociationNodeIdsReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetAssociationNodeIdsResponse)) return false;
        GetAssociationNodeIdsResponse other = (GetAssociationNodeIdsResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getAssociationNodeIdsReturn==null && other.getGetAssociationNodeIdsReturn()==null) || 
             (this.getAssociationNodeIdsReturn!=null &&
              java.util.Arrays.equals(this.getAssociationNodeIdsReturn, other.getGetAssociationNodeIdsReturn())));
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
        if (getGetAssociationNodeIdsReturn() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getGetAssociationNodeIdsReturn());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getGetAssociationNodeIdsReturn(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetAssociationNodeIdsResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getAssociationNodeIdsResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getAssociationNodeIdsReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "getAssociationNodeIdsReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "item"));
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
