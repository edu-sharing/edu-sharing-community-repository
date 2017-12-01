package org.edu_sharing.restservices.usage.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Usages {

	
	private List<Usage> usages;
	
	public Usages() {
		// TODO Auto-generated constructor stub
	}
	
	public Usages(List<Usage> usages) {
		this.usages = usages;
	}
	
	public List<Usage> getUsages() {
		return usages;
	}
	
	public void setUsages(List<Usage> usages) {
		this.usages = usages;
	}
	
	public static class Usage{
		private java.lang.String appUser;

	    private java.lang.String appUserMail;

	    private java.lang.String courseId;

	    private java.lang.Integer distinctPersons;

	    private java.util.Calendar fromUsed;

	    private java.lang.String appId;

	    private java.lang.String nodeId;

	    private java.util.Calendar toUsed;

	    private java.lang.Integer usageCounter;

	    private java.lang.String parentNodeId;

	    private java.lang.String usageVersion;

	    private java.lang.String usageXmlParams;

	    private java.lang.String resourceId;
	    
	    private String guid;

	    
	    @ApiModelProperty(required = true, value = "")
		@JsonProperty("appUser")
		public java.lang.String getAppUser() {
			return appUser;
		}

		public void setAppUser(java.lang.String appUser) {
			this.appUser = appUser;
		}

		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("appUserMail")
		public java.lang.String getAppUserMail() {
			return appUserMail;
		}

		public void setAppUserMail(java.lang.String appUserMail) {
			this.appUserMail = appUserMail;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("courseId")
		public java.lang.String getCourseId() {
			return courseId;
		}

		public void setCourseId(java.lang.String courseId) {
			this.courseId = courseId;
		}

		@ApiModelProperty(required = false, value = "")
		@JsonProperty("distinctPersons")
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

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("appId")
		public java.lang.String getAppId() {
			return appId;
		}
		
		public void setAppId(java.lang.String appId) {
			this.appId = appId;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("nodeId")
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

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("parentNodeId")
		public java.lang.String getParentNodeId() {
			return parentNodeId;
		}

		public void setParentNodeId(java.lang.String parentNodeId) {
			this.parentNodeId = parentNodeId;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("usageVersion")
		public java.lang.String getUsageVersion() {
			return usageVersion;
		}

		public void setUsageVersion(java.lang.String usageVersion) {
			this.usageVersion = usageVersion;
		}

		@ApiModelProperty(required = false, value = "")
		@JsonProperty("usageXmlParams")
		public java.lang.String getUsageXmlParams() {
			return usageXmlParams;
		}

		public void setUsageXmlParams(java.lang.String usageXmlParams) {
			this.usageXmlParams = usageXmlParams;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("resourceId")
		public java.lang.String getResourceId() {
			return resourceId;
		}

		public void setResourceId(java.lang.String resourceId) {
			this.resourceId = resourceId;
		}
	    
		@ApiModelProperty(required = false, value = "")
		@JsonProperty("guid")
		public String getGuid() {
			return guid;
		}

		public void setGuid(String guid) {
			this.guid = guid;
		}
	}
}
