/**
 * Crud.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.crud;

public interface Crud extends java.rmi.Remote {
    public java.lang.String update(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.lang.String nodeId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException;
    public java.lang.String create(java.lang.String username, java.lang.String ticket, java.lang.String nodeType, java.lang.String repositoryId, java.util.HashMap properties, byte[] content, byte[] icon) throws java.rmi.RemoteException;
}
