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
 *
 */
public class MetadataSetList implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String id;
	
	
	String childAssoc;
	
	List<MetadataSetListProperty> properties;
	
	MetadataSet parent;
	
	MetadataSetValue label;

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
	 * @return the properties
	 */
	public List<MetadataSetListProperty> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<MetadataSetListProperty> properties) {
		this.properties = properties;
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
	 * @return the childAssoc
	 */
	public String getChildAssoc() {
		return childAssoc;
	}

	/**
	 * @param childAssoc the childAssoc to set
	 */
	public void setChildAssoc(String childAssoc) {
		this.childAssoc = childAssoc;
	}

	/**
	 * @return the label
	 */
	public MetadataSetValue getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(MetadataSetValue label) {
		this.label = label;
	}
	
	
	
	
	
	
}
