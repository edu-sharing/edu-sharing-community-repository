/**
 * KeyValue.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.types;

public class KeyValue  implements java.io.Serializable {
    private java.lang.String key;

    private java.lang.String value;

    public KeyValue() {
    }

    public KeyValue(
           java.lang.String key,
           java.lang.String value) {
           this.key = key;
           this.value = value;
    }


    /**
     * Gets the key value for this KeyValue.
     * 
     * @return key
     */
    public java.lang.String getKey() {
        return key;
    }


    /**
     * Sets the key value for this KeyValue.
     * 
     * @param key
     */
    public void setKey(java.lang.String key) {
        this.key = key;
    }


    /**
     * Gets the value value for this KeyValue.
     * 
     * @return value
     */
    public java.lang.String getValue() {
        return value;
    }


    /**
     * Sets the value value for this KeyValue.
     * 
     * @param value
     */
    public void setValue(java.lang.String value) {
        this.value = value;
    }

   

}
