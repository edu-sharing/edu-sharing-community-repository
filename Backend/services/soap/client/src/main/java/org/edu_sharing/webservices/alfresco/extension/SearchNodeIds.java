/**
 * SearchNodeIds.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SearchNodeIds  implements java.io.Serializable {
    private java.lang.String store;

    private java.lang.String luceneQuery;

    private java.lang.String permission;

    public SearchNodeIds() {
    }

    public SearchNodeIds(
           java.lang.String store,
           java.lang.String luceneQuery,
           java.lang.String permission) {
           this.store = store;
           this.luceneQuery = luceneQuery;
           this.permission = permission;
    }


    /**
     * Gets the store value for this SearchNodeIds.
     * 
     * @return store
     */
    public java.lang.String getStore() {
        return store;
    }


    /**
     * Sets the store value for this SearchNodeIds.
     * 
     * @param store
     */
    public void setStore(java.lang.String store) {
        this.store = store;
    }


    /**
     * Gets the luceneQuery value for this SearchNodeIds.
     * 
     * @return luceneQuery
     */
    public java.lang.String getLuceneQuery() {
        return luceneQuery;
    }


    /**
     * Sets the luceneQuery value for this SearchNodeIds.
     * 
     * @param luceneQuery
     */
    public void setLuceneQuery(java.lang.String luceneQuery) {
        this.luceneQuery = luceneQuery;
    }


    /**
     * Gets the permission value for this SearchNodeIds.
     * 
     * @return permission
     */
    public java.lang.String getPermission() {
        return permission;
    }


    /**
     * Sets the permission value for this SearchNodeIds.
     * 
     * @param permission
     */
    public void setPermission(java.lang.String permission) {
        this.permission = permission;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SearchNodeIds)) return false;
        SearchNodeIds other = (SearchNodeIds) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.store==null && other.getStore()==null) || 
             (this.store!=null &&
              this.store.equals(other.getStore()))) &&
            ((this.luceneQuery==null && other.getLuceneQuery()==null) || 
             (this.luceneQuery!=null &&
              this.luceneQuery.equals(other.getLuceneQuery()))) &&
            ((this.permission==null && other.getPermission()==null) || 
             (this.permission!=null &&
              this.permission.equals(other.getPermission())));
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
        if (getStore() != null) {
            _hashCode += getStore().hashCode();
        }
        if (getLuceneQuery() != null) {
            _hashCode += getLuceneQuery().hashCode();
        }
        if (getPermission() != null) {
            _hashCode += getPermission().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SearchNodeIds.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">searchNodeIds"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("store");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "store"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("luceneQuery");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "luceneQuery"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("permission");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permission"));
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
