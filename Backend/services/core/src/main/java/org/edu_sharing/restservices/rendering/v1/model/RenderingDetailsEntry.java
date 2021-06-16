package org.edu_sharing.restservices.rendering.v1.model;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class RenderingDetailsEntry {

	String detailsSnippet;
	
	String mimeType;
	
	Node node;
	
	@ApiModelProperty(required = true, value = "")
	  @JsonProperty("detailsSnippet")
	public String getDetailsSnippet() {
		return detailsSnippet;
	}
	
	public void setDetailsSnippet(String detailsSnippet) {
		this.detailsSnippet = detailsSnippet;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("mimeType")
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public void setNode(Node node) {
		this.node = node;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("node")
	public Node getNode() {
		return node;
	}
	
}
