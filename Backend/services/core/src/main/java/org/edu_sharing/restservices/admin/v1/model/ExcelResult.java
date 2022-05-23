package org.edu_sharing.restservices.admin.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExcelResult {
	@JsonProperty
	private int rows;

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}
	
}
