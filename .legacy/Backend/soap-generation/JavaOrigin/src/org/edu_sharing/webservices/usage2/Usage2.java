package org.edu_sharing.webservices.usage2;

import java.util.Calendar;

public class Usage2 {

	
	public Usage2Result getUsage(String eduRef, String lmsId, String courseId, String user, String resourceId) throws Usage2Exception{
    	return null;
    }
    public Usage2Result[] getUsagesByEduRef(String eduRef, String user) throws Usage2Exception{
    	return null;
    }
    public boolean deleteUsage(String eduRef, String user, String lmsId, String courseId, String resourceId) throws Usage2Exception{
    	return false;
    }
   
    public Usage2Result setUsage(String eduRef, String user, String lmsId, String courseId, String userMail, Calendar fromUsed, Calendar toUsed, int distinctPersons, String version, String resourceId, String xmlParams) throws Usage2Exception{
    	return null;
    }
	
}
