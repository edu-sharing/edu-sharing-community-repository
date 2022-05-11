package org.alfresco.repo.search.impl.solr;

import org.alfresco.service.cmr.search.SearchParameters;

public class ESSearchParameters extends SearchParameters {

	String[] authorities = null;
	
	
	public String[] getAuthorities() {
		return authorities;
	}
	
	public void setAuthorities(String[] authorities) {
		this.authorities = authorities;
	}
	
	String groupBy = null;
	
	public String getGroupBy() {
		return groupBy;
	}
	
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
	
	String groupConfig = null;
	
	public String getGroupConfig() {
		return groupConfig;
	}
	
	public void setGroupConfig(String groupConfig) {
		this.groupConfig = groupConfig;
	}
}
