package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataCreate;
import org.edu_sharing.restservices.mds.v1.model.*;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class Mds {
		
	public static class Create {
		private boolean onlyMetadata;

		public boolean isOnlyMetadata() {
			return onlyMetadata;
		}

		public void setOnlyMetadata(boolean onlyMetadata) {
			this.onlyMetadata = onlyMetadata;
		}
		public Create(MetadataCreate create) {
			this.onlyMetadata=create.isOnlyMetadata();
		}
	}
	private String name = null;
	private Create create = null;
	private List<MdsWidget> widgets = null;
	private List<MdsView> views;
	private List<MdsGroup> groups;
	private List<MdsList> lists;
	private List<MdsSort> sorts;


	@JsonProperty(required = true)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@JsonProperty(required = false)
	public Create getCreate() {
		return create;
	}
	public void setCreate(Create create) {
		this.create = create;
	}
	@JsonProperty(required = true)
	public List<MdsWidget> getWidgets() {
		return widgets;
	}
	
	

	public void setWidgets(List<MdsWidget> widgets) {
		this.widgets = widgets;
	}
	@JsonProperty(required = true)
	public List<MdsView> getViews() {
		return views;
	}

	public void setViews(List<MdsView> views) {
		this.views = views;
	}
	@JsonProperty(required = true)
	public List<MdsGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<MdsGroup> groups) {
		this.groups = groups;
	}
	@JsonProperty(required = true)
	public List<MdsList> getLists() {
		return lists;
	}
	public void setLists(List<MdsList> lists) {
		this.lists = lists;
	}
	@JsonProperty(required = true)
	public List<MdsSort> getSorts() {
		return sorts;
	}

	public void setSorts(List<MdsSort> sorts) {
		this.sorts = sorts;
	}
}
