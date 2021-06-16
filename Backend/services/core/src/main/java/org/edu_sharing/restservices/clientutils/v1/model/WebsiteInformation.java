package org.edu_sharing.restservices.clientutils.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebsiteInformation {
	private String title,page,description,license;
	private String[] keywords;
	
	public WebsiteInformation(){}
	public WebsiteInformation(org.edu_sharing.service.clientutils.WebsiteInformation info){
		if(info==null)
			return;
		this.title=info.getTitle();
		this.page=info.getPage();
		this.description=info.getDescription();
		this.keywords=info.getKeywords();
		if(info.getLicense()!=null)
			this.license=info.getLicense().getName()+" "+info.getLicense().getCcVersion();
	}
	@JsonProperty("page")
	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}
	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@JsonProperty("keywords")
	public String[] getKeywords() {
		return keywords;
	}
	public void setKeywords(String[] keywords) {
		this.keywords = keywords;
	}
	@JsonProperty("license")
	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		this.license = license;
	}
	
	
	
}
