package org.edu_sharing.restservices.admin.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Application {
	@JsonProperty
	private String id;
	@JsonProperty
	private String title;
	@JsonProperty
	private String webserverUrl;
	@JsonProperty
	private String clientBaseUrl;
	@JsonProperty
	private String type;
	@JsonProperty
	private String subtype;
	@JsonProperty
	private String repositoryType;
	@JsonProperty
	private String xml;
	@JsonProperty
	private String file;
	@JsonProperty
	private String contentUrl;
	@JsonProperty
	private String configUrl;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getContentUrl() {
		return contentUrl;
	}
	public void setContentUrl(String contentUrl) {
		this.contentUrl = contentUrl;
	}
	public String getConfigUrl() {
		return configUrl;
	}
	public void setConfigUrl(String configUrl) {
		this.configUrl = configUrl;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getWebserverUrl() {
		return webserverUrl;
	}
	public void setWebserverUrl(String webserverUrl) {
		this.webserverUrl = webserverUrl;
	}
	public String getClientBaseUrl() {
		return clientBaseUrl;
	}
	public void setClientBaseUrl(String clientBaseUrl) {
		this.clientBaseUrl = clientBaseUrl;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getSubtype() {
		return subtype;
	}
	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}
	public String getXml() {
		return xml;
	}
	public void setXml(String xml) {
		this.xml = xml;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getRepositoryType() {
		return repositoryType;
	}
	public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
	}
	
	
}
