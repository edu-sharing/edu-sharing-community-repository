package org.edu_sharing.restservices.mds.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsType;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsTypeEntries {

	private List<MdsType> mdsTypes = null;

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("types")
	public List<MdsType> getTypes() {
		return mdsTypes;
	}

	public void setTypes(List<MdsType> mdsTypes) {
		this.mdsTypes = mdsTypes;
	}
}
