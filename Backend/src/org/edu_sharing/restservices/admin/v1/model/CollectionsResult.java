package org.edu_sharing.restservices.admin.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionsResult {
	@JsonProperty
	private int count;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
}
