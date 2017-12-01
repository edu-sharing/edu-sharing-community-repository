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

/**
 * @author rudolph
 */
public class MetadataSet implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String id;
	
	String label;
	
	Boolean hidden;
	
	
	MetadataSetModelType metadataSetModel;
	List<MetadataSetModelType> metadataSetModelTypes;
	
	List<MetadataSetFormsForm> metadataSetForms;
	
	
	List<MetadataSetView> metadataSetViews;
	
	List<MetadataSetList> metadataSetLists;
	
	
	MetadataSetQueries metadataSetQueries;
	
	MetadataSets parent;
	
	
	boolean lateInitDone = false;
	
	public MetadataSet() {
	}

	/**
	 * @return the metadataSetModels
	 */
	public List<MetadataSetModelType> getMetadataSetModelTypes() {
		return metadataSetModelTypes;
	}

	/**
	 * @param metadataSetModelTypes the metadataSetModels to set
	 */
	public void setMetadataSetModelTypes(List<MetadataSetModelType> metadataSetModelTypes) {
		this.metadataSetModelTypes = metadataSetModelTypes;
		
		for(MetadataSetModelType mdsm:this.metadataSetModelTypes  ){
			if(mdsm != null){
				mdsm.setParent(this);
			}
		}
	}
	
	
	public MetadataSetModelType getMetadataSetModelType(String type){
		for(MetadataSetModelType mdsmt:metadataSetModelTypes){
			if(mdsmt.getType().equals(type)){
				return mdsmt;
			}
		}
		return null;
	}

	/**
	 * @return the metadataSetForms
	 */
	public List<MetadataSetFormsForm> getMetadataSetForms() {
		return metadataSetForms;
	}

	/**
	 * @param metadataSetForms the metadataSetForms to set
	 */
	public void setMetadataSetForms(List<MetadataSetFormsForm> metadataSetForms) {
		this.metadataSetForms = metadataSetForms;
		
		for(MetadataSetFormsForm mdsgf:this.metadataSetForms  ){
			if(mdsgf != null){
				mdsgf.setParent(this);
			}
		}
	}


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}


	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}


	public Boolean getHidden() {
		return hidden;
	}


	/**
	 * @return the parent
	 */
	public MetadataSets getParent() {
		return parent;
	}


	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSets parent) {
		this.parent = parent;
	}


	/**
	 * @return the metadataSetViews
	 */
	public List<MetadataSetView> getMetadataSetViews() {
		return metadataSetViews;
	}


	/**
	 * @param metadataSetViews the metadataSetViews to set
	 */
	public void setMetadataSetViews(List<MetadataSetView> metadataSetViews) {
		this.metadataSetViews = metadataSetViews;
		for(MetadataSetView child:this.metadataSetViews  ){
			if(child != null){
				child.setParent(this);
			}
		}
	}


	/**
	 * @return the metadataSetLists
	 */
	public List<MetadataSetList> getMetadataSetLists() {
		return metadataSetLists;
	}


	/**
	 * @param metadataSetLists the metadataSetLists to set
	 */
	public void setMetadataSetLists(List<MetadataSetList> metadataSetLists) {
		this.metadataSetLists = metadataSetLists;
		for(MetadataSetList child:this.metadataSetLists  ){
			if(child != null){
				child.setParent(this);
			}
		}
	}
	
	
	public MetadataSetFormsForm getMetadataSetForm(String id, String childAssoc){
		for(MetadataSetFormsForm mdsf: this.metadataSetForms){
			if(mdsf.getId() != null && mdsf.getId().equals(id)){
				if(childAssoc != null){
					if(mdsf.getChildAssociation() != null && mdsf.getChildAssociation().equals(childAssoc)){
						return mdsf;
					}
				}else{
					return mdsf;
				}
			}
		}
		return null;
	}


	public MetadataSetQueries getMetadataSetQueries() {
		return metadataSetQueries;
	}


	public void setMetadataSetQueries(MetadataSetQueries metadataSetQueries) {
		this.metadataSetQueries = metadataSetQueries;
	}
	
	
	public void setLateInitDone(boolean lateInitDone) {
		this.lateInitDone = lateInitDone;
	}


	public boolean isLateInitDone() {
		return lateInitDone;
	}
	
	
}
