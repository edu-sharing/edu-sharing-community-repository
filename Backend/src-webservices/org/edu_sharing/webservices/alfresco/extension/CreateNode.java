/**
 * CreateNode.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class CreateNode  implements java.io.Serializable {
    private java.lang.String parentID;

    private java.lang.String nodeTypeString;

    private java.lang.String childAssociation;

    private java.util.HashMap props;

    public CreateNode() {
    }

    public CreateNode(
           java.lang.String parentID,
           java.lang.String nodeTypeString,
           java.lang.String childAssociation,
           java.util.HashMap props) {
           this.parentID = parentID;
           this.nodeTypeString = nodeTypeString;
           this.childAssociation = childAssociation;
           this.props = props;
    }


    /**
     * Gets the parentID value for this CreateNode.
     * 
     * @return parentID
     */
    public java.lang.String getParentID() {
        return parentID;
    }


    /**
     * Sets the parentID value for this CreateNode.
     * 
     * @param parentID
     */
    public void setParentID(java.lang.String parentID) {
        this.parentID = parentID;
    }


    /**
     * Gets the nodeTypeString value for this CreateNode.
     * 
     * @return nodeTypeString
     */
    public java.lang.String getNodeTypeString() {
        return nodeTypeString;
    }


    /**
     * Sets the nodeTypeString value for this CreateNode.
     * 
     * @param nodeTypeString
     */
    public void setNodeTypeString(java.lang.String nodeTypeString) {
        this.nodeTypeString = nodeTypeString;
    }


    /**
     * Gets the childAssociation value for this CreateNode.
     * 
     * @return childAssociation
     */
    public java.lang.String getChildAssociation() {
        return childAssociation;
    }


    /**
     * Sets the childAssociation value for this CreateNode.
     * 
     * @param childAssociation
     */
    public void setChildAssociation(java.lang.String childAssociation) {
        this.childAssociation = childAssociation;
    }


    /**
     * Gets the props value for this CreateNode.
     * 
     * @return props
     */
    public java.util.HashMap getProps() {
        return props;
    }


    /**
     * Sets the props value for this CreateNode.
     * 
     * @param props
     */
    public void setProps(java.util.HashMap props) {
        this.props = props;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CreateNode)) return false;
        CreateNode other = (CreateNode) obj;
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
            ((this.nodeTypeString==null && other.getNodeTypeString()==null) || 
             (this.nodeTypeString!=null &&
              this.nodeTypeString.equals(other.getNodeTypeString()))) &&
            ((this.childAssociation==null && other.getChildAssociation()==null) || 
             (this.childAssociation!=null &&
              this.childAssociation.equals(other.getChildAssociation()))) &&
            ((this.props==null && other.getProps()==null) || 
             (this.props!=null &&
              this.props.equals(other.getProps())));
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
        if (getNodeTypeString() != null) {
            _hashCode += getNodeTypeString().hashCode();
        }
        if (getChildAssociation() != null) {
            _hashCode += getChildAssociation().hashCode();
        }
        if (getProps() != null) {
            _hashCode += getProps().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CreateNode.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", ">createNode"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parentID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "parentID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodeTypeString");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "nodeTypeString"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("childAssociation");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "childAssociation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("props");
        elemField.setXmlName(new javax.xml.namespace.QName("http://extension.alfresco.webservices.edu_sharing.org", "props"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://xml.apache.org/xml-soap", "Map"));
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
