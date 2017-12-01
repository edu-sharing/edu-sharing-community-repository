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

public class ValidatorMinChars implements Validator{
	
	public ValidatorMinChars(){
	}
	
	public boolean check(String value) {
		if(value == null || value.trim().length() < 3){
			return false;
		}
		return true;
	};
	
	@Override
	public String getMessageId() {
		return Validator.MINCHARS;
	}
}
