package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataSetInfo;

import com.fasterxml.jackson.annotation.JsonProperty;


public class MdsEntries {
	@JsonProperty(required = true)
	private List<MetadataSetInfo> metadatasets;

	public List<MetadataSetInfo> getMetadatasets() {
		return metadatasets;
	}

	public void setMetadatasets(List<MetadataSetInfo> metadatasets) {
		this.metadatasets = metadatasets;
	}
	
}
