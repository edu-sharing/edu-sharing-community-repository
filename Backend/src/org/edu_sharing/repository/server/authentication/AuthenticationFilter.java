package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LocaleValidator;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.springframework.context.ApplicationContext;

public class AuthenticationFilter implements javax.servlet.Filter {
	
	public static final String LOGIN_SUCCESS_REDIRECT_URL = "LOGIN_SUCCESS_REDIRECT_URL";
	
	public static final String PATH_LOGIN_JSP = "/components/login";
	
	public static final String PATH_INDEX_JSP = "/index.html";
	
	public static final String PATH_SHIBBOLETH = "/shibboleth";
	 
	Logger log = Logger.getLogger(AuthenticationFilter.class);
	
	TokenService tokenService;
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		ApplicationContext eduApplicationContext = 
				org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

		tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");
		
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) req;
	    HttpServletResponse httpRes = (HttpServletResponse) res;
	        
	    //alfresco WEBDAV fix (was done in index.jsp before but this is protected by this filter)
	    if (httpReq.getMethod().equalsIgnoreCase("PROPFIND") ||
	    		httpReq.getMethod().equalsIgnoreCase("OPTIONS"))
	    {
	    	log.info("webdav auth redirecting to /webdav/");
	    	httpRes.sendRedirect(httpReq.getContextPath() + "/webdav/");
	    	return;
	    } 
	    
	    //set the locale
	    String locale = httpReq.getParameter("locale");
	    if(LocaleValidator.validate(locale)){
	    	log.info("current locale:"+locale);
	    	httpReq.getSession().setAttribute(CCConstants.AUTH_LOCALE,locale);
	    }
	    
	    //find out if we have to do the guest login
	  	String user = httpReq.getParameter("user");
	  	if(user != null && user.equals("guest")){
	  		boolean guestAuthenticated = authenticateByGuest(httpReq);
	  		if(guestAuthenticated){
	  			chain.doFilter(req,res);
	  			return;
	  		}
	  	}
	    
	  	AuthenticationTool authTool = null;
	  	try{
	  		authTool = (AuthenticationTool)RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
	  	}catch(Throwable e){
	  		log.error(e.getMessage(), e);
	  		httpRes.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	  	}
	  	//find out if we have to do the ticket login
	  	try{
			String ticket =  httpReq.getParameter("ticket");
  		  		if(ticket != null && !ticket.equals("")){
  				
  				if(authTool.validateTicket(ticket)){
  					//if its APIClient user name is ignored and is figured out with authentication service
  					
  					/**
  					 * auth type ticket means no profile editing allowed, 
  					 * authenticated by an lms or other connected system that
  					 * manages the users
  					 */
  					authTool.storeAuthInfoInSession(user, ticket, CCConstants.AUTH_TYPE_TICKET, httpReq.getSession());
  					chain.doFilter(req,res);
  					return;
  				}
	  		}
		}catch(Throwable e){
				e.printStackTrace();
		}
	  	
	  	
	  	String accessToken = req.getParameter(CCConstants.REQUEST_PARAM_ACCESSTOKEN);
	  	if(accessToken != null && !accessToken.trim().equals("")){
	  		
			//oAuth
			try {
				
				Token token = tokenService.getToken(accessToken);

				if (token != null) {
					
					//validate and set current user
					authTool.storeAuthInfoInSession(
							token.getUsername(), 
							token.getTicket(),
							CCConstants.AUTH_TYPE_OAUTH,
							httpReq.getSession());						
				}										
			} catch (Exception ex) {
				ex.printStackTrace();
				httpRes.sendError(HttpServletResponse.SC_FORBIDDEN,ex.getMessage());
				return;
			}
	  	}
  		
	    
	    //find out if the user is already authenticated
	    try{
			
			String ticket = authTool.getTicketFromSession( httpReq.getSession());
			log.debug("ticket from session:"+ticket);
			
			if(authTool.validateTicket(ticket)){
						
				//default stuff
				chain.doFilter(req,res);
				return;
			}
			
		}catch(Throwable e){
			e.printStackTrace();
		}
		
		//for async GWT calls
		if(httpReq.getServletPath().contains(CCConstants.EDU_SHARING_SERVLET_PATH_GWT_RPC)){
			log.info("unauthorized call to gwt servlet");
			httpRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		
		//redirect to login page
		this.redirectToLoginpage(httpReq, httpRes);
	}
	
	private void redirectToLoginpage(HttpServletRequest req, HttpServletResponse resp) throws IOException,ServletException{
		
		//remember the URL the user wants to get
		String loginSuccessRedirectUrl = req.getRequestURI() +( req.getQueryString() != null ? "?"+req.getQueryString() : "");
		
		if(loginSuccessRedirectUrl.contains(CCConstants.REQUEST_PARAM_DISABLE_GUESTFILTER)){
			loginSuccessRedirectUrl = UrlTool.removeParam(loginSuccessRedirectUrl, CCConstants.REQUEST_PARAM_DISABLE_GUESTFILTER);
		}
				
		if(req.getParameter(CCConstants.WORKSPACE_PARAM_TRUNK) != null && req.getParameter(CCConstants.WORKSPACE_PARAM_TRUNK).equals(CCConstants.WORKSPACE_PARAM_TRUNK_VALUE_INVITED)){
			
			//remove trunk param here cause it's only needed cause of anchor is added here (server side does not get anchors)
			loginSuccessRedirectUrl = loginSuccessRedirectUrl.replace("&"+CCConstants.WORKSPACE_PARAM_TRUNK+"="+CCConstants.WORKSPACE_PARAM_TRUNK_VALUE_INVITED,"");
			loginSuccessRedirectUrl += CCConstants.WORKSPACE_INVITED_ANCHOR;
		}
		
		log.info(LOGIN_SUCCESS_REDIRECT_URL+":"+loginSuccessRedirectUrl);
		req.getSession().setAttribute(LOGIN_SUCCESS_REDIRECT_URL, loginSuccessRedirectUrl);
		
		String allowedAuthTypes = ApplicationInfoList.getHomeRepository().getAllowedAuthenticationTypes();
		
		if(allowedAuthTypes != null && !allowedAuthTypes.trim().equals("")){
			String shibbUrl = URLTool.addSSOPathWhenConfigured(URLTool.getBaseUrl()) + ( req.getQueryString() != null ? "?"+req.getQueryString() : "");
			resp.sendRedirect(shibbUrl);
		}else{
			RequestDispatcher rp = req.getRequestDispatcher(AuthenticationFilter.PATH_LOGIN_JSP);
			rp.forward(req,resp);
		}
	  
	}
	
	private boolean authenticateByGuest(HttpServletRequest req) {
		ApplicationInfo repHomeInfo = ApplicationInfoList.getRepositoryInfo(CCConstants.REPOSITORY_FILE_HOME);
		String guestUn = repHomeInfo.getGuest_username();
		String guestPw = repHomeInfo.getGuest_password();
		if(guestUn == null || guestPw == null){
			log.info("guest login not allowed");
			return false;
		}
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(null);
			HashMap<String,String> result = authTool.createNewSession(guestUn, guestPw);
			
			//save ticket in session
			HttpSession session = req.getSession();
			authTool.storeAuthInfoInSession(guestUn, result.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT,session);
			return true;
		}catch(Throwable e){
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
}
