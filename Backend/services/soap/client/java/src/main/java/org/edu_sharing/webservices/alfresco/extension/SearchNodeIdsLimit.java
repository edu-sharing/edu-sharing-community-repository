/**
 * SearchNodeIdsLimit.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SearchNodeIdsLimit  implements java.io.Serializable {
    private java.lang.String luceneString;

    private int limit;

    public SearchNodeIdsLimit() {
    }

    public SearchNodeIdsLimit(
           java.lang.String luceneString,
           int limit) {
           this.luceneString = luceneString;
           this.limit = limit;
    }


    /**
     * Gets the luceneString value for this SearchNodeIdsLimit.
     * 
     * @return luceneString
     */
    public java.lang.String getLuceneString() {
        return luceneString;
    }


    /**
     * Sets the luceneString value for this SearchNodeIdsLimit.
     * 
     * @param luceneString
     */
    public void setLuceneString(java.lang.String luceneString) {
        this.luceneString = luceneString;
    }


    /**
     * Gets the limit value for this SearchNodeIdsLimit.
     * 
     * @return limit
     */
    public int getLimit() {
        return limit;
    }


    /**
     * Sets the limit value for this SearchNodeIdsLimit.
     * 
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SearchNodeIdsLimit)) return false;
        SearchNodeIdsLimit other = (SearchNodeIdsLimit) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.luceneString==null && other.getLuceneString()==null) || 
             (this.luceneString!=null &&
              this.luceneString.equals(other.getLuceneString()))) &&
            this.limit == other.getLimit();
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
        if (getLuceneString() != null) {
            _hashCode += getLuceneString().hashCode();
        }
        _hashCode += getLimit();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SearchNodeIdsLimit.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">searchNodeIdsLimit"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("luceneString");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "luceneString"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("limit");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "limit"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
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
