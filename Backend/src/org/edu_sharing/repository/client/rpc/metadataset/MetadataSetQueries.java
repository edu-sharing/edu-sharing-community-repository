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
package org.edu_sharing.repository.client.rpc.metadataset;

import java.util.List;

public class MetadataSetQueries implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String statementsearchword = null;
	
	/**
	 * query part that can't be influenced by user
	 */
	String basequery = null;
	
	List<MetadataSetQuery> metadataSetQueries;
	
	boolean allowSearchWithoutCriteria = false;

	public List<MetadataSetQuery> getMetadataSetQueries() {
		return metadataSetQueries;
	}

	public void setMetadataSetQueries(List<MetadataSetQuery> metadataSetQueries) {
		this.metadataSetQueries = metadataSetQueries;
		if(metadataSetQueries != null){
			for(MetadataSetQuery mdsq : metadataSetQueries){
				mdsq.setParent(this);
			}
		}
	}

	public String getStatementsearchword() {
		return statementsearchword;
	}

	public void setStatementsearchword(String statementsearchword) {
		this.statementsearchword = statementsearchword;
	}

	public String getBasequery() {
		return basequery;
	}

	public void setBasequery(String basequery) {
		this.basequery = basequery;
	}

	public boolean isAllowSearchWithoutCriteria() {
		return allowSearchWithoutCriteria;
	}

	public void setAllowSearchWithoutCriteria(String allowSearchWithoutCriteria) {
		this.allowSearchWithoutCriteria = new Boolean(allowSearchWithoutCriteria).booleanValue();
	}	
	
	
	
	
	
}
