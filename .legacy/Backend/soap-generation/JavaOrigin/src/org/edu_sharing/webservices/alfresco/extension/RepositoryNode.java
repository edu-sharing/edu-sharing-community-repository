/**
 * RepositoryNode.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class RepositoryNode  implements java.io.Serializable {
    private java.lang.String nodeId;

    private org.edu_sharing.webservices.alfresco.extension.KeyValue[] properties;

    public RepositoryNode() {
    }

    public RepositoryNode(
           java.lang.String nodeId,
           org.edu_sharing.webservices.alfresco.extension.KeyValue[] properties) {
           this.nodeId = nodeId;
           this.properties = properties;
    }


    /**
     * Gets the nodeId value for this RepositoryNode.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this RepositoryNode.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the properties value for this RepositoryNode.
     * 
     * @return properties
     */
    public org.edu_sharing.webservices.alfresco.extension.KeyValue[] getProperties() {
        return properties;
    }


    /**
     * Sets the properties value for this RepositoryNode.
     * 
     * @param properties
     */
    public void setProperties(org.edu_sharing.webservices.alfresco.extension.KeyValue[] properties) {
        this.properties = properties;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RepositoryNode)) return false;
        RepositoryNode other = (RepositoryNode) obj;
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
            ((this.properties==null && other.getProperties()==null) || 
             (this.properties!=null &&
              java.util.Arrays.equals(this.properties, other.getProperties())));
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
        if (getProperties() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getProperties());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getProperties(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

  

}
