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

import java.util.HashMap;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;



public class EduAuthenticationComponent{
	
	
	Logger logger = Logger.getLogger(EduAuthenticationComponent.class);
	
    private List<AuthMethodInterface> ccAuthMethod;
    
    AuthenticationComponent authenticationComponent;
    
    public void init(){
		
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		this.authenticationComponent = (AuthenticationComponent)applicationContext.getBean("authenticationComponent");
	}
    
    /**
     * @TODO dont allow shadow users to authenticate in the standard way or create an random password 
     * 
     * the authclass must be declared in the property ccAuthMethod List in the file authentication-services-context.xml 
     * 
     * This Method isnt part of the alfresco AbstractAuthenticationComponent Class
     * it will be called by special Methods of the campuscontent implementation of AuthenticationService 
     * 
     * @param authClassName
     * @param params
     */
    public void authenticate(String authClassName, HashMap<String,String> params) throws AuthenticationException{
    	
		String username = null;
		//only allow classes that are in ccAuthMethod List
		for(AuthMethodInterface authenticator:ccAuthMethod){
			if(authenticator.getClass().getName().equals(authClassName)){
		
				logger.info("authenticator:" + authClassName);
				username = authenticator.authenticate(params);
			}
		}
		if(username == null || username.trim().equals("")){
			logger.info("Auth failed for class:" + authClassName);
			throw new AuthenticationException(AuthenticationExceptionMessages.AUTHENTICATION_FAILED);
		}
		else{
			//inform Alfresco that the following user authenticated successfully
			authenticationComponent.setCurrentUser(username);
		}
		
    }
    
 
    /**
     * IOC
     * @param ccAuthMethod
     */
	public void setCcAuthMethod(List ccAuthMethod) {
		this.ccAuthMethod = ccAuthMethod;
	}	
}
