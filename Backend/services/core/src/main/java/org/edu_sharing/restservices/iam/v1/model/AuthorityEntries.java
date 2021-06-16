package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class AuthorityEntries {

	private List<Authority> list = null;
	private Pagination pagination = null;

	/**
	 **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("authorities")
	public List<Authority> getList() {
		return list;
	}

	public void setList(List<Authority> list) {
		this.list = list;
	}

}
