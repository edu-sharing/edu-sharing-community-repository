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
package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class SearchCriterias implements Serializable {
	
	
	public SearchCriterias(){
		
	}

	String[] contentkind;
	String[] aspects;
	
		
	public String[] getAspects() {
		return aspects;
	}

	public void setAspects(String[] aspects) {
		this.aspects = aspects;
	}

	/**
	 * searchword
	 */
	
	String searchWord;
	
	
	/**
	 * @TODO build the queryString on the server side so that othe repositories can translate it in their language
	 */
	String metadataSetQuery = null;

	
	/**
	 * for getting the serverside representation of metadataset queries
	 */
	String repositoryId = null;
	
	String metadataSetId = null;
	
	/*optional*/
	String standaloneMetadataSetName = null;

	/**
	 * @deprecated
	 * @return
	 */
	public boolean criteriasEmpty(){
		boolean result = true;
		
		//we don't need contentkind anymore
		if(contentkind != null && contentkind.length > 0 ) result = false;
		
			
		//boolean chameleon;
		
		
		/**
		 * searchword
		 */
		if(searchWord != null && !searchWord.equals("")) result = false;
		
		return result;
	}

	/**
	 * @return the contentkind
	 */
	public String[] getContentkind() {
		return contentkind;
	}

	/**
	 * @param contentkind the contentkind (Alfresco NodeTypes) to set
	 */
	public void setContentkind(String[] contentkind) {
		this.contentkind = contentkind;
	}

	

	/**
	 * @return the searchWord
	 */
	public String getSearchWord() {
		return searchWord;
	}

	/**
	 * @param searchWord the searchWord to set
	 */
	public void setSearchWord(String searchWord) {
		this.searchWord = searchWord;
	}


	public String getMetadataSetQuery() {
		return metadataSetQuery;
	}

	public void setMetadataSetQuery(String metadataSetQuery) {
		this.metadataSetQuery = metadataSetQuery;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getMetadataSetId() {
		return metadataSetId;
	}

	public void setMetadataSetId(String metadataSetId) {
		this.metadataSetId = metadataSetId;
	}
	
	public String getStandaloneMetadataSetName() {
		return standaloneMetadataSetName;
	}
	
	public void setStandaloneMetadataSetName(String standaloneMetadataSetName) {
		this.standaloneMetadataSetName = standaloneMetadataSetName;
	}
	
	
}
