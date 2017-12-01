package org.edu_sharing.repository.client.rpc;


public class Everyone extends Authority {

	public Everyone() {
		super("EVERYONE");
		setAuthorityName("EVERYONE");
	}


	@Override
	public String getAuthorityDisplayName() {
		return getAuthorityType();
	}
	
}
