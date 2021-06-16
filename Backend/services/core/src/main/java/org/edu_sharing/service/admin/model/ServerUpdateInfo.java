package org.edu_sharing.service.admin.model;

public class ServerUpdateInfo {

	String id;
	
	String description;

	long executedAt;

	public ServerUpdateInfo() {
	}
	
	public ServerUpdateInfo(String id, String description) {
		this.id = id;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getExecutedAt() {
		return executedAt;
	}

	public void setExecutedAt(long executedAt) {
		this.executedAt = executedAt;
	}
}
