package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class MetadataCreate implements Serializable{
	private boolean onlyMetadata=false;

	public boolean isOnlyMetadata() {
		return onlyMetadata;
	}

	public void setOnlyMetadata(boolean onlyMetadata) {
		this.onlyMetadata = onlyMetadata;
	}
}
