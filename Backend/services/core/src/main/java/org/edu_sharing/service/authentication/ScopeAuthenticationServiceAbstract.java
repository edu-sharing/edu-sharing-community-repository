package org.edu_sharing.service.authentication;

import javax.servlet.http.HttpSession;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.springframework.context.ApplicationContext;

public abstract class ScopeAuthenticationServiceAbstract implements ScopeAuthenticationService{

	Logger logger = Logger.getLogger(ScopeAuthenticationServiceAbstract.class);
	
	@Override
	public String authenticate(String username, String password, String scope) {
		String result = Login.STATUS_CODE_INVALID_CREDENTIALS;
		
		boolean scopeAllowed = checkScope(username, scope);
		
		if(scopeAllowed){
			try{
				ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
				ServiceRegistry serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
				AuthenticationService authService = serviceRegistry.getAuthenticationService();
				authService.authenticate(username, password.toCharArray());
				//invalidate guest session if login was valid
				Context.getCurrentInstance().getRequest().getSession(true).invalidate();
				AuthenticationToolAPI authToolApi = new AuthenticationToolAPI();
				HttpSession session = Context.getCurrentInstance().getRequest().getSession(true);
				authToolApi.storeAuthInfoInSession(username, authService.getCurrentTicket(), 
						CCConstants.AUTH_TYPE_DEFAULT, session);
				session.setAttribute(CCConstants.AUTH_SCOPE, scope);
				
				setScopeForCurrentThread();
				result = Login.STATUS_CODE_OK;
				
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
		}
		
		return result;
	}
	
	@Override
	public void setScopeForCurrentThread() {
		
		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		if(session != null){
			String scope = (String)session.getAttribute(CCConstants.AUTH_SCOPE);
			if(scope != null) NodeServiceInterceptor.setEduSharingScope(scope);
		}
		
	}
	
}
