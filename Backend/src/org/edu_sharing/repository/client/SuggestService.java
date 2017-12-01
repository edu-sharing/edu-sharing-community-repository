package org.edu_sharing.repository.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.SuggestOracle;

@RemoteServiceRelativePath("eduservlet/suggestservice")
public interface SuggestService extends RemoteService {

	public ArrayList<? extends  SuggestOracle.Suggestion> request(String query,Integer mdsPropertyId);
	
	public String getValue(String key,Integer mdsPropertyId);
}
