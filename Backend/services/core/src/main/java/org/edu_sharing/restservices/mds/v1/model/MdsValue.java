package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.metadataset.v2.MetadataKey;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "")
public class MdsValue {
	private String id,caption,description,parent;
	public MdsValue(){};
	public MdsValue(MetadataKey key) {
		id=key.getKey();
		caption=key.getCaption();
		description=key.getDescription();
		parent=key.getParent();
	}

	@JsonProperty
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@JsonProperty(required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	@JsonProperty
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}