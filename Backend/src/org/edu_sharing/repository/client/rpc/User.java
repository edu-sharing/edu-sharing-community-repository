package org.edu_sharing.repository.client.rpc;


public class User extends Authority {

	String nodeId;
	String email;
	String givenName;
	String surname;
	String repositoryId;
	String username;

	public User() {
		super("USER");
	}


	public String getNodeId() {
		return nodeId;
	}


	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getGivenName() {
		return givenName;
	}


	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}


	public String getSurname() {
		return surname;
	}


	public void setSurname(String surname) {
		this.surname = surname;
	}
	
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public String getRepositoryId() {
		return repositoryId;
	}
	
	public void setUsername(String username) {
		setAuthorityName(this.username = username);
	}
	public String getUsername() {
		return username;
	}
	
	@Override
	public String getAuthorityDisplayName() {
		return getGivenName() + " " + getSurname();
	}
	
}
