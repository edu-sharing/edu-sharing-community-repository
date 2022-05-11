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
package org.edu_sharing.repository.server;

import java.util.HashMap;

import javax.servlet.http.HttpSession;

public interface AuthenticationTool {
	public HashMap<String, String> createNewSession(String userName, String password) throws Exception;

	/**
	 * username and ticket are stored in a ThreadLocal variable in Both implementations API and WS Client
	 * so that the current thread is authenticated and we don't need to pass the ticket to every method call
	 * 
	 * @param userName
	 * @param ticket
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, String> getUserInfo(String userName, String ticket) throws Exception;
	
	public void logout(String ticket);

	/**
	 * 
	 * stores username and ticket in session.
	 * 
	 * username is only stored because of MCAlfrescoWSClient, native client finds out the username by validating ticket.
	 * 
	 * if there is already another ticket it will be invalidated and the new one overwrites the old.
	 *
	 * @param username
	 * @param ticket
	 * @param session
	 */
	public void storeAuthInfoInSession(String username, String ticket, String authType, HttpSession session);
	
	/**
	 * returns validated AuthenticationInfo
	 * 
	 * @param session
	 * @return
	 */
	public HashMap<String,String> validateAuthentication(HttpSession session);
	
	/**
	 * returns AuthenticationInfo without validation
	 * @param session
	 * @return
	 */
	public HashMap<String,String> getAuthentication(HttpSession session);
	
	/**
	 * 
	 * @param session
	 * @return
	 */
	public String getTicketFromSession(HttpSession session);
	
	/**
	 * 
	 * @param ticket
	 * @return
	 */
	public boolean validateTicket(String ticket);
}
