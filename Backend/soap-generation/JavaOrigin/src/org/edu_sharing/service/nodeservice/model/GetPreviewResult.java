package org.edu_sharing.service.nodeservice.model;

import java.io.Serializable;

public class GetPreviewResult implements Serializable {

	private static final long serialVersionUID = 1L;

	String url;
	
	String type;
	
	boolean createActionIsRunning = true;
	
	public static final String TYPE_EXTERNAL = "TYPE_EXTERNAL";
	public static final String TYPE_USERDEFINED = "TYPE_USERDEFINED";
	public static final String TYPE_GENERATED = "TYPE_GENERATED";
	public static final String TYPE_DEFAULT = "TYPE_DEFAULT";
	
	
	public GetPreviewResult() {
	}
	
	public GetPreviewResult(String url, String type, boolean createActionIsRunning) {
		this.url = url;
		this.type = type;
		this.createActionIsRunning = createActionIsRunning;
	}

	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isCreateActionRunning() {
		return createActionIsRunning;
	}

	public void setCreateActionIsRunning(boolean createActionIsRunning) {
		this.createActionIsRunning = createActionIsRunning;
	}
	
	
}
