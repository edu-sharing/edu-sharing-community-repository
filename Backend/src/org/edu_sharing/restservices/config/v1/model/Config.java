package org.edu_sharing.restservices.config.v1.model;

import org.edu_sharing.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
	@JsonProperty private Values current;
	@JsonProperty private Values global;
	
	public Values getCurrent() {
		return current;
	}
	public void setCurrent(Values current) {
		this.current = current;
	}
	public Values getGlobal() {
		return global;
	}
	public void setGlobal(Values global) {
		this.global = global;
	}	
}
