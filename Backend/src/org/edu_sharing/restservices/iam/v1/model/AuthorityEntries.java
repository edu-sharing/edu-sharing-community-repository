package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class AuthorityEntries {

	private List<Authority> list = null;
	private Pagination pagination = null;

	/**
	 **/
	@Schema(required = true, description = "")
	@JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("authorities")
	public List<Authority> getList() {
		return list;
	}

	public void setList(List<Authority> list) {
		this.list = list;
	}

}
