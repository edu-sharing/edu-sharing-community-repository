package org.edu_sharing.repository.client.rpc;

public class ACE implements java.io.Serializable {

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
	
	@Override
	public int hashCode() {
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
	
	@Override
	public boolean equals(Object obj) {
		
		if( !(obj instanceof ACE) ) return false;
		
		ACE ace = (ACE)obj;
		
		if(ace.getAuthority().equals(this.getAuthority()) &&
				ace.getPermission().equals(this.getPermission()) &&
				ace.isInherited == this.isInherited){
			
			return true;
		
		}
		
		return false;
	}
}
