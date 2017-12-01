package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class CollectionEntries {

	private List<Collection> collections = new ArrayList<Collection>();
	private List<CollectionReference> references = new ArrayList<CollectionReference>();

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("collections")
	public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}


	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("references")
	public List<CollectionReference> getReferences() {
		return references;
	}

	public void setReferences(List<CollectionReference> references) {
		this.references = references;
	}
}
