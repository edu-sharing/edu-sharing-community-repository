package org.edu_sharing.repository.server;

import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.edu_sharing.repository.client.tools.CCConstants;

public class AuthenticationToolYouTube extends AuthenticationToolAbstract {

	public AuthenticationToolYouTube(String repId){
	}
	
	@Override
	public HashMap<String, String> createNewSession(String userName, String password) throws Exception {
		return null;
	}
	
	@Override
	public HashMap<String, String> getUserInfo(String userName, String ticket) throws Exception {
		return null;
	}
	
	@Override
	public void logout(String ticket) {		
	}  
	
	@Override
	public HashMap<String, String> validateAuthentication(HttpSession session) {
		HashMap<String,String> authInfo = new HashMap<String,String>();
		authInfo.put(CCConstants.AUTH_USERNAME, "youtube");
		authInfo.put(CCConstants.AUTH_TICKET, "youtube");
		return authInfo;
	}
	
	@Override
	public boolean validateTicket(String ticket) {
		return true;
	}
	
}
