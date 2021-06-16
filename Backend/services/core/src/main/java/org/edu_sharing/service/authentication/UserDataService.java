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
package org.edu_sharing.service.authentication;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

public interface UserDataService {
	
	public void updateUser(String repUsername, String directoryUsername) throws Exception;
	
	Map<QName, Serializable> getRepositoryUserProperties(String directoryUsername) throws Exception ;
	
	Map<QName, Serializable> getRepositoryUserProperties(String directoryUsernameProp, String directoryUsernameValue) throws Exception;
	
	Map<String, String> getDirectoryUserProperties(String directoryUsernameProp, String directoryUsernameValue, String[] propsToReturn) throws Exception;
}
