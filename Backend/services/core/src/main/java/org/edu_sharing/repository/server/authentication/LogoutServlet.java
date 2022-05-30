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
package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class LogoutServlet extends HttpServlet{
	
	Logger logger = Logger.getLogger(LogoutServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		
		//logout from homeRepository
		HttpSession session = req.getSession();
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			
			String ticket = authTool.getTicketFromSession(session);
			authTool.logout(ticket);
			
			//logout from remoteRepositories
			NetworkAuthentication netAuth = (NetworkAuthentication)session.getAttribute(CCConstants.SESSION_FEDERATED_AUTH);
			if(netAuth != null){
				for(Map.Entry<String,NetworkAuthentication.Authentication> entry: netAuth.getAuthStore().entrySet()){
					if(entry.getValue().getTicket() != null){
						RepoFactory.getAuthenticationToolInstance(entry.getKey()).logout(entry.getValue().getTicket());
					}
				}
			}
		}catch(org.alfresco.repo.security.authentication.AuthenticationException e){
			logger.error("it seems that ticket is already invalid:"+e.getMessage(),e);
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}finally{
			//remove things stored in session(i.e the ticket) by invalidating 
			if(session != null) session.invalidate();
		}
		
		//if lms context the logout hanlder from lms must also invalidate repo ticket
		String ticket =  req.getParameter("ticket");
	  	if(ticket != null && !ticket.equals("")){
	  		try{
	  			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
	  			authTool.logout(ticket);
	  		}catch(Throwable e){
				logger.error(e.getMessage(),e);
			}
	  	}
		/**
		 * Shibboleth Service Provider FrontChannel Logout
		 */
	  	String returnUrl = req.getParameter("return");
	  	if(returnUrl != null && !returnUrl.trim().equals("")){
	  		returnUrl = URLDecoder.decode(returnUrl);
	  		resp.sendRedirect(returnUrl);
	  		return;
	  	}
	  	
	  	/**
	  	 * redirect to "source" after guest logout 
	  	 */
	  	String redirectUrl = req.getParameter("source");
		if(redirectUrl == null || redirectUrl.trim().equals("")){
		  	redirectUrl = req.getContextPath();
			
			String mode = req.getParameter("mode");
			if(mode != null && !mode.trim().equals("")){
				redirectUrl = UrlTool.setParam(redirectUrl, "mode", mode);
			}
			String locale = req.getParameter("locale");
			if(locale != null && !locale.trim().equals("")){
				redirectUrl = UrlTool.setParam(redirectUrl, "locale", locale);
			}
		}
		
		resp.sendRedirect(redirectUrl);
		
	}
	
}
