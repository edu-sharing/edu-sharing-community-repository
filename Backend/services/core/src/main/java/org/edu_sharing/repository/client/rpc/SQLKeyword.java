package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class SQLKeyword implements Serializable,  HasKey {
	
	String keyword;
	
	public SQLKeyword() {
	}
	
	public String getDisplayString() {
		return this.keyword;
	}
	
	@Override
	public String getKey() {
		return this.keyword;
	}
	
	public String getReplacementString() {
		return this.keyword;
	}
	
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public String getKeyword() {
		return keyword;
	}

}
