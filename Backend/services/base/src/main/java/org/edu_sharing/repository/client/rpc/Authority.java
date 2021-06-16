package org.edu_sharing.repository.client.rpc;


public abstract class Authority implements java.io.Serializable {

	String authorityName;
	String authorityType;
	
	public Authority(String authorityType) {
		this.authorityType = authorityType;
	}
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

	public String getAuthorityName() {
		return this.authorityName;
	}
	
	public abstract String getAuthorityDisplayName();
	
	public void setAuthorityType(String authorityType) {
		this.authorityType = authorityType;
	}

	public String getAuthorityType() {
		return this.authorityType;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof Authority) {

			Authority other = (Authority) obj;
			
			return (   getAuthorityType().equals(other.getAuthorityType())
					&& getAuthorityName().equals(other.getAuthorityName()));
			
		}
		
		return false;
	}

	@Override
	public int hashCode() {

		return (getAuthorityType() + getAuthorityName()).hashCode();
	}
	
	
	
}
