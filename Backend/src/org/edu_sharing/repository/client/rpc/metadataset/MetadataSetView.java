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
public class MetadataSetView implements com.google.gwt.user.client.rpc.IsSerializable {

	
	String id;
	
	List<MetadataSetViewProperty> properties;
	
	MetadataSet parent;

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
	public List<MetadataSetViewProperty> getProperties() {
		return properties;
	}
	
	public MetadataSetViewProperty getProperty(String propertyName){
		for(MetadataSetViewProperty mdsvp: properties){
			if(mdsvp.getName().equals(propertyName)){
				return mdsvp;
			}
		}
		return null;
	}
	

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<MetadataSetViewProperty> properties) {
		this.properties = properties;
		for(MetadataSetViewProperty prop: this.properties){
			if(prop != null){
				prop.setParent(this);
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
	
	
}
