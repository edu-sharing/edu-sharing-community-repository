/**
 * AuthenticateByAppResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticateByAppResponse  implements java.io.Serializable {
    private org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByAppReturn;

    public AuthenticateByAppResponse() {
    }

    public AuthenticateByAppResponse(
           org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByAppReturn) {
           this.authenticateByAppReturn = authenticateByAppReturn;
    }


    /**
     * Gets the authenticateByAppReturn value for this AuthenticateByAppResponse.
     * 
     * @return authenticateByAppReturn
     */
    public org.edu_sharing.webservices.authentication.AuthenticationResult getAuthenticateByAppReturn() {
        return authenticateByAppReturn;
    }


    /**
     * Sets the authenticateByAppReturn value for this AuthenticateByAppResponse.
     * 
     * @param authenticateByAppReturn
     */
    public void setAuthenticateByAppReturn(org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByAppReturn) {
        this.authenticateByAppReturn = authenticateByAppReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticateByAppResponse)) return false;
        AuthenticateByAppResponse other = (AuthenticateByAppResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.authenticateByAppReturn==null && other.getAuthenticateByAppReturn()==null) || 
             (this.authenticateByAppReturn!=null &&
              this.authenticateByAppReturn.equals(other.getAuthenticateByAppReturn())));
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
        if (getAuthenticateByAppReturn() != null) {
            _hashCode += getAuthenticateByAppReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticateByAppResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", ">authenticateByAppResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticateByAppReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "authenticateByAppReturn"));
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
