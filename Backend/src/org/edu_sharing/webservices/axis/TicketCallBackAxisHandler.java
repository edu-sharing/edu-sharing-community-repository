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


import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.handler.WSHandlerConstants;

public class TicketCallBackAxisHandler extends BasicHandler
{
	   @SuppressWarnings("unused")
	   private static final Log logger = LogFactory.getLog(TicketCallBackAxisHandler.class);
	   private static final String BEAN_NAME = "ticketCallbackHandler";
	   private static final long serialVersionUID = -135125831180499667L;

	   /**
	    * @see org.apache.axis.Handler#invoke(org.apache.axis.MessageContext)
	    */
	   public void invoke(MessageContext msgContext) throws AxisFault
	   {
	      // get hold of the Spring context and retrieve the AuthenticationService
	      //HttpServletRequest req = (HttpServletRequest)msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
	      //ServletContext servletCtx = req.getSession().getServletContext();
	      //WebApplicationContext webAppCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletCtx);
	      //TicketCallbackHandler callback = (TicketCallbackHandler)webAppCtx.getBean(BEAN_NAME);
	      
		   TicketCallbackHandler callback = new TicketCallbackHandler();
	      
	      // store the callback in the context where the WS-Security handler can pick it up from
	      msgContext.setProperty(WSHandlerConstants.PW_CALLBACK_REF, callback);
	   }
	}
