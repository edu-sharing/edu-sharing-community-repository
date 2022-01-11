package org.edu_sharing.restservices.config.v1.model;

import java.util.Map;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Variables {
	@JsonProperty private Map<String,String> global;
	@JsonProperty private Map<String,String> current;


	public Map<String, String> getGlobal() {
		return global;
	}

	public void setGlobal(Map<String, String> global) {
		this.global = global;
	}

	public Map<String, String> getCurrent() {
		return current;
	}

	public void setCurrent(Map<String, String> current) {
		this.current = current;
	}	
}
