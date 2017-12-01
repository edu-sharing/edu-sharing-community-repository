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

import java.util.ArrayList;
import java.util.List;

/**
 * @author rudolph
 *
 */
public class MetadataSetValueKatalog extends MetadataSetValue{
	MetadataSetValue parentValue;
	
	
	List<MetadataSetValue> children = new ArrayList<MetadataSetValue>();
	
	//search statement mapping 
	String statement = null;
	
	/**
	 * @return the parent
	 */
	public MetadataSetValue getParentValue() {
		return parentValue;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParentValue(MetadataSetValue parent) {
		this.parentValue = parent;
		if(this.parentValue != null && this.parentValue instanceof MetadataSetValueKatalog){
			((MetadataSetValueKatalog)this.parentValue).addChild(this);
		}
	}
	
	
	public void addChild(MetadataSetValue child){
		children.add(child);
	}

	public List<MetadataSetValue> getChildren() {
		return children;
	}


	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}
	
	
	
}
