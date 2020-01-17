package org.edu_sharing.service.nodeservice.model;

import java.io.Serializable;

public class GetPreviewResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private String url;

	private boolean isIcon;

	public GetPreviewResult() {
	}
	
	public GetPreviewResult(String url, boolean isIcon) {
		this.url = url;
		this.isIcon = isIcon;
	}

	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isIcon() {
		return isIcon;
	}

	public void setIcon(boolean icon) {
		isIcon = icon;
	}
}
