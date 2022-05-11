package org.edu_sharing.repository.client.rpc;

public class ACL implements java.io.Serializable {

	boolean inherited = false;
	
	ACE[] aces = null;
	
	public ACL() {
	}
	
	public boolean isInherited() {
		return inherited;
	}
	
	public ACE[] getAces() {
		return aces;
	}

	public void setAces(ACE[] aces) {
		this.aces = aces;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}
	
}
