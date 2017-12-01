/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.rpc;

import java.util.HashMap;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;

/**
 * @author rudolph
 */
public class RepositoryInfo implements com.google.gwt.user.client.rpc.IsSerializable {

	/**
	 * repositoryId, repository Properties
	 */
	HashMap<String, HashMap<String,String>> repInfoMap;
	
	/**
	 * repositoryId, MetadataSets
	 */
	HashMap<String,MetadataSets> repMetadataSetsMap;
	
	/**
	 * Help URLs
	 */
	String helpUrlCC = null;
	String helpUrlES = null;
	String helpUrlCustom = null;
	
	String helpUrlShare = null; 
	Boolean fuzzyUserSearch = false;
	
	
	public RepositoryInfo() {
	}

	/**
	 * @return the repInfoMap
	 */
	public HashMap<String, HashMap<String, String>> getRepInfoMap() {
		return repInfoMap;
	}

	/**
	 * @param repInfoMap the repInfoMap to set
	 */
	public void setRepInfoMap(HashMap<String, HashMap<String, String>> repInfoMap) {
		this.repInfoMap = repInfoMap;
	}

	/**
	 * @return the repMetadataSetsMap
	 */
	public HashMap<String, MetadataSets> getRepMetadataSetsMap() {
		return repMetadataSetsMap;
	}

	/**
	 * @param repMetadataSetsMap the repMetadataSetsMap to set
	 */
	public void setRepMetadataSetsMap(HashMap<String, MetadataSets> repMetadataSetsMap) {
		this.repMetadataSetsMap = repMetadataSetsMap;
	}

	public String getHelpUrlCC() {
		return helpUrlCC;
	}

	public void setHelpUrlCC(String helpUrlCC) {
		this.helpUrlCC = helpUrlCC;
	}

	public String getHelpUrlES() {
		return helpUrlES;
	}

	public void setHelpUrlES(String helpUrlES) {
		this.helpUrlES = helpUrlES;
	}

	public String getHelpUrlCustom() {
		return helpUrlCustom;
	}

	public void setHelpUrlCustom(String helpUrlCustom) {
		this.helpUrlCustom = helpUrlCustom;
	}
	
	public void setHelpUrlShare(String helpUrlShare) {
		this.helpUrlShare = helpUrlShare;
	}
	
	public String getHelpUrlShare() {
		return helpUrlShare;
	}

	public boolean isFuzzyUserSearch() {
		return this.fuzzyUserSearch;
	}

	public void setFuzzyUserSearch(boolean fuzzyUserSearch) {
		this.fuzzyUserSearch = fuzzyUserSearch;
	}
}
