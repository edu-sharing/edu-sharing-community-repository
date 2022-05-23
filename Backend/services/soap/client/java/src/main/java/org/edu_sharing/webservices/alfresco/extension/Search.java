/**
 * Search.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class Search  implements java.io.Serializable {
    private org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias;

    private java.lang.String metadatasetId;

    private int start;

    private int nrOfResults;

    private java.lang.String[] facettes;

    public Search() {
    }

    public Search(
           org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias,
           java.lang.String metadatasetId,
           int start,
           int nrOfResults,
           java.lang.String[] facettes) {
           this.searchCriterias = searchCriterias;
           this.metadatasetId = metadatasetId;
           this.start = start;
           this.nrOfResults = nrOfResults;
           this.facettes = facettes;
    }


    /**
     * Gets the searchCriterias value for this Search.
     * 
     * @return searchCriterias
     */
    public org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] getSearchCriterias() {
        return searchCriterias;
    }


    /**
     * Sets the searchCriterias value for this Search.
     * 
     * @param searchCriterias
     */
    public void setSearchCriterias(org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias) {
        this.searchCriterias = searchCriterias;
    }

    public org.edu_sharing.webservices.alfresco.extension.SearchCriteria getSearchCriterias(int i) {
        return this.searchCriterias[i];
    }

    public void setSearchCriterias(int i, org.edu_sharing.webservices.alfresco.extension.SearchCriteria _value) {
        this.searchCriterias[i] = _value;
    }


    /**
     * Gets the metadatasetId value for this Search.
     * 
     * @return metadatasetId
     */
    public java.lang.String getMetadatasetId() {
        return metadatasetId;
    }


    /**
     * Sets the metadatasetId value for this Search.
     * 
     * @param metadatasetId
     */
    public void setMetadatasetId(java.lang.String metadatasetId) {
        this.metadatasetId = metadatasetId;
    }


    /**
     * Gets the start value for this Search.
     * 
     * @return start
     */
    public int getStart() {
        return start;
    }


    /**
     * Sets the start value for this Search.
     * 
     * @param start
     */
    public void setStart(int start) {
        this.start = start;
    }


    /**
     * Gets the nrOfResults value for this Search.
     * 
     * @return nrOfResults
     */
    public int getNrOfResults() {
        return nrOfResults;
    }


    /**
     * Sets the nrOfResults value for this Search.
     * 
     * @param nrOfResults
     */
    public void setNrOfResults(int nrOfResults) {
        this.nrOfResults = nrOfResults;
    }


    /**
     * Gets the facettes value for this Search.
     * 
     * @return facettes
     */
    public java.lang.String[] getFacettes() {
        return facettes;
    }


    /**
     * Sets the facettes value for this Search.
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

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Search)) return false;
        Search other = (Search) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.searchCriterias==null && other.getSearchCriterias()==null) || 
             (this.searchCriterias!=null &&
              java.util.Arrays.equals(this.searchCriterias, other.getSearchCriterias()))) &&
            ((this.metadatasetId==null && other.getMetadatasetId()==null) || 
             (this.metadatasetId!=null &&
              this.metadatasetId.equals(other.getMetadatasetId()))) &&
            this.start == other.getStart() &&
            this.nrOfResults == other.getNrOfResults() &&
            ((this.facettes==null && other.getFacettes()==null) || 
             (this.facettes!=null &&
              java.util.Arrays.equals(this.facettes, other.getFacettes())));
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
        if (getSearchCriterias() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSearchCriterias());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSearchCriterias(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getMetadatasetId() != null) {
            _hashCode += getMetadatasetId().hashCode();
        }
        _hashCode += getStart();
        _hashCode += getNrOfResults();
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
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Search.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">search"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("searchCriterias");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "searchCriterias"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "SearchCriteria"));
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("metadatasetId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "metadatasetId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("start");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "start"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nrOfResults");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nrOfResults"));
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
