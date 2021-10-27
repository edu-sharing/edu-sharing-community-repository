package org.edu_sharing.restservices.mds.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsDesc;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsEntries {

	private List<MdsDesc> mdss = null;

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty("metadatasets")
	public List<MdsDesc> getMetadatasets() {
		return mdss;
	}

	public void setMetadatasets(List<MdsDesc> mdsRefs) {
		this.mdss = mdsRefs;
	}
}
