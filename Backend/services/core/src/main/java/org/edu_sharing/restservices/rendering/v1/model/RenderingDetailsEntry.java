package org.edu_sharing.restservices.rendering.v1.model;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class RenderingDetailsEntry {

	String detailsSnippet;
	
	String mimeType;
	
	Node node;
	
	@Schema(required = true, description = "")
	  @JsonProperty("detailsSnippet")
	public String getDetailsSnippet() {
		return detailsSnippet;
	}
	
	public void setDetailsSnippet(String detailsSnippet) {
		this.detailsSnippet = detailsSnippet;
	}
	
	@Schema(required = true, description = "")
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
	
	@Schema(required = true, description = "")
	@JsonProperty("node")
	public Node getNode() {
		return node;
	}
	
}
