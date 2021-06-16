/**
 * AuthenticationException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticationException  extends org.apache.axis.AxisFault  implements java.io.Serializable {
    private java.lang.Object cause1;

    private java.lang.String message1;

    public AuthenticationException() {
    }

    public AuthenticationException(
           java.lang.Object cause1,
           java.lang.String message1) {
        this.cause1 = cause1;
        this.message1 = message1;
    }


    /**
     * Gets the cause1 value for this AuthenticationException.
     * 
     * @return cause1
     */
    public java.lang.Object getCause1() {
        return cause1;
    }


    /**
     * Sets the cause1 value for this AuthenticationException.
     * 
     * @param cause1
     */
    public void setCause1(java.lang.Object cause1) {
        this.cause1 = cause1;
    }


    /**
     * Gets the message1 value for this AuthenticationException.
     * 
     * @return message1
     */
    public java.lang.String getMessage1() {
        return message1;
    }


    /**
     * Sets the message1 value for this AuthenticationException.
     * 
     * @param message1
     */
    public void setMessage1(java.lang.String message1) {
        this.message1 = message1;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationException)) return false;
        AuthenticationException other = (AuthenticationException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.cause1==null && other.getCause1()==null) || 
             (this.cause1!=null &&
              this.cause1.equals(other.getCause1()))) &&
            ((this.message1==null && other.getMessage1()==null) || 
             (this.message1!=null &&
              this.message1.equals(other.getMessage1())));
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
        if (getCause1() != null) {
            _hashCode += getCause1().hashCode();
        }
        if (getMessage1() != null) {
            _hashCode += getMessage1().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cause1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "cause"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("message1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "message"));
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


    /**
     * Writes the exception data to the faultDetails
     */
    public void writeDetails(javax.xml.namespace.QName qname, org.apache.axis.encoding.SerializationContext context) throws java.io.IOException {
        context.serialize(qname, null, this);
    }
}
