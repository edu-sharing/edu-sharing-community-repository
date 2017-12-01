/**
 * Usage2.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage2;

public interface Usage2 extends java.rmi.Remote {
    public org.edu_sharing.webservices.usage2.Usage2Result[] getUsagesByEduRef(java.lang.String eduRef, java.lang.String user) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception;
    public boolean deleteUsage(java.lang.String eduRef, java.lang.String user, java.lang.String lmsId, java.lang.String courseId, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception;
    public org.edu_sharing.webservices.usage2.Usage2Result getUsage(java.lang.String eduRef, java.lang.String lmsId, java.lang.String courseId, java.lang.String user, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception;
    public org.edu_sharing.webservices.usage2.Usage2Result setUsage(java.lang.String eduRef, java.lang.String user, java.lang.String lmsId, java.lang.String courseId, java.lang.String userMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage2.Usage2Exception;
}
