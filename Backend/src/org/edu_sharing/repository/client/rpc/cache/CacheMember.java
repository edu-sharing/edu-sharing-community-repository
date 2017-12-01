package org.edu_sharing.repository.client.rpc.cache;

import java.io.Serializable;

public class CacheMember implements Serializable{

	String name;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
