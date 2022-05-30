/**
 * SetPermissions.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SetPermissions  implements java.io.Serializable {
    private java.lang.String nodeId;

    private org.edu_sharing.repository.client.rpc.ACE[] aces;

    public SetPermissions() {
    }

    public SetPermissions(
           java.lang.String nodeId,
           org.edu_sharing.repository.client.rpc.ACE[] aces) {
           this.nodeId = nodeId;
           this.aces = aces;
    }


    /**
     * Gets the nodeId value for this SetPermissions.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this SetPermissions.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the aces value for this SetPermissions.
     * 
     * @return aces
     */
    public org.edu_sharing.repository.client.rpc.ACE[] getAces() {
        return aces;
    }


    /**
     * Sets the aces value for this SetPermissions.
     * 
     * @param aces
     */
    public void setAces(org.edu_sharing.repository.client.rpc.ACE[] aces) {
        this.aces = aces;
    }

    public org.edu_sharing.repository.client.rpc.ACE getAces(int i) {
        return this.aces[i];
    }

    public void setAces(int i, org.edu_sharing.repository.client.rpc.ACE _value) {
        this.aces[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SetPermissions)) return false;
        SetPermissions other = (SetPermissions) obj;
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
            ((this.aces==null && other.getAces()==null) || 
             (this.aces!=null &&
              java.util.Arrays.equals(this.aces, other.getAces())));
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
        if (getAces() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAces());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAces(), i);
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
        new org.apache.axis.description.TypeDesc(SetPermissions.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">setPermissions"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("aces");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "aces"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://rpc.client.repository.edu_sharing.org", "ACE"));
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
