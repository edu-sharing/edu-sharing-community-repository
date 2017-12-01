package org.edu_sharing.service.usage;

import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;

public class Usage {

	private java.lang.String appUser;

    private java.lang.String appUserMail;

    private java.lang.String courseId;

    private java.lang.Integer distinctPersons;

    private java.util.Calendar fromUsed;

    private java.lang.String lmsId;

    private java.lang.String nodeId;

    private java.util.Calendar toUsed;

    private java.lang.Integer usageCounter;

    private java.lang.String parentNodeId;

    private java.lang.String usageVersion;

    private java.lang.String usageXmlParams;

    private java.lang.String resourceId;
    
    private String guid;

    
    
	public java.lang.String getAppUser() {
		return appUser;
	}

	public void setAppUser(java.lang.String appUser) {
		this.appUser = appUser;
	}

	public java.lang.String getAppUserMail() {
		return appUserMail;
	}

	public void setAppUserMail(java.lang.String appUserMail) {
		this.appUserMail = appUserMail;
	}

	public java.lang.String getCourseId() {
		return courseId;
	}

	public void setCourseId(java.lang.String courseId) {
		this.courseId = courseId;
	}

	public java.lang.Integer getDistinctPersons() {
		return distinctPersons;
	}

	public void setDistinctPersons(java.lang.Integer distinctPersons) {
		this.distinctPersons = distinctPersons;
	}

	public java.util.Calendar getFromUsed() {
		return fromUsed;
	}

	public void setFromUsed(java.util.Calendar fromUsed) {
		this.fromUsed = fromUsed;
	}

	public java.lang.String getLmsId() {
		return lmsId;
	}

	public void setLmsId(java.lang.String lmsId) {
		this.lmsId = lmsId;
	}

	public java.lang.String getNodeId() {
		return nodeId;
	}

	public void setNodeId(java.lang.String nodeId) {
		this.nodeId = nodeId;
	}

	public java.util.Calendar getToUsed() {
		return toUsed;
	}

	public void setToUsed(java.util.Calendar toUsed) {
		this.toUsed = toUsed;
	}

	public java.lang.Integer getUsageCounter() {
		return usageCounter;
	}

	public void setUsageCounter(java.lang.Integer usageCounter) {
		this.usageCounter = usageCounter;
	}

	public java.lang.String getParentNodeId() {
		return parentNodeId;
	}

	public void setParentNodeId(java.lang.String parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	public java.lang.String getUsageVersion() {
		return usageVersion;
	}

	public void setUsageVersion(java.lang.String usageVersion) {
		this.usageVersion = usageVersion;
	}

	public java.lang.String getUsageXmlParams() {
		return usageXmlParams;
	}

	public void setUsageXmlParams(java.lang.String usageXmlParams) {
		this.usageXmlParams = usageXmlParams;
	}

	public java.lang.String getResourceId() {
		return resourceId;
	}

	public void setResourceId(java.lang.String resourceId) {
		this.resourceId = resourceId;
	}
    
    
	public Map<String,String> toMap(){
		Map<String,String> result = new HashMap<String,String>();
		result.put(CCConstants.CCM_PROP_USAGE_APPID, this.getLmsId());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSER, this.getAppUser());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, this.getAppUserMail());
		if(this.getUsageCounter() != null) result.put(CCConstants.CCM_PROP_USAGE_COUNTER, this.getUsageCounter().toString());
		result.put(CCConstants.CCM_PROP_USAGE_COURSEID, this.getCourseId());
		if( this.getFromUsed() != null)	result.put(CCConstants.CCM_PROP_USAGE_FROM, this.getFromUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_GUID, this.getGuid());
		if(this.getDistinctPersons() != null) result.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, this.getDistinctPersons().toString());
		result.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, this.getParentNodeId());
		result.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, this.getResourceId());
		if(this.getToUsed() != null) result.put(CCConstants.CCM_PROP_USAGE_TO, this.getToUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_VERSION, this.getUsageVersion());
		result.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, this.getUsageXmlParams());
		
		return result;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
    
	
}
