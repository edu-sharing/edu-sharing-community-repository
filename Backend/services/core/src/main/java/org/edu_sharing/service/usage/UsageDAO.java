package org.edu_sharing.service.usage;

import java.util.HashMap;

public interface UsageDAO {
	
	public HashMap<String, HashMap<String, Object>> getUsages(String nodeId);

	HashMap<String, Object> getUsageOnNodeOrParents(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception;

	public HashMap<String, Object> getUsage(String lmsId, String courseId, String objectNodeId, String resourceId) throws Exception;
	
	public HashMap<String, Object> getUsage(String usageId) throws Exception;
	
	public HashMap<String, HashMap<String, Object>> getUsagesByCourse(String lmsId, String courseId) throws Exception;
	
	public HashMap<String, HashMap<String, Object>> getUsagesByAppId(String appId) throws Exception;
	
	public HashMap<String, HashMap<String, Object>> getUsages(String repositoryId,
			String nodeId,
			Long from,
			Long to) throws Exception;
	
	public String createUsage(String parentId, HashMap<String,Object> properties);
	
	public void updateUsage(String usageNodeId, HashMap<String,Object> properties);
	
	
	public void removeUsage(String appId, String courseId, String parentNodeId, String resourceId) throws Exception;
	
	public boolean removeUsages(String appId,String courseId) throws Exception;
	
}
