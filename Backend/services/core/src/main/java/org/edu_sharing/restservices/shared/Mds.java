package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class Mds {

	private MdsRef ref = null;
	private List<MdsType> types = null;
	private List<MdsForm> forms = null;
	private List<MdsList> lists = null;
	private List<MdsView> views = null;
	private MdsQueries queries = null;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("ref")
	public MdsRef getRef() {
		return ref;
	}

	public void setRef(MdsRef ref) {
		this.ref = ref;
	}


	@ApiModelProperty(required = true, value = "")
	@JsonProperty("types")
	public List<MdsType> getModel() {
		return types;
	}

	public void setTypes(List<MdsType> types) {
		this.types = types;
	}


	@ApiModelProperty(required = true, value = "")
	@JsonProperty("forms")
	public List<MdsForm> getForms() {
		return forms;
	}

	public void setForms(List<MdsForm> forms) {
		this.forms = forms;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("lists")
	public List<MdsList> getLists() {
		return lists;
	}

	public void setLists(List<MdsList> lists) {
		this.lists = lists;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("views")
	public List<MdsView> getViews() {
		return views;
	}

	public void setViews(List<MdsView> views) {
		this.views = views;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("queries")
	public MdsQueries getQueries() {
		return queries;
	}

	public void setQueries(MdsQueries queries) {
		this.queries = queries;
	}
}
