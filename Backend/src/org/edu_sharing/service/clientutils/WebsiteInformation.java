package org.edu_sharing.service.clientutils;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.model.NodeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebsiteInformation {
	private Map<String, String[]> lrmiProperties;
	private final List<Node> duplicateNodes = new ArrayList<>();

	public void setLrmiProperties(Map<String, String[]> properties) {
        this.lrmiProperties = properties;
    }

    public Map<String, String[]> getLrmiProperties() {
        return lrmiProperties;
    }

    public static class License{
		public License(String name,String ccVersion){
			this.name=name;
			this.ccVersion=ccVersion;
		}
		private String name,ccVersion;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getCcVersion() {
			return ccVersion;
		}
		public void setCcVersion(String ccVersion) {
			this.ccVersion = ccVersion;
		}
		
	}
	private String title,page,description;
	private String[] keywords;
	private License license;
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String[] getKeywords() {
		return keywords;
	}

	public void setKeywords(String[] keywords) {
		if(keywords!=null){
			for(int i=0;i<keywords.length;i++){
				keywords[i]=keywords[i].trim();
			}
		}
		this.keywords = keywords;		
	}

	public List<Node> getDuplicateNodes() {
		return duplicateNodes;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public License getLicense() {
		return license;
	}

	public void setLicense(License license) {
		this.license = license;
	}
	
	
}
