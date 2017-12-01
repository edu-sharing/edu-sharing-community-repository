package org.edu_sharing.restservices.admin.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XMLResult {
	@JsonProperty
	private String xml;

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}
	
}
