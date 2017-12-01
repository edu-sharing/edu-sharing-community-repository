package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class ACE implements Serializable {

	
	String authority = null;
	
	String permission = null;
	
	String accessStatus = null;
	
	String authorityType = null;
	
	User user = null;
	
	Group group = null;
	
	int id;
	
	boolean isInherited = false;
	
	boolean isEdited = false;
	
	public ACE() {
		// TODO Auto-generated constructor stub
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getAccessStatus() {
		return accessStatus;
	}

	public void setAccessStatus(String accessStatus) {
		this.accessStatus = accessStatus;
	}

	public String getAuthorityType() {
		return authorityType;
	}

	public void setAuthorityType(String authorityType) {
		this.authorityType = authorityType;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
	
	public boolean isInherited() {
		return isInherited;
	}
	
	public void setInherited(boolean isInherited) {
		this.isInherited = isInherited;
	}
	
	
	
	public int getId() {
		 return hashCode();
	}
	
	@Override
	public int hashCode() {
		//http://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
		//final int prime = 31;
		//hm but we had an json exception on gwt side, maybe it's to big
		final int prime = 3;
	      int result = 1;
	      result = prime * result + ((accessStatus == null) ? 0 : accessStatus.hashCode());
	      result = prime * result + ((authority == null) ? 0 : authority.hashCode());
	      result = prime * result + ((permission == null) ? 0 : permission.hashCode());
	      result = prime * result + new Boolean(isInherited).toString().hashCode();
	      return result;
	}
	
	public void setEdited(boolean isEdited) {
		this.isEdited = isEdited;
	}
	
	public boolean isEdited() {
		return isEdited;
	}
}
