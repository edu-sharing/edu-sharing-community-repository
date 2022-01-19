/**
 * Authentication.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.authentication;

public interface Authentication extends java.rmi.Remote {
    public boolean checkTicket(java.lang.String username, java.lang.String ticket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByApp(java.lang.String applicationId, java.lang.String username, java.lang.String email, java.lang.String ticket, boolean createUser) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByTrustedApp(java.lang.String applicationId, java.lang.String ticket, org.edu_sharing.webservices.types.KeyValue[] ssoData) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticateByCAS(java.lang.String username, java.lang.String proxyTicket) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.authentication.AuthenticationResult authenticate(java.lang.String username, java.lang.String password) throws java.rmi.RemoteException, org.edu_sharing.webservices.authentication.AuthenticationException;
}
