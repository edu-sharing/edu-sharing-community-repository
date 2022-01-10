package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class Mds {

	private MdsRef ref = null;
	private List<MdsType> types = null;
	private List<MdsForm> forms = null;
	private List<MdsList> lists = null;
	private List<MdsView> views = null;
	private MdsQueries queries = null;

	@Schema(required = true, description = "")
	@JsonProperty("ref")
	public MdsRef getRef() {
		return ref;
	}

	public void setRef(MdsRef ref) {
		this.ref = ref;
	}


	@Schema(required = true, description = "")
	@JsonProperty("types")
	public List<MdsType> getModel() {
		return types;
	}

	public void setTypes(List<MdsType> types) {
		this.types = types;
	}


	@Schema(required = true, description = "")
	@JsonProperty("forms")
	public List<MdsForm> getForms() {
		return forms;
	}

	public void setForms(List<MdsForm> forms) {
		this.forms = forms;
	}

	@Schema(required = true, description = "")
	@JsonProperty("lists")
	public List<MdsList> getLists() {
		return lists;
	}

	public void setLists(List<MdsList> lists) {
		this.lists = lists;
	}

	@Schema(required = true, description = "")
	@JsonProperty("views")
	public List<MdsView> getViews() {
		return views;
	}

	public void setViews(List<MdsView> views) {
		this.views = views;
	}

	@Schema(required = true, description = "")
	@JsonProperty("queries")
	public MdsQueries getQueries() {
		return queries;
	}

	public void setQueries(MdsQueries queries) {
		this.queries = queries;
	}
}
