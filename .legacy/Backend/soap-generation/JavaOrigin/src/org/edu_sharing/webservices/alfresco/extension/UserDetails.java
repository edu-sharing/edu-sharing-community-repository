package org.edu_sharing.webservices.alfresco.extension;

public class UserDetails implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private String userName;
	private String firstName;
	private String lastName;
	private String email;
	
	private String password;
	
	public UserDetails() {
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
}
