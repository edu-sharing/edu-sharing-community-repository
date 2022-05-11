package org.edu_sharing.webservices.types;

public class Child implements java.io.Serializable {
	private KeyValue[] properties;
	String[] aspects;
	String iconUrl;
	String previewUrl;

	public String[] getAspects() {
		return aspects;
	}

	public void setAspects(String[] aspects) {
		this.aspects = aspects;
	}

	public KeyValue[] getProperties() {
		return properties;
	}

	public void setProperties(KeyValue[] properties) {
		this.properties = properties;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
}
