/**
 * SearchCriteria.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class SearchCriteria  implements java.io.Serializable {
    private java.lang.String property;

    private java.lang.String queryId;

    private java.lang.String[] values;

    public SearchCriteria() {
    }

    public SearchCriteria(
           java.lang.String property,
           java.lang.String queryId,
           java.lang.String[] values) {
           this.property = property;
           this.queryId = queryId;
           this.values = values;
    }


    /**
     * Gets the property value for this SearchCriteria.
     * 
     * @return property
     */
    public java.lang.String getProperty() {
        return property;
    }


    /**
     * Sets the property value for this SearchCriteria.
     * 
     * @param property
     */
    public void setProperty(java.lang.String property) {
        this.property = property;
    }


    /**
     * Gets the queryId value for this SearchCriteria.
     * 
     * @return queryId
     */
    public java.lang.String getQueryId() {
        return queryId;
    }


    /**
     * Sets the queryId value for this SearchCriteria.
     * 
     * @param queryId
     */
    public void setQueryId(java.lang.String queryId) {
        this.queryId = queryId;
    }


    /**
     * Gets the values value for this SearchCriteria.
     * 
     * @return values
     */
    public java.lang.String[] getValues() {
        return values;
    }


    /**
     * Sets the values value for this SearchCriteria.
     * 
     * @param values
     */
    public void setValues(java.lang.String[] values) {
        this.values = values;
    }
}
