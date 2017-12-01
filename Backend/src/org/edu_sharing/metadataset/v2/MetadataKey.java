package org.edu_sharing.metadataset.v2;

public class MetadataKey extends MetadataTranslatable {
	
	private String key,caption,parent;

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		if ( parent==null || parent.trim().isEmpty() )
			this.parent=null;
		else
			this.parent = parent;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
}
