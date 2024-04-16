/**
 * MoveNode.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class MoveNode  implements java.io.Serializable {
    private java.lang.String newParentId;

    private java.lang.String childAssocType;

    private java.lang.String nodeId;

    public MoveNode() {
    }

    public MoveNode(
           java.lang.String newParentId,
           java.lang.String childAssocType,
           java.lang.String nodeId) {
           this.newParentId = newParentId;
           this.childAssocType = childAssocType;
           this.nodeId = nodeId;
    }


    /**
     * Gets the newParentId value for this MoveNode.
     * 
     * @return newParentId
     */
    public java.lang.String getNewParentId() {
        return newParentId;
    }


    /**
     * Sets the newParentId value for this MoveNode.
     * 
     * @param newParentId
     */
    public void setNewParentId(java.lang.String newParentId) {
        this.newParentId = newParentId;
    }


    /**
     * Gets the childAssocType value for this MoveNode.
     * 
     * @return childAssocType
     */
    public java.lang.String getChildAssocType() {
        return childAssocType;
    }


    /**
     * Sets the childAssocType value for this MoveNode.
     * 
     * @param childAssocType
     */
    public void setChildAssocType(java.lang.String childAssocType) {
        this.childAssocType = childAssocType;
    }


    /**
     * Gets the nodeId value for this MoveNode.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this MoveNode.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MoveNode)) return false;
        MoveNode other = (MoveNode) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.newParentId==null && other.getNewParentId()==null) || 
             (this.newParentId!=null &&
              this.newParentId.equals(other.getNewParentId()))) &&
            ((this.childAssocType==null && other.getChildAssocType()==null) || 
             (this.childAssocType!=null &&
              this.childAssocType.equals(other.getChildAssocType()))) &&
            ((this.nodeId==null && other.getNodeId()==null) || 
             (this.nodeId!=null &&
              this.nodeId.equals(other.getNodeId())));
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
        if (getNewParentId() != null) {
            _hashCode += getNewParentId().hashCode();
        }
        if (getChildAssocType() != null) {
            _hashCode += getChildAssocType().hashCode();
        }
        if (getNodeId() != null) {
            _hashCode += getNodeId().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MoveNode.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">moveNode"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("newParentId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "newParentId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("childAssocType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childAssocType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeId"));
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
