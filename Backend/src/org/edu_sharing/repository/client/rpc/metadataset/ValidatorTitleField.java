package org.edu_sharing.repository.client.rpc.metadataset;

public class ValidatorTitleField implements Validator{

	@Override
	public boolean check(String value) {
		
		if(value == null){
			return false;
		}
		
		if(value.trim().equals("")){
			return false;
		}
		
		//check if its only an file suffix and no name
		if(value.startsWith(".") && value.indexOf(".", 1) == -1){
			return false;
		}
				
		return true;
	}
	
	public String getMessageId() {
		return MANDATORYTITLE;
	};
}
