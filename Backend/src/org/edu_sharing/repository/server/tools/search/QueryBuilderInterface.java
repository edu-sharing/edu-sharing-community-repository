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
package org.edu_sharing.repository.server.tools.search;

import java.util.HashMap;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;


public interface QueryBuilderInterface {
	
	public String getSearchString();
	
	/**
	 * set the alfresco Type like folder, content, io, or map
	 * @param _contentkind
	 */
	public void setContentKind(String[] _contentkind);
	public void setAspects(String[] _aspects);

	public void setSearchWord(String _searchWord);	
	
	public void setMetadataSetQuery(String repositoryId, String metadataSetId, String standaloneMetadataSetName, HashMap<MetadataSetQuery,HashMap<MetadataSetQueryProperty,String[]>> map);
	
}
