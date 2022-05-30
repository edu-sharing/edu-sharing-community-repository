/**
 * Usage.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage;

public interface Usage extends java.rmi.Remote {
    public boolean deleteUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.usage.UsageResult getUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String resourceId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public boolean deleteUsages(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public boolean usageAllowed(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public void setUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String appUserMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
    public org.edu_sharing.webservices.usage.UsageResult[] getUsageByParentNodeId(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String parentNodeId) throws java.rmi.RemoteException, org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException;
}
