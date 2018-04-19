package org.edu_sharing.alfresco.authentication.subsystems;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;

public class SubsystemChainingAuthenticationService extends org.alfresco.repo.security.authentication.subsystems.SubsystemChainingAuthenticationService {

	Logger logger = Logger.getLogger(SubsystemChainingAuthenticationService.class);
	
	
	static ThreadLocal<String> successFullAuthenticationMethod = new ThreadLocal<String>();
	 /**
     * {@inheritDoc}
     */
    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        preAuthenticationCheck(userName);
        List<AuthenticationService> usableAuthenticationServices = getUsableAuthenticationServices();
        int counter = usableAuthenticationServices.size();
        for (AuthenticationService authService : usableAuthenticationServices)
        {
            try
            {
            	    counter--;
                authService.authenticate(userName, password);
                if (logger.isDebugEnabled())
                {
                    logger.debug("authenticate "+userName+" with "+getId(authService)+" SUCCEEDED");
                }
                successFullAuthenticationMethod.set(getId(authService));
                return;
            }
            catch (AuthenticationException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("authenticate "+userName+" with "+getId(authService)+(counter == 0 ? " FAILED (end of chain)" : " failed (try next in chain)"));
                }
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Failed to authenticate");

    }
	
    public static void setSuccessFullAuthenticationMethod(String successFullAuthenticationMethod) {
		SubsystemChainingAuthenticationService.successFullAuthenticationMethod.set(successFullAuthenticationMethod);
	}
    
    public static String getSuccessFullAuthenticationMethod() {
		return successFullAuthenticationMethod.get();
	}
}
