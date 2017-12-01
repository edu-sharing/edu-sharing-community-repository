/**
 * Facette.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class Facette  implements java.io.Serializable {
    private org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs;

    private java.lang.String property;

    public Facette() {
    }

    public Facette(
           org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs,
           java.lang.String property) {
           this.facettePairs = facettePairs;
           this.property = property;
    }


    /**
     * Gets the facettePairs value for this Facette.
     * 
     * @return facettePairs
     */
    public org.edu_sharing.webservices.alfresco.extension.FacettePair[] getFacettePairs() {
        return facettePairs;
    }


    /**
     * Sets the facettePairs value for this Facette.
     * 
     * @param facettePairs
     */
    public void setFacettePairs(org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs) {
        this.facettePairs = facettePairs;
    }


    /**
     * Gets the property value for this Facette.
     * 
     * @return property
     */
    public java.lang.String getProperty() {
        return property;
    }


    /**
     * Sets the property value for this Facette.
     * 
     * @param property
     */
    public void setProperty(java.lang.String property) {
        this.property = property;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Facette)) return false;
        Facette other = (Facette) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.facettePairs==null && other.getFacettePairs()==null) || 
             (this.facettePairs!=null &&
              java.util.Arrays.equals(this.facettePairs, other.getFacettePairs()))) &&
            ((this.property==null && other.getProperty()==null) || 
             (this.property!=null &&
              this.property.equals(other.getProperty())));
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
        if (getFacettePairs() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFacettePairs());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFacettePairs(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getProperty() != null) {
            _hashCode += getProperty().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Facette.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "Facette"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("facettePairs");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "facettePairs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "FacettePair"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("property");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "property"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
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
