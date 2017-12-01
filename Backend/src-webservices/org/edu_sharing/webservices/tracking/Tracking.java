/**
 * Tracking.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.tracking;

public interface Tracking extends java.rmi.Remote {
    public void trackEvent(org.edu_sharing.webservices.tracking.TrackingItem event) throws java.rmi.RemoteException, org.edu_sharing.webservices.tracking.TrackingException;
    public void trackEvents(org.edu_sharing.webservices.tracking.TrackingItem[] events) throws java.rmi.RemoteException, org.edu_sharing.webservices.tracking.TrackingException;
}
