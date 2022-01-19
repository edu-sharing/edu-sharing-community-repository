/**
 * GetChildrenCheckPermissions.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class GetChildrenCheckPermissions  implements java.io.Serializable {
    private java.lang.String parentID;

    private java.lang.String[] permissionsOnChild;

    public GetChildrenCheckPermissions() {
    }

    public GetChildrenCheckPermissions(
           java.lang.String parentID,
           java.lang.String[] permissionsOnChild) {
           this.parentID = parentID;
           this.permissionsOnChild = permissionsOnChild;
    }


    /**
     * Gets the parentID value for this GetChildrenCheckPermissions.
     * 
     * @return parentID
     */
    public java.lang.String getParentID() {
        return parentID;
    }


    /**
     * Sets the parentID value for this GetChildrenCheckPermissions.
     * 
     * @param parentID
     */
    public void setParentID(java.lang.String parentID) {
        this.parentID = parentID;
    }


    /**
     * Gets the permissionsOnChild value for this GetChildrenCheckPermissions.
     * 
     * @return permissionsOnChild
     */
    public java.lang.String[] getPermissionsOnChild() {
        return permissionsOnChild;
    }


    /**
     * Sets the permissionsOnChild value for this GetChildrenCheckPermissions.
     * 
     * @param permissionsOnChild
     */
    public void setPermissionsOnChild(java.lang.String[] permissionsOnChild) {
        this.permissionsOnChild = permissionsOnChild;
    }

    public java.lang.String getPermissionsOnChild(int i) {
        return this.permissionsOnChild[i];
    }

    public void setPermissionsOnChild(int i, java.lang.String _value) {
        this.permissionsOnChild[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetChildrenCheckPermissions)) return false;
        GetChildrenCheckPermissions other = (GetChildrenCheckPermissions) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.parentID==null && other.getParentID()==null) || 
             (this.parentID!=null &&
              this.parentID.equals(other.getParentID()))) &&
            ((this.permissionsOnChild==null && other.getPermissionsOnChild()==null) || 
             (this.permissionsOnChild!=null &&
              java.util.Arrays.equals(this.permissionsOnChild, other.getPermissionsOnChild())));
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
        if (getParentID() != null) {
            _hashCode += getParentID().hashCode();
        }
        if (getPermissionsOnChild() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPermissionsOnChild());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPermissionsOnChild(), i);
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
        new org.apache.axis.description.TypeDesc(GetChildrenCheckPermissions.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">getChildrenCheckPermissions"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parentID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("permissionsOnChild");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "permissionsOnChild"));
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
