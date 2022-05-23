/**
 * GuessMimetypeResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GuessMimetypeResponse  implements java.io.Serializable {
    private java.lang.String guessMimetypeReturn;

    public GuessMimetypeResponse() {
    }

    public GuessMimetypeResponse(
           java.lang.String guessMimetypeReturn) {
           this.guessMimetypeReturn = guessMimetypeReturn;
    }


    /**
     * Gets the guessMimetypeReturn value for this GuessMimetypeResponse.
     * 
     * @return guessMimetypeReturn
     */
    public java.lang.String getGuessMimetypeReturn() {
        return guessMimetypeReturn;
    }


    /**
     * Sets the guessMimetypeReturn value for this GuessMimetypeResponse.
     * 
     * @param guessMimetypeReturn
     */
    public void setGuessMimetypeReturn(java.lang.String guessMimetypeReturn) {
        this.guessMimetypeReturn = guessMimetypeReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GuessMimetypeResponse)) return false;
        GuessMimetypeResponse other = (GuessMimetypeResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.guessMimetypeReturn==null && other.getGuessMimetypeReturn()==null) || 
             (this.guessMimetypeReturn!=null &&
              this.guessMimetypeReturn.equals(other.getGuessMimetypeReturn())));
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
        if (getGuessMimetypeReturn() != null) {
            _hashCode += getGuessMimetypeReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GuessMimetypeResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">guessMimetypeResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("guessMimetypeReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "guessMimetypeReturn"));
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
