package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataGroup;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "")
public class MdsGroup {
	private String id;
	private List<String> views;
	private MetadataGroup.Rendering rendering;

	public MdsGroup(){}
	public MdsGroup(MetadataGroup group) {
		this.id=group.getId();
		this.rendering=group.getRendering();
		this.views=group.getViews();
	}
		
		
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty("views")
	public List<String> getViews() {
		return views;
	}
	public void setViews(List<String> views) {
			this.views = views;
		}

	public MetadataGroup.Rendering getRendering() {
		return rendering;
	}

	public void setRendering(MetadataGroup.Rendering rendering) {
		this.rendering = rendering;
	}
}

