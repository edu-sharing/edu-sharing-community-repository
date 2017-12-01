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

public class ValidatorMinimalOneCriteria implements Validator {
	
	@Override
	public boolean check(String value) {
		
		if(value == null || value.trim().equals("")){
			return false;
		}else{
			return true;
		}
	}
	@Override
	public String getMessageId() {
		return Validator.MINIMAL_ONE_CRITERIA;
	}
}
