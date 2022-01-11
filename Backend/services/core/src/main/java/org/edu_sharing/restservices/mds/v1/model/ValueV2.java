package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.metadataset.v2.MetadataKey;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel(description = "")
public class ValueV2{
	private String id,caption,description,parent;
	public ValueV2(){};
	public ValueV2(MetadataKey key) {
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

	@JsonProperty
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