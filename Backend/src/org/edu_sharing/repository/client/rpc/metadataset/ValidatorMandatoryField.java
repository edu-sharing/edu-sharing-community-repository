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
public class ValidatorMandatoryField implements Validator {
	
	
	public ValidatorMandatoryField(){
	}
	
	public boolean check(String value) {
		if(value == null || value.trim().equals("")){
			return false;
		}else{
			return true;
		}
	};
	/* (non-Javadoc)
	 * @see org.edu_sharing.repository.client.tools.forms.Validator#getMessage()
	 */
	@Override
	public String getMessageId() {
		return MANDATORY;
	}
}
