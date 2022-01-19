/**
 * SearchNodes.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SearchNodes  implements java.io.Serializable {
    private java.lang.String store;

    private java.lang.String luceneQuery;

    private java.lang.String permission;

    private java.lang.String[] propertiesToReturn;

    public SearchNodes() {
    }

    public SearchNodes(
           java.lang.String store,
           java.lang.String luceneQuery,
           java.lang.String permission,
           java.lang.String[] propertiesToReturn) {
           this.store = store;
           this.luceneQuery = luceneQuery;
           this.permission = permission;
           this.propertiesToReturn = propertiesToReturn;
    }


    /**
     * Gets the store value for this SearchNodes.
     * 
     * @return store
     */
    public java.lang.String getStore() {
        return store;
    }


    /**
     * Sets the store value for this SearchNodes.
     * 
     * @param store
     */
    public void setStore(java.lang.String store) {
        this.store = store;
    }


    /**
     * Gets the luceneQuery value for this SearchNodes.
     * 
     * @return luceneQuery
     */
    public java.lang.String getLuceneQuery() {
        return luceneQuery;
    }


    /**
     * Sets the luceneQuery value for this SearchNodes.
     * 
     * @param luceneQuery
     */
    public void setLuceneQuery(java.lang.String luceneQuery) {
        this.luceneQuery = luceneQuery;
    }


    /**
     * Gets the permission value for this SearchNodes.
     * 
     * @return permission
     */
    public java.lang.String getPermission() {
        return permission;
    }


    /**
     * Sets the permission value for this SearchNodes.
     * 
     * @param permission
     */
    public void setPermission(java.lang.String permission) {
        this.permission = permission;
    }


    /**
     * Gets the propertiesToReturn value for this SearchNodes.
     * 
     * @return propertiesToReturn
     */
    public java.lang.String[] getPropertiesToReturn() {
        return propertiesToReturn;
    }


    /**
     * Sets the propertiesToReturn value for this SearchNodes.
     * 
     * @param propertiesToReturn
     */
    public void setPropertiesToReturn(java.lang.String[] propertiesToReturn) {
        this.propertiesToReturn = propertiesToReturn;
    }

    public java.lang.String getPropertiesToReturn(int i) {
        return this.propertiesToReturn[i];
    }

    public void setPropertiesToReturn(int i, java.lang.String _value) {
        this.propertiesToReturn[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SearchNodes)) return false;
        SearchNodes other = (SearchNodes) obj;
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
              this.permission.equals(other.getPermission()))) &&
            ((this.propertiesToReturn==null && other.getPropertiesToReturn()==null) || 
             (this.propertiesToReturn!=null &&
              java.util.Arrays.equals(this.propertiesToReturn, other.getPropertiesToReturn())));
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
        if (getPropertiesToReturn() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPropertiesToReturn());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPropertiesToReturn(), i);
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
        new org.apache.axis.description.TypeDesc(SearchNodes.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">searchNodes"));
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
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertiesToReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "propertiesToReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
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
