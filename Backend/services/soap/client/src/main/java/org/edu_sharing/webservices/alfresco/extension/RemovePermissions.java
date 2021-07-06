/**
 * RemovePermissions.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class RemovePermissions  implements java.io.Serializable {
    private java.lang.String nodeId;

    private java.lang.String _authority;

    private java.lang.String[] _permissions;

    public RemovePermissions() {
    }

    public RemovePermissions(
           java.lang.String nodeId,
           java.lang.String _authority,
           java.lang.String[] _permissions) {
           this.nodeId = nodeId;
           this._authority = _authority;
           this._permissions = _permissions;
    }


    /**
     * Gets the nodeId value for this RemovePermissions.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this RemovePermissions.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the _authority value for this RemovePermissions.
     * 
     * @return _authority
     */
    public java.lang.String get_authority() {
        return _authority;
    }


    /**
     * Sets the _authority value for this RemovePermissions.
     * 
     * @param _authority
     */
    public void set_authority(java.lang.String _authority) {
        this._authority = _authority;
    }


    /**
     * Gets the _permissions value for this RemovePermissions.
     * 
     * @return _permissions
     */
    public java.lang.String[] get_permissions() {
        return _permissions;
    }


    /**
     * Sets the _permissions value for this RemovePermissions.
     * 
     * @param _permissions
     */
    public void set_permissions(java.lang.String[] _permissions) {
        this._permissions = _permissions;
    }

    public java.lang.String get_permissions(int i) {
        return this._permissions[i];
    }

    public void set_permissions(int i, java.lang.String _value) {
        this._permissions[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RemovePermissions)) return false;
        RemovePermissions other = (RemovePermissions) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId()))) &&
            ((this._authority==null && other.get_authority()==null) || 
             (this._authority!=null &&
              this._authority.equals(other.get_authority()))) &&
            ((this._permissions==null && other.get_permissions()==null) || 
             (this._permissions!=null &&
              java.util.Arrays.equals(this._permissions, other.get_permissions())));
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
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        if (get_authority() != null) {
            _hashCode += get_authority().hashCode();
        }
        if (get_permissions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(get_permissions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(get_permissions(), i);
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
        new org.apache.axis.description.TypeDesc(RemovePermissions.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">removePermissions"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("_authority");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "_authority"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("_permissions");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "_permissions"));
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
