package org.edu_sharing.restservices.connector.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ConnectorList {
	private String url;
	
	private Connector[] connectors;
	
	@JsonProperty("connectors")
	public Connector[] getConnectors() {
		return connectors;
	}
	public void setConnectors(Connector[] connectors) {
		this.connectors = connectors;
	}
	@JsonProperty("url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

}
