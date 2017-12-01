package org.edu_sharing.repository.client;

import java.util.ArrayList;

import org.edu_sharing.repository.client.rpc.SchoolContextValues;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("eduservlet/schoolcontextservice")
public interface SchoolContextService extends RemoteService {

	
	SchoolContextValues getSchoolContextValues(String nodeRefFederalState, String nodeRefTypeOfSchool, String nodeRefSchoolSubject,String nodeRefAgeGroup);
	
	SchoolContextValues getSchoolContextValues();
	
	public ArrayList<String[]> getSchoolContextDisplayPath(String nodeId);
	
}
