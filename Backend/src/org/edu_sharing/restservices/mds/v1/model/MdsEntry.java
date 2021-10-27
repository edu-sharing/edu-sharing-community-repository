package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.restservices.shared.Mds;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsEntry {

	private Mds mds = null;

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("mds")
	public Mds getMds() {
		return mds;
	}

	public void setMds(Mds mds) {
		this.mds = mds;
	}
	
	
}
