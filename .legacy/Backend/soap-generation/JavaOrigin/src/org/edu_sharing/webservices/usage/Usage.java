/*
 * 
 */

package org.edu_sharing.webservices.usage;

public class Usage {
    public org.edu_sharing.webservices.usage.UsageResult getUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String resourceId) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	return null;
    }
    public org.edu_sharing.webservices.usage.UsageResult[] getUsageByParentNodeId(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String parentNodeId) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	return null;
    }
    public boolean deleteUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String resourceId) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	return false;
    }
    public boolean deleteUsages(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String appSessionId, java.lang.String appCurrentUserId, java.lang.String lmsId, java.lang.String courseId) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	return false;
    }
    public boolean usageAllowed(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String nodeId, java.lang.String lmsId, java.lang.String courseId) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	return false;
    }
    public void setUsage(java.lang.String repositoryTicket, java.lang.String repositoryUsername, java.lang.String lmsId, java.lang.String courseId, java.lang.String parentNodeId, java.lang.String appUser, java.lang.String appUserMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, java.lang.String version, java.lang.String resourceId, java.lang.String xmlParams) throws org.edu_sharing.webservices.usage.UsageException, org.edu_sharing.webservices.authentication.AuthenticationException{
    	
    }
}
