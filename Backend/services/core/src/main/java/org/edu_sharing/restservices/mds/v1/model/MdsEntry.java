package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.restservices.shared.Mds;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsEntry {

	private Mds mds = null;

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("mds")
	public Mds getMds() {
		return mds;
	}

	public void setMds(Mds mds) {
		this.mds = mds;
	}
	
	
}
