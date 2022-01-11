/**
 * Facette.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public class Facette  implements java.io.Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8776821636556800555L;

	private org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs;

    private java.lang.String property;

    public Facette() {
    }

    public Facette(
           org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs,
           java.lang.String property) {
           this.facettePairs = facettePairs;
           this.property = property;
    }


    /**
     * Gets the facettePairs value for this Facette.
     * 
     * @return facettePairs
     */
    public org.edu_sharing.webservices.alfresco.extension.FacettePair[] getFacettePairs() {
        return facettePairs;
    }


    /**
     * Sets the facettePairs value for this Facette.
     * 
     * @param facettePairs
     */
    public void setFacettePairs(org.edu_sharing.webservices.alfresco.extension.FacettePair[] facettePairs) {
        this.facettePairs = facettePairs;
    }


    /**
     * Gets the property value for this Facette.
     * 
     * @return property
     */
    public java.lang.String getProperty() {
        return property;
    }


    /**
     * Sets the property value for this Facette.
     * 
     * @param property
     */
    public void setProperty(java.lang.String property) {
        this.property = property;
    }

}
