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
public class MetadataSetFormsForm implements com.google.gwt.user.client.rpc.IsSerializable {
	
	
	String id;
	
	List<MetadataSetFormsPanel> panels;
	
	/**
	 * this is when its not the default Alfresco child Association
	 * or it can be different depending on the Parent (like contributer)
	 * 
	 * it will be set as a hidden param in create and update form
	 * only important if its the root type, else it comes from MetadataSetModel -> children
	 */
	String childAssociation;
	
	
	MetadataSet parent;

	
	public MetadataSetFormsForm() {
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
	 * @return the panels
	 */
	public List<MetadataSetFormsPanel> getPanels() {
		return panels;
	}
	/**
	 * @param panels the panels to set
	 */
	public void setPanels(List<MetadataSetFormsPanel> panels) {
		this.panels = panels;
		
		for(MetadataSetFormsPanel panel:this.panels){
			if(panel != null){
				panel.setParent(this);
			}
		}
	}

	/**
	 * @return the parent
	 */
	public MetadataSet getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSet parent) {
		this.parent = parent;
	}

	/**
	 * @return the childAssociation
	 */
	public String getChildAssociation() {
		return childAssociation;
	}

	/**
	 * @param childAssociation the childAssociation to set
	 */
	public void setChildAssociation(String childAssociation) {
		this.childAssociation = childAssociation;
	}
	
	
	public MetadataSetFormsPanel getMetadataSetPanel(String name){
		for(MetadataSetFormsPanel panel:panels){
			if(panel.getName() != null && panel.getName().equals(name)){
				return panel;
			}
		}
		return null;
	}
	
}
