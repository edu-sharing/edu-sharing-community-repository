package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

@ApiModel(description = "")
public class CollectionEntries {

	private List<Node> collections = new ArrayList<>();
	private Pagination pagination;
	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("collections")
	public List<Node> getCollections() {
		return collections;
	}

	public void setCollections(List<Node> collections) {
		this.collections = collections;
	}

	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}
}
