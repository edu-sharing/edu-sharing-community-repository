/**
 * RenderInfo.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.render;

public interface RenderInfo extends java.rmi.Remote {
    public org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoRepo(java.lang.String userName, java.lang.String nodeId, java.lang.String version) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.render.RenderInfoResult getRenderInfoLMS(java.lang.String userName, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId, java.lang.String resourceId, java.lang.String version) throws java.rmi.RemoteException;
}
