/**
 * AuthenticateByTrustedApp.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authbyapp;

public class AuthenticateByTrustedApp  implements java.io.Serializable {
    private java.lang.String applicationId;

    private org.edu_sharing.webservices.types.KeyValue[] ssoData;

    public AuthenticateByTrustedApp() {
    }

    public AuthenticateByTrustedApp(
           java.lang.String applicationId,
           org.edu_sharing.webservices.types.KeyValue[] ssoData) {
           this.applicationId = applicationId;
           this.ssoData = ssoData;
    }


    /**
     * Gets the applicationId value for this AuthenticateByTrustedApp.
     * 
     * @return applicationId
     */
    public java.lang.String getApplicationId() {
        return applicationId;
    }


    /**
     * Sets the applicationId value for this AuthenticateByTrustedApp.
     * 
     * @param applicationId
     */
    public void setApplicationId(java.lang.String applicationId) {
        this.applicationId = applicationId;
    }


    /**
     * Gets the ssoData value for this AuthenticateByTrustedApp.
     * 
     * @return ssoData
     */
    public org.edu_sharing.webservices.types.KeyValue[] getSsoData() {
        return ssoData;
    }


    /**
     * Sets the ssoData value for this AuthenticateByTrustedApp.
     * 
     * @param ssoData
     */
    public void setSsoData(org.edu_sharing.webservices.types.KeyValue[] ssoData) {
        this.ssoData = ssoData;
    }

    public org.edu_sharing.webservices.types.KeyValue getSsoData(int i) {
        return this.ssoData[i];
    }

    public void setSsoData(int i, org.edu_sharing.webservices.types.KeyValue _value) {
        this.ssoData[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticateByTrustedApp)) return false;
        AuthenticateByTrustedApp other = (AuthenticateByTrustedApp) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.applicationId==null && other.getApplicationId()==null) || 
             (this.applicationId!=null &&
              this.applicationId.equals(other.getApplicationId()))) &&
            ((this.ssoData==null && other.getSsoData()==null) || 
             (this.ssoData!=null &&
              java.util.Arrays.equals(this.ssoData, other.getSsoData())));
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
        if (getApplicationId() != null) {
            _hashCode += getApplicationId().hashCode();
        }
        if (getSsoData() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSsoData());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSsoData(), i);
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
        new org.apache.axis.description.TypeDesc(AuthenticateByTrustedApp.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", ">authenticateByTrustedApp"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("applicationId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "applicationId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ssoData");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authbyapp.webservices.edu_sharing.org", "ssoData"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"));
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
