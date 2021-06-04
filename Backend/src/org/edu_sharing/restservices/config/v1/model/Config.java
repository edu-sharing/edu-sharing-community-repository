package org.edu_sharing.restservices.config.v1.model;

import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
	@JsonProperty private Values current;
	@JsonProperty private Values global;
	@JsonProperty private Language language;
	
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
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}	
	
}
