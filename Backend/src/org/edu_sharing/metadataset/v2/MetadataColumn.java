package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class MetadataColumn implements Serializable {
	private String id;
	private boolean showDefault=true;
	private String format;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public boolean isShowDefault() {
		return showDefault;
	}
	public void setShowDefault(boolean showDefault) {
		this.showDefault = showDefault;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	
	
}
