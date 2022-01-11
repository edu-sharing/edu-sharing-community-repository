package org.edu_sharing.restservices.mds.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import org.edu_sharing.restservices.shared.MdsDesc;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsEntries {

	private List<MdsDesc> mdss = null;

	/**
	   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("metadatasets")
	public List<MdsDesc> getMetadatasets() {
		return mdss;
	}

	public void setMetadatasets(List<MdsDesc> mdsRefs) {
		this.mdss = mdsRefs;
	}
}
