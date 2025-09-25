package org.edu_sharing.repository.server;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class AuthenticationToolWikimedia extends AuthenticationToolAbstract {

	@Override
	public Map<String, String> createNewSession(String userName, String password) throws Exception {
		return null;
	}
	
	@Override
	public Map<String, String> getUserInfo(String userName, String ticket) throws Exception {
		return null;
	}
	
	@Override
	public void logout(String ticket) {		
	}  
	
	@Override
	public Map<String, String> validateAuthentication(HttpSession session) {
		Map<String,String> authInfo = new HashMap<>();
		authInfo.put(CCConstants.AUTH_USERNAME, "wikimedia");
		authInfo.put(CCConstants.AUTH_TICKET, "wikimedia");
		return authInfo;
	}
	
	@Override
	public boolean validateTicket(String ticket) {
		return true;
	}
}
