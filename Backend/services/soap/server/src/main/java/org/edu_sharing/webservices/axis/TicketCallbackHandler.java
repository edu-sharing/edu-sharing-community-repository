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
package org.edu_sharing.webservices.axis;



import java.io.IOException;
import java.rmi.RemoteException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSPasswordCallback;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;


/**
 * CallbackHandler that verifies the given ticket in the password element of the UsernameToken
 * header is still a valid ticket
 * 
 * @author gavinc
 */
public class TicketCallbackHandler implements CallbackHandler
{
   private static final Log logger = LogFactory.getLog(TicketCallbackHandler.class);
      
  
   /**
    * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
    */
   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
   {
	  
	  //logger.info("was called!!!");
	  ApplicationContext applicationContext =  AlfAppContextGate.getApplicationContext();
	  ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	  
	 
	  
      for (int i = 0; i < callbacks.length; i++) 
      {
         if (callbacks[i] instanceof WSPasswordCallback) 
         {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
            String ticket = pc.getPassword();
            
            if (logger.isDebugEnabled())
            {
               logger.debug("Ticket: " + ticket);
            }

            // ensure the ticket is valid
            try
            {
            	 serviceRegistry.getAuthenticationService().validate(ticket);
            }
            catch (AuthenticationException ae)
            {
               if (logger.isDebugEnabled())
                  logger.debug("Ticket validation failed: " + ae.getMessage());
               
               // NOTE: Throwing AuthenticationFault just gets consumed and the ws-security handler 
               //       reports a missing password; we would need to modify the WSS4J code to let
               //       the exception bubble up so for now just let the default message get thrown
               //throw new AuthenticationFault(701, "Authentication failed due to an invalid ticket");
               throw new RemoteException("Authentication failed due to an invalid ticket");
            }
            
            if (logger.isDebugEnabled())
               logger.debug("Ticket validated successfully");
            
            // if all is well set the password to return as the given ticket
            pc.setPassword(pc.getPassword());
         }
         else 
         {
            throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
         }
      }
   }
}