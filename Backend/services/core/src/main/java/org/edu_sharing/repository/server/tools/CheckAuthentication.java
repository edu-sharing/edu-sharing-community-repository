/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;


public class CheckAuthentication {
	
	private static Log logger = LogFactory.getLog(CheckAuthentication.class);
	
	public boolean isAdmin(String repId, HashMap authInfo){
		boolean result = false;

		try {
			MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repId, authInfo);
			result = mcAlfrescoBaseClient.isAdmin((String) authInfo.get(CCConstants.AUTH_USERNAME));
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}
}
