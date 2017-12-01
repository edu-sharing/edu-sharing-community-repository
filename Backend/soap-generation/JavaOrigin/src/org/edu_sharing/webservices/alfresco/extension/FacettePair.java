/**
 * FacettePair.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class FacettePair  implements java.io.Serializable {
    private java.lang.Integer count;

    private java.lang.String value;

    public FacettePair() {
    }

    public FacettePair(
           java.lang.Integer count,
           java.lang.String value) {
           this.count = count;
           this.value = value;
    }


    /**
     * Gets the count value for this FacettePair.
     * 
     * @return count
     */
    public java.lang.Integer getCount() {
        return count;
    }


    /**
     * Sets the count value for this FacettePair.
     * 
     * @param count
     */
    public void setCount(java.lang.Integer count) {
        this.count = count;
    }


    /**
     * Gets the value value for this FacettePair.
     * 
     * @return value
     */
    public java.lang.String getValue() {
        return value;
    }


    /**
     * Sets the value value for this FacettePair.
     * 
     * @param value
     */
    public void setValue(java.lang.String value) {
        this.value = value;
    }

   
}
