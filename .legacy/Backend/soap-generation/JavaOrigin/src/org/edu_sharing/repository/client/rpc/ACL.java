package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class ACL implements Serializable {

	boolean inherited = false;
	
	ACE[] aces = null;
	
	public ACL() {
		// TODO Auto-generated constructor stub
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
