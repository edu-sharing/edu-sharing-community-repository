package org.edu_sharing.repository.client.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;

public class SQLKeyword implements IsSerializable, SuggestOracle.Suggestion,  HasKey {
	
	String keyword;
	
	public SQLKeyword() {
	}
	
	@Override
	public String getDisplayString() {
		return this.keyword;
	}
	
	@Override
	public String getKey() {
		return this.keyword;
	}
	
	@Override
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
