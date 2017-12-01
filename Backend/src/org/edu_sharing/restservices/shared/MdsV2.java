package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.restservices.mds.v1.model.GroupV2;
import org.edu_sharing.restservices.mds.v1.model.ListV2;
import org.edu_sharing.restservices.mds.v1.model.ViewV2;
import org.edu_sharing.restservices.mds.v1.model.WidgetV2;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsV2 {
		
	private String name = null;
	private List<WidgetV2> widgets = null;
	private List<ViewV2> views;
	private List<GroupV2> groups;
	private List<ListV2> lists;
	

	@JsonProperty("name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@JsonProperty("widgets")
	public List<WidgetV2> getWidgets() {
		return widgets;
	}
	
	

	public void setWidgets(List<WidgetV2> widgets) {
		this.widgets = widgets;
	}
	@JsonProperty("views")
	public List<ViewV2> getViews() {
		return views;
	}

	public void setViews(List<ViewV2> views) {
		this.views = views;
	}
	@JsonProperty("groups")
	public List<GroupV2> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupV2> groups) {
		this.groups = groups;
	}
	@JsonProperty("lists")
	public List<ListV2> getLists() {
		return lists;
	}
	public void setLists(List<ListV2> lists) {
		this.lists = lists;
	}
	
}
