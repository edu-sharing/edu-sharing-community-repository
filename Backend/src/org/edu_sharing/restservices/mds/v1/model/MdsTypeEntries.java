package org.edu_sharing.restservices.mds.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsType;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsTypeEntries {

	private List<MdsType> mdsTypes = null;

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("types")
	public List<MdsType> getTypes() {
		return mdsTypes;
	}

	public void setTypes(List<MdsType> mdsTypes) {
		this.mdsTypes = mdsTypes;
	}
}
