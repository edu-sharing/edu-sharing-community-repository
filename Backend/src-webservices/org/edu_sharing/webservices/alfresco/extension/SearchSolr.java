/**
 * SearchSolr.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SearchSolr  implements java.io.Serializable {
    private java.lang.String query;

    private int startIdx;

    private int nrOfresults;

    private java.lang.String[] facettes;

    private int facettesMinCount;

    private int facettesLimit;

    public SearchSolr() {
    }

    public SearchSolr(
           java.lang.String query,
           int startIdx,
           int nrOfresults,
           java.lang.String[] facettes,
           int facettesMinCount,
           int facettesLimit) {
           this.query = query;
           this.startIdx = startIdx;
           this.nrOfresults = nrOfresults;
           this.facettes = facettes;
           this.facettesMinCount = facettesMinCount;
           this.facettesLimit = facettesLimit;
    }


    /**
     * Gets the query value for this SearchSolr.
     * 
     * @return query
     */
    public java.lang.String getQuery() {
        return query;
    }


    /**
     * Sets the query value for this SearchSolr.
     * 
     * @param query
     */
    public void setQuery(java.lang.String query) {
        this.query = query;
    }


    /**
     * Gets the startIdx value for this SearchSolr.
     * 
     * @return startIdx
     */
    public int getStartIdx() {
        return startIdx;
    }


    /**
     * Sets the startIdx value for this SearchSolr.
     * 
     * @param startIdx
     */
    public void setStartIdx(int startIdx) {
        this.startIdx = startIdx;
    }


    /**
     * Gets the nrOfresults value for this SearchSolr.
     * 
     * @return nrOfresults
     */
    public int getNrOfresults() {
        return nrOfresults;
    }


    /**
     * Sets the nrOfresults value for this SearchSolr.
     * 
     * @param nrOfresults
     */
    public void setNrOfresults(int nrOfresults) {
        this.nrOfresults = nrOfresults;
    }


    /**
     * Gets the facettes value for this SearchSolr.
     * 
     * @return facettes
     */
    public java.lang.String[] getFacettes() {
        return facettes;
    }


    /**
     * Sets the facettes value for this SearchSolr.
     * 
     * @param facettes
     */
    public void setFacettes(java.lang.String[] facettes) {
        this.facettes = facettes;
    }

    public java.lang.String getFacettes(int i) {
        return this.facettes[i];
    }

    public void setFacettes(int i, java.lang.String _value) {
        this.facettes[i] = _value;
    }


    /**
     * Gets the facettesMinCount value for this SearchSolr.
     * 
     * @return facettesMinCount
     */
    public int getFacettesMinCount() {
        return facettesMinCount;
    }


    /**
     * Sets the facettesMinCount value for this SearchSolr.
     * 
     * @param facettesMinCount
     */
    public void setFacettesMinCount(int facettesMinCount) {
        this.facettesMinCount = facettesMinCount;
    }


    /**
     * Gets the facettesLimit value for this SearchSolr.
     * 
     * @return facettesLimit
     */
    public int getFacettesLimit() {
        return facettesLimit;
    }


    /**
     * Sets the facettesLimit value for this SearchSolr.
     * 
     * @param facettesLimit
     */
    public void setFacettesLimit(int facettesLimit) {
        this.facettesLimit = facettesLimit;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SearchSolr)) return false;
        SearchSolr other = (SearchSolr) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.query==null && other.getQuery()==null) || 
             (this.query!=null &&
              this.query.equals(other.getQuery()))) &&
            this.startIdx == other.getStartIdx() &&
            this.nrOfresults == other.getNrOfresults() &&
            ((this.facettes==null && other.getFacettes()==null) || 
             (this.facettes!=null &&
              java.util.Arrays.equals(this.facettes, other.getFacettes()))) &&
            this.facettesMinCount == other.getFacettesMinCount() &&
            this.facettesLimit == other.getFacettesLimit();
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
        if (getQuery() != null) {
            _hashCode += getQuery().hashCode();
        }
        _hashCode += getStartIdx();
        _hashCode += getNrOfresults();
        if (getFacettes() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFacettes());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFacettes(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += getFacettesMinCount();
        _hashCode += getFacettesLimit();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SearchSolr.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">searchSolr"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("query");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "query"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startIdx");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "startIdx"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nrOfresults");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfresults"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("facettes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("facettesMinCount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettesMinCount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("facettesLimit");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettesLimit"));
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
