/**
 * AuthenticateResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticateResponse  implements java.io.Serializable {
    private org.edu_sharing.webservices.authentication.AuthenticationResult authenticateReturn;

    public AuthenticateResponse() {
    }

    public AuthenticateResponse(
           org.edu_sharing.webservices.authentication.AuthenticationResult authenticateReturn) {
           this.authenticateReturn = authenticateReturn;
    }


    /**
     * Gets the authenticateReturn value for this AuthenticateResponse.
     * 
     * @return authenticateReturn
     */
    public org.edu_sharing.webservices.authentication.AuthenticationResult getAuthenticateReturn() {
        return authenticateReturn;
    }


    /**
     * Sets the authenticateReturn value for this AuthenticateResponse.
     * 
     * @param authenticateReturn
     */
    public void setAuthenticateReturn(org.edu_sharing.webservices.authentication.AuthenticationResult authenticateReturn) {
        this.authenticateReturn = authenticateReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticateResponse)) return false;
        AuthenticateResponse other = (AuthenticateResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.authenticateReturn==null && other.getAuthenticateReturn()==null) || 
             (this.authenticateReturn!=null &&
              this.authenticateReturn.equals(other.getAuthenticateReturn())));
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
        if (getAuthenticateReturn() != null) {
            _hashCode += getAuthenticateReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticateResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", ">authenticateResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticateReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "AuthenticationResult"));
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
