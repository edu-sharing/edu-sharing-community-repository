/**
 * AuthenticateByCAS.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticateByCAS  implements java.io.Serializable {
    private java.lang.String username;

    private java.lang.String proxyTicket;

    public AuthenticateByCAS() {
    }

    public AuthenticateByCAS(
           java.lang.String username,
           java.lang.String proxyTicket) {
           this.username = username;
           this.proxyTicket = proxyTicket;
    }


    /**
     * Gets the username value for this AuthenticateByCAS.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this AuthenticateByCAS.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }


    /**
     * Gets the proxyTicket value for this AuthenticateByCAS.
     * 
     * @return proxyTicket
     */
    public java.lang.String getProxyTicket() {
        return proxyTicket;
    }


    /**
     * Sets the proxyTicket value for this AuthenticateByCAS.
     * 
     * @param proxyTicket
     */
    public void setProxyTicket(java.lang.String proxyTicket) {
        this.proxyTicket = proxyTicket;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticateByCAS)) return false;
        AuthenticateByCAS other = (AuthenticateByCAS) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.username==null && other.getUsername()==null) || 
             (this.username!=null &&
              this.username.equals(other.getUsername()))) &&
            ((this.proxyTicket==null && other.getProxyTicket()==null) || 
             (this.proxyTicket!=null &&
              this.proxyTicket.equals(other.getProxyTicket())));
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
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        if (getProxyTicket() != null) {
            _hashCode += getProxyTicket().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticateByCAS.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", ">authenticateByCAS"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("username");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("proxyTicket");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "proxyTicket"));
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
