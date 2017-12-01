package org.edu_sharing.webservices.render;

import java.util.List;

import org.edu_sharing.webservices.types.KeyValue;
import org.edu_sharing.webservices.usage.UsageResult;

public class RenderInfoResult {

	/**
	 * 
Permissions(nur wegen gast?), Usage(nur wegen xml param), Version,modifiedDate (Content),technicalSize?

	 */
	
	
	Boolean guestReadAllowed;
	
	Boolean publishRight;
	
	Boolean userReadAllowed;
	
	UsageResult usage;
	
	int contentHash;
	
	KeyValue[] properties;
	
	KeyValue[] labels;
	
	
	String previewUrl;
	
	String mimeTypeUrl;
	
	String[] aspects;
	
	String mdsTemplate;

	public Boolean getGuestReadAllowed() {
		return guestReadAllowed;
	}

	public void setGuestReadAllowed(Boolean guestReadAllowed) {
		this.guestReadAllowed = guestReadAllowed;
	}

	public Boolean getPublishRight() {
		return publishRight;
	}

	public void setPublishRight(Boolean publishRight) {
		this.publishRight = publishRight;
	}

	public Boolean getUserReadAllowed() {
		return userReadAllowed;
	}

	public void setUserReadAllowed(Boolean userReadAllowed) {
		this.userReadAllowed = userReadAllowed;
	}

	public UsageResult getUsage() {
		return usage;
	}

	public void setUsage(UsageResult usage) {
		this.usage = usage;
	}


	public int getContentHash() {
		return contentHash;
	}

	public void setContentHash(int contentHash) {
		this.contentHash = contentHash;
	}

	
	public void setProperties(KeyValue[] properties) {
		this.properties = properties;
	}
	
	public KeyValue[] getProperties() {
		return properties;
	}
	
	public void setLabels(KeyValue[] labels) {
		this.labels = labels;
	}
	
	public KeyValue[] getLabels() {
		return labels;
	}
	
	
	public void setAspects(String[] aspects) {
		this.aspects = aspects;
	}
	
	public String[] getAspects() {
		return aspects;
	}
	
	public void setMimeTypeUrl(String mimeTypeUrl) {
		this.mimeTypeUrl = mimeTypeUrl;
	}
	
	public String getMimeTypeUrl() {
		return mimeTypeUrl;
	}
	
	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}
	
	public String getPreviewUrl() {
		return previewUrl;
	}
	
	public void setmdsTemplate(String mdsTemplate) {
		this.mdsTemplate = mdsTemplate;
	}
	
	public String getMdsTemplate() {
		return mdsTemplate;
	}
	
}
