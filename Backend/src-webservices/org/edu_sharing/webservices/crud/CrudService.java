/**
 * CrudService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.crud;

public interface CrudService extends javax.xml.rpc.Service {
    public java.lang.String getcrudAddress();

    public org.edu_sharing.webservices.crud.Crud getcrud() throws javax.xml.rpc.ServiceException;

    public org.edu_sharing.webservices.crud.Crud getcrud(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
