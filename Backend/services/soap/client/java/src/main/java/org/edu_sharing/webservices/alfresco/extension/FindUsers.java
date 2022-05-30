/**
 * FindUsers.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class FindUsers  implements java.io.Serializable {
    private org.edu_sharing.webservices.alfresco.extension.KeyValue[] searchProps;

    private java.lang.String eduGroupNodeId;

    private int from;

    private int nrOfResults;

    public FindUsers() {
    }

    public FindUsers(
           org.edu_sharing.webservices.alfresco.extension.KeyValue[] searchProps,
           java.lang.String eduGroupNodeId,
           int from,
           int nrOfResults) {
           this.searchProps = searchProps;
           this.eduGroupNodeId = eduGroupNodeId;
           this.from = from;
           this.nrOfResults = nrOfResults;
    }


    /**
     * Gets the searchProps value for this FindUsers.
     * 
     * @return searchProps
     */
    public org.edu_sharing.webservices.alfresco.extension.KeyValue[] getSearchProps() {
        return searchProps;
    }


    /**
     * Sets the searchProps value for this FindUsers.
     * 
     * @param searchProps
     */
    public void setSearchProps(org.edu_sharing.webservices.alfresco.extension.KeyValue[] searchProps) {
        this.searchProps = searchProps;
    }

    public org.edu_sharing.webservices.alfresco.extension.KeyValue getSearchProps(int i) {
        return this.searchProps[i];
    }

    public void setSearchProps(int i, org.edu_sharing.webservices.alfresco.extension.KeyValue _value) {
        this.searchProps[i] = _value;
    }


    /**
     * Gets the eduGroupNodeId value for this FindUsers.
     * 
     * @return eduGroupNodeId
     */
    public java.lang.String getEduGroupNodeId() {
        return eduGroupNodeId;
    }


    /**
     * Sets the eduGroupNodeId value for this FindUsers.
     * 
     * @param eduGroupNodeId
     */
    public void setEduGroupNodeId(java.lang.String eduGroupNodeId) {
        this.eduGroupNodeId = eduGroupNodeId;
    }


    /**
     * Gets the from value for this FindUsers.
     * 
     * @return from
     */
    public int getFrom() {
        return from;
    }


    /**
     * Sets the from value for this FindUsers.
     * 
     * @param from
     */
    public void setFrom(int from) {
        this.from = from;
    }


    /**
     * Gets the nrOfResults value for this FindUsers.
     * 
     * @return nrOfResults
     */
    public int getNrOfResults() {
        return nrOfResults;
    }


    /**
     * Sets the nrOfResults value for this FindUsers.
     * 
     * @param nrOfResults
     */
    public void setNrOfResults(int nrOfResults) {
        this.nrOfResults = nrOfResults;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof FindUsers)) return false;
        FindUsers other = (FindUsers) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.searchProps==null && other.getSearchProps()==null) || 
             (this.searchProps!=null &&
              java.util.Arrays.equals(this.searchProps, other.getSearchProps()))) &&
            ((this.eduGroupNodeId==null && other.getEduGroupNodeId()==null) || 
             (this.eduGroupNodeId!=null &&
              this.eduGroupNodeId.equals(other.getEduGroupNodeId()))) &&
            this.from == other.getFrom() &&
            this.nrOfResults == other.getNrOfResults();
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
        if (getSearchProps() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSearchProps());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSearchProps(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getEduGroupNodeId() != null) {
            _hashCode += getEduGroupNodeId().hashCode();
        }
        _hashCode += getFrom();
        _hashCode += getNrOfResults();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(FindUsers.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">findUsers"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("searchProps");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchProps"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "KeyValue"));
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eduGroupNodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "eduGroupNodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("from");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "from"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nrOfResults");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfResults"));
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
