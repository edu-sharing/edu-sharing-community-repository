/**
 * GetRenderInfoLMSResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.render;

public class GetRenderInfoLMSResponse  implements java.io.Serializable {
    private org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoLMSReturn;

    public GetRenderInfoLMSResponse() {
    }

    public GetRenderInfoLMSResponse(
           org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoLMSReturn) {
           this.getRenderInfoLMSReturn = getRenderInfoLMSReturn;
    }


    /**
     * Gets the getRenderInfoLMSReturn value for this GetRenderInfoLMSResponse.
     * 
     * @return getRenderInfoLMSReturn
     */
    public org.edu_sharing.webservices.render.RenderInfoResult getGetRenderInfoLMSReturn() {
        return getRenderInfoLMSReturn;
    }


    /**
     * Sets the getRenderInfoLMSReturn value for this GetRenderInfoLMSResponse.
     * 
     * @param getRenderInfoLMSReturn
     */
    public void setGetRenderInfoLMSReturn(org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoLMSReturn) {
        this.getRenderInfoLMSReturn = getRenderInfoLMSReturn;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetRenderInfoLMSResponse)) return false;
        GetRenderInfoLMSResponse other = (GetRenderInfoLMSResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getRenderInfoLMSReturn==null && other.getGetRenderInfoLMSReturn()==null) || 
             (this.getRenderInfoLMSReturn!=null &&
              this.getRenderInfoLMSReturn.equals(other.getGetRenderInfoLMSReturn())));
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
        if (getGetRenderInfoLMSReturn() != null) {
            _hashCode += getGetRenderInfoLMSReturn().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetRenderInfoLMSResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", ">getRenderInfoLMSResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getRenderInfoLMSReturn");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "getRenderInfoLMSReturn"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "RenderInfoResult"));
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
