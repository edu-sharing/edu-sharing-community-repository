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
public class ValidatorIntField implements Validator{

	/* (non-Javadoc)
	 * @see org.edu_sharing.repository.client.rpc.metadataset.Validator#check(java.lang.String)
	 */
	@Override
	public boolean check(String value) {
		if(value != null && !value.trim().equals("")){
			try{
				Integer.parseInt(value);
				return true;
			}catch(NumberFormatException e){
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.edu_sharing.repository.client.rpc.metadataset.Validator#getMessageId()
	 */
	@Override
	public String getMessageId() {
		return Validator.INT;
	}
}
