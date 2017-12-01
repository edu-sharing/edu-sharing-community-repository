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

/**
 * @author rudolph
 */
public class MetadataSetModelChild implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String name = null;
	String childassoc = null;
	
	
	MetadataSetModelType parent;

	
	public MetadataSetModelChild() {
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the childassoc
	 */
	public String getChildassoc() {
		return childassoc;
	}

	/**
	 * @param childassoc the childassoc to set
	 */
	public void setChildassoc(String childassoc) {
		this.childassoc = childassoc;
	}

	/**
	 * @return the parent
	 */
	public MetadataSetModelType getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSetModelType parent) {
		this.parent = parent;
	}
	
}
