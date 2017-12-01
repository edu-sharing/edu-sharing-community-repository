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
public class MetadataSetModelType implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String type = null;
	
	List<MetadataSetModelProperty> properties = null;
	
	List<MetadataSetModelChild> children = null;
	
	MetadataSet parent;

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the properties
	 */
	public List<MetadataSetModelProperty> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<MetadataSetModelProperty> properties) {
		this.properties = properties;
		
		for(MetadataSetModelProperty mdsmp:this.properties){
			if(mdsmp != null){
				mdsmp.setParent(this);
			}
		}
	}
	
	public MetadataSetModelProperty getMetadataSetModelProperty(String property){
		if(this.properties != null){
			for(MetadataSetModelProperty mdsmp : this.properties){
				if(mdsmp.getName().equals(property)) return mdsmp;
			}
		}
		return null;
	}

	/**
	 * @return the children
	 */
	public List<MetadataSetModelChild> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<MetadataSetModelChild> children) {
		this.children = children;
		
		for(MetadataSetModelChild mdsmc:this.children){
			if(mdsmc != null){
				mdsmc.setParent(this);
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
