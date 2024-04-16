/**
 * Child.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.types;

public class Child  implements java.io.Serializable {
    private java.lang.String[] aspects;

    private java.lang.String iconUrl;

    private java.lang.String previewUrl;

    private org.edu_sharing.webservices.types.KeyValue[] properties;

    public Child() {
    }

    public Child(
           java.lang.String[] aspects,
           java.lang.String iconUrl,
           java.lang.String previewUrl,
           org.edu_sharing.webservices.types.KeyValue[] properties) {
           this.aspects = aspects;
           this.iconUrl = iconUrl;
           this.previewUrl = previewUrl;
           this.properties = properties;
    }


    /**
     * Gets the aspects value for this Child.
     * 
     * @return aspects
     */
    public java.lang.String[] getAspects() {
        return aspects;
    }


    /**
     * Sets the aspects value for this Child.
     * 
     * @param aspects
     */
    public void setAspects(java.lang.String[] aspects) {
        this.aspects = aspects;
    }


    /**
     * Gets the iconUrl value for this Child.
     * 
     * @return iconUrl
     */
    public java.lang.String getIconUrl() {
        return iconUrl;
    }


    /**
     * Sets the iconUrl value for this Child.
     * 
     * @param iconUrl
     */
    public void setIconUrl(java.lang.String iconUrl) {
        this.iconUrl = iconUrl;
    }


    /**
     * Gets the previewUrl value for this Child.
     * 
     * @return previewUrl
     */
    public java.lang.String getPreviewUrl() {
        return previewUrl;
    }


    /**
     * Sets the previewUrl value for this Child.
     * 
     * @param previewUrl
     */
    public void setPreviewUrl(java.lang.String previewUrl) {
        this.previewUrl = previewUrl;
    }


    /**
     * Gets the properties value for this Child.
     * 
     * @return properties
     */
    public org.edu_sharing.webservices.types.KeyValue[] getProperties() {
        return properties;
    }


    /**
     * Sets the properties value for this Child.
     * 
     * @param properties
     */
    public void setProperties(org.edu_sharing.webservices.types.KeyValue[] properties) {
        this.properties = properties;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Child)) return false;
        Child other = (Child) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.aspects==null && other.getAspects()==null) || 
             (this.aspects!=null &&
              java.util.Arrays.equals(this.aspects, other.getAspects()))) &&
            ((this.iconUrl==null && other.getIconUrl()==null) || 
             (this.iconUrl!=null &&
              this.iconUrl.equals(other.getIconUrl()))) &&
            ((this.previewUrl==null && other.getPreviewUrl()==null) || 
             (this.previewUrl!=null &&
              this.previewUrl.equals(other.getPreviewUrl()))) &&
            ((this.properties==null && other.getProperties()==null) || 
             (this.properties!=null &&
              java.util.Arrays.equals(this.properties, other.getProperties())));
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
        if (getAspects() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAspects());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAspects(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getIconUrl() != null) {
            _hashCode += getIconUrl().hashCode();
        }
        if (getPreviewUrl() != null) {
            _hashCode += getPreviewUrl().hashCode();
        }
        if (getProperties() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getProperties());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getProperties(), i);
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
        new org.apache.axis.description.TypeDesc(Child.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "Child"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("aspects");
        elemField.setXmlName(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "aspects"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("iconUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "iconUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("previewUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "previewUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("properties");
        elemField.setXmlName(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "properties"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
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
