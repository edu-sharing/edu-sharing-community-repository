package org.edu_sharing.repository.client.rpc;


public class Owner extends Authority {

	public Owner() {
		super("OWNER");
		setAuthorityName("OWNER");
	}


	@Override
	public String getAuthorityDisplayName() {
		return getAuthorityType();
	}
	
}
