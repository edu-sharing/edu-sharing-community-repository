package org.edu_sharing.service.schoolcontext;

import org.edu_sharing.repository.client.rpc.SchoolContextValues;

public interface SchoolContextService {

	public SchoolContextValues getSchoolConextValues();
	
	public SchoolContextValues getSchoolConextValues(String nodeRefFederalState, String nodeRefTypeOfSchool, String nodeRefSchoolSubject, String nodeRefAgeGroup);
	
}
