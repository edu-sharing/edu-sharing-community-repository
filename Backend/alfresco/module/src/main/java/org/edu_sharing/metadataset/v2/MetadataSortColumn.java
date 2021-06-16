package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class MetadataSortColumn implements Serializable {
	private String id;
	private String mode;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
}
