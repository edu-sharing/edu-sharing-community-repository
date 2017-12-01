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

import org.edu_sharing.repository.client.tools.CCConstants;

/**
 * @author rudolph
 */
public class MetadataSets implements com.google.gwt.user.client.rpc.IsSerializable {
	
	
	List<MetadataSet> metadataSets;
	
	
	public MetadataSets() {
	}

	/**
	 * @return the metadataSets
	 */
	public List<MetadataSet> getMetadataSets() {
		return metadataSets;
	}

	/**
	 * @param metadataSets the metadataSets to set
	 */
	public void setMetadataSets(List<MetadataSet> metadataSets) {
		this.metadataSets = metadataSets;
		
		//set Parent
		for(MetadataSet mds : metadataSets){
			if(mds != null){
				mds.setParent(this);
			}
		}
	}
	
	/**
	 * if id is null or no metadataset with id is found it returns default metadataset
	 * @param id
	 * @return
	 */
	public MetadataSet getMetadataSetById(String id){
		
		if(id == null) id = CCConstants.metadatasetdefault_id;
		MetadataSet defaultMetadataSet = null;
		if(this.metadataSets != null){
			for(MetadataSet mds: this.metadataSets){
				if(mds.getId() != null && mds.getId().equals(id)){
					return mds;
				}
				if(mds.getId() != null && mds.getId().equals(CCConstants.metadatasetdefault_id)){
					defaultMetadataSet = mds;
				}
			}
		}
		return defaultMetadataSet;
	}
	
	
	
}
