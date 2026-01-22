package org.edu_sharing.repository.client.rpc;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class User extends Authority {

	private String nodeId;
	private String avatarNodeId;
	private String email;
	private String givenName;
	private String surname;
	private String repositoryId;
	private String username;
	private Map<String, Serializable> profileSettings;
	private Map<String, Serializable> properties;

	public User() {
		super("USER");
	}

	@Override
	public String getAuthorityDisplayName() {
		return getGivenName() + " " + getSurname();
	}
}
