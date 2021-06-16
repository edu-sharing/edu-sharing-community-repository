/**
 * AuthenticateByApp.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticateByApp  implements java.io.Serializable {
    private java.lang.String applicationId;

    private java.lang.String username;

    private java.lang.String email;

    private java.lang.String ticket;

    private boolean createUser;

    public AuthenticateByApp() {
    }

    public AuthenticateByApp(
           java.lang.String applicationId,
           java.lang.String username,
           java.lang.String email,
           java.lang.String ticket,
           boolean createUser) {
           this.applicationId = applicationId;
           this.username = username;
           this.email = email;
           this.ticket = ticket;
           this.createUser = createUser;
    }


    /**
     * Gets the applicationId value for this AuthenticateByApp.
     * 
     * @return applicationId
     */
    public java.lang.String getApplicationId() {
        return applicationId;
    }


    /**
     * Sets the applicationId value for this AuthenticateByApp.
     * 
     * @param applicationId
     */
    public void setApplicationId(java.lang.String applicationId) {
        this.applicationId = applicationId;
    }


    /**
     * Gets the username value for this AuthenticateByApp.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this AuthenticateByApp.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }


    /**
     * Gets the email value for this AuthenticateByApp.
     * 
     * @return email
     */
    public java.lang.String getEmail() {
        return email;
    }


    /**
     * Sets the email value for this AuthenticateByApp.
     * 
     * @param email
     */
    public void setEmail(java.lang.String email) {
        this.email = email;
    }


    /**
     * Gets the ticket value for this AuthenticateByApp.
     * 
     * @return ticket
     */
    public java.lang.String getTicket() {
        return ticket;
    }


    /**
     * Sets the ticket value for this AuthenticateByApp.
     * 
     * @param ticket
     */
    public void setTicket(java.lang.String ticket) {
        this.ticket = ticket;
    }


    /**
     * Gets the createUser value for this AuthenticateByApp.
     * 
     * @return createUser
     */
    public boolean isCreateUser() {
        return createUser;
    }


    /**
     * Sets the createUser value for this AuthenticateByApp.
     * 
     * @param createUser
     */
    public void setCreateUser(boolean createUser) {
        this.createUser = createUser;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticateByApp)) return false;
        AuthenticateByApp other = (AuthenticateByApp) obj;
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
            ((this.username==null && other.getUsername()==null) || 
             (this.username!=null &&
              this.username.equals(other.getUsername()))) &&
            ((this.email==null && other.getEmail()==null) || 
             (this.email!=null &&
              this.email.equals(other.getEmail()))) &&
            ((this.ticket==null && other.getTicket()==null) || 
             (this.ticket!=null &&
              this.ticket.equals(other.getTicket()))) &&
            this.createUser == other.isCreateUser();
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
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        if (getEmail() != null) {
            _hashCode += getEmail().hashCode();
        }
        if (getTicket() != null) {
            _hashCode += getTicket().hashCode();
        }
        _hashCode += (isCreateUser() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticateByApp.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", ">authenticateByApp"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("applicationId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "applicationId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("username");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "username"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("email");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "email"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ticket");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "ticket"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("createUser");
        elemField.setXmlName(new javax.xml.namespace.QName("http://authentication.webservices.edu_sharing.org", "createUser"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
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
