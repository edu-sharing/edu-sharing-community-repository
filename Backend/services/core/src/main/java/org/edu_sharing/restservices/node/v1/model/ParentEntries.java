package org.edu_sharing.restservices.node.v1.model;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


public class ParentEntries  extends NodeEntries{
	@JsonProperty
	private String scope;

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}
	
}
