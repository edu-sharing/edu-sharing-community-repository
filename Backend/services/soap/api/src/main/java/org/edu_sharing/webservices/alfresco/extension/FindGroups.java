/**
 * FindGroups.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class FindGroups  implements java.io.Serializable {
    private java.lang.String searchWord;

    private java.lang.String eduGroupNodeId;

    private int from;

    private int nrOfResults;

    public FindGroups() {
    }

    public FindGroups(
           java.lang.String searchWord,
           java.lang.String eduGroupNodeId,
           int from,
           int nrOfResults) {
           this.searchWord = searchWord;
           this.eduGroupNodeId = eduGroupNodeId;
           this.from = from;
           this.nrOfResults = nrOfResults;
    }


    /**
     * Gets the searchWord value for this FindGroups.
     * 
     * @return searchWord
     */
    public java.lang.String getSearchWord() {
        return searchWord;
    }


    /**
     * Sets the searchWord value for this FindGroups.
     * 
     * @param searchWord
     */
    public void setSearchWord(java.lang.String searchWord) {
        this.searchWord = searchWord;
    }


    /**
     * Gets the eduGroupNodeId value for this FindGroups.
     * 
     * @return eduGroupNodeId
     */
    public java.lang.String getEduGroupNodeId() {
        return eduGroupNodeId;
    }


    /**
     * Sets the eduGroupNodeId value for this FindGroups.
     * 
     * @param eduGroupNodeId
     */
    public void setEduGroupNodeId(java.lang.String eduGroupNodeId) {
        this.eduGroupNodeId = eduGroupNodeId;
    }


    /**
     * Gets the from value for this FindGroups.
     * 
     * @return from
     */
    public int getFrom() {
        return from;
    }


    /**
     * Sets the from value for this FindGroups.
     * 
     * @param from
     */
    public void setFrom(int from) {
        this.from = from;
    }


    /**
     * Gets the nrOfResults value for this FindGroups.
     * 
     * @return nrOfResults
     */
    public int getNrOfResults() {
        return nrOfResults;
    }


    /**
     * Sets the nrOfResults value for this FindGroups.
     * 
     * @param nrOfResults
     */
    public void setNrOfResults(int nrOfResults) {
        this.nrOfResults = nrOfResults;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof FindGroups)) return false;
        FindGroups other = (FindGroups) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.searchWord==null && other.getSearchWord()==null) || 
             (this.searchWord!=null &&
              this.searchWord.equals(other.getSearchWord()))) &&
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
        if (getSearchWord() != null) {
            _hashCode += getSearchWord().hashCode();
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
        new org.apache.axis.description.TypeDesc(FindGroups.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">findGroups"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("searchWord");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchWord"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
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
