/**
 * TrackingItem.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.tracking;

public class TrackingItem  implements java.io.Serializable {
    private java.lang.String activity;

    private java.lang.String applicationId;

    private org.edu_sharing.webservices.tracking.TrackingItemContext[] context;

    private java.lang.String place;

    private java.lang.String session;

    private long time;

    private java.lang.String version;

    public TrackingItem() {
    }

    public TrackingItem(
           java.lang.String activity,
           java.lang.String applicationId,
           org.edu_sharing.webservices.tracking.TrackingItemContext[] context,
           java.lang.String place,
           java.lang.String session,
           long time,
           java.lang.String version) {
           this.activity = activity;
           this.applicationId = applicationId;
           this.context = context;
           this.place = place;
           this.session = session;
           this.time = time;
           this.version = version;
    }


    /**
     * Gets the activity value for this TrackingItem.
     * 
     * @return activity
     */
    public java.lang.String getActivity() {
        return activity;
    }


    /**
     * Sets the activity value for this TrackingItem.
     * 
     * @param activity
     */
    public void setActivity(java.lang.String activity) {
        this.activity = activity;
    }


    /**
     * Gets the applicationId value for this TrackingItem.
     * 
     * @return applicationId
     */
    public java.lang.String getApplicationId() {
        return applicationId;
    }


    /**
     * Sets the applicationId value for this TrackingItem.
     * 
     * @param applicationId
     */
    public void setApplicationId(java.lang.String applicationId) {
        this.applicationId = applicationId;
    }


    /**
     * Gets the context value for this TrackingItem.
     * 
     * @return context
     */
    public org.edu_sharing.webservices.tracking.TrackingItemContext[] getContext() {
        return context;
    }


    /**
     * Sets the context value for this TrackingItem.
     * 
     * @param context
     */
    public void setContext(org.edu_sharing.webservices.tracking.TrackingItemContext[] context) {
        this.context = context;
    }


    /**
     * Gets the place value for this TrackingItem.
     * 
     * @return place
     */
    public java.lang.String getPlace() {
        return place;
    }


    /**
     * Sets the place value for this TrackingItem.
     * 
     * @param place
     */
    public void setPlace(java.lang.String place) {
        this.place = place;
    }


    /**
     * Gets the session value for this TrackingItem.
     * 
     * @return session
     */
    public java.lang.String getSession() {
        return session;
    }


    /**
     * Sets the session value for this TrackingItem.
     * 
     * @param session
     */
    public void setSession(java.lang.String session) {
        this.session = session;
    }


    /**
     * Gets the time value for this TrackingItem.
     * 
     * @return time
     */
    public long getTime() {
        return time;
    }


    /**
     * Sets the time value for this TrackingItem.
     * 
     * @param time
     */
    public void setTime(long time) {
        this.time = time;
    }


    /**
     * Gets the version value for this TrackingItem.
     * 
     * @return version
     */
    public java.lang.String getVersion() {
        return version;
    }


    /**
     * Sets the version value for this TrackingItem.
     * 
     * @param version
     */
    public void setVersion(java.lang.String version) {
        this.version = version;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof TrackingItem)) return false;
        TrackingItem other = (TrackingItem) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.activity==null && other.getActivity()==null) || 
             (this.activity!=null &&
              this.activity.equals(other.getActivity()))) &&
            ((this.applicationId==null && other.getApplicationId()==null) || 
             (this.applicationId!=null &&
              this.applicationId.equals(other.getApplicationId()))) &&
            ((this.context==null && other.getContext()==null) || 
             (this.context!=null &&
              java.util.Arrays.equals(this.context, other.getContext()))) &&
            ((this.place==null && other.getPlace()==null) || 
             (this.place!=null &&
              this.place.equals(other.getPlace()))) &&
            ((this.session==null && other.getSession()==null) || 
             (this.session!=null &&
              this.session.equals(other.getSession()))) &&
            this.time == other.getTime() &&
            ((this.version==null && other.getVersion()==null) || 
             (this.version!=null &&
              this.version.equals(other.getVersion())));
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
        if (getActivity() != null) {
            _hashCode += getActivity().hashCode();
        }
        if (getApplicationId() != null) {
            _hashCode += getApplicationId().hashCode();
        }
        if (getContext() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getContext());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getContext(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPlace() != null) {
            _hashCode += getPlace().hashCode();
        }
        if (getSession() != null) {
            _hashCode += getSession().hashCode();
        }
        _hashCode += new Long(getTime()).hashCode();
        if (getVersion() != null) {
            _hashCode += getVersion().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(TrackingItem.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "TrackingItem"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("activity");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "activity"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("applicationId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "applicationId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("context");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "context"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "TrackingItemContext"));
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "items"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("place");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "place"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("session");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "session"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("time");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "time"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("version");
        elemField.setXmlName(new javax.xml.namespace.QName("http://tracking.webservices.edu_sharing.org", "version"));
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
