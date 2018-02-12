package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.MimeTypes;
import org.edu_sharing.repository.client.tools.Theme;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gwt.core.client.GWT;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class Node {

	private NodeRef ref = null;
	private NodeRef parent = null;
	private String type = null;
	private List<String> aspects = new ArrayList<String>();
	private String name = null;
	private String title = null;
	private String metadataset = null;
	private String description = null;
	private String repositoryType = null;
	private Date createdAt = null;
	private Person createdBy = null;
	private Date modifiedAt = null;
	private Person modifiedBy = null;
	private List<String> access = null;
	private String contentVersion = null;
	private String contentUrl = null;
	private String downloadUrl = null;
	private HashMap<String,String[]> properties = null;
	private String mimetype = null;
	private String mediatype = null;
	private String size = null;
	private Preview preview = null;
	private String iconURL;
	private String licenseURL;
	private boolean isDirectory;
	private Collection collection;
	private Person owner;
	private int commentCount;

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("ref")
	public NodeRef getRef() {
		return ref;
	}

	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("collection")
	public Collection getCollection() {
		return collection;
	}

	public void setRef(NodeRef ref) {
		this.ref = ref;
	}
	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("parent")
	public NodeRef getParent() {
		return parent;
	}

	public void setParent(NodeRef parent) {
		this.parent = parent;
	}
	
	@JsonProperty
	public int getCommentCount() {
		return commentCount;
	}


	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}


	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("type")
	public String getType() {
		return type;
	}
	@ApiModelProperty(value = "")
	@JsonProperty("iconURL")
	public String getIconURL() {
		return iconURL;
	}
	@ApiModelProperty(value = "")
	@JsonProperty("licenseURL")
	public String getLicenseURL() {
		return licenseURL;
	}
	@ApiModelProperty(value = "")
	@JsonProperty("isDirectory")
	public boolean isDirectory() {
		return isDirectory;
	}
	
	
	public void setType(String type) {
		this.type = type;
	}
	 
	/**
	   **/
	@ApiModelProperty(value = "")
	@JsonProperty("aspects")
	public List<String> getAspects() {
		return aspects;
	}

	public void setAspects(List<String> aspects) {
		this.aspects = aspects;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("createdAt")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("createdBy")
	public Person getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Person createdBy) {
		this.createdBy = createdBy;
	}
	
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("owner")
		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("modifiedAt")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
	public Date getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Date modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("modifiedBy")
	public Person getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Person modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("contentVersion")
	public String getContentVersion() {
		return contentVersion;
	}

	public void setContentVersion(String contentVersion) {
		this.contentVersion = contentVersion;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("contentUrl")
	public String getContentUrl() {
		return contentUrl;
	}

	public void setContentUrl(String contentUrl) {
		this.contentUrl = contentUrl;
	}
	
	
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("downloadUrl")
	public String getDownloadUrl() {
		return downloadUrl;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("access")
	public List<String> getAccess() {
		return access;
	}

	public void setAccess(List<String> access) {
		this.access = access;
	}

	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("properties")
	public HashMap<String, String[]> getProperties() {
		return properties;
	}

	public void setProperties(HashMap<String, String[]> properties) {
		this.properties = properties;
	}


	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("mimetype")
	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}
	/**
	   **/
		@ApiModelProperty(value = "")
		@JsonProperty("mediatype")
		public String getMediatype() {
			return mediatype;
		}

		public void setMediatype(String mediatype) {
			this.mediatype = mediatype;
		}
	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("size")
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}


	@JsonProperty("metadataset")
	public String getMetadataset() {
		return metadataset;
	}


	public void setMetadataset(String metadataset) {
		this.metadataset = metadataset;
	}


	/**
   **/
	@ApiModelProperty(value = "")
	@JsonProperty("preview")
	public Preview getPreview() {
		return preview;
	}

	public void setPreview(Preview preview) {
		this.preview = preview;
	}
	
	
	
	@JsonProperty("repositoryType")
	public String getRepositoryType() {
		return repositoryType;
	}


	public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Node {\n");

		sb.append("  ref: ").append(ref).append("\n");
		sb.append("  parent: ").append(parent).append("\n");
		sb.append("  type: ").append(type).append("\n");
		sb.append("  aspects: ").append(aspects).append("\n");
		sb.append("  name: ").append(name).append("\n");
		sb.append("  title: ").append(title).append("\n");
		sb.append("  description: ").append(description).append("\n");
		sb.append("  createdAt: ").append(createdAt).append("\n");
		sb.append("  createdBy: ").append(createdBy).append("\n");
		sb.append("  modifiedAt: ").append(modifiedAt).append("\n");
		sb.append("  modifiedBy: ").append(modifiedBy).append("\n");
		sb.append("  access: ").append(access).append("\n");
		sb.append("  contentVersion: ").append(contentVersion).append("\n");
		sb.append("  contentUrl: ").append(contentUrl).append("\n");
		sb.append("  properties: ").append(properties).append("\n");
		sb.append("  mimetype: ").append(mimetype).append("\n");
		sb.append("  size: ").append(size).append("\n");
		sb.append("  preview: ").append(preview).append("\n");
		sb.append("}\n");
		return sb.toString();
	}

	public void setIconURL(String iconURL) {
		this.iconURL=iconURL;
		
	}
	public void setLicenseURL(String licenseURL) {
		this.licenseURL=licenseURL;
		
	}
	public void setIsDirectory(boolean isDirectory) {
		this.isDirectory=isDirectory;
	}

	public void setCollection(Collection collection) {
		this.collection=collection;
	}
}
