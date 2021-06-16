package org.edu_sharing.restservices.usage.v1.model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.Node;

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
		private String appUser;

	    private String appUserMail;

	    private String courseId;

	    private Integer distinctPersons;

	    private Calendar fromUsed;

	    private String appId;

	    private String nodeId;

	    private Calendar toUsed;

	    private Integer usageCounter;

	    private String parentNodeId;

	    private String usageVersion;

	    private Parameters usageXmlParams;
	    
	    private String usageXmlParamsRaw;

	    private String resourceId;
	    
	    private String guid;
		private String appSubtype;
		private String appType;
		private String type;
		
		private Date created;
		
		private Date modified;


		@ApiModelProperty(required = true, value = "")
		@JsonProperty("appUser")
		public String getAppUser() {
			return appUser;
		}

		public void setAppUser(String appUser) {
			this.appUser = appUser;
		}

		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("appUserMail")
		public String getAppUserMail() {
			return appUserMail;
		}

		public void setAppUserMail(String appUserMail) {
			this.appUserMail = appUserMail;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("courseId")
		public String getCourseId() {
			return courseId;
		}

		public void setCourseId(String courseId) {
			this.courseId = courseId;
		}

		@ApiModelProperty(required = false, value = "")
		@JsonProperty("distinctPersons")
		public Integer getDistinctPersons() {
			return distinctPersons;
		}

		public void setDistinctPersons(Integer distinctPersons) {
			this.distinctPersons = distinctPersons;
		}

		
		public Calendar getFromUsed() {
			return fromUsed;
		}

		public void setFromUsed(Calendar fromUsed) {
			this.fromUsed = fromUsed;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("appId")
		public String getAppId() {
			return appId;
		}
		
		public void setAppId(String appId) {
			this.appId = appId;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("nodeId")
		public String getNodeId() {
			return nodeId;
		}

		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}

		public Calendar getToUsed() {
			return toUsed;
		}

		public void setToUsed(Calendar toUsed) {
			this.toUsed = toUsed;
		}

		
		public Integer getUsageCounter() {
			return usageCounter;
		}

		public void setUsageCounter(Integer usageCounter) {
			this.usageCounter = usageCounter;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("parentNodeId")
		public String getParentNodeId() {
			return parentNodeId;
		}

		public void setParentNodeId(String parentNodeId) {
			this.parentNodeId = parentNodeId;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("usageVersion")
		public String getUsageVersion() {
			return usageVersion;
		}

		public void setUsageVersion(String usageVersion) {
			this.usageVersion = usageVersion;
		}

		@ApiModelProperty(required = false, value = "")
		@JsonProperty("usageXmlParams")
		public Parameters getUsageXmlParams() {
			return usageXmlParams;
		}

		public void setUsageXmlParams(Parameters usageXmlParams) {
			this.usageXmlParams = usageXmlParams;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("resourceId")
		public String getResourceId() {
			return resourceId;
		}

		public void setResourceId(String resourceId) {
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

        public void setAppSubtype(String appSubtype) {
            this.appSubtype = appSubtype;
        }

        public String getAppSubtype() {
            return appSubtype;
        }

		public void setAppType(String appType) {
			this.appType = appType;
		}

		public String getAppType() {
			return appType;
		}

		@JsonProperty
        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
        
        public void setCreated(Date created) {
			this.created = created;
		}
        
        public Date getCreated() {
			return created;
		}
        
        public void setModified(Date modified) {
			this.modified = modified;
		}
        
        public Date getModified() {
			return modified;
		}

        @XmlRootElement(name = "usage")
		public static class Parameters {
			@XmlElement public General general;

			public static class General {
				@XmlElement
				public String referencedInName;
				@XmlElement
				public String referencedInType;
				@XmlElement
				public String referencedInInstance;
			}
		}
        
        public void setUsageXmlParamsRaw(String usageXmlParamsRaw) {
			this.usageXmlParamsRaw = usageXmlParamsRaw;
		}
        
        @JsonProperty("usageXmlParamsRaw")
        public String getUsageXmlParamsRaw() {
			return usageXmlParamsRaw;
		}
	}
	public static class CollectionUsage extends Usage {
		private Node collection;

		public Node getCollection() {
			return collection;
		}

		public void setCollection(Node collection) {
			this.collection = collection;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof CollectionUsage){
				CollectionUsage that= (CollectionUsage) obj;
				return that.collection.getRef().equals(collection.getRef());
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			if(collection!=null)
				return Objects.hash(collection.getRef());
			return super.hashCode();
		}
	}
	
	public static class NodeUsage extends Usage{
		private Node node;
		
		public Node getNode() {
			return node;
		}
		
		public void setNode(Node node) {
			this.node = node;
		}
	}
	
}
