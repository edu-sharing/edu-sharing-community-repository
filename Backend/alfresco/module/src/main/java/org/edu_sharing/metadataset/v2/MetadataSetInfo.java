package org.edu_sharing.metadataset.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataSetInfo {
	private String id,name;

	@JsonProperty(required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonProperty(required = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
