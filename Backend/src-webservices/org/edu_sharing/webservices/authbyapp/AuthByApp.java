/**
 * AuthByApp.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authbyapp;

public interface AuthByApp extends java.rmi.Remote {
    public boolean checkTicket(java.lang.String ticket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, org.edu_sharing.webservices.types.KeyValue[] ssoData) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
}
