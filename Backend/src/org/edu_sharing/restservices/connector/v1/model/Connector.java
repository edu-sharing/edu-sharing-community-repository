package org.edu_sharing.restservices.connector.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class Connector {
	private String id;
	private String icon;
	private boolean showNew;
	private String url;
	private String[] parameters;
	private ConnectorFileType[] filetypes;
	@JsonProperty("icon")
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@ApiModelProperty(required = true, value = "false")
	@JsonProperty("showNew")
	public boolean isShowNew() {
		return showNew;
	}
	public void setShowNew(boolean showNew) {
		this.showNew = showNew;
	}
	@JsonProperty("parameters")
	public String[] getParameters() {
		return parameters;
	}
	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}
	@JsonProperty("filetypes")
	public ConnectorFileType[] getFiletypes() {
		return filetypes;
	}
	public void setFiletypes(ConnectorFileType[] filetypes) {
		this.filetypes = filetypes;
	}
}
