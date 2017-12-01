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
public class MetadataSetFormsProperty extends MetadataSetBaseProperty{
	

	List<Validator> validators = null;
	

	MetadataSetFormsPanel parent;
	
	
	public MetadataSetFormsProperty() {
	}


	/**
	 * @return the validators
	 */
	public List<Validator> getValidators() {
		return validators;
	}

	
	/**
	 * @param validators the validators to set
	 */
	public void setValidators(List<Validator> validators) {
		this.validators = validators;
	}
	

	/**
	 * @return the parent
	 */
	public MetadataSetFormsPanel getParent() {
		return parent;
	}

	
	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSetFormsPanel parent) {
		this.parent = parent;
	}
	
}
