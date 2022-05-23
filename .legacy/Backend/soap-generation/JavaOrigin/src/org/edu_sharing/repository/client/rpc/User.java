package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;


public class User implements Serializable {

	String nodeId;
	String email;
	String givenName;
	String surname;
	String repositoryId;
	String username;
		
	boolean fuzzySearchMode;
	
	
	String authorityName;
	String authorityType;
	
	public User() {
		this(false);	
	}

	public User(boolean fuzzySearchMode) {
		this.fuzzySearchMode = fuzzySearchMode;
		this.authorityType = "USER";
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
	
	
	public String getAuthorityDisplayName() {
		return 
			  (this.fuzzySearchMode) 
			? (getGivenName() + " " + getSurname())
			: (getEmail());
	}
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

	public String getAuthorityName() {
		return this.authorityName;
	}
	
	
	public void setAuthorityType(String authorityType) {
		this.authorityType = authorityType;
	}

	public String getAuthorityType() {
		return this.authorityType;
	}
	
}
